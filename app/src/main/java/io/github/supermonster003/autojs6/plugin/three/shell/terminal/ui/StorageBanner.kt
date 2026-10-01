package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.os.Build
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.storage.StorageAccess

/**
 * Storage banner of roadmap D18: when a requested directory lives on shared storage and the plugin
 * lacks its own grant, the session starts in `$HOME` and this banner names the directory with a
 * "Grant" action (API 30+ opens the all-files-access switch, older systems request the legacy
 * permissions); once the grant exists the action becomes "Re-enter directory".
 * zh-CN: D18 的存储横幅: 请求目录位于共享存储且插件无自身授权时, 会话在 `$HOME` 启动, 横幅给出目录与 "授予" 动作
 * (API 30+ 打开全部文件访问开关, 更低版本请求旧式权限); 授权后动作变为 "重新进入目录".
 */
internal class StorageBanner(
    private val activity: HostAppearanceActivity,
    private val banner: TerminalBanner,
    private val requestLegacyPermissions: () -> Unit,
    private val onReenter: (String) -> Unit,
) {

    private val kit: UiKit get() = activity.kit

    /** The directory that could not be entered, until the user re-enters or dismisses. */
    var pendingDirectory: String? = null
        private set

    fun showPermissionRequired(directory: String) {
        pendingDirectory = directory
        banner.resetDismissal()
        render()
    }

    /** Re-renders after a permission result or a return from the system settings. */
    fun refresh() {
        if (pendingDirectory != null) render()
    }

    fun hide() {
        pendingDirectory = null
        banner.hide()
    }

    private fun render() {
        val directory = pendingDirectory ?: return
        val message = activity.getString(R.string.terminal_storage_permission_required, directory)
        if (StorageAccess.state(activity).isGranted) {
            banner.show(message, activity.getString(R.string.terminal_action_reenter_directory) to {
                hide()
                onReenter(directory)
            })
        } else {
            banner.show(message, activity.getString(R.string.terminal_action_grant) to ::requestGrant)
        }
    }

    fun requestGrant() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            requestLegacyPermissions()
            return
        }
        val opened = StorageAccess.resolvableAllFilesAccessIntents(activity).any { ExternalIntents.startSafely(activity, it) }
        if (!opened) kit.toast(R.string.terminal_storage_settings_unavailable)
    }

}
