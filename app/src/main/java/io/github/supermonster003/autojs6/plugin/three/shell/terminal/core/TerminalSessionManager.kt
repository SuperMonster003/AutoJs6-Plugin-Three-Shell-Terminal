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

    private val nextId = AtomicLong(1)
    private val sessions = LinkedHashMap<String, Session>()
    private val listeners = ArrayList<() -> Unit>()

    val activeSessions: List<Session>
        @Synchronized get() = sessions.values.filter { it.pty.isProcessAlive }

    val hasSessions: Boolean
        @Synchronized get() = sessions.values.any { it.pty.isProcessAlive }

    /** Every registered session including ones whose shell exited but was not closed yet. zh-CN: 全部已注册会话, 含已退出但尚未关闭者. */
    val allSessions: List<Session>
        @Synchronized get() = sessions.values.toList()

    @Synchronized
    fun get(id: String?): Session? = id?.let(sessions::get)

    /**
     * Prepares the on-disk layout, builds the environment and starts an interactive shell in [directory].
     *
     * @param extraEnvironment variables laid over the session environment (Node.js integration, request overrides)
     * @param title            label of the session; defaults to [command], then the directory name
     * @param command          command line the caller is about to type into the new shell, used for the default title
     * zh-CN: 准备磁盘目录布局, 构建环境变量, 并在 [directory] 启动交互式 shell.
     */
    @JvmOverloads
    fun create(
        context: Context,
        directory: String,
        extraEnvironment: Map<String, String?> = emptyMap(),
        title: String? = null,
        command: String? = null,
    ): Session {
        val paths = TerminalPaths.of(context).ensureLayout()
        val environment = TerminalEnvironment.build(buildSpec(paths, extraEnvironment))
        val argv = TerminalSessionLauncher.buildCommand(directory)
        val pty = TerminalPtySession(argv, environment)
        val session = Session(nextId.getAndIncrement().toString(), pty, directory, paths, defaultTitle(directory, title, command))
        synchronized(this) { sessions[session.id] = session }
        pty.setFinishCallback { remove(session.id) }
        pty.onProcessReaped = { remove(session.id) }
        pty.start()
        ThreeShellTerminalSessionService.ensureStarted(context)
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
        val live = activeSessions
        live.forEach { close(it.id) }
        return live.size
    }

    @Synchronized
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    @Synchronized
    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun remove(id: String) {
        val removed = synchronized(this) { sessions.remove(id) } ?: return
        removed.pty.onProcessExited = null
        notifyChanged()
    }

    private fun notifyChanged() {
        val snapshot = synchronized(this) { listeners.toList() }
        snapshot.forEach { runCatching { it() } }
    }

}
