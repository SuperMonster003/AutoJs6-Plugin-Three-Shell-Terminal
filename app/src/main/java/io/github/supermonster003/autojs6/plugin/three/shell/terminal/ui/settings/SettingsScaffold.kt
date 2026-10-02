package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.HostAppearanceActivity
import kotlin.math.max

/** The shell of a standalone screen: background column, toolbar and the scrolling content column. */
internal class Scaffold(val root: LinearLayout, val toolbar: Toolbar, val scroll: NestedScrollView, val content: LinearLayout)

/** A column measured no wider than [SettingsMetrics.MAX_CONTENT_WIDTH], centered by its parent. */
internal class BoundedColumn(context: Context) : LinearLayout(context) {

    private val maximum = (SettingsMetrics.MAX_CONTENT_WIDTH * context.resources.displayMetrics.density).toInt()

    init {
        orientation = VERTICAL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        val bounded = if (size > maximum) MeasureSpec.makeMeasureSpec(maximum, MeasureSpec.getMode(widthMeasureSpec)) else widthMeasureSpec
        super.onMeasure(bounded, heightMeasureSpec)
    }

}

/**
 * Builds the shared screen shell (standalone settings specification, sections 4 and 12): a status
 * bar spacer, a toolbar wired to [HostAppearanceActivity.navigateBack], and a scrolling content
 * column that pads for the side system bars, the cutout and the larger of navigation bar and
 * keyboard. [horizontalPaddingDp] is 0 for settings rows (they pad themselves) and the screen
 * margin for prose.
 * zh-CN: 共用的界面外壳: 状态栏占位, 接到 [HostAppearanceActivity.navigateBack] 的工具栏, 以及为系统栏 / 开孔 /
 * 键盘留白的滚动内容列; 设置行自带左右留白时 [horizontalPaddingDp] 为 0, 文档类界面取屏幕留白.
 */
internal fun HostAppearanceActivity.buildScaffold(title: CharSequence, horizontalPaddingDp: Int = 0): Scaffold {
    val kit = kit
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(kit.palette.background)
    }
    val statusBar = View(this).apply { setBackgroundColor(kit.palette.background) }
    root.addView(statusBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0))
    val toolbar = Toolbar(this).apply {
        this.title = title
        setBackgroundColor(kit.palette.background)
        setTitleTextAppearance(this@buildScaffold, R.style.TextAppearance_ThreeShellTerminal_ToolbarTitle)
        setTitleTextColor(kit.palette.text)
        minimumHeight = actionBarHeight()
        setContentInsetsRelative(kit.dp(16), kit.dp(8))
        navigationIcon = kit.tintedDrawable(R.drawable.ic_settings_back, kit.palette.text)
        setNavigationContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description)
        setNavigationOnClickListener { navigateBack() }
        overflowIcon = overflowIcon?.let { kit.tinted(it, kit.palette.text) }
    }
    root.addView(toolbar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayShowTitleEnabled(true)
    val content = BoundedColumn(this).apply {
        setPaddingRelative(kit.dp(horizontalPaddingDp), kit.dp(8), kit.dp(horizontalPaddingDp), kit.dp(SettingsMetrics.SECTION_TOP))
    }
    val centered = FrameLayout(this).apply {
        addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL))
    }
    val scroll = NestedScrollView(this).apply {
        isFillViewport = true
        addView(centered, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
    applySystemBarInsets(root, statusBar)
    return Scaffold(root, toolbar, scroll, content)
}

private fun Activity.actionBarHeight(): Int {
    val value = TypedValue()
    return if (theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, value, true) && value.type == TypedValue.TYPE_DIMENSION) {
        TypedValue.complexToDimensionPixelSize(value.data, resources.displayMetrics)
    } else {
        (56 * resources.displayMetrics.density).toInt()
    }
}

/** Edge to edge: the spacer takes the status bar, [root] pads for the side bars and the larger of navigation bar and keyboard. */
@Suppress("DEPRECATION")
internal fun Activity.applySystemBarInsets(root: View, statusBar: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
    } else {
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
    root.setOnApplyWindowInsetsListener { _, insets ->
        val left: Int
        val top: Int
        val right: Int
        val bottom: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val system = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsets.Type.ime())
            left = system.left; top = system.top; right = system.right; bottom = max(system.bottom, keyboard.bottom)
        } else {
            left = insets.systemWindowInsetLeft; top = insets.systemWindowInsetTop
            right = insets.systemWindowInsetRight; bottom = insets.systemWindowInsetBottom
        }
        root.setPadding(left, 0, right, bottom)
        statusBar.layoutParams?.let { params ->
            if (params.height != top) {
                params.height = top
                statusBar.layoutParams = params
            }
        }
        insets
    }
    root.requestApplyInsets()
    root.post(root::requestApplyInsets)
}
