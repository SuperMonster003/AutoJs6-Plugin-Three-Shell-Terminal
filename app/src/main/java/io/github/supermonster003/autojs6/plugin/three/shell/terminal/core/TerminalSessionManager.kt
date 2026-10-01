package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.content.Context
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.service.ThreeShellTerminalSessionService
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide registry of terminal sessions so an activity can be recreated (rotation, theme
 * change) or left and re-entered without losing the running shell, and so the host-facing Binder
 * service answers from the same list (plugin roadmap D15). While any session exists,
 * [ThreeShellTerminalSessionService] keeps the process in the foreground.
 *
 * zh-CN: 进程级终端会话注册表, 使 Activity 重建 (旋转, 主题切换) 或离开再进入时不丢失正在运行的 shell,
 * 并让面向宿主的 Binder 服务从同一列表作答 (路线图 D15). 只要存在会话, [ThreeShellTerminalSessionService]
 * 就让进程保持前台.
 */
object TerminalSessionManager {

    class Session internal constructor(
        val id: String,
        val pty: TerminalPtySession,
        val initialDirectory: String,
        val paths: TerminalPaths,
        title: String,
        val createdAt: Long = System.currentTimeMillis(),
    ) {
        /**
         * Short label for lists and notifications: the explicit title of the request, otherwise the
         * command that started the session, otherwise the directory name. Updated when a command is
         * typed on behalf of the user.
         * zh-CN: 列表与通知使用的短标签: 请求给出的标题, 否则启动命令, 否则目录名; 代用户键入命令时更新.
         */
        @Volatile
        var title: String = title

        /** Exit code of the shell once it has been reaped, null while it runs. zh-CN: shell 被回收后的退出码, 运行中为 null. */
        val exitCode: Int? get() = pty.exitCode

        val isAlive: Boolean get() = pty.isProcessAlive
    }

    /** Receives the exit code of a session's shell (main thread) right before the session leaves the registry. zh-CN: 会话 shell 的退出码 (主线程), 在会话移出注册表之前收到. */
    fun interface ExitListener {
        fun onExited(session: Session, exitCode: Int)
    }

    /**
     * Whether [create] starts the foreground session service. The service component always runs in
     * the main process, so a debug endpoint hosting sessions in another process turns this off; a
     * service started for sessions it cannot see would stop at once, which some OEM builds punish
     * with `ForegroundServiceDidNotStartInTimeException`.
     * zh-CN: [create] 是否启动前台会话服务. 服务组件始终运行于主进程, 在其他进程托管会话的调试端点须关闭此开关.
     */
    @Volatile
    var foregroundServiceEnabled: Boolean = true

    private val nextId = AtomicLong(1)
    private val sessions = LinkedHashMap<String, Session>()
    private val closingSessions = LinkedHashMap<String, Session>()
    private val listeners = ArrayList<() -> Unit>()
    private val exitListeners = ArrayList<ExitListener>()
    private val closeAllListeners = ArrayList<() -> Int>()

    val activeSessions: List<Session>
        @Synchronized get() = sessions.values.filter { it.pty.isProcessAlive }

    val hasSessions: Boolean
        @Synchronized get() = sessions.values.any { it.pty.isProcessAlive }

    /** Every registered session including ones whose shell exited but was not closed yet. zh-CN: 全部已注册会话, 含已退出但尚未关闭者. */
    val allSessions: List<Session>
        @Synchronized get() = sessions.values.toList()

    /** Includes shells removed from the visible list but still inside the SIGHUP grace period. */
    internal val sessionsAwaitingExit: List<Session>
        @Synchronized get() = sessions.values.toList() + closingSessions.values

    @Synchronized
    fun get(id: String?): Session? = id?.let(sessions::get)

    /**
     * Allocates the id of a session that will be created later, so a caller can announce a pending
     * session (Binder `openSession`) under the id the registry will use.
     * zh-CN: 预先分配稍后创建的会话 id, 使调用方 (Binder `openSession`) 能以注册表将使用的 id 公布待命会话.
     */
    fun reserveId(): String = nextId.getAndIncrement().toString()

