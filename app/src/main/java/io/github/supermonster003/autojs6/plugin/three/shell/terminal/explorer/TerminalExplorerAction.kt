package io.github.supermonster003.autojs6.plugin.three.shell.terminal.explorer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalExplorerActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.threeShellTerminalPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.toPluginInfo
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.explorer.api.ExplorerActionCapabilityKeys
import org.autojs.plugin.explorer.api.ExplorerActionCatalogKeys
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionPluginIds
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionTargetKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.terminal.api.TerminalContract

/**
 * "Open in terminal" for the AutoJs6 file manager (roadmap P8.1, appendix D Q10 option B): one
 * overflow action for a single directory, declared through Explorer Action protocol v23 so that
 * the host discloses the canonical path of the directory the user chose and nothing else. The
 * path grants nothing; the shell reaches it only with this plugin's own storage access, and the
 * terminal falls back to its home directory with the storage banner when that access is missing.
 *
 * zh-CN: AutoJs6 文件管理器的 "在终端中打开" (P8.1, 附录 D Q10 方案 B): 面向单个目录的溢出菜单动作, 经
 * Explorer Action 协议 v23 声明, 宿主只披露用户所选目录的规范路径. 路径本身不授予任何权限; shell 仅凭本插件
 * 自身的存储权限访问它, 缺少权限时终端回退到主目录并显示存储横幅.
 */
internal object TerminalExplorerAction {

    /** Resource name the host resolves in this package for the menu title. zh-CN: 宿主在本包中解析的菜单标题资源名. */
    const val LABEL_RESOURCE_NAME = "terminal_explorer_open_in_terminal"

    /** Title used when the host cannot resolve [LABEL_RESOURCE_NAME]. zh-CN: 宿主无法解析资源时使用的标题. */
    const val LABEL_FALLBACK = "Open in terminal"

    /**
     * The catalog entry as plain values so the JVM tests can freeze it; [catalog] maps it onto the
     * Bundle keys of the contract.
     * zh-CN: 以纯值表示的目录项, 供 JVM 测试冻结; [catalog] 将其映射为契约的 Bundle 键.
     */
    data class Spec(
        val protocolVersion: Int,
        val id: String,
        val labelResourceName: String,
        val labelFallback: String,
        val activityClassName: String,
        val presentation: Int,
        val targetKind: Int,
        val cardinality: Int,
        val accessMode: Int,
        val placement: Int,
        val filesystemPath: Boolean,
    )

    val spec = Spec(
        protocolVersion = ThreeShellTerminalPlugin.EXPLORER_PROTOCOL_VERSION,
        id = ThreeShellTerminalPlugin.EXPLORER_DIRECTORY_ACTION_ID,
        labelResourceName = LABEL_RESOURCE_NAME,
        labelFallback = LABEL_FALLBACK,
        activityClassName = ThreeShellTerminalExplorerActivity::class.java.name,
        presentation = ExplorerActionValues.PRESENTATION_ACTIVITY,
        targetKind = ExplorerActionValues.TARGET_DIRECTORY,
        cardinality = ExplorerActionValues.CARDINALITY_SINGLE,
        accessMode = ExplorerActionValues.ACCESS_READ_ONLY,
        placement = ExplorerActionValues.PLACEMENT_OVERFLOW,
        filesystemPath = true,
    )

