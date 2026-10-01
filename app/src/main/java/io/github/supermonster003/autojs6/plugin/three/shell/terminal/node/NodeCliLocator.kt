package io.github.supermonster003.autojs6.plugin.three.shell.terminal.node

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import androidx.core.content.pm.PackageInfoCompat
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import org.autojs.plugin.nodejs.api.NodeJsPluginActions
import org.autojs.plugin.nodejs.api.NodeJsPluginIds
import org.autojs.plugin.terminal.api.TerminalContract
import java.io.File

/**
 * Decides whether the terminal can offer `node` / `npm`, and why not otherwise.
 *
 * Resolution order (roadmap D17): the integration switch must be on, a Node.js Runtime plugin must
 * be installed and enabled, its signer must pass [NodeCliTrust], it must declare the launcher
 * contract in its manifest, the launcher must exist as a real ELF file in the plugin's native
 * library directory, and a one-shot `--version` run must succeed on this device. The result is
 * cached per plugin build (package, version code, last update time), so an update or a reinstall
 * of the Node.js plugin misses the cache by construction; the switch is never cached.
 *
 * zh-CN: 判定终端能否提供 `node` / `npm`, 不能时给出原因. 顺序 (路线图 D17): 集成开关打开, 已安装且启用的 Node.js
 * 运行时插件, 签名通过 [NodeCliTrust], manifest 声明启动器契约, native 库目录中存在 ELF 启动器, 一次性 `--version`
 * 探测成功. 结果按插件构建 (包名, 版本号, 更新时间) 缓存, 因此更新或重装天然不命中缓存; 开关状态不缓存.
 */
object NodeCliLocator {

    /** Package of the official Node.js Runtime plugin. zh-CN: 官方 Node.js 运行时插件包名. */
    const val OFFICIAL_PLUGIN_PACKAGE = ThreeShellTerminalPlugin.NODEJS_PACKAGE_NAME

    data class Launcher(
        val packageName: String,
        val versionCode: Long,
        val nativeLibraryDir: String,
        val executable: File,
        val descriptor: NodeCliMetadata.Descriptor,
    )

    /** An installed runtime service with the package facts the cache key is built from. zh-CN: 已安装的运行时服务及缓存键所需的包信息. */
    data class Candidate(
        val serviceInfo: ServiceInfo,
        val versionCode: Long,
        val lastUpdateTime: Long,
    ) {
        val packageName: String get() = serviceInfo.packageName
        val cacheKey: String get() = "$packageName@$versionCode:$lastUpdateTime"
    }

    sealed class Resolution {

        data class Available(val launcher: Launcher, val probe: NodeCliProbe.Result) : Resolution()

        sealed class Unavailable : Resolution() {

            /** The Node.js integration is switched off in the plugin settings. zh-CN: 插件设置中关闭了 Node.js 集成. */
            object IntegrationDisabled : Unavailable()

            /** No enabled runtime plugin service is installed at all. zh-CN: 完全没有已启用的运行时插件. */
            object PluginMissing : Unavailable()

            /** Installed, but signed by neither the official key nor this plugin's key (D17). zh-CN: 已安装但签名既非官方也非本插件 (D17). */
            data class PluginNotTrusted(val packageName: String, val signers: List<String>) : Unavailable()

            /** The plugin predates the launcher contract. zh-CN: 插件早于启动器契约. */
            data class PluginTooOld(val packageName: String, val reason: String) : Unavailable()

            /** The contract is declared but the launcher file is absent or not an ELF. zh-CN: 已声明契约但启动器文件缺失或不是 ELF. */
            data class ExecutableMissing(val packageName: String, val abi: String, val executableName: String) : Unavailable()

            /** The device refused to run the launcher, or it failed to report a version. zh-CN: 设备拒绝运行启动器, 或启动器未能报告版本. */
            data class ExecDenied(val launcher: Launcher, val probe: NodeCliProbe.Result) : Unavailable()

            /** The launcher runs, but installing the npm / corepack archive or linking commands failed. zh-CN: 启动器可运行, 但安装 npm / corepack 资产或创建命令链接失败. */
            data class SetupFailed(val launcher: Launcher, val message: String) : Unavailable()

        }

        /** The contract vocabulary (`TerminalContract.NODE_CLI_*`) for capabilities and the environment document. zh-CN: 能力表与环境文档使用的契约词汇. */
        val contractState: String
            get() = when (this) {
                is Available -> TerminalContract.NODE_CLI_AVAILABLE
                is Unavailable.IntegrationDisabled -> TerminalContract.NODE_CLI_DISABLED
                is Unavailable.PluginMissing -> TerminalContract.NODE_CLI_PLUGIN_MISSING
                is Unavailable.PluginNotTrusted -> TerminalContract.NODE_CLI_PLUGIN_UNTRUSTED
                is Unavailable.PluginTooOld -> TerminalContract.NODE_CLI_PLUGIN_TOO_OLD
                is Unavailable.ExecutableMissing -> TerminalContract.NODE_CLI_EXECUTABLE_MISSING
                is Unavailable.ExecDenied -> TerminalContract.NODE_CLI_EXEC_DENIED
                is Unavailable.SetupFailed -> TerminalContract.NODE_CLI_SETUP_FAILED
            }

    }

    private data class CacheEntry(val key: String, val resolution: Resolution)

    private val lock = Any()
    private var cache: CacheEntry? = null

