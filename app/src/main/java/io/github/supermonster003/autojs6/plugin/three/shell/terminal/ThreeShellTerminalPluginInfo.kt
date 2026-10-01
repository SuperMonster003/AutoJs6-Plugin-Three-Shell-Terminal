package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.content.Context
import android.os.Build
import android.os.Bundle
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo

/** Collects the installed package version, the packaged ABIs and the localized metadata of this plugin. */
internal fun Context.threeShellTerminalPluginRuntimeInfo(): ThreeShellTerminalPluginRuntimeInfo {
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
 * Capability negotiation bundle. P0 only reports the minimum host build; roadmap P1.1 / P2.4 add
 * the terminal contract version, the feature set, the session limits and the Node CLI state.
 */
internal fun ThreeShellTerminalPluginRuntimeInfo.capabilitiesBundle(): Bundle = Bundle().apply {
    putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, requiresHostVersion)
}
