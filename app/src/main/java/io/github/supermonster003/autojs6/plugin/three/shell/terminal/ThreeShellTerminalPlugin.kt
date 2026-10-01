package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.nodejs.api.NodeJsPluginActions

/**
 * Identity constants shared by the manifest, the Binder services, the documentation, and the
 * tests. They must stay identical to the host-side registration (see `ROADMAP.md`, decision D12
 * and phase P1.2); the JVM manifest contract test fails when the manifest drifts from them.
 *
 * Roadmap P1.1 replaces the literal action, category, descriptor and host version with the
 * constants of the host `terminal-api` module (`TerminalActions` / `TerminalIds`), exactly as
 * 3-Setup Installer references `InstallerActions` / `InstallerIds`.
 */
object ThreeShellTerminalPlugin {

    const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.shell.terminal"
    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"

    /** Node.js Runtime plugin whose `NODE_CLI_*` manifest contract supplies node / npm / corepack (roadmap D17). */
    const val NODEJS_PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.nodejs"
    const val NODEJS_SERVICE_ACTION = NodeJsPluginActions.RUNTIME

    const val ID = "three-shell-terminal"
    const val ENGINE = "terminal"
    const val VARIANT = "default"
    const val AUTHOR = "SuperMonster003"

    /** Discovery contract of [ThreeShellTerminalPluginService]. */
    const val SERVICE_ACTION = "org.autojs.plugin.TERMINAL"
    const val SERVICE_CATEGORY = "terminal"

    /** Discovery contract of [ThreeShellTerminalPluginInfoService]. */
    const val INFO_ACTION = PluginActions.INFO

    /**
     * Binder descriptor of the `ITerminalPlugin` AIDL that the host `terminal-api` module will
     * define (roadmap P1.1). The placeholder Binder of P0 already carries it so that the contract
     * test asserts the same descriptor before and after the real stub lands.
     */
    const val SERVICE_DESCRIPTOR = "org.autojs.plugin.terminal.api.ITerminalPlugin"

    /**
     * Minimum AutoJs6 `versionCode`. 5303 (AutoJs6 6.8.0) is the build the skeleton was written
     * against; roadmap P1.4 replaces it with the build that ships `terminal-api`.
     */
    const val REQUIRED_HOST_VERSION = 5303L

    /** Native page alignment of the vendored pty libraries, declared in the manifest (roadmap D14). */
    const val NATIVE_PAGE_ALIGNMENT = 16384
}