    /**
     * Prepares the on-disk layout, builds the environment and starts a shell in [directory].
     *
     * @param extraEnvironment variables laid over the session environment (Node.js integration, request overrides)
     * @param title            label of the session; defaults to [command], then the directory name
     * @param command          command line the session runs or the caller is about to type, used for the default title
     * @param id               an id from [reserveId], otherwise a fresh one
     * @param argv             the child's argv; defaults to the interactive wrapper of [TerminalSessionLauncher]
     * zh-CN: 准备磁盘目录布局, 构建环境变量, 并在 [directory] 启动 shell.
     */
    @JvmOverloads
    fun create(
        context: Context,
        directory: String,
        extraEnvironment: Map<String, String?> = emptyMap(),
        title: String? = null,
        command: String? = null,
        id: String? = null,
        argv: List<String>? = null,
    ): Session {
        TerminalDataLifecycle.ticket()
        val paths = TerminalPaths.of(context).ensureLayout()
        val environment = TerminalEnvironment.build(buildSpec(paths, extraEnvironment))
        val pty = TerminalPtySession(argv ?: TerminalSessionLauncher.buildCommand(directory), environment)
        val session = Session(id ?: reserveId(), pty, directory, paths, defaultTitle(directory, title, command))
        synchronized(this) {
            check(session.id !in sessions) { "Session ${session.id} already exists" }
            sessions[session.id] = session
        }
        pty.setFinishCallback { remove(session.id) }
        pty.onProcessReaped = { code ->
            synchronized(this) { closingSessions.remove(session.id) }
            notifyExited(session, code)
            remove(session.id)
        }
        pty.start()
        if (foregroundServiceEnabled) ThreeShellTerminalSessionService.ensureStarted(context)
        notifyChanged()
        return session
    }

    fun buildSpec(paths: TerminalPaths, extraEnvironment: Map<String, String?> = emptyMap()) = TerminalEnvironment.Spec(
        home = paths.home.path,
        prefix = paths.prefix.path,
        tmpDir = paths.tmp.path,
        profile = paths.profile.path,
        pathPrepend = listOf(paths.bin.path),
        extras = extraEnvironment,
    )

    /**
     * The label a new session gets: the explicit [title], else the first line of [command], else the
     * last path segment of [directory] (the directory itself for `/`).
     * zh-CN: 新会话的标签: 显式 [title], 否则 [command] 的首行, 否则 [directory] 的最后一段.
     */
    @JvmStatic
    fun defaultTitle(directory: String, title: String? = null, command: String? = null): String {
        title?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        command?.lineSequence()?.map(String::trim)?.firstOrNull { it.isNotEmpty() }?.let { return it }
        val name = File(directory).name
        return if (name.isEmpty()) directory else name
    }

    fun close(id: String) {
        val session = synchronized(this) { sessions[id] } ?: return
        session.pty.finish()
        remove(id)
    }

    /** Ends every live session; returns how many were closed. zh-CN: 结束全部存活会话, 返回关闭数. */
    fun closeAll(): Int {
        val pendingClosed = synchronized(this) { closeAllListeners.toList() }.sumOf { it() }
        val live = activeSessions
        live.forEach { close(it.id) }
        return pendingClosed + live.size
    }

    @Synchronized
    internal fun addCloseAllListener(listener: () -> Int) { closeAllListeners.add(listener) }

    @Synchronized
    internal fun removeCloseAllListener(listener: () -> Int) { closeAllListeners.remove(listener) }

    @Synchronized
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    @Synchronized
    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    @Synchronized
    fun addExitListener(listener: ExitListener) {
        exitListeners.add(listener)
    }

    @Synchronized
    fun removeExitListener(listener: ExitListener) {
        exitListeners.remove(listener)
    }

    private fun remove(id: String) {
        val removed = synchronized(this) {
            sessions.remove(id)?.also { if (it.exitCode == null) closingSessions[id] = it }
        } ?: return
        removed.pty.onProcessExited = null
        notifyChanged()
    }

    private fun notifyChanged() {
        val snapshot = synchronized(this) { listeners.toList() }
        snapshot.forEach { runCatching { it() } }
    }

    private fun notifyExited(session: Session, exitCode: Int) {
        val snapshot = synchronized(this) { exitListeners.toList() }
        snapshot.forEach { runCatching { it.onExited(session, exitCode) } }
    }

}
