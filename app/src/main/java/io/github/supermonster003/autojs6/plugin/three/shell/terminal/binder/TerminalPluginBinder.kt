package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.capabilitiesBundle
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.SessionAssembly
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalEnvironment
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPtySession
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliState
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.TerminalNodeEnvironment
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.storage.StorageAccess
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.threeShellTerminalPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.toPluginInfo
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.terminal.api.ITerminalCallback
import org.autojs.plugin.terminal.api.ITerminalPlugin
import org.autojs.plugin.terminal.api.TerminalContract
import org.autojs.plugin.terminal.api.TerminalErrorCodes
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong

/**
 * The `ITerminalPlugin` router (roadmap P2.4, appendix B). Every method first runs the
 * [CallerGuard], then answers without waiting for I/O on the Binder thread (B.3): session work
 * that needs the main thread is posted there, a session whose toolchain still has to be prepared
 * is answered as `pending` and completed on a worker, input is queued per session, and output
 * flows through [OutputSubscription] pipes. Errors follow protocol V1: `String` / `Bundle` methods
 * return error documents, void methods throw `IllegalArgumentException("CODE: detail")`, and
 * anything unexpected becomes `INTERNAL` instead of crashing the process.
 * zh-CN: `ITerminalPlugin` 路由 (P2.4, 附录 B). 每个方法先过 [CallerGuard], 再不在 Binder 线程等待 I/O 地回答 (B.3):
 * 需要主线程的会话工作投递到主线程, 工具链尚待准备的会话以 `pending` 回答并在工作线程完成, 输入按会话排队,
 * 输出经 [OutputSubscription] 管道流出. 错误遵循协议 V1: `String` / `Bundle` 方法返回错误文档, void 方法抛出
 * `IllegalArgumentException("CODE: detail")`, 意外错误一律变为 `INTERNAL` 而非崩溃.
 */
