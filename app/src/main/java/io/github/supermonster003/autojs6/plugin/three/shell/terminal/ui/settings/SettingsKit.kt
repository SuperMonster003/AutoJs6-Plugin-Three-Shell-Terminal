package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.ColorPolicy
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.UiKit

/**
 * Metrics of the standalone settings specification (sections 3 to 6) shared by the settings, about
 * and document screens: 24 dp screen margin, 72 / 56 dp row minimums, 40 dp icon slot, 16 / 14 sp
 * text, 1 dp dividers from the text start. Only neutral surfaces appear here; the accent tints controls.
 * zh-CN: 独立设置页规范 (第 3 至 6 节) 的尺寸常量, 设置 / 关于 / 文档三类界面共用.
 */
internal object SettingsMetrics {
    const val SCREEN_MARGIN = 24
    const val ICON_SIZE = 24
    const val ICON_SLOT = 40
    const val ROW_MIN_HEIGHT = 72
    const val SINGLE_ROW_MIN_HEIGHT = 56
    const val ROW_PADDING = 12
    const val TITLE_SUMMARY_GAP = 4
    const val SECTION_TOP = 24
    const val SECTION_BOTTOM = 8
    const val CHEVRON_GAP = 16
    const val TEXT_TITLE = 16f
    const val TEXT_SUMMARY = 14f
    const val TEXT_SECTION = 14f
    const val TEXT_PAGE_TITLE = 20f
    const val MAX_CONTENT_WIDTH = 840
    const val DISABLED_ALPHA = 0.42f

    /** Text start of rows with an icon; the in-group divider starts here as well. */
    const val TEXT_START = SCREEN_MARGIN + ICON_SLOT

    val medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
}

/** One settings row: the whole row is the click target, title and summary carry the description. */
internal class SettingRow(val view: LinearLayout, val title: TextView, val summary: TextView, val switch: MaterialSwitch?) {

    fun setSummary(value: CharSequence?) {
        summary.text = value
        summary.visibility = if (value.isNullOrEmpty()) View.GONE else View.VISIBLE
        refreshDescription()
    }

    fun setEnabled(enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else SettingsMetrics.DISABLED_ALPHA
        switch?.isEnabled = enabled
    }

    /** Programmatic switch update that does not fire the row's toggle callback. */
    fun setChecked(checked: Boolean) {
        switch?.isChecked = checked
        refreshDescription()
    }

    fun refreshDescription() {
        view.contentDescription = listOf(title.text, summary.text.takeIf { summary.visibility == View.VISIBLE })
            .filter { !it.isNullOrEmpty() }.joinToString(", ")
    }

}

internal fun UiKit.tintedDrawable(@DrawableRes resource: Int, color: Int) =
    AppCompatResources.getDrawable(context, resource)?.let { tinted(it, color) }

/** Body text in the palette colors; [medium] switches to the medium weight. */
internal fun UiKit.text(value: CharSequence?, sizeSp: Float = SettingsMetrics.TEXT_SUMMARY, color: Int = palette.text, medium: Boolean = false): TextView =
    TextView(context).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        setLinkTextColor(palette.accent)
        highlightColor = ColorPolicy.withAlpha(palette.accent, 0x55)
        if (medium) typeface = SettingsMetrics.medium
        setLineSpacing(0f, 1.15f)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

/** Muted group header, exposed to accessibility services as a heading. */
internal fun UiKit.sectionHeader(title: CharSequence): TextView = TextView(context).apply {
    text = title
    textSize = SettingsMetrics.TEXT_SECTION
    typeface = SettingsMetrics.medium
    setTextColor(palette.muted)
    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    setPaddingRelative(dp(SettingsMetrics.SCREEN_MARGIN), dp(SettingsMetrics.SECTION_TOP), dp(SettingsMetrics.SCREEN_MARGIN), dp(SettingsMetrics.SECTION_BOTTOM))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isAccessibilityHeading = true
}

/** 1 dp divider inset from the logical start by [insetStartDp]. */
internal fun UiKit.hairline(insetStartDp: Int = SettingsMetrics.TEXT_START): View = View(context).apply {
    setBackgroundColor(palette.divider)
    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { marginStart = dp(insetStartDp) }
}

/** Secondary explanation below a group. */
internal fun UiKit.pageCaption(value: CharSequence): TextView = text(value, SettingsMetrics.TEXT_SUMMARY, palette.muted).apply {
    setPaddingRelative(dp(SettingsMetrics.SCREEN_MARGIN), dp(SettingsMetrics.TITLE_SUMMARY_GAP), dp(SettingsMetrics.SCREEN_MARGIN), dp(SettingsMetrics.SECTION_BOTTOM))
}

private fun UiKit.rowShell(tag: String?, hasSummary: Boolean): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = dp(if (hasSummary) SettingsMetrics.ROW_MIN_HEIGHT else SettingsMetrics.SINGLE_ROW_MIN_HEIGHT)
    setPaddingRelative(dp(SettingsMetrics.SCREEN_MARGIN), dp(SettingsMetrics.ROW_PADDING), dp(SettingsMetrics.SCREEN_MARGIN), dp(SettingsMetrics.ROW_PADDING))
    this.tag = tag
}

