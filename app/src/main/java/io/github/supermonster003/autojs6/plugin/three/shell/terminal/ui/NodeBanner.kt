package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSettingsActions
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator.Resolution

/**
 * Explains why `node` is unavailable and offers the one action that helps (roadmap D17): install
 * or update the Node.js Runtime plugin (its GitHub Releases page; the AutoJs6 plugin center lists
 * the same package), enable the integration switch, or show the probe report. The npm menu items
 * call [ensureAvailable] so they explain instead of typing a command that cannot run.
 * zh-CN: 说明 `node` 为何不可用并给出唯一有用的动作 (D17): 安装 / 更新 Node.js 运行时插件 (GitHub Release 页,
 * AutoJs6 插件中心列出同一包), 启用集成开关, 或显示探测报告; npm 菜单项先经 [ensureAvailable] 解释原因.
 */
internal class NodeBanner(
    private val activity: HostAppearanceActivity,
    private val banner: TerminalBanner,
    private val settings: TerminalSettingsActions,
    private val onIntegrationEnabled: () -> Unit,
) {

    private val kit: UiKit get() = activity.kit

    private class Remedy(val label: String, val run: () -> Unit)

    fun render(resolution: Resolution?) {
        if (resolution == null || resolution is Resolution.Available) {
            banner.hide()
            return
        }
        val remedy = remedy(resolution)
        banner.show(message(resolution), remedy.label to remedy.run)
    }

    fun message(resolution: Resolution): String = when (resolution) {
        is Resolution.Available -> ""
        is Resolution.Unavailable.IntegrationDisabled -> activity.getString(R.string.terminal_node_integration_disabled)
        is Resolution.Unavailable.PluginMissing -> activity.getString(R.string.terminal_node_missing)
        is Resolution.Unavailable.PluginNotTrusted -> activity.getString(R.string.terminal_node_plugin_not_trusted)
        is Resolution.Unavailable.PluginTooOld -> activity.getString(R.string.terminal_node_plugin_too_old)
        is Resolution.Unavailable.ExecutableMissing -> activity.getString(R.string.terminal_node_executable_missing, resolution.abi)
        is Resolution.Unavailable.ExecDenied -> activity.getString(R.string.terminal_node_exec_denied)
        is Resolution.Unavailable.SetupFailed -> activity.getString(R.string.terminal_npm_setup_failed)
    }

    /**
     * True when node is available; otherwise shows the reason with the remedy as the positive
     * action and returns false.
     */
    fun ensureAvailable(resolution: Resolution?): Boolean {
        if (resolution is Resolution.Available) return true
        val effective = resolution ?: Resolution.Unavailable.PluginMissing
        val remedy = remedy(effective)
        kit.materialDialog()
            .setTitle(R.string.terminal_nodejs)
            .setMessage(message(effective))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(remedy.label) { _, _ -> remedy.run() }
            .show().also { kit.tintDialogButtons(it) }
        return false
    }

    fun showProbeDetails(resolution: Resolution) {
        val report = NodeProbeReport.render(resolution)
        kit.messageDialog(activity.getString(R.string.terminal_node_probe), report, activity.getString(android.R.string.copy) to {
            Clipboard.set(activity, report)
            kit.toast(R.string.terminal_copied_to_clipboard)
        })
    }

    private fun remedy(resolution: Resolution): Remedy = when (resolution) {
        is Resolution.Unavailable.PluginMissing -> Remedy(activity.getString(R.string.terminal_action_install_node_runtime), ::openRuntimeReleases)
        is Resolution.Unavailable.PluginTooOld -> Remedy(activity.getString(R.string.terminal_action_update_node_runtime), ::openRuntimeReleases)
        is Resolution.Unavailable.IntegrationDisabled -> Remedy(activity.getString(R.string.terminal_action_enable), ::enableIntegration)
        else -> Remedy(activity.getString(R.string.terminal_action_details)) { showProbeDetails(resolution) }
    }

    private fun openRuntimeReleases() {
        if (!ExternalIntents.browse(activity, NODEJS_RUNTIME_RELEASES_URL)) kit.toast(R.string.terminal_error_occurred)
    }

    /** Turns the switch on; the running shell keeps its environment, new sessions get node. zh-CN: 打开开关; 运行中的 shell 环境不变, 新会话获得 node. */
    private fun enableIntegration() {
        settings.setNodeIntegration(true)
        kit.toast(R.string.terminal_node_enabled_for_new_sessions)
        onIntegrationEnabled()
    }

    companion object {
        /** Releases of the official Node.js Runtime plugin (`.readme/common.json` `nodejs_plugin_url`). */
        const val NODEJS_RUNTIME_RELEASES_URL = "https://github.com/SuperMonster003/AutoJs6-Plugin-NodeJs-Runtime/releases"
    }

}
