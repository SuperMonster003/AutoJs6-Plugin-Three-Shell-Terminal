package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.nodejs.api.NodeJsPluginActions
import org.autojs.plugin.terminal.api.ITerminalPlugin
import org.autojs.plugin.terminal.api.TerminalActions
import org.autojs.plugin.terminal.api.TerminalContract
import org.autojs.plugin.terminal.api.TerminalIds

/**
 * Identity constants shared by the manifest, the Binder services, the documentation, and the
 * tests. Since roadmap P1.1 they are provided by the host `terminal-api` contract module
 * ([TerminalActions] / [TerminalIds] / [TerminalContract], staged as `libs/terminal-api.aar`),
 * exactly as 3-Setup Installer references `InstallerActions` / `InstallerIds`; the JVM manifest
 * contract test fails when the manifest drifts from them.
 */
object ThreeShellTerminalPlugin {

    const val PACKAGE_NAME = TerminalIds.DEFAULT_PACKAGE_NAME
    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"

    /** Node.js Runtime plugin whose `NODE_CLI_*` manifest contract supplies node / npm / corepack (roadmap D17). */
    const val NODEJS_PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.nodejs"
    const val NODEJS_SERVICE_ACTION = NodeJsPluginActions.RUNTIME

    const val ID = TerminalIds.PLUGIN_ID
    const val ENGINE = TerminalIds.ENGINE
    const val VARIANT = TerminalIds.VARIANT_DEFAULT
    const val AUTHOR = "SuperMonster003"

    /** Discovery contract of [ThreeShellTerminalPluginService]. */
    const val SERVICE_ACTION = TerminalActions.SERVICE_ACTION
    const val SERVICE_CATEGORY = TerminalActions.SERVICE_CATEGORY

    /** Discovery contract of [ThreeShellTerminalPluginInfoService]. */
    const val INFO_ACTION = PluginActions.INFO

    /** Signature permission guarding every exported component (manifest `android:permission`). */
    const val PLUGIN_PERMISSION = TerminalActions.PLUGIN_PERMISSION

    /** Actions of the terminal entry and settings Activities (roadmap D19 / D29, added in P2 / P5). */
    const val OPEN_TERMINAL_ACTION = TerminalActions.OPEN_TERMINAL
    const val OPEN_SETTINGS_ACTION = TerminalActions.OPEN_SETTINGS

    /**
     * Binder descriptor of the `ITerminalPlugin` AIDL. The placeholder Binder of P0 / P1 already
     * carries it so that the contract test asserts the same descriptor before and after the real
     * stub lands in roadmap P2.4.
     */
    const val SERVICE_DESCRIPTOR = ITerminalPlugin.DESCRIPTOR

    /** Contract version implemented by this plugin (roadmap appendix B.6). */
    const val CONTRACT_VERSION = TerminalContract.CONTRACT_VERSION

    /**
     * Minimum AutoJs6 `versionCode`: the 6.8.0 host build that ships `terminal-api`
     * ([TerminalIds.REQUIRED_HOST_VERSION_CODE]); roadmap P1.4 confirms the value.
     */
    const val REQUIRED_HOST_VERSION = TerminalIds.REQUIRED_HOST_VERSION_CODE

    /** Native page alignment of the vendored pty libraries, declared in the manifest (roadmap D14). */
    const val NATIVE_PAGE_ALIGNMENT = 16384
}
