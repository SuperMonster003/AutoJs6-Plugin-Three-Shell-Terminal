package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.BackgroundWork
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.Clipboard
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.NodeProbeReport

/**
 * The Node.js environment probe as a screen (roadmap D29, P5.1): the same report the terminal's
 * banner shows in a dialog, resolved off the main thread with the integration switch honored, plus
 * "copy" and "check again" (a forced refresh that bypasses the resolution cache).
 * zh-CN: 以界面形式呈现的 Node.js 环境探测 (D29, P5.1): 与终端横幅对话框相同的报告, 在工作线程解析并尊重集成开关,
 * 另提供 "复制" 与 "重新检测" (跳过解析缓存的强制刷新).
 */
class NodeProbeActivity : HostAppearanceActivity() {

    internal lateinit var scaffold: Scaffold
        private set

    private lateinit var progress: LinearProgressIndicator
    private lateinit var report: TextView
    private var generation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scaffold = buildScaffold(getString(R.string.terminal_node_probe), horizontalPaddingDp = SettingsMetrics.SCREEN_MARGIN)
        progress = LinearProgressIndicator(this).apply {
            isIndeterminate = true
            kit.applyThemeToControls(this)
        }
        report = TextView(this).apply {
            tag = TAG_REPORT
            typeface = Typeface.MONOSPACE
            textSize = 13f
            setTextColor(palette.text)
            setTextIsSelectable(true)
            kit.tintTextHandles(this)
            setLineSpacing(0f, 1.2f)
            textDirection = View.TEXT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            addView(textButton(getString(android.R.string.copy), TAG_COPY) {
                val text = report.text?.toString().orEmpty()
                if (text.isNotEmpty()) {
                    Clipboard.set(this@NodeProbeActivity, text)
                    kit.toast(R.string.terminal_copied_to_clipboard)
                }
            })
            addView(textButton(getString(R.string.settings_probe_again), TAG_AGAIN) { probe(refresh = true) })
        }
        with(scaffold.content) {
            addView(
                progress,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = kit.dp(SettingsMetrics.SECTION_BOTTOM)
                    bottomMargin = kit.dp(SettingsMetrics.CHEVRON_GAP)
                },
            )
            addView(report, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(
                actions,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = kit.dp(SettingsMetrics.SECTION_TOP)
                },
            )
        }
        setContentView(scaffold.root)
        probe(refresh = false)
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

    private fun probe(refresh: Boolean) {
        val expected = ++generation
        progress.visibility = View.VISIBLE
        report.text = getString(R.string.terminal_node_probe_running)
        val app = applicationContext
        val enabled = TerminalPreferences(this).nodeIntegrationEnabled
        BackgroundWork.run({ NodeProbeReport.render(NodeCliLocator.resolve(app, refresh, enabled)) }) { result ->
            if (expected != generation || isFinishing || isDestroyed) return@run
            progress.visibility = View.GONE
            report.text = result.getOrElse { getString(R.string.terminal_error_occurred) }
        }
    }

    companion object {
        const val TAG_REPORT = "probe-report"
        const val TAG_COPY = "probe-copy"
        const val TAG_AGAIN = "probe-again"
    }

}
