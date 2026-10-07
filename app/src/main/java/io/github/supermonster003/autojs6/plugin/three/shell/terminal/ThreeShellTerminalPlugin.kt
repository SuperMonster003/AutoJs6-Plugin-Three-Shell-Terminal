package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.nodejs.api.NodeJsPluginActions
import org.autojs.plugin.terminal.api.ITerminalPlugin
import org.autojs.plugin.terminal.api.TerminalActions
import org.autojs.plugin.terminal.api.TerminalCapabilityKeys
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
     * Contract features this build implements (appendix B.1): Node CLI integration, output
     * subscriptions, transcripts and, since roadmap P5.1, the standalone settings page reached
     * through [TerminalActions.OPEN_SETTINGS] (AGENTS.md: never declare an unimplemented capability).
     * zh-CN: 本构建实现的契约特性: Node CLI 集成, 输出订阅, 转录, 以及自 P5.1 起经 `TERMINAL_SETTINGS` 进入的独立设置页.
     */
    val FEATURES: List<String> = listOf(
        TerminalCapabilityKeys.FEATURE_NODE_CLI,
        TerminalCapabilityKeys.FEATURE_OUTPUT_SUBSCRIPTION,
        TerminalCapabilityKeys.FEATURE_SETTINGS,
        TerminalCapabilityKeys.FEATURE_TRANSCRIPT,
    )

    /**
     * Minimum AutoJs6 `versionCode`: the 6.8.0 host build that ships `terminal-api`
     * ([TerminalIds.REQUIRED_HOST_VERSION_CODE]); roadmap P1.4 confirms the value.
     */
    const val REQUIRED_HOST_VERSION = TerminalIds.REQUIRED_HOST_VERSION_CODE

    /** Native page alignment of the vendored pty libraries, declared in the manifest (roadmap D14). */
    const val NATIVE_PAGE_ALIGNMENT = 16384

    /**
     * Explorer Action discovery and launch actions of the file manager's "Open in terminal"
     * directory action (roadmap P8.1, appendix D Q10), provided by the host `explorer-action-api` AAR.
     * zh-CN: 文件管理器 "在终端中打开" 目录动作的 Explorer Action 发现与启动 action (P8.1, 附录 D Q10).
     */
    const val EXPLORER_ACTION = ExplorerActionPluginActions.EXPLORER_ACTION
    const val EXPLORER_EXECUTE_ACTION = ExplorerActionPluginActions.EXECUTE

    /** Catalog id of the directory action. zh-CN: 目录动作的目录项 id. */
    const val EXPLORER_DIRECTORY_ACTION_ID = "three-shell-terminal.open-directory"

    /**
     * Explorer Action protocol version the catalog declares: v23 is the first in which the host
     * discloses the canonical path of a single selected directory (`filesystemPath`).
     * zh-CN: 目录声明的 Explorer Action 协议版本: v23 起宿主披露单个所选目录的规范路径 (`filesystemPath`).
     */
    const val EXPLORER_PROTOCOL_VERSION = 23

    /**
     * First AutoJs6 build that accepts protocol v23 and no longer hard-codes "Open in terminal"
     * in the directory menu; older hosts never list the action and keep their own menu item.
     * zh-CN: 首个接受协议 v23 并移除目录菜单硬编码 "在终端中打开" 的宿主构建; 更早的宿主不列出该动作, 保留自身菜单项.
     */
    const val EXPLORER_REQUIRED_HOST_VERSION = 5320L
}
