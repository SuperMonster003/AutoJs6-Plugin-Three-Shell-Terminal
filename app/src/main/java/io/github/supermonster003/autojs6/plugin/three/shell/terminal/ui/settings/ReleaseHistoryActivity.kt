package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.BackgroundWork
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.HostAppearanceActivity

/**
 * Offline viewer of one [BundledDocument] (AGENTS.md 12, roadmap P5.2): the text is read from the
 * APK assets off the main thread and rendered through [DocumentText] (Markdown) or shown verbatim
 * (license texts). No WebView, no scripts, no remote resources.
 * zh-CN: 单个 [BundledDocument] 的离线阅读界面 (AGENTS.md 12, P5.2): 在工作线程读取 APK 资产, Markdown 经
 * [DocumentText] 渲染, 许可证原文照显; 没有 WebView, 脚本与远程资源.
 */
class ReleaseHistoryActivity : HostAppearanceActivity() {

    internal lateinit var scaffold: Scaffold
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val document = BundledDocument.of(intent.getStringExtra(EXTRA_DOCUMENT))
        scaffold = buildScaffold(getString(document.title), horizontalPaddingDp = SettingsMetrics.SCREEN_MARGIN)
        val progress = LinearProgressIndicator(this).apply {
            isIndeterminate = true
            kit.applyThemeToControls(this)
        }
        val content = TextView(this).apply {
            tag = TAG_DOCUMENT
            textSize = SettingsMetrics.TEXT_SUMMARY + 0.5f
            setTextColor(palette.text)
            setLinkTextColor(palette.accent)
            setTextIsSelectable(true)
            kit.tintTextHandles(this)
            setLineSpacing(0f, 1.25f)
            textDirection = View.TEXT_DIRECTION_LOCALE
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            text = getString(R.string.document_loading)
        }
        scaffold.content.addView(
            progress,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = kit.dp(SettingsMetrics.SECTION_BOTTOM)
                bottomMargin = kit.dp(SettingsMetrics.CHEVRON_GAP)
            },
        )
        scaffold.content.addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        setContentView(scaffold.root)
        val locale = resources.configuration.locales[0]
        val assets = assets
        BackgroundWork.run({
            when (document) {
                BundledDocument.HISTORY -> ReleaseHistory.load(locale) { ReleaseHistory.read(assets, it) }
                else -> ReleaseHistory.read(assets, requireNotNull(document.assetPath))
            }
        }) { result ->
            if (isFinishing || isDestroyed) return@run
            progress.visibility = View.GONE
            val text = result.getOrNull()
            if (text == null) {
                content.tag = TAG_ERROR
                content.setTextColor(palette.muted)
                content.text = getString(R.string.release_history_error)
            } else {
                content.text = if (document.markdown) DocumentText.render(text, palette) else text
            }
        }
    }

    companion object {

        const val EXTRA_DOCUMENT = "document"
        const val TAG_DOCUMENT = "document"
        const val TAG_ERROR = "document-error"

        internal fun intent(context: Context, document: BundledDocument): Intent =
            Intent(context, ReleaseHistoryActivity::class.java).putExtra(EXTRA_DOCUMENT, document.key)

    }

}
