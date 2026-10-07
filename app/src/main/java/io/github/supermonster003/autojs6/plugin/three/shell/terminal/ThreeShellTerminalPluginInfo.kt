package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.content.Context
import android.os.Build
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliState
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.terminal.api.TerminalCapabilityKeys
import org.autojs.plugin.terminal.api.TerminalContract

/**
 * Collects the installed package version, the packaged ABIs and the localized metadata of this plugin.
 * [includeNodeCliState] is false for identities that never report the Node CLI state (the Explorer
 * Action identity), so answering them neither inspects nor warms up the Node.js runtime.
 * zh-CN: 收集已安装包版本, 打包的 ABI 与本地化元数据. 不报告 Node CLI 状态的身份 (Explorer Action) 传 false,
 * 应答时既不检查也不预热 Node.js 运行时.
 */
internal fun Context.threeShellTerminalPluginRuntimeInfo(includeNodeCliState: Boolean = true): ThreeShellTerminalPluginRuntimeInfo {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    return ThreeShellTerminalPluginRuntimeInfo(
        name = getString(R.string.app_name),
        description = getString(R.string.plugin_description),
        instruction = resources.openRawResource(R.raw.plugin_instruction)
            .bufferedReader()
            .use { it.readText() },
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = versionCode,
        versionDate = getString(R.string.plugin_version_date),
        supportedAbis = NativeLibraryInventory.supportedAbis(this),
        nodeCliState = if (includeNodeCliState) NodeCliState.contractState(this) else "",
    )
}

/** Maps the pure-data view onto the host contract parcelable. */
internal fun ThreeShellTerminalPluginRuntimeInfo.toPluginInfo(): PluginInfo {
    val runtimeInfo = this
    return PluginInfo().apply {
        name = runtimeInfo.name
        description = runtimeInfo.description
        instruction = runtimeInfo.instruction
        author = runtimeInfo.author
        collaborators = null
        versionName = runtimeInfo.versionName
        versionCode = runtimeInfo.versionCode
        versionDate = runtimeInfo.versionDate
        id = runtimeInfo.id
        engine = runtimeInfo.engine
        variant = runtimeInfo.variant
        supportedAbis = runtimeInfo.supportedAbis
        capabilities = runtimeInfo.capabilitiesBundle()
    }
}

/**
 * Capabilities negotiated with the host (appendix B.1): the host build, the contract version the
 * host validates in `getInfo().capabilities`, the implemented features, the two ceilings a host
 * must respect before calling, and the current Node CLI state.
 * zh-CN: 与宿主协商的能力表: 宿主版本, 宿主在 `getInfo().capabilities` 中校验的契约版本, 已实现特性, 两项上限与当前 Node CLI 状态.
 */
internal fun ThreeShellTerminalPluginRuntimeInfo.capabilitiesBundle(): Bundle = Bundle().apply {
    putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, requiresHostVersion)
    putInt(TerminalCapabilityKeys.CONTRACT_VERSION, ThreeShellTerminalPlugin.CONTRACT_VERSION)
    putStringArray(TerminalCapabilityKeys.FEATURES_KEY, ThreeShellTerminalPlugin.FEATURES.toTypedArray())
    putInt(TerminalCapabilityKeys.MAX_SESSIONS, TerminalContract.MAX_SESSIONS)
    putInt(TerminalCapabilityKeys.MAX_SUBSCRIPTIONS, TerminalContract.MAX_SUBSCRIPTIONS_PER_SESSION)
    putString(TerminalCapabilityKeys.NODE_CLI, nodeCliState)
}
