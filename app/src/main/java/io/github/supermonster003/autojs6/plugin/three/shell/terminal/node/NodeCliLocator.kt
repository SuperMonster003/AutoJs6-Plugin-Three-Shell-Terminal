package io.github.supermonster003.autojs6.plugin.three.shell.terminal.node

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import androidx.core.content.pm.PackageInfoCompat
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import org.autojs.plugin.nodejs.api.NodeJsPluginActions
import org.autojs.plugin.nodejs.api.NodeJsPluginIds
import java.io.File

/**
 * Decides whether the terminal can offer `node` / `npm`, and why not otherwise.
 *
 * Resolution order: a Node.js Runtime plugin must be installed and enabled, it must declare the
 * launcher contract in its manifest, the launcher must exist as a real ELF file in the plugin's
 * native library directory, and a one-shot `--version` run must succeed on this device. The result
 * is cached per plugin build (package, version code, last update time), so an update or a
 * reinstall of the Node.js plugin misses the cache by construction. The signer trust rule and the
 * settings switch of plugin roadmap D17 / P2.3 are layered on top of [discover].
 *
 * zh-CN: 判定终端能否提供 `node` / `npm`, 不能时给出原因. 结果按插件构建 (包名, 版本号, 更新时间) 缓存,
 * 因此 Node.js 插件更新或重装天然不命中缓存. 路线图 D17 / P2.3 的签名信任规则与设置开关叠加在 [discover] 之上.
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

            /** No enabled runtime plugin service is installed at all. zh-CN: 完全没有已启用的运行时插件. */
            object PluginMissing : Unavailable()

            /** The plugin predates the launcher contract. zh-CN: 插件早于启动器契约. */
            data class PluginTooOld(val packageName: String, val reason: String) : Unavailable()

            /** The contract is declared but the launcher file is absent or not an ELF. zh-CN: 已声明契约但启动器文件缺失或不是 ELF. */
            data class ExecutableMissing(val packageName: String, val abi: String, val executableName: String) : Unavailable()

            /** The device refused to run the launcher, or it failed to report a version. zh-CN: 设备拒绝运行启动器, 或启动器未能报告版本. */
            data class ExecDenied(val launcher: Launcher, val probe: NodeCliProbe.Result) : Unavailable()

            /** The launcher runs, but installing the npm / corepack archive or linking commands failed. zh-CN: 启动器可运行, 但安装 npm / corepack 资产或创建命令链接失败. */
            data class SetupFailed(val launcher: Launcher, val message: String) : Unavailable()

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
     * @param refresh re-run the launcher check even when a cached result exists
     * zh-CN: 阻塞调用, 每个插件构建首次使用时运行启动器探测; 勿在主线程使用.
     */
    @JvmStatic
    @JvmOverloads
    fun resolve(context: Context, refresh: Boolean = false): Resolution {
        val candidate = discover(context) ?: return Resolution.Unavailable.PluginMissing
        val cacheKey = candidate.cacheKey
        if (!refresh) {
            synchronized(lock) { cache?.takeIf { it.key == cacheKey }?.let { return it.resolution } }
        }
        val resolution = resolveUncached(candidate)
        synchronized(lock) { cache = CacheEntry(cacheKey, resolution) }
        return resolution
    }

    fun cachedOrNull(): Resolution? = synchronized(lock) { cache?.resolution }

    fun invalidate() = synchronized(lock) { cache = null }

    /** Pure resolution of one candidate's manifest and native library directory, probe included. zh-CN: 对单个候选的 manifest 与 native 库目录的解析, 含探测. */
    @JvmStatic
    fun resolveUncached(candidate: Candidate): Resolution {
        val packageName = candidate.packageName
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
