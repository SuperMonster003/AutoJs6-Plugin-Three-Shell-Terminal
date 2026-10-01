package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog

/**
 * Invisible Activity that hosts the session manager when no terminal screen is in front
 * (roadmap P3.2): the tap target of the session notification and, from P3.3, the `manager=true`
 * entry of the host protocol. Back or "Close" dismisses the dialog and finishes the Activity, so
 * the caller (host, launcher) comes back without a terminal appearing in between.
 *
 * zh-CN: 无前台终端界面时承载会话管理器的透明 Activity (P3.2): 会话通知的点击目标, P3.3 起也是宿主协议
 * `manager=true` 的入口. 返回或 "关闭" 会关闭对话框并结束 Activity, 调用方 (宿主, 启动器) 直接回到前台.
 */
class TerminalManagerActivity : HostAppearanceActivity() {

    internal var dialog: AlertDialog? = null
        private set

    override fun hasUnconfirmedDialog(): Boolean = dialog?.isShowing == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog = TerminalManagerDialog.show(this, onDismissed = {
            if (!isFinishing && !isDestroyed) finish()
        })
    }

    /** `singleTop`: a second tap on the notification keeps the manager that is already showing. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        dialog?.takeIf { it.isShowing }?.dismiss()
        dialog = null
        super.onDestroy()
    }

    companion object {

        fun intent(context: Context): Intent = Intent(context, TerminalManagerActivity::class.java)

        fun launch(context: Context) {
            ExternalIntents.startSafely(context, intent(context))
        }

    }

}
