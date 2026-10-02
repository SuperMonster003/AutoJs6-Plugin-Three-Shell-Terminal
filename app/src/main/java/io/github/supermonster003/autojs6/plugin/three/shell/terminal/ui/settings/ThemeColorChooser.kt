package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.ColorPolicy
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.ThemeAccentRoles
import kotlin.math.min

/**
 * The theme color picker of the standalone settings specification (section 8): "Follow AutoJs6"
 * with the host color, the 16 shared presets, a HEX / rgb() field with a live preview, and the
 * choose-then-confirm rule: only OK calls [onConfirm] (null means follow AutoJs6), Cancel, Back and
 * outside taps discard the draft. The dialog is 24 dp rounded, at most 560 dp wide and 85% high.
 * zh-CN: 独立设置页规范第 8 节的主题色选择器: 带宿主颜色的 "跟随 AutoJs6", 16 个共用预设, 带实时预览的 HEX /
 * rgb() 输入框, 以及先选后确定: 仅 OK 调用 [onConfirm] (null 表示跟随 AutoJs6), 取消 / 返回 / 点击外部丢弃草稿;
 * 对话框 24 dp 圆角, 最宽 560 dp, 最高 85%.
 */
internal object ThemeColorChooser {

    const val TAG_FOLLOW = "theme-follow"
    const val TAG_CUSTOM = "theme-custom"
    const val TAG_INPUT = "theme-color-input"
    const val TAG_PREVIEW = "theme-color-preview"
    const val TAG_PRESET_PREFIX = "theme-preset-"

    fun show(activity: HostAppearanceActivity, currentColor: Int?, hostColor: Int, onConfirm: (Int?) -> Unit): AlertDialog {
        val kit = activity.kit
        val palette = kit.palette
        val dark = palette.isDark
        fun dp(value: Int) = kit.dp(value)
        val metrics = activity.resources.displayMetrics
        val width = min(dp(560), metrics.widthPixels - dp(48))
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(dp(24), dp(8), dp(24), dp(8))
        }
        val tint = kit.controlTintList()
        fun radio(title: CharSequence, tag: String) = MaterialRadioButton(activity).apply {
            text = title
            this.tag = tag
            setTextColor(palette.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            buttonTintList = tint
            minimumHeight = dp(48)
            setPaddingRelative(0, dp(8), 0, dp(8))
        }
        val followTitle = activity.getString(R.string.app_settings_follow_autojs6)
        val followLabel = SpannableString("$followTitle\n${ThemeColorValue.hex(hostColor)}").apply {
            setSpan(RelativeSizeSpan(14f / 16f), followTitle.length + 1, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(palette.muted), followTitle.length + 1, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val follow = radio(followLabel, TAG_FOLLOW)
        content.addView(follow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.theme_picker_presets)
                setTextColor(palette.muted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPaddingRelative(0, dp(12), 0, dp(8))
            },
        )
        val columns = ((width - dp(48)) / dp(56)).coerceIn(1, 8)
        val grid = GridLayout(activity).apply { columnCount = columns }
        content.addView(grid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val custom = radio(activity.getString(R.string.theme_picker_custom), TAG_CUSTOM)
        content.addView(custom, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val errorTint = ColorStateList.valueOf(palette.danger)
        val field = TextInputLayout(activity).apply {
            hint = activity.getString(R.string.theme_picker_input)
            placeholderText = "#RRGGBB / rgb(r, g, b)"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = palette.surface
            setBoxStrokeColorStateList(
                ColorStateList(
                    arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_focused), intArrayOf()),
                    intArrayOf(palette.outline, palette.accent, palette.muted),
                ),
            )
            defaultHintTextColor = ColorStateList.valueOf(palette.muted)
            hintTextColor = ColorStateList.valueOf(palette.accent)
            setPlaceholderTextColor(ColorStateList.valueOf(palette.muted))
            setErrorTextColor(errorTint)
            setErrorIconTintList(errorTint)
            setBoxStrokeErrorColor(errorTint)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursorColor = ColorStateList.valueOf(palette.accent)
                cursorErrorColor = errorTint
            }
        }
        // Material wraps the field context with its own overlay; below API 29 only this input's cursor and handles are tinted.
        val inputContext: Context = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) LegacyInputTintContext(field.context, palette.accent) else field.context
        val input = TextInputEditText(inputContext).apply {
            tag = TAG_INPUT
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            textDirection = View.TEXT_DIRECTION_LTR
            filters = arrayOf(InputFilter.LengthFilter(64))
            setTextColor(palette.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setText(ThemeColorValue.hex(currentColor ?: hostColor))
            highlightColor = ColorPolicy.withAlpha(palette.accent, 0x55)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) kit.tintEditText(this)
        }
        (inputContext as? LegacyInputTintContext)?.register(input)
        field.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        input.backgroundTintList = null
        content.addView(field, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
        val previewLabel = activity.getString(R.string.theme_picker_preview)
        val preview = MaterialButton(activity).apply {
            tag = TAG_PREVIEW
            text = previewLabel
            isAllCaps = false
            isClickable = false
            isFocusable = false
            cornerRadius = dp(12)
            strokeWidth = dp(1)
            strokeColor = ColorStateList.valueOf(palette.outline)
            minimumHeight = dp(48)
        }
        content.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        val value = TextView(activity).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(palette.muted)
        }
        content.addView(value, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        // AlertDialog owns the title and the fixed button footer; only this content scrolls.
        val scroll = BoundedScrollView(activity, (metrics.heightPixels * 0.85f).toInt() - dp(152)).apply {
            addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        scroll.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop && input.hasFocus()) {
                input.post { if (input.hasFocus()) input.requestRectangleOnScreen(Rect(0, 0, input.width, input.height), true) }
            }
        }
        var followsHost = currentColor == null
        var fromPreset = currentColor != null && currentColor in ThemeColorValue.PRESETS
        var syncingPreset = false
        var selected = currentColor ?: hostColor
        var valid = true
        val swatches = mutableListOf<Pair<Int, MaterialButton>>()
        lateinit var dialog: AlertDialog
        val invalidMessage = activity.getString(R.string.theme_picker_invalid)