private fun UiKit.rowIcon(@DrawableRes icon: Int): ImageView = ImageView(context).apply {
    setImageDrawable(tintedDrawable(icon, palette.muted))
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    layoutParams = LinearLayout.LayoutParams(dp(SettingsMetrics.ICON_SIZE), dp(SettingsMetrics.ICON_SIZE)).apply {
        marginEnd = dp(SettingsMetrics.ICON_SLOT - SettingsMetrics.ICON_SIZE)
    }
}

private fun UiKit.rowText(title: CharSequence, summary: CharSequence?, titleColor: Int): Triple<LinearLayout, TextView, TextView> {
    val titleView = TextView(context).apply {
        text = title
        textSize = SettingsMetrics.TEXT_TITLE
        setTextColor(titleColor)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    val summaryView = TextView(context).apply {
        text = summary
        textSize = SettingsMetrics.TEXT_SUMMARY
        setTextColor(palette.muted)
        setLineSpacing(0f, 1.1f)
        setPaddingRelative(0, dp(SettingsMetrics.TITLE_SUMMARY_GAP), 0, 0)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        visibility = if (summary.isNullOrEmpty()) View.GONE else View.VISIBLE
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(titleView)
        addView(summaryView)
    }
    return Triple(column, titleView, summaryView)
}

/** Tappable row: optional leading icon, title, summary, optional chevron (navigation / dialog rows). */
internal fun UiKit.settingRow(
    title: CharSequence,
    summary: CharSequence? = null,
    @DrawableRes icon: Int? = null,
    tag: String? = null,
    chevron: Boolean = true,
    titleColor: Int = palette.text,
    onClick: (() -> Unit)? = null,
): SettingRow {
    val shell = rowShell(tag, !summary.isNullOrEmpty())
    icon?.let { shell.addView(rowIcon(it)) }
    val (column, titleView, summaryView) = rowText(title, summary, titleColor)
    shell.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    if (chevron && onClick != null) {
        shell.addView(
            ImageView(context).apply {
                setImageDrawable(tintedDrawable(R.drawable.ic_settings_chevron, palette.muted))
                alpha = 0.7f
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                layoutParams = LinearLayout.LayoutParams(dp(SettingsMetrics.ICON_SIZE), dp(SettingsMetrics.ICON_SIZE)).apply { marginStart = dp(SettingsMetrics.CHEVRON_GAP) }
            },
        )
    }
    if (onClick != null) {
        shell.isClickable = true
        shell.isFocusable = true
        selectableBackground(shell)
        shell.setOnClickListener { onClick() }
    }
    return SettingRow(shell, titleView, summaryView, null).also { it.refreshDescription() }
}

/** Row with a trailing Material switch; one tap anywhere toggles exactly once and the row carries the switch semantics. */
internal fun UiKit.switchRow(
    title: CharSequence,
    summary: CharSequence? = null,
    @DrawableRes icon: Int? = null,
    checked: Boolean,
    tag: String? = null,
    onToggle: (Boolean) -> Unit,
): SettingRow {
    val shell = rowShell(tag, !summary.isNullOrEmpty())
    icon?.let { shell.addView(rowIcon(it)) }
    val (column, titleView, summaryView) = rowText(title, summary, palette.text)
    shell.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    val switch = MaterialSwitch(context).apply {
        isChecked = checked
        thumbTintList = switchThumbTintList()
        trackTintList = switchTrackTintList()
        trackDecorationTintList = android.content.res.ColorStateList.valueOf(palette.outline)
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    shell.addView(switch, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(SettingsMetrics.ROW_PADDING) })
    shell.isClickable = true
    shell.isFocusable = true
    selectableBackground(shell)
    shell.accessibilityDelegate = object : View.AccessibilityDelegate() {
        @Suppress("DEPRECATION") // setChecked(int) only exists from API 36; the boolean form still works everywhere.
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.className = Switch::class.java.name
            info.isCheckable = true
            info.isChecked = switch.isChecked
        }
    }
    val row = SettingRow(shell, titleView, summaryView, switch)
    shell.setOnClickListener {
        switch.isChecked = !switch.isChecked
        row.refreshDescription()
        onToggle(switch.isChecked)
    }
    return row.also { it.refreshDescription() }
}

/** Selectable label + value pair for read-only details (About). */
internal fun UiKit.infoBlock(label: CharSequence, value: CharSequence, tag: String? = null): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    this.tag = tag
    setPaddingRelative(dp(SettingsMetrics.SCREEN_MARGIN), dp(SettingsMetrics.ROW_PADDING), dp(SettingsMetrics.SCREEN_MARGIN), dp(SettingsMetrics.ROW_PADDING))
    addView(text(label, 12.5f, palette.accent, medium = true))
    addView(
        text(value, SettingsMetrics.TEXT_SUMMARY + 0.5f).apply {
            setTextIsSelectable(true)
            tintTextHandles(this)
            setPaddingRelative(0, dp(SettingsMetrics.TITLE_SUMMARY_GAP), 0, 0)
        },
    )
}

internal fun UiKit.switchThumbTintList() = android.content.res.ColorStateList(
    arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_checked), intArrayOf()),
    intArrayOf(ColorPolicy.withAlpha(palette.muted, 0x55), palette.onPrimary, palette.muted),
)

internal fun UiKit.switchTrackTintList() = android.content.res.ColorStateList(
    arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_checked), intArrayOf()),
    intArrayOf(ColorPolicy.withAlpha(palette.muted, 0x24), palette.primary, palette.surfaceVariant),
)
