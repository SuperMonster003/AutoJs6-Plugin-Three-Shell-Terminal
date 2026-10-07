package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.FinishedCommands
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings.Scaffold
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings.SettingsMetrics
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings.buildScaffold
import org.autojs.plugin.terminal.api.TerminalContract

/**
 * The output of a finished command, opened from its completion notice (roadmap P8): exit code, directory
 * and the end of the output kept in memory, with copy / share (bounded like the terminal's) and "Open
 * terminal here". After a process restart the output is gone and the screen says so; the directory still
 * comes from the notice. Not exported.
 * zh-CN: 已结束命令的输出, 由完成通知打开 (P8): 退出码, 目录与内存中保留的输出末尾, 提供复制 / 分享 (与终端相同的上限)
 * 与 "在此打开终端". 进程重启后输出不再可用, 界面会说明; 目录仍取自通知. 不导出.
 */
class CommandResultActivity : HostAppearanceActivity() {

    internal lateinit var scaffold: Scaffold
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val result = FinishedCommands.get(sessionId)
        val directory = result?.directory ?: intent.getStringExtra(EXTRA_DIRECTORY)
        scaffold = buildScaffold(getString(R.string.command_result_title), horizontalPaddingDp = SettingsMetrics.SCREEN_MARGIN)
        val paths = TerminalPaths.of(this)
        val status = TextView(this).apply {
            tag = TAG_STATUS
            setTextColor(palette.muted)
            textSize = 14f
            text = buildString {
                if (result != null) append(getString(R.string.command_result_exit_code, result.exitCode))
                if (directory != null) {
                    if (isNotEmpty()) append('\n')
                    append(paths.toTildePath(directory))
                }
            }
        }
        val output = TextView(this).apply {
            tag = TAG_OUTPUT
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setTextIsSelectable(true)
            kit.tintTextHandles(this)
            textDirection = View.TEXT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setTextColor(palette.terminalForeground)
            setBackgroundColor(palette.terminalBackground)
            val padding = kit.dp(12)
            setPadding(padding, padding, padding, padding)
            text = result?.transcript?.trimEnd()?.takeIf { it.isNotEmpty() } ?: getString(R.string.command_result_unavailable)
        }
        val transcript = result?.transcript?.trimEnd()?.takeIf { it.isNotEmpty() }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            if (transcript != null) {
                addView(textButton(getString(R.string.terminal_copy_transcript), TAG_COPY) { Clipboard.copyAndReport(this@CommandResultActivity, transcript) })
                addView(textButton(getString(R.string.terminal_share_transcript), TAG_SHARE) { ExternalIntents.shareText(this@CommandResultActivity, transcript) })
            }
            if (directory != null) {
                addView(textButton(getString(R.string.command_result_open_terminal), TAG_OPEN) {
                    ExternalIntents.startSafely(
                        this@CommandResultActivity,
                        TerminalActivity.intent(this@CommandResultActivity, directory).putExtra(TerminalContract.EXTRA_NEW_SESSION, true),
                    )
                    finish()
                })
            }
        }
        with(scaffold.content) {
            addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = kit.dp(SettingsMetrics.SECTION_BOTTOM)
            })
            addView(output, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(actions, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = kit.dp(SettingsMetrics.SECTION_TOP)
            })
        }
        setContentView(scaffold.root)
        // The latest output matters most: start at the end.
        scaffold.scroll.post { scaffold.scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun textButton(label: CharSequence, tag: String, onClick: () -> Unit): MaterialButton =
        MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
            text = label
            this.tag = tag
            isAllCaps = false
            setTextColor(palette.accent)
            rippleColor = android.content.res.ColorStateList.valueOf(palette.accentRipple)
            minHeight = kit.dp(48)
            setOnClickListener { onClick() }
        }

    companion object {

        private const val EXTRA_SESSION_ID = "session_id"
        private const val EXTRA_DIRECTORY = "directory"
        const val TAG_STATUS = "command-status"
        const val TAG_OUTPUT = "command-output"
        const val TAG_COPY = "command-copy"
        const val TAG_SHARE = "command-share"
        const val TAG_OPEN = "command-open"

        fun intent(context: Context, sessionId: String, directory: String): Intent =
            Intent(context, CommandResultActivity::class.java)
                .putExtra(EXTRA_SESSION_ID, sessionId)
                .putExtra(EXTRA_DIRECTORY, directory)

    }

}