        @SuppressLint("SetTextI18n") // HEX / RGB are fixed technical notations, not prose.
        fun render() {
            follow.isChecked = followsHost
            custom.isChecked = !followsHost && !fromPreset
            val color = if (followsHost) hostColor else selected
            val roles = ThemeAccentRoles.fromSeed(color, dark)
            preview.backgroundTintList = ColorStateList.valueOf(roles.primary)
            preview.setTextColor(roles.onPrimary)
            preview.contentDescription = "$previewLabel ${ThemeColorValue.hex(color)}"
            value.text = "${ThemeColorValue.hex(color)}  ${ThemeColorValue.rgb(color)}"
            for ((seed, button) in swatches) {
                button.isChecked = !followsHost && fromPreset && valid && seed == selected
                button.text = if (button.isChecked) "✓" else ""
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = followsHost || valid
        }

        val presetsLabel = activity.getString(R.string.theme_picker_presets)
        for ((index, color) in ThemeColorValue.PRESETS.withIndex()) {
            val swatch = MaterialButton(activity).apply {
                contentDescription = "$presetsLabel ${ThemeColorValue.hex(color)}"
                tag = TAG_PRESET_PREFIX + ThemeColorValue.hex(color)
                isCheckable = true
                minimumWidth = dp(48)
                minimumHeight = dp(48)
                cornerRadius = dp(24)
                insetTop = 0
                insetBottom = 0
                setPadding(0, 0, 0, 0)
                backgroundTintList = ColorStateList.valueOf(color)
                setTextColor(ThemeColorValue.onColor(color))
                strokeWidth = dp(1)
                strokeColor = ColorStateList.valueOf(palette.muted)
                setOnClickListener {
                    syncingPreset = true
                    try {
                        input.setText(ThemeColorValue.hex(color))
                    } finally {
                        syncingPreset = false
                    }
                    followsHost = false
                    fromPreset = true
                    selected = color
                    valid = true
                    field.error = null
                    render()
                }
            }
            swatches += color to swatch
            grid.addView(
                swatch,
                GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(index / columns)
                    columnSpec = GridLayout.spec(index % columns, 1f)
                    this.width = dp(48)
                    this.height = dp(48)
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                    setGravity(Gravity.CENTER)
                },
            )
        }
        dialog = kit.materialDialog()
            .setTitle(R.string.app_settings_theme_color)
            .setView(scroll)
            .setBackground(GradientDrawable().apply { setColor(palette.surface); cornerRadius = kit.dpF(24f) })
            .setBackgroundInsetStart(0).setBackgroundInsetEnd(0)
            .setBackgroundInsetTop(0).setBackgroundInsetBottom(0)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        follow.setOnClickListener {
            followsHost = true
            field.error = null
            render()
        }
        custom.setOnClickListener {
            followsHost = false
            fromPreset = false
            field.error = if (valid) null else invalidMessage
            render()
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (syncingPreset) return
                val parsed = ThemeColorValue.parse(s?.toString().orEmpty())
                followsHost = false
                fromPreset = false
                valid = parsed != null
                if (parsed != null) selected = parsed
                field.error = if (valid) null else invalidMessage
                render()
            }
        })
        fun fitVisibleWindow() {
            val window = dialog.window ?: return
            val visible = Rect().also(activity.window.decorView::getWindowVisibleDisplayFrame)
            val availableHeight = visible.height().takeIf { it > 0 } ?: metrics.heightPixels
            val height = (availableHeight * 0.85f).toInt().coerceAtLeast(1)
            // Follow the visible window (including the keyboard) while the title and footer stay fixed.
            if (window.attributes.width != width || window.attributes.height != height) window.setLayout(width, height)
        }
        val geometryListener = ViewTreeObserver.OnGlobalLayoutListener { fitVisibleWindow() }
        dialog.setOnShowListener {
            kit.tintDialogButtons(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                if (followsHost || valid) {
                    dialog.dismiss()
                    onConfirm(if (followsHost) null else selected)
                }
            }
            dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.apply {
                setTextColor(palette.text)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsMetrics.TEXT_PAGE_TITLE)
            }
            dialog.window?.let { window ->
                // A floating dialog paints no system bar surface; inherit the owner's icon appearance.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    window.insetsController?.setSystemBarsAppearance(activity.window.insetsController?.systemBarsAppearance ?: 0, mask)
                } else {
                    @Suppress("DEPRECATION")
                    val mask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (window.decorView.systemUiVisibility and mask.inv()) or
                        (activity.window.decorView.systemUiVisibility and mask)
                }
                // The visible-frame listener above sizes the dialog; ADJUST_RESIZE still moves the IME out of the way below API 30.
                @Suppress("DEPRECATION")
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            dialog.window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener(geometryListener)
            activity.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(geometryListener)
            fitVisibleWindow()
            render()
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_DESTROY) dialog.dismiss() }
        activity.lifecycle.addObserver(observer)
        dialog.setOnDismissListener {
            activity.lifecycle.removeObserver(observer)
            dialog.window?.decorView?.viewTreeObserver?.takeIf { it.isAlive }?.removeOnGlobalLayoutListener(geometryListener)
            activity.window.decorView.viewTreeObserver.takeIf { it.isAlive }?.removeOnGlobalLayoutListener(geometryListener)
        }
        dialog.show()
        return dialog
    }

    private class BoundedScrollView(context: Context, private val maxHeight: Int) : ScrollView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                return
            }
            val limit = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) maxHeight else min(maxHeight, MeasureSpec.getSize(heightMeasureSpec))
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(limit.coerceAtLeast(1), MeasureSpec.AT_MOST))
        }
    }

}
