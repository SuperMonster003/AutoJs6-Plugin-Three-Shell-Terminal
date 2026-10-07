package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import jackpal.androidterm.PtyBridge
import jackpal.androidterm.emulatorview.TermSession
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

/**
 * A [TermSession] backed by a pty the plugin owns: the master side is opened here, the child is
 * forked / exec'd with a fully controlled argv and environment, its exit is reaped by a watcher
 * thread, and the window size and UTF-8 mode are pushed to the pty through [PtyBridge].
 *
 * Sticky Ctrl / Alt modifiers armed by the toolbar are applied to the next chunk of keyboard input.
 *
 * zh-CN: 由插件自有 pty 支撑的 [TermSession]: 在此打开主端, 以完全受控的 argv 与环境 fork / exec 子进程,
 * 由看护线程回收其退出状态, 并通过 [PtyBridge] 向 pty 下发窗口尺寸与 UTF-8 模式.
 * 工具栏设置的粘滞 Ctrl / Alt 修饰键会应用到下一段键盘输入.
 */
class TerminalPtySession(
    private val command: List<String>,
    private val environment: Map<String, String>,
) : TermSession(true) {

    private val ptmx: ParcelFileDescriptor = ParcelFileDescriptor.open(File("/dev/ptmx"), ParcelFileDescriptor.MODE_READ_WRITE)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val processLock = Any()
    @Volatile
    private var finished = false
    @Volatile
    private var started = false
    private var emulatorInitialized = false
    private var pendingMessages = ArrayList<String>()

    @Volatile
    var pid: Int = -1
        private set

    @Volatile
    var exitCode: Int? = null
        private set

    val isProcessAlive: Boolean get() = started && !finished && exitCode == null

    var ctrlArmed: Boolean = false
        private set

    var altArmed: Boolean = false
        private set

    /** Main-thread callback with the exit code once the child has been reaped. zh-CN: 子进程被回收后在主线程回调退出码. */
    var onProcessExited: ((Int) -> Unit)? = null

    /** Called on any keyboard input after the child exited. zh-CN: 子进程退出后收到任意键盘输入时回调. */
    var onInputAfterExit: (() -> Unit)? = null

    /**
     * Registry hook invoked (main thread) after [onProcessExited] so the session can be dropped even
     * when no activity is attached.
     * zh-CN: 注册表钩子, 在 [onProcessExited] 之后于主线程调用, 使没有 Activity 依附时会话也能被移除.
     */
    internal var onProcessReaped: ((Int) -> Unit)? = null

    var onModifiersChanged: ((ctrl: Boolean, alt: Boolean) -> Unit)? = null

    /**
     * Observer of the raw pty output (plugin roadmap D21 output subscriptions). Invoked on the
     * session's handler thread (the thread that created the session, normally the main thread)
     * with the bytes exactly as the child wrote them, before they reach the screen; synthetic
     * messages appended by this class are not delivered. Must return quickly and never block.
     * zh-CN: 原始 pty 输出的观察者 (路线图 D21 输出订阅), 在会话的 handler 线程 (创建会话的线程, 通常为主线程)
     * 以子进程写出的原样字节调用, 早于上屏; 本类追加的提示文本不会送达. 须快速返回且不得阻塞.
     */
    fun interface OutputTap {
        fun onOutput(data: ByteArray, offset: Int, count: Int)
    }

    private val tapLock = Any()

    @Volatile
    private var outputTaps: List<OutputTap> = emptyList()

    @Volatile
    private var finishListeners: List<() -> Unit> = emptyList()

    fun addOutputTap(tap: OutputTap) = synchronized(tapLock) { outputTaps = outputTaps + tap }

    fun removeOutputTap(tap: OutputTap) = synchronized(tapLock) { outputTaps = outputTaps - tap }

    /**
     * Runs once after [finish] completed (explicit close, or end of output after the child
     * exited), on the finishing thread; every byte the child wrote has passed the taps by then.
     * zh-CN: 在 [finish] 完成后调用一次 (显式关闭, 或子进程退出后输出读尽), 在执行 finish 的线程上; 此时子进程写出的
     * 全部字节都已经过观察者.
     */
    fun addFinishListener(listener: () -> Unit) = synchronized(tapLock) { finishListeners = finishListeners + listener }

    fun removeFinishListener(listener: () -> Unit) = synchronized(tapLock) { finishListeners = finishListeners - listener }

    override fun processInput(data: ByteArray, offset: Int, count: Int) {
        val taps = outputTaps
        if (taps.isNotEmpty()) {
            taps.forEach { tap -> runCatching { tap.onOutput(data, offset, count) } }
        }
        super.processInput(data, offset, count)
    }

    /**
     * Writes bytes to the pty exactly as given, bypassing the sticky Ctrl / Alt modifiers of the
     * key bar (host input over Binder, plugin roadmap D20). Dropped, with `false`, after the child
     * exited or before the emulator runs.
     * zh-CN: 把字节原样写入 pty, 绕过快捷键栏的粘滞 Ctrl / Alt 修饰键 (宿主经 Binder 的输入, 路线图 D20); 子进程退出后
     * 或模拟器尚未运行时丢弃并返回 false.
     */
    fun writeRaw(data: ByteArray, offset: Int, count: Int): Boolean {
        if (finished || exitCode != null || !isRunning) return false
        super.write(data, offset, count)
        return true
    }

    init {
        val io = PtyIo(ptmx)
        setTermIn(io.input)
        setTermOut(io.output)
        setDefaultUTF8Mode(true)
    }

    /**
     * Forks the child on a worker thread and keeps that thread as the exit watcher.
     * zh-CN: 在工作线程 fork 子进程, 并让该线程继续担任退出看护.
     */
    fun start() {
        check(!started) { "Session already started" }
        started = true
        // Only the executable goes into the thread name: command lines are never logged or exposed (protocol V1, security boundary).
        thread(name = "TerminalPtySession-${command.firstOrNull()}") {
            val childPid = try {
                synchronized(processLock) {
                    if (finished) null else PtyBridge.createSubprocess(
                        ptmx,
                        command.first(),
                        command.toTypedArray(),
                        TerminalEnvironment.toEnvp(environment),
                    ).also { pid = it }
                }
            } catch (e: IOException) {
                Log.w(TAG, "Failed to start terminal child: ${e.message}")
                exitCode = START_FAILURE_CODE
                mainHandler.post {
                    appendMessage("\r\n[Failed to start shell: ${e.message}]\r\n")
                    onProcessExited?.invoke(START_FAILURE_CODE)
                    onProcessReaped?.invoke(START_FAILURE_CODE)
                    finish()
                }
                return@thread
            }
            val code = childPid?.let(PtyBridge::waitFor) ?: 0
            exitCode = code
            mainHandler.post {
                appendMessage("\r\n[Process completed (code $code)]\r\n")
                onProcessExited?.invoke(code)
                onProcessReaped?.invoke(code)
                // An unattached session has no reader to observe EOF and release the pty.
                if (!emulatorInitialized) finish()
            }
        }
    }

    override fun initializeEmulator(columns: Int, rows: Int) {
        if (finished) return
        super.initializeEmulator(columns, rows)
        emulatorInitialized = true
        runCatching { PtyBridge.setUtf8Mode(ptmx.fd, true) }
        applyWindowSize(columns, rows)
        val pending = pendingMessages
        pendingMessages = ArrayList()
        pending.forEach(::appendMessage)
    }

    override fun updateSize(columns: Int, rows: Int) {
        if (finished) return
        super.updateSize(columns, rows)
        applyWindowSize(columns, rows)
    }

    private fun applyWindowSize(columns: Int, rows: Int) {
        runCatching { PtyBridge.setWindowSize(ptmx.fd, rows, columns, 0, 0) }
            .onFailure { Log.w(TAG, "Failed to set pty window size: ${it.message}") }
    }

    override fun write(data: ByteArray, offset: Int, count: Int) {
        if (finished || exitCode != null) {
            onInputAfterExit?.invoke()
            return
        }
        if (!ctrlArmed && !altArmed) {
            super.write(data, offset, count)
            return
        }
        val ctrl = ctrlArmed
        val alt = altArmed
        clearModifiers()
        super.write(TerminalKeySequences.applyModifiers(String(data, offset, count, Charsets.UTF_8), ctrl, alt))
    }

    fun toggleCtrl() = setModifiers(!ctrlArmed, altArmed)

    fun toggleAlt() = setModifiers(ctrlArmed, !altArmed)

    fun clearModifiers() = setModifiers(ctrl = false, alt = false)

    private fun setModifiers(ctrl: Boolean, alt: Boolean) {
        if (ctrlArmed == ctrl && altArmed == alt) return
        ctrlArmed = ctrl
        altArmed = alt
        onModifiersChanged?.invoke(ctrl, alt)
    }

    /**
     * Sends a raw key sequence, bypassing the sticky modifiers (Alt is still honored for cursor keys).
     * zh-CN: 发送原始按键序列, 绕过粘滞修饰键 (光标键仍应用 Alt).
     */
    fun sendKey(key: TerminalKeySequences.Key) {
        if (finished || exitCode != null) {
            onInputAfterExit?.invoke()
            return
        }
        val alt = altArmed
        clearModifiers()
        val sequence = TerminalKeySequences.sequence(key, applicationCursorKeys = false)
        super.write(if (alt) TerminalKeySequences.ESC + sequence else sequence)
    }

    /**
     * The shell's current working directory, read from procfs; null before start or after exit.
     * zh-CN: 从 procfs 读取的 shell 当前工作目录; 启动前或退出后为 null.
     */
    fun currentDirectory(): File? {
        val childPid = pid
        if (childPid <= 0 || exitCode != null) return null
        return runCatching { File("/proc/$childPid/cwd").canonicalFile }.getOrNull()?.takeIf { it.isDirectory }
    }

    /** True while any process still runs under the shell. zh-CN: shell 下仍有进程运行时为 true. */
    fun hasLiveChildren(): Boolean {
        val parent = pid
        if (parent <= 0 || exitCode != null) return false
        val proc = File("/proc").listFiles { file -> file.isDirectory && file.name.all(Char::isDigit) } ?: return false
        return proc.any { dir ->
            runCatching {
                val stat = File(dir, "stat").readText()
                // "<pid> (<comm>) <state> <ppid> ..."; comm may contain spaces, so parse after the last ')'.
                // zh-CN: comm 可能含空格, 因此从最后一个 ')' 之后解析.
                val rest = stat.substringAfterLast(')').trim().split(' ')
                rest.getOrNull(1)?.toIntOrNull() == parent
            }.getOrDefault(false)
        }
    }

    /** Hangs up the whole process group so background jobs die with the shell. zh-CN: 挂断整个进程组, 使后台作业随 shell 一起结束. */
    fun hangup() {
        val childPid = pid
        if (childPid > 0 && exitCode == null) {
            runCatching { PtyBridge.sendSignal(-childPid, PtyBridge.SIGHUP) }
        }
    }

    fun interrupt() {
        val childPid = pid
        if (childPid > 0 && exitCode == null) {
            runCatching { PtyBridge.sendSignal(-childPid, PtyBridge.SIGINT) }
        }
    }

    override fun finish() {
        synchronized(processLock) {
            if (finished) return
            finished = true
            hangup()
            // Give SIGHUP a grace period, then also kill the child directly in case it had not
            // called setsid() yet when the group signal was sent. Never wait on the main thread.
            if (pid > 0 && exitCode == null) {
                val childPid = pid
                mainHandler.postDelayed({
                    if (exitCode == null) {
                        runCatching { PtyBridge.sendSignal(-childPid, PtyBridge.SIGKILL) }
                        runCatching { PtyBridge.sendSignal(childPid, PtyBridge.SIGKILL) }
                    }
                }, CLOSE_GRACE_MILLIS)
            }
        }
        // The P6-patched TermSession also closes safely before emulator/writer initialization.
        super.finish()
        pendingMessages.clear()
        finishListeners.forEach { listener -> runCatching(listener) }
    }

    private fun appendMessage(message: String) {
        if (finished) return
        if (!isRunning) {
            pendingMessages.add(message)
            return
        }
        val bytes = message.toByteArray(Charsets.UTF_8)
        runCatching { appendToEmulator(bytes, 0, bytes.size) }
        notifyUpdate()
    }

    companion object {
        private const val TAG = "TerminalPtySession"
        const val START_FAILURE_CODE = -1
        private const val CLOSE_GRACE_MILLIS = 500L
    }

}
