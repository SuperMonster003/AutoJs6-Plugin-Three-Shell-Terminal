package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import androidx.appcompat.app.AlertDialog
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.SessionAssembly
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.TerminalNodeSetup

/**
 * The screen-side session start shared by the terminal Activity and the session manager: probes
 * whether the Node.js toolchain still has to be extracted (progress dialog only then), plans the
 * session off the main thread ([SessionAssembly.plan]) and starts it on the main thread. A caller
 * that lost interest (newer request, dismissed dialog, finishing Activity) says so through
 * [stillWanted] and the shell is never started for it.
 *
 * zh-CN: 终端 Activity 与会话管理器共用的界面侧启动: 先探测是否仍需解压 Node.js 工具链 (仅此时显示进度),
 * 在工作线程规划会话, 在主线程启动; 调用方若已不再需要 (更新的请求, 已关闭的对话框, 正在结束的 Activity),
 * 通过 [stillWanted] 表明, 则不会为其启动 shell.
 */
internal object SessionStarter {

    data class Started(val plan: SessionAssembly.Plan, val session: TerminalSessionManager.Session)

    fun start(
        activity: HostAppearanceActivity,
        preferences: TerminalPreferences,
        requestedDirectory: String?,
        command: String? = null,
        stillWanted: () -> Boolean,
        onProgress: (AlertDialog?) -> Unit = {},
        onDone: (Result<Started>) -> Unit,
    ) {
        val app = activity.applicationContext
        val kit = activity.kit
        BackgroundWork.run({
            val resolution = NodeCliLocator.resolve(app, integrationEnabled = preferences.nodeIntegrationEnabled)
            TerminalNodeSetup.needsInstall(TerminalPaths.of(app), resolution)
        }) { probe ->
            if (!stillWanted()) return@run
            val progress = if (probe.getOrDefault(false)) kit.progressDialog(kit.string(R.string.terminal_preparing_npm)) else null
            onProgress(progress)
            BackgroundWork.run({ SessionAssembly.plan(app, requestedDirectory, preferences) }) { planned ->
                progress?.dismiss()
                onProgress(null)
                if (!stillWanted()) return@run
                onDone(planned.mapCatching { plan -> Started(plan, SessionAssembly.start(activity, plan, command = command)) })
            }
        }
    }

}