    /**
     * The runtime plugin the terminal would use: the official package when installed, otherwise the
     * newest other implementation of the `org.autojs.plugin.nodejs.RUNTIME` service. Blocking
     * (package manager queries); call it off the main thread.
     * zh-CN: 终端将使用的运行时插件: 已安装官方包时取官方包, 否则取最新的其它实现. 阻塞调用, 勿在主线程使用.
     */
    @JvmStatic
    fun discover(context: Context): Candidate? {
        val pm = context.applicationContext.packageManager
        return queryRuntimeServices(context.applicationContext)
            .mapNotNull { serviceInfo ->
                val packageInfo = packageInfo(pm, serviceInfo.packageName) ?: return@mapNotNull null
                Candidate(serviceInfo, PackageInfoCompat.getLongVersionCode(packageInfo), packageInfo.lastUpdateTime)
            }
            .sortedWith(
                compareByDescending<Candidate> { it.packageName == OFFICIAL_PLUGIN_PACKAGE }
                    .thenByDescending { it.versionCode }
                    .thenByDescending { it.lastUpdateTime },
            )
            .firstOrNull()
    }

    /**
     * Blocking; runs the launcher probe on first use per plugin build. Call it off the main thread.
     *
     * @param refresh            re-run the launcher check even when a cached result exists
     * @param integrationEnabled the settings switch; false short-circuits before any discovery
     * zh-CN: 阻塞调用, 每个插件构建首次使用时运行启动器探测; 勿在主线程使用. 开关关闭时在任何发现之前短路.
     */
    @JvmStatic
    @JvmOverloads
    fun resolve(
        context: Context,
        refresh: Boolean = false,
        integrationEnabled: Boolean = TerminalPreferences(context).nodeIntegrationEnabled,
    ): Resolution {
        val candidate = when (val gated = gate(integrationEnabled) { discover(context) }) {
            is Gate.Stop -> return gated.resolution
            is Gate.Proceed -> gated.candidate
        }
        val cacheKey = candidate.cacheKey
        if (!refresh) {
            synchronized(lock) { cache?.takeIf { it.key == cacheKey }?.let { return it.resolution } }
        }
        val resolution = resolveUncached(context, candidate)
        synchronized(lock) { cache = CacheEntry(cacheKey, resolution) }
        return resolution
    }

    /** Outcome of [gate]: the candidate to resolve, or a final unavailable state. zh-CN: [gate] 的结果: 待解析的候选, 或最终的不可用态. */
    sealed class Gate {
        data class Proceed(val candidate: Candidate) : Gate()
        data class Stop(val resolution: Resolution.Unavailable) : Gate()
    }

    /**
     * The decisions taken before any cache lookup: [Resolution.Unavailable.IntegrationDisabled]
     * without calling [discover], [Resolution.Unavailable.PluginMissing] when it yields nothing,
     * otherwise the [Candidate] to resolve.
     * zh-CN: 缓存查询之前的判定: 开关关闭则不调用 [discover] 直接返回禁用态; 无候选则返回缺失态; 否则返回待解析的候选.
     */
    @JvmStatic
    fun gate(integrationEnabled: Boolean, discover: () -> Candidate?): Gate {
        if (!integrationEnabled) return Gate.Stop(Resolution.Unavailable.IntegrationDisabled)
        return discover()?.let { Gate.Proceed(it) } ?: Gate.Stop(Resolution.Unavailable.PluginMissing)
    }

    fun cachedOrNull(): Resolution? = synchronized(lock) { cache?.resolution }

    fun invalidate() = synchronized(lock) { cache = null }

    /**
     * Resolution of one candidate without the cache: trust (D17), manifest contract, launcher file, probe.
     * zh-CN: 不经缓存解析单个候选: 信任 (D17), manifest 契约, 启动器文件, 探测.
     */
    @JvmStatic
    fun resolveUncached(context: Context, candidate: Candidate): Resolution {
        val packageName = candidate.packageName
        val verdict = NodeCliTrust.verdict(context, packageName)
        if (verdict is NodeCliTrust.Verdict.Untrusted) {
            return Resolution.Unavailable.PluginNotTrusted(packageName, verdict.signers)
        }
        val descriptor = when (val parsed = NodeCliMetadata.parse(candidate.serviceInfo.metaData?.toMap())) {
            is NodeCliMetadata.ParseResult.Missing -> return Resolution.Unavailable.PluginTooOld(packageName, parsed.reason)
            is NodeCliMetadata.ParseResult.Valid -> parsed.descriptor
        }
        val nativeLibraryDir = candidate.serviceInfo.applicationInfo?.nativeLibraryDir
        val executable = nativeLibraryDir?.let { File(it, descriptor.executableName) }
        if (executable == null || !NodeCliMetadata.isElf(executable)) {
            return Resolution.Unavailable.ExecutableMissing(
                packageName = packageName,
                abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                executableName = descriptor.executableName,
            )
        }
        val launcher = Launcher(packageName, candidate.versionCode, nativeLibraryDir, executable, descriptor)
        val probe = NodeCliProbe.run(executable)
        return if (probe.succeeded) {
            Resolution.Available(launcher, probe)
        } else {
            Resolution.Unavailable.ExecDenied(launcher, probe)
        }
    }

    private fun queryRuntimeServices(context: Context): List<ServiceInfo> {
        val intent = Intent(NodeJsPluginActions.RUNTIME).apply {
            addCategory(NodeJsPluginIds.ENGINE)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        val flags = PackageManager.GET_META_DATA
        val resolved = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentServices(intent, flags)
            }
        }.getOrDefault(emptyList())
        return resolved.mapNotNull { it.serviceInfo }.filter { it.exported && it.enabled && it.applicationInfo?.enabled != false }
    }

    private fun packageInfo(pm: PackageManager, packageName: String) = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun Bundle.toMap(): Map<String, Any?> = keySet().associateWith { get(it) }

}
