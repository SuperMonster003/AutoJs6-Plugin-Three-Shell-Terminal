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
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSettingsActions
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator.Resolution
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.BackgroundWork
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.Clipboard
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.NodeProbeReport

/**
 * The Node.js environment probe as a screen (roadmap D29, P5.1): the same report the terminal's
 * banner shows in a dialog, resolved off the main thread with the integration switch honored, plus
 * "copy" and "check again" (a forced refresh that bypasses the resolution cache). A Node.js Runtime
 * signed by an unknown key adds "Trust this signer" (roadmap P8, D34 Q3): a confirmation names the
 * package, the exact current signer digests and what trusting them allows; the settings page revokes it.
 * zh-CN: 以界面形式呈现的 Node.js 环境探测 (D29, P5.1): 与终端横幅对话框相同的报告, 在工作线程解析并尊重集成开关,
 * 另提供 "复制" 与 "重新检测" (跳过解析缓存的强制刷新). 未知密钥签名的 Node.js Runtime 另有 "信任此签名" (P8, D34 Q3):
 * 确认框列出包名, 当前签名摘要及信任的后果; 设置页可撤销.
 */
class NodeProbeActivity : HostAppearanceActivity() {

    internal lateinit var scaffold: Scaffold
        private set

    private lateinit var progress: LinearProgressIndicator
    private lateinit var report: TextView
    private lateinit var trust: MaterialButton
    private var generation = 0
    private var untrusted: Resolution.Unavailable.PluginNotTrusted? = null

    /** The last confirmation dialog, for instrumentation. zh-CN: 最近的确认对话框, 供 instrumentation 使用. */
    internal var prompt: androidx.appcompat.app.AlertDialog? = null
        private set

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
                    Clipboard.copyAndReport(this@NodeProbeActivity, text)
                }
            })
            addView(textButton(getString(R.string.settings_probe_again), TAG_AGAIN) { probe(refresh = true) })
        }
        trust = textButton(getString(R.string.terminal_node_trust_signer), TAG_TRUST) { confirmTrust() }.apply {
            setTextColor(palette.danger)
            visibility = View.GONE
        }
        actions.addView(trust, 0)
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
        trust.visibility = View.GONE
        untrusted = null
        report.text = getString(R.string.terminal_node_probe_running)
        val app = applicationContext
        val enabled = TerminalPreferences(this).nodeIntegrationEnabled
        val resolve = resolverForTest ?: { context, force, integration -> NodeCliLocator.resolve(context, force, integration) }
        BackgroundWork.run({ resolve(app, refresh, enabled) }) { result ->
            if (expected != generation || isFinishing || isDestroyed) return@run
            progress.visibility = View.GONE
            val resolution = result.getOrNull()
            report.text = resolution?.let(NodeProbeReport::render) ?: getString(R.string.terminal_error_occurred)
            untrusted = (resolution as? Resolution.Unavailable.PluginNotTrusted)?.takeIf { it.current.isNotEmpty() }
            trust.visibility = if (untrusted != null) View.VISIBLE else View.GONE
        }
    }

    /**
     * Two explicit steps: the button, then a destructive-styled confirmation that names the package and the exact
     * digests. Nothing is stored before the second step.
     * zh-CN: 两个明确步骤: 按钮, 然后是列出包名与确切摘要的破坏性样式确认框; 第二步之前不保存任何内容.
     */
    private fun confirmTrust() {
        val target = untrusted ?: return
        val digests = target.current.joinToString("\n") { "SHA-256: $it" }
        prompt = kit.confirmDialog(
            getString(R.string.terminal_node_trust_title),
            getString(R.string.terminal_node_trust_confirm, target.packageName, digests),
            getString(R.string.terminal_node_trust_action),
            destructive = true,
        ) {
            if (TerminalSettingsActions(this).trustNodeSigners(target.packageName, target.current)) {
                kit.toast(R.string.terminal_node_trusted, long = true)
                probe(refresh = true)
            } else {
                kit.toast(R.string.terminal_error_occurred)
            }
        }
    }

    companion object {
        const val TAG_REPORT = "probe-report"
        const val TAG_COPY = "probe-copy"
        const val TAG_AGAIN = "probe-again"
        const val TAG_TRUST = "probe-trust"

        /** Instrumentation stand-in for [NodeCliLocator.resolve] (no third-party signed runtime exists on test devices). */
        @Volatile
        internal var resolverForTest: ((android.content.Context, Boolean, Boolean) -> Resolution)? = null
    }

}
