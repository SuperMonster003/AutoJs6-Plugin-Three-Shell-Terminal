package io.github.supermonster003.autojs6.plugin.three.shell.terminal.node

import android.content.Context
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator.Resolution
import org.autojs.plugin.terminal.api.TerminalContract
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The Node CLI state answered to the host without running the launcher probe on the calling
 * thread (roadmap P2.4, appendix B.3: Binder methods return immediately). A cached resolution is
 * final. Otherwise the static verdict (trust, manifest contract, launcher file) is answered right
 * away and the full resolution is scheduled on a worker so the next query is served from the
 * cache; a launcher that passes every static check is reported `available` until the probe says
 * otherwise, and every session plan runs the probe anyway before using the launcher.
 * zh-CN: 不在调用线程探测启动器即可回答宿主的 Node CLI 状态: 缓存结果为最终答案; 否则先回答静态判定
 * (信任, manifest 契约, 启动器文件) 并在工作线程预热完整解析, 下次查询即命中缓存; 静态检查全部通过的
 * 启动器在探测前报告 `available`, 会话计划在使用前仍会探测.
 */
object NodeCliState {

    /** One answer for the capability table and the environment document. zh-CN: 供能力表与环境文档使用的一次回答. */
    data class Snapshot(
        val state: String,
        val version: String?,
        val packageName: String?,
        val pluginVersion: String?,
    ) {
        val available: Boolean get() = state == TerminalContract.NODE_CLI_AVAILABLE
        val reason: String? get() = state.takeUnless { available }
    }

    private const val TAG = "NodeCliState"
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "NodeCliWarmUp").apply { isDaemon = true } }
    private val warming = AtomicBoolean()

    @JvmStatic
    @JvmOverloads
    fun snapshot(context: Context, preferences: TerminalPreferences = TerminalPreferences(context)): Snapshot {
        val app = context.applicationContext
        val enabled = preferences.nodeIntegrationEnabled
        val candidate = when (val gated = NodeCliLocator.gate(enabled) { NodeCliLocator.discover(app) }) {
            is NodeCliLocator.Gate.Stop -> return describe(app, gated.resolution)
            is NodeCliLocator.Gate.Proceed -> gated.candidate
        }
        NodeCliLocator.cachedResolution(candidate)?.let { return describe(app, it) }
        warmUp(app, enabled)
        return when (val inspection = NodeCliLocator.inspect(app, candidate)) {
            is NodeCliLocator.Inspection.Unavailable -> describe(app, inspection.resolution)
            is NodeCliLocator.Inspection.Ready -> Snapshot(
                state = TerminalContract.NODE_CLI_AVAILABLE,
                version = null,
                packageName = inspection.launcher.packageName,
                pluginVersion = versionName(app, inspection.launcher.packageName),
            )
        }
    }

    @JvmStatic
    @JvmOverloads
    fun contractState(context: Context, preferences: TerminalPreferences = TerminalPreferences(context)): String = snapshot(context, preferences).state

    /**
     * Runs the full resolution (probe included) once on the worker so later queries hit the cache.
     * zh-CN: 在工作线程完整解析一次 (含探测), 之后的查询命中缓存.
     */
    @JvmStatic
    @JvmOverloads
    fun warmUp(context: Context, integrationEnabled: Boolean = TerminalPreferences(context).nodeIntegrationEnabled) {
        if (!warming.compareAndSet(false, true)) return
        val app = context.applicationContext
        executor.execute {
            try {
                NodeCliLocator.resolve(app, refresh = false, integrationEnabled = integrationEnabled)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Node CLI warm-up failed: ${e.message}")
            } finally {
                warming.set(false)
            }
        }
    }

    /** The contract view of a finished resolution. zh-CN: 已完成解析的契约视图. */
    @JvmStatic
    fun describe(context: Context, resolution: Resolution): Snapshot = when (resolution) {
        is Resolution.Available -> Snapshot(
            state = resolution.contractState,
            version = resolution.probe.versionOutput,
            packageName = resolution.launcher.packageName,
            pluginVersion = versionName(context, resolution.launcher.packageName),
        )
        is Resolution.Unavailable -> {
            val packageName = when (resolution) {
                Resolution.Unavailable.IntegrationDisabled, Resolution.Unavailable.PluginMissing -> null
                is Resolution.Unavailable.PluginNotTrusted -> resolution.packageName
                is Resolution.Unavailable.PluginTooOld -> resolution.packageName
                is Resolution.Unavailable.ExecutableMissing -> resolution.packageName
                is Resolution.Unavailable.ExecDenied -> resolution.launcher.packageName
                is Resolution.Unavailable.SetupFailed -> resolution.launcher.packageName
            }
            Snapshot(resolution.contractState, null, packageName, packageName?.let { versionName(context, it) })
        }
    }

    private fun versionName(context: Context, packageName: String): String? =
        runCatching { context.packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()

}
