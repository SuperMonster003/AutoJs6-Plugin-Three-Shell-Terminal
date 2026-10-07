package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliTrust
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.TerminalNodeEnvironment
import java.io.IOException
import java.util.concurrent.Executors

/** Logic shared by the P3 terminal dialogs and P5 settings page; no UI or permission prompts. */
class TerminalSettingsActions internal constructor(context: Context, private val paths: TerminalPaths) {
    constructor(context: Context) : this(context, TerminalPaths.of(context))

    private val preferences = TerminalPreferences(context)
    private val main = Handler(Looper.getMainLooper())

    fun setRegistry(choice: String, customUrl: String? = null) = preferences.setRegistry(choice, customUrl)

    val registrySummary: String get() = preferences.npmRegistry ?: TerminalNodeEnvironment.DEFAULT_REGISTRY

    fun setIgnoreScripts(enabled: Boolean) {
        preferences.npmIgnoreScripts = enabled
    }

    fun setNodeIntegration(enabled: Boolean) {
        preferences.nodeIntegrationEnabled = enabled
        NodeCliLocator.invalidate()
    }

    val trustedNodeSigners: List<NodeCliTrust.UserTrust> get() = preferences.trustedNodeSigners

    /**
     * Records the user's confirmation of [current] for [packageName] (roadmap P8, D34 Q3) after the UI showed the
     * digests and the consequences; false for a malformed package name or digest list. New sessions pick it up.
     * zh-CN: 记录用户对 [packageName] 的 [current] 签名的确认 (P8, D34 Q3), 界面须先展示摘要与后果; 非法输入返回 false.
     */
    fun trustNodeSigners(packageName: String, current: List<String>): Boolean {
        val trust = NodeCliTrust.UserTrust.of(packageName, current) ?: return false
        preferences.addTrustedNodeSigners(trust)
        NodeCliLocator.invalidate()
        return true
    }

    fun revokeNodeSigners(trust: NodeCliTrust.UserTrust) {
        preferences.removeTrustedNodeSigners(trust)
        NodeCliLocator.invalidate()
    }

    /**
     * The UI asks for confirmation before calling this destructive operation. Completion runs on
     * the main thread, with the number of live and pending sessions closed, or a failure. Only home
     * and usr are cleared; preferences and projects elsewhere remain in place.
     */
    fun clearData(onComplete: (Result<Int>) -> Unit) {
        main.post {
            if (!TerminalDataLifecycle.beginClear()) {
                onComplete(Result.failure(IllegalStateException("Terminal data is already being cleared")))
                return@post
            }
            val stopped = runCatching { TerminalSessionManager.sessionsAwaitingExit to TerminalSessionManager.closeAll() }
            if (stopped.isFailure) {
                TerminalDataLifecycle.endClear()
                onComplete(Result.failure(stopped.exceptionOrNull()!!))
                return@post
            }
            val (sessions, closed) = stopped.getOrThrow()
            worker.execute {
                val result = runCatching {
                    val deadline = SystemClock.elapsedRealtime() + CLOSE_TIMEOUT_MS
                    while (sessions.any { it.pty.exitCode == null }) {
                        if (SystemClock.elapsedRealtime() >= deadline) throw IOException("Shells did not stop; terminal data was not cleared")
                        Thread.sleep(20)
                    }
                    synchronized(TerminalDataLifecycle.ioLock) {
                        TerminalDataCleaner.clearAndRebuild(paths)
                        NodeCliLocator.invalidate()
                    }
                    closed
                }
                TerminalDataLifecycle.endClear()
                main.post { onComplete(result) }
            }
        }
    }

    private companion object {
        const val CLOSE_TIMEOUT_MS = 5_000L
        val worker = Executors.newSingleThreadExecutor { Thread(it, "TerminalClearData").apply { isDaemon = true } }
    }
}