    /** The `getActionCatalog()` answer. zh-CN: `getActionCatalog()` 的返回值. */
    fun catalog(): Bundle = Bundle().apply {
        putInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION, spec.protocolVersion)
        putParcelableArrayList(
            ExplorerActionCatalogKeys.ACTIONS,
            arrayListOf(
                Bundle().apply {
                    putString(ExplorerActionCatalogKeys.ID, spec.id)
                    putString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME, spec.labelResourceName)
                    putString(ExplorerActionCatalogKeys.LABEL_FALLBACK, spec.labelFallback)
                    putString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME, spec.activityClassName)
                    putInt(ExplorerActionCatalogKeys.PRESENTATION, spec.presentation)
                    putInt(ExplorerActionCatalogKeys.TARGET_KIND, spec.targetKind)
                    putInt(ExplorerActionCatalogKeys.CARDINALITY, spec.cardinality)
                    putInt(ExplorerActionCatalogKeys.ACCESS_MODE, spec.accessMode)
                    putInt(ExplorerActionCatalogKeys.PLACEMENT, spec.placement)
                    putBoolean(ExplorerActionCatalogKeys.FILESYSTEM_PATH, spec.filesystemPath)
                },
            ),
        )
    }

    /**
     * The Explorer Action identity: the plugin's own metadata with engine `explorer-action` and only
     * the two capabilities the host's Explorer Action registry reads. The plugin center keeps showing
     * the terminal identity of the INFO service for this package.
     * zh-CN: Explorer Action 身份: 插件自身元数据, engine 为 `explorer-action`, 只带宿主 Explorer Action
     * 注册表读取的两项能力. 插件中心对本包仍显示 INFO 服务的终端身份.
     */
    fun pluginInfo(context: Context): PluginInfo =
        context.threeShellTerminalPluginRuntimeInfo(includeNodeCliState = false).toExplorerPluginInfo()

    internal fun ThreeShellTerminalPluginRuntimeInfo.toExplorerPluginInfo(): PluginInfo = toPluginInfo().apply {
        engine = ExplorerActionPluginIds.ENGINE
        capabilities = Bundle().apply {
            putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ThreeShellTerminalPlugin.EXPLORER_REQUIRED_HOST_VERSION)
            putInt(ExplorerActionCapabilityKeys.PROTOCOL_VERSION, ThreeShellTerminalPlugin.EXPLORER_PROTOCOL_VERSION)
        }
    }

}

/**
 * One validated `EXPLORER_ACTION_EXECUTE` request of the directory action: the directory the
 * terminal opens.
 * zh-CN: 一次通过校验的目录动作 `EXPLORER_ACTION_EXECUTE` 请求: 终端要打开的目录.
 */
internal data class ExplorerDirectoryRequest(val directory: String) {

    /** The fields of one target Bundle this action reads. zh-CN: 本动作读取的单个目标 Bundle 字段. */
    data class Target(val kind: Int?, val filesystemPath: String?)

    companion object {

        /** Reads the request extras; malformed or foreign extras yield null. zh-CN: 读取请求 extras; 格式错误或异常的 extras 返回 null. */
        fun parse(intent: Intent): ExplorerDirectoryRequest? = runCatching {
            val targets = targetBundles(intent)?.map { bundle ->
                Target(
                    kind = if (bundle.containsKey(ExplorerActionTargetKeys.KIND)) bundle.getInt(ExplorerActionTargetKeys.KIND) else null,
                    filesystemPath = bundle.getString(ExplorerActionTargetKeys.FILESYSTEM_PATH),
                )
            }
            of(
                action = intent.action,
                actionId = intent.getStringExtra(ExplorerActionIntentExtras.ACTION_ID),
                protocolVersion = intent.getIntExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, 0),
                targets = targets,
            )
        }.getOrNull()

        /**
         * Pure rule: the execute action, this catalog's action id and protocol version, exactly one
         * directory target and an absolute, normalized path within both the protocol and the
         * terminal path ceilings. Anything else is refused.
         * zh-CN: 纯规则: 执行 action, 本目录项的 id 与协议版本, 恰好一个目录目标, 以及同时满足协议与终端路径上限的
         * 已规范化绝对路径. 其余一律拒绝.
         */
        fun of(action: String?, actionId: String?, protocolVersion: Int, targets: List<Target>?): ExplorerDirectoryRequest? {
            if (action != ThreeShellTerminalPlugin.EXPLORER_EXECUTE_ACTION) return null
            if (actionId != ThreeShellTerminalPlugin.EXPLORER_DIRECTORY_ACTION_ID) return null
            if (protocolVersion != ThreeShellTerminalPlugin.EXPLORER_PROTOCOL_VERSION) return null
            val target = targets?.singleOrNull() ?: return null
            if (target.kind != ExplorerActionValues.TARGET_DIRECTORY) return null
            val path = target.filesystemPath ?: return null
            return ExplorerDirectoryRequest(path).takeIf { isAcceptablePath(path) }
        }

        internal fun isAcceptablePath(path: String): Boolean = path.startsWith('/') &&
            path.length <= ExplorerActionProtocol.MAX_FILESYSTEM_PATH_LENGTH &&
            path.toByteArray(Charsets.UTF_8).size <= TerminalContract.MAX_PATH_BYTES &&
            path.none { it == '\u0000' || it.isISOControl() } &&
            path.split('/').none { it == "." || it == ".." }

        private fun targetBundles(intent: Intent): List<Bundle>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(ExplorerActionIntentExtras.TARGETS, Bundle::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(ExplorerActionIntentExtras.TARGETS)
            }

    }

}
