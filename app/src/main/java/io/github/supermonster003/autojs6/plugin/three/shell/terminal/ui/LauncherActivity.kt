package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.app.Activity
import android.os.Bundle

/**
 * Target of the launcher icon (roadmap D30 / P3.3; the four icon aliases of P5.3 point here): an
 * invisible forwarder that opens the terminal screen without extras, so the most recent live
 * session comes back or a new one starts in the plugin's home directory. It shares the terminal's
 * task affinity, hence the task the home screen creates is the terminal's own task and Back from
 * the terminal returns to the home screen.
 *
 * zh-CN: 启动器图标的目标 (D30 / P3.3; P5.3 的四个图标 alias 指向这里): 不可见转发器, 不带 extras 打开终端界面,
 * 因此恢复最近的存活会话或在插件主目录新建会话. 与终端共用 taskAffinity, 桌面创建的任务即终端自己的任务,
 * 从终端返回即回到桌面.
 */
class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ExternalIntents.startSafely(this, TerminalActivity.intent(this))
        finish()
    }

}