internal class TerminalPluginBinder(
    context: Context,
    private val guard: CallerGuard = HostCallerGuard(context),
    private val planSession: (Context, String?, TerminalPreferences) -> SessionAssembly.Plan = { ctx, cwd, preferences -> SessionAssembly.plan(ctx, cwd, preferences) },
) : ITerminalPlugin.Stub(), Closeable {

    private val context: Context = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "TerminalBinder") }
    private val lock = Any()
    private val pending = LinkedHashMap<String, PendingSession>()
    private val subscriptions = LinkedHashMap<String, OutputSubscription>()
    private val inputWriters = HashMap<String, ExecutorService>()
    private val queuedInputBytes = HashMap<String, Int>()
    private val subscriptionIds = AtomicLong(1)
    private val handedOut = ThreadLocal<ParcelFileDescriptor>()
    private val callbacks = CallbackRegistry { ownerPid -> closeSubscriptionsOf(ownerPid) }

    @Volatile
    private var closed = false

    private val sessionsListener: () -> Unit = { publishSessions() }
    private val exitListener = TerminalSessionManager.ExitListener { session, exitCode -> onSessionExited(session, exitCode) }

    init {
        TerminalSessionManager.addListener(sessionsListener)
        TerminalSessionManager.addExitListener(exitListener)
    }

    /** A session answered as `pending` while its plan runs on the worker. zh-CN: 计划仍在工作线程运行时以 `pending` 回答的会话. */
    private class PendingSession(val id: String, val cwd: String, val title: String) {
        val createdAt: Long = System.currentTimeMillis()

        @Volatile
        var cancelled = false
        val subscribers = ArrayList<Pair<OutputSubscription, Boolean>>()
        val input = ByteArrayOutputStream()
    }

    // ---- metadata ---------------------------------------------------------------------------

    override fun getInfo(): PluginInfo = action("getInfo") { context.threeShellTerminalPluginRuntimeInfo().toPluginInfo() }

    override fun getCapabilities(): Bundle = action("getCapabilities") { context.threeShellTerminalPluginRuntimeInfo().capabilitiesBundle() }

    override fun getEnvironment(): String = document("getEnvironment") {
        val paths = TerminalPaths.of(context)
        val preferences = TerminalPreferences(context)
        val node = NodeCliState.snapshot(context, preferences)
        TerminalDocuments.environment(
            home = paths.home.path,
            prefix = paths.prefix.path,
            shell = TerminalEnvironment.DEFAULT_SHELL,
            node = TerminalDocuments.NodeView(node.available, node.version, node.packageName, node.pluginVersion, node.reason),
            npmRegistry = preferences.npmRegistry ?: TerminalNodeEnvironment.DEFAULT_REGISTRY,
            storageAccess = StorageAccess.state(context).contractValue,
            pluginVersion = pluginVersionName(),
            contractVersion = ThreeShellTerminalPlugin.CONTRACT_VERSION,
        )
    }

    // ---- sessions ---------------------------------------------------------------------------

    override fun listSessions(): String = document("listSessions") { TerminalDocuments.sessions(sessionViews()) }

    override fun openSession(requestJson: String?): String = document("openSession") {
        val request = TerminalDocuments.parseOpenRequest(requestJson)
        val paths = TerminalPaths.of(context)
        val resolved = StorageAccess.resolveDirectory(context, request.cwd, paths.home)
        resolved.fallbackReason?.let { reason -> throw TerminalFailure(reason, directoryMessage(reason)) }
        val cwd = resolved.directory.path
        val preferences = TerminalPreferences(context)
        val title = TerminalSessionManager.defaultTitle(cwd, request.title, request.command)
        val record = synchronized(lock) {
            if (sessionCountLocked() >= TerminalContract.MAX_SESSIONS) {
                throw TerminalFailure(TerminalErrorCodes.SESSION_LIMIT, "at most ${TerminalContract.MAX_SESSIONS} sessions may be open")
            }
            PendingSession(TerminalSessionManager.reserveId(), cwd, title).also { pending[it.id] = it }
        }
        // Even a warm plan touches disk and refreshes command links. All preparation belongs on the worker.
        val answer = TerminalDocuments.session(pendingView(record)).toString()
        worker.execute { completePending(record, request, preferences) }
        publishSessions()
        answer
    }

    override fun closeSession(sessionId: String?): Boolean = action("closeSession") {
        val id = sessionId ?: return@action false
        val cancelled = synchronized(lock) { pending.remove(id)?.also { it.cancelled = true } }
        if (cancelled != null) {
            cancelled.subscribers.forEach { (subscription, _) -> subscription.abort() }
            callbacks.dispatch { it.onSessionExited(id, TerminalPtySession.START_FAILURE_CODE) }
            publishSessions()
            return@action true
        }
        if (TerminalSessionManager.get(id) == null) return@action false
        onMain { TerminalSessionManager.close(id) }
        true
    }

    override fun closeAllSessions(): Int = action("closeAllSessions") {
        val cancelled = synchronized(lock) { pending.values.toList().also { records -> records.forEach { it.cancelled = true }; pending.clear() } }
        cancelled.forEach { record ->
            record.cancelled = true
            record.subscribers.forEach { (subscription, _) -> subscription.abort() }
            callbacks.dispatch { it.onSessionExited(record.id, TerminalPtySession.START_FAILURE_CODE) }
        }
        val closedNow = onMain { TerminalSessionManager.closeAll() }
        if (cancelled.isNotEmpty()) publishSessions()
        cancelled.size + closedNow
    }

    override fun writeInput(sessionId: String?, data: ByteArray?) {
        action("writeInput") {
            val bytes = data ?: return@action
            if (bytes.size > TerminalContract.MAX_INPUT_BYTES) {
                throw IllegalArgumentException("${TerminalErrorCodes.INVALID_ARGUMENT}: input exceeds ${TerminalContract.MAX_INPUT_BYTES} bytes")
            }
            val id = sessionId ?: return@action
            if (bytes.isEmpty()) return@action
            val queued = synchronized(lock) {
                pending[id]?.let { record ->
                    if (record.input.size() + bytes.size > TerminalContract.MAX_INPUT_BYTES) {
                        throw TerminalFailure(TerminalErrorCodes.INVALID_ARGUMENT, "pending input queue is full", id)
                    }
                    record.input.write(bytes)
                    true
                } ?: false
            }
            if (queued) return@action
            val session = TerminalSessionManager.get(id) ?: return@action
            queueInput(session, bytes)
        }
    }

    override fun subscribeOutput(sessionId: String?, optionsJson: String?): Bundle = bundle("subscribeOutput") {
        val fromStart = TerminalDocuments.parseSubscribeOptions(optionsJson)
        val id = sessionId?.takeIf { it.isNotBlank() } ?: throw TerminalFailure(TerminalErrorCodes.INVALID_ARGUMENT, "sessionId is required")
        val callerPid = Binder.getCallingPid()
        val (subscription, waiting) = synchronized(lock) {
            val record = pending[id]
            val session = if (record == null) TerminalSessionManager.get(id) else null
            if (record == null && session == null) throw TerminalFailure(TerminalErrorCodes.SESSION_NOT_FOUND, "no session $id", id)
            if (session != null && !session.isAlive) throw TerminalFailure(TerminalErrorCodes.SESSION_CLOSED, "session $id has exited", id)
            if (subscriptions.values.count { it.sessionId == id && it.isActive } >= TerminalContract.MAX_SUBSCRIPTIONS_PER_SESSION) {
                throw TerminalFailure(
                    TerminalErrorCodes.SUBSCRIPTION_LIMIT,
                    "at most ${TerminalContract.MAX_SUBSCRIPTIONS_PER_SESSION} subscriptions per session",
                    id,
                )
            }
            val subscriptionId = "sub-" + subscriptionIds.getAndIncrement()
            val created = OutputSubscription(
                id = subscriptionId,
                sessionId = id,
                ownerPid = callerPid,
                onOverflow = { dropped -> callbacks.dispatch { it.onOutputOverflow(id, subscriptionId, dropped) } },
                onClosed = ::forget,
            )
            subscriptions[subscriptionId] = created
            record?.subscribers?.add(created to fromStart)
            created to (record != null)
        }
        subscription.start()
        if (!waiting) {
            onMain {
                val session = TerminalSessionManager.get(id)
                if (session == null) subscription.finish() else subscription.attach(session.pty, fromStart)
            }
        }
        if (callerPid != Process.myPid()) handedOut.set(subscription.readEnd)
        Bundle().apply {
            putString(TerminalContract.KEY_SUBSCRIPTION_ID, subscription.id)
            putParcelable(TerminalContract.KEY_FD, subscription.readEnd)
        }
    }

    override fun unsubscribeOutput(subscriptionId: String?) {
        action("unsubscribeOutput") {
            val subscription = synchronized(lock) { subscriptionId?.let(subscriptions::remove) } ?: return@action
            subscription.abort()
        }
    }

    override fun readTranscript(sessionId: String?, maxBytes: Int): Bundle = bundle("readTranscript") {
        if (maxBytes <= 0) throw TerminalFailure(TerminalErrorCodes.INVALID_ARGUMENT, "maxBytes must be positive")
        val id = sessionId?.takeIf { it.isNotBlank() } ?: throw TerminalFailure(TerminalErrorCodes.INVALID_ARGUMENT, "sessionId is required")
        val limit = minOf(maxBytes, TerminalContract.MAX_TRANSCRIPT_BYTES)
        val waiting = synchronized(lock) { id in pending }
        val text = if (waiting) {
            ""
        } else {
            val session = TerminalSessionManager.get(id) ?: throw TerminalFailure(TerminalErrorCodes.SESSION_NOT_FOUND, "no session $id", id)
            onMain { runCatching { session.pty.transcriptText }.getOrNull().orEmpty() }
        }
        val (trimmed, truncated) = TerminalDocuments.trimTranscript(text, limit)
        Bundle().apply {
            putString(TerminalContract.KEY_TEXT, trimmed)
            putBoolean(TerminalContract.KEY_TRUNCATED, truncated)
        }
    }

    // ---- callbacks --------------------------------------------------------------------------

    override fun registerCallback(callback: ITerminalCallback?) {
        action("registerCallback") {
            val target = callback ?: throw IllegalArgumentException("${TerminalErrorCodes.INVALID_ARGUMENT}: callback is required")
            callbacks.register(target, Binder.getCallingPid())
        }
    }

    override fun unregisterCallback(callback: ITerminalCallback?) {
        action("unregisterCallback") {
            callback?.let(callbacks::unregister)
        }
    }

    // ---- lifecycle --------------------------------------------------------------------------

    /**
     * Every client unbound: the host is gone, so its callbacks are dropped and its subscriptions
     * drain and close; sessions keep running for the next binding (and the UI).
     * zh-CN: 所有客户端已解绑: 宿主离开, 丢弃其回调, 其订阅刷完后关闭; 会话继续运行以待下次绑定 (与界面).
     */
    fun onClientsGone() {
        callbacks.clear()
        val open = synchronized(lock) { subscriptions.values.toList().also { subscriptions.clear() } }
        open.forEach(OutputSubscription::abort)
    }

    override fun close() {
        closed = true
        TerminalSessionManager.removeListener(sessionsListener)
        TerminalSessionManager.removeExitListener(exitListener)
        onClientsGone()
        worker.shutdown()
        synchronized(lock) {
            inputWriters.values.forEach { it.shutdown() }
            inputWriters.clear()
        }
    }

    /**
     * A read end written into a reply Parcel is duplicated by the kernel, so this process's copy
     * must be closed once the transaction is over; `Bundle` does not propagate the return-value flag
     * that would do it automatically. Local (in-process) callers own the object itself.
     * zh-CN: 写入回复 Parcel 的读端由内核复制, 事务结束后须关闭本进程的副本; `Bundle` 不会传递自动关闭的返回值标志. 进程内调用方直接持有对象.
     */
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        try {
            return super.onTransact(code, data, reply, flags)
        } finally {
            handedOut.get()?.let { descriptor ->
                handedOut.remove()
                runCatching { descriptor.close() }
            }
        }
    }

    // ---- test visibility --------------------------------------------------------------------

    internal val callbackCount: Int get() = callbacks.size

    internal fun openSubscriptionIds(sessionId: String): List<String> = synchronized(lock) {
        subscriptions.values.filter { it.sessionId == sessionId && !it.isClosed }.map { it.id }
    }

    // ---- internals --------------------------------------------------------------------------

    private fun completePending(record: PendingSession, request: TerminalDocuments.OpenRequest, preferences: TerminalPreferences) {
        try {
            if (record.cancelled) return
            val plan = planSession(context, request.cwd, preferences)
            onMain {
                synchronized(lock) {
                    if (record.cancelled) return@onMain
                    // Transfer queued input and taps atomically with removal of the pending record.
                    val session = startSession(plan, request, record.title, record.id)
                    pending.remove(record.id)
                    record.subscribers.forEach { (subscription, fromStart) -> subscription.attach(session.pty, fromStart) }
                    val input = record.input.toByteArray()
                    if (input.isNotEmpty()) queueInput(session, input)
                }
                publishSessions()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Pending session ${record.id} failed to start: ${e.javaClass.simpleName}")
            val subscribers = synchronized(lock) {
                pending.remove(record.id)
                record.subscribers.map { it.first }
            }
            subscribers.forEach(OutputSubscription::finish)
            if (!record.cancelled) {
                callbacks.dispatch { it.onSessionExited(record.id, TerminalPtySession.START_FAILURE_CODE) }
                publishSessions()
            }
        }
    }

    /**
     * Main thread: creates the session and gives it a default emulator size right away, so the
     * reader thread runs and output reaches taps and transcripts without a view attached.
     * zh-CN: 主线程: 创建会话并立即给定默认终端尺寸, 使读线程运行, 输出无需视图即可到达 tap 与 transcript.
     */
    private fun startSession(plan: SessionAssembly.Plan, request: TerminalDocuments.OpenRequest, title: String, id: String?): TerminalSessionManager.Session {
        val session = SessionAssembly.start(context, plan, title, request.command, request.env, request.keepOpen, id)
        session.pty.updateSize(DEFAULT_COLUMNS, DEFAULT_ROWS)
        return session
    }

    private fun queueInput(session: TerminalSessionManager.Session, bytes: ByteArray) {
        if (!session.isAlive) return
        val executor = synchronized(lock) {
            val queued = queuedInputBytes[session.id] ?: 0
            if (queued + bytes.size > MAX_QUEUED_INPUT_BYTES) {
                throw TerminalFailure(TerminalErrorCodes.INVALID_ARGUMENT, "input queue is full", session.id)
            }
            queuedInputBytes[session.id] = queued + bytes.size
            inputWriters.getOrPut(session.id) {
                Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "TerminalInput-${session.id}").apply { isDaemon = true } }
            }
        }
        try {
            executor.execute {
                try {
                    session.pty.writeRaw(bytes, 0, bytes.size)
                } finally {
                    releaseQueuedInput(session.id, bytes.size)
                }
            }
        } catch (_: RejectedExecutionException) {
            releaseQueuedInput(session.id, bytes.size)
        }
    }

    private fun releaseQueuedInput(id: String, size: Int) = synchronized(lock) {
        val remaining = (queuedInputBytes[id] ?: 0) - size
        if (remaining > 0) queuedInputBytes[id] = remaining else queuedInputBytes.remove(id)
    }

    private fun onSessionExited(session: TerminalSessionManager.Session, exitCode: Int) {
        if (closed) return
        synchronized(lock) { inputWriters.remove(session.id) }?.shutdown()
        callbacks.dispatch { it.onSessionExited(session.id, exitCode) }
        // Subscriptions normally finish from the pty finish listener once the reader thread hits EOF; a session
        // whose emulator never ran, or whose pty is held open by a grandchild, gets the same closure after a grace period.
        // zh-CN: 订阅通常在读线程读到 EOF 后由 pty 的结束监听器结束; 模拟器从未运行或 pty 被孙进程占住的会话在宽限期后同样关闭.
        mainHandler.postDelayed({ subscriptionsOf(session.id).forEach(OutputSubscription::finish) }, EXIT_FLUSH_GRACE_MS)
    }

    private fun publishSessions() {
        if (closed || callbacks.size == 0) return
        val views = sessionViews()
        val json = TerminalDocuments.sessions(views)
        callbacks.dispatch { it.onSessionsChanged(views.size, json) }
    }

    private fun closeSubscriptionsOf(ownerPid: Int) {
        if (ownerPid == Process.myPid()) return
        val dead = synchronized(lock) {
            subscriptions.values.filter { it.ownerPid == ownerPid }.also { gone -> gone.forEach { subscriptions.remove(it.id) } }
        }
        dead.forEach(OutputSubscription::abort)
    }

    private fun subscriptionsOf(sessionId: String): List<OutputSubscription> = synchronized(lock) {
        subscriptions.values.filter { it.sessionId == sessionId }
    }

    private fun forget(subscription: OutputSubscription) {
        synchronized(lock) { subscriptions.remove(subscription.id) }
    }

    private fun sessionCountLocked(): Int {
        val ids = TerminalSessionManager.allSessions.mapTo(HashSet()) { it.id }
        ids.addAll(pending.keys)
        return ids.size
    }

    private fun sessionViews(): List<TerminalDocuments.SessionView> {
        val live = TerminalSessionManager.allSessions
        val liveIds = live.mapTo(HashSet()) { it.id }
        val waiting = synchronized(lock) { pending.values.filter { it.id !in liveIds }.map(::pendingView) }
        return (live.map(::sessionView) + waiting).sortedWith(compareBy({ it.createdAt }, { it.id.toLongOrNull() ?: Long.MAX_VALUE }))
    }

    private fun sessionView(session: TerminalSessionManager.Session): TerminalDocuments.SessionView {
        val exitCode = session.exitCode
        val exited = exitCode != null || !session.isAlive
        return TerminalDocuments.SessionView(
            id = session.id,
            cwd = session.pty.currentDirectory()?.path ?: session.initialDirectory,
            title = session.title,
            createdAt = session.createdAt,
            alive = !exited,
            exitCode = exitCode,
            pid = session.pty.pid,
            state = if (exited) TerminalContract.STATE_EXITED else TerminalContract.STATE_RUNNING,
        )
    }

    private fun pendingView(record: PendingSession) = TerminalDocuments.SessionView(
        id = record.id,
        cwd = record.cwd,
        title = record.title,
        createdAt = record.createdAt,
        alive = true,
        exitCode = null,
        pid = -1,
        state = TerminalContract.STATE_PENDING,
    )

    private fun directoryMessage(reason: String): String = when (reason) {
        TerminalErrorCodes.STORAGE_PERMISSION_REQUIRED -> "shared storage access has not been granted to the plugin"
        else -> "the requested directory does not exist or is not readable"
    }

    private fun pluginVersionName(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val task = FutureTask<T>(Callable { block() })
        if (!mainHandler.post(task)) throw IllegalStateException("${TerminalErrorCodes.INTERNAL}: main thread is not available")
        return try {
            task.get(MAIN_THREAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        } catch (_: TimeoutException) {
            task.cancel(false)
            throw IllegalStateException("${TerminalErrorCodes.INTERNAL}: main thread did not respond")
        }
    }

    /** Void / scalar methods: contract failures become encoded `IllegalArgumentException`s, the unexpected `INTERNAL`. */
    private inline fun <T> action(name: String, block: () -> T): T {
        guard.enforceHost()
        return try {
            block()
        } catch (e: SecurityException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: TerminalFailure) {
            throw e.toIllegalArgument()
        } catch (e: Throwable) {
            Log.e(TAG, "$name failed", e)
            throw IllegalStateException("${TerminalErrorCodes.INTERNAL}: ${e.javaClass.simpleName}")
        }
    }

    /** `String` methods: failures are error documents. */
    private inline fun document(name: String, block: () -> String): String {
        guard.enforceHost()
        return try {
            block()
        } catch (e: SecurityException) {
            throw e
        } catch (e: TerminalFailure) {
            e.toJson()
        } catch (e: IOException) {
            Log.w(TAG, "$name failed: ${e.message}")
            TerminalDocuments.error(TerminalErrorCodes.PTY_FAILED, "the pty could not be opened")
        } catch (e: Throwable) {
            Log.e(TAG, "$name failed", e)
            TerminalDocuments.error(TerminalErrorCodes.INTERNAL, e.javaClass.simpleName)
        }
    }

    /** `Bundle` methods: failures are error documents under `KEY_ERROR_JSON`. */
    private inline fun bundle(name: String, block: () -> Bundle): Bundle {
        guard.enforceHost()
        return try {
            block()
        } catch (e: SecurityException) {
            throw e
        } catch (e: TerminalFailure) {
            errorBundle(e.toJson())
        } catch (e: Throwable) {
            Log.e(TAG, "$name failed", e)
            errorBundle(TerminalDocuments.error(TerminalErrorCodes.INTERNAL, e.javaClass.simpleName))
        }
    }

    private fun errorBundle(json: String): Bundle = Bundle().apply { putString(TerminalContract.KEY_ERROR_JSON, json) }

    companion object {
        private const val TAG = "TerminalPluginBinder"

        /** Emulator size of a session created without a view (a view resizes it on attach). zh-CN: 无视图会话的终端尺寸. */
        const val DEFAULT_COLUMNS = 80
        const val DEFAULT_ROWS = 24

        private const val MAX_QUEUED_INPUT_BYTES = 4 * TerminalContract.MAX_INPUT_BYTES
        private const val MAIN_THREAD_TIMEOUT_MS = 10_000L
        private const val EXIT_FLUSH_GRACE_MS = 2_000L
    }

}
