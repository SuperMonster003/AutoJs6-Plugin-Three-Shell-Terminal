package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalKeySequences.Key
import java.util.Locale

/**
 * Two rows of shell keys with a fixed navigation pad, ported from the host terminal: Esc / Tab,
 * the sticky Ctrl / Alt modifiers, common shell symbols and the cursor keys. Colors come from the
 * screen's [TerminalPalette]; the armed modifiers use the accent tone.
 * zh-CN: 两行 shell 按键与固定导航区 (自宿主迁入): Esc / Tab, 粘滞 Ctrl / Alt, 常用符号与方向键; 颜色来自界面调色板,
 * 已激活的修饰键使用强调色.
 */
class TerminalToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onKey(key: Key)
        fun onText(text: String)
        fun onToggleCtrl()
        fun onToggleAlt()
    }

    var listener: Listener? = null

    private val kit = UiKit.of(context)
    private val palette get() = kit.palette
    private var column: LinearLayout
    private val ctrlButton: TextView
    private val altButton: TextView
    private val buttons = ArrayList<TextView>()

    init {
        orientation = HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        gravity = Gravity.CENTER_VERTICAL
        val symbols = LinearLayout(context).apply { orientation = HORIZONTAL }
        addView(
            HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                isFillViewport = true
                addView(symbols, ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            },
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f),
        )
        column = newColumn(symbols)
        keyButton(Key.ESC)
        keyButton(Key.TAB)
        column = newColumn(symbols)
        textButton("/")
        ctrlButton = button("CTRL") { listener?.onToggleCtrl() }
        column = newColumn(symbols)
        textButton("|")
        altButton = button("ALT") { listener?.onToggleAlt() }
        listOf("-" to "`", "~" to "\\", "$" to "_", "=" to ":", "'" to "\"", "(" to ")", "[" to "]", "{" to "}", "&" to ";", "*" to "?").forEach { (top, bottom) ->
            column = newColumn(symbols)
            textButton(top)
            textButton(bottom)
        }
        column = newColumn(symbols)
        keyButton(Key.DELETE)
        textButton("!")
        addView(View(context).apply { setBackgroundColor(palette.divider) }, LayoutParams(kit.dp(1), LayoutParams.MATCH_PARENT))
        val navigation = LinearLayout(context).apply { orientation = HORIZONTAL }
        addView(navigation, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        listOf(Key.HOME to Key.LEFT, Key.UP to Key.DOWN, Key.END to Key.RIGHT, Key.PAGE_UP to Key.PAGE_DOWN).forEach { (top, bottom) ->
            column = newColumn(navigation)
            keyButton(top)
            keyButton(bottom)
        }
        setModifiers(ctrl = false, alt = false)
    }

    private fun newColumn(parent: LinearLayout) = LinearLayout(context).apply {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        parent.addView(this, LayoutParams(kit.dp(44), LayoutParams.WRAP_CONTENT))
    }

    fun setModifiers(ctrl: Boolean, alt: Boolean) {
        applyModifierState(ctrlButton, ctrl)
        applyModifierState(altButton, alt)
    }

    private fun applyModifierState(button: TextView, active: Boolean) {
        button.isSelected = active
        if (active) {
            button.background = ColorDrawable(palette.accentTone)
            button.setTextColor(palette.accent)
            button.setTypeface(null, Typeface.BOLD)
        } else {
            kit.selectableBackground(button, borderless = true)
            button.setTextColor(palette.text)
            button.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun keyButton(key: Key) = button(key.label.uppercase(Locale.ROOT)) { listener?.onKey(key) }

    private fun textButton(text: String) = button(text) { listener?.onText(text) }

    private fun button(label: String, onClick: () -> Unit): TextView = TextView(context).apply {
        text = label
        gravity = Gravity.CENTER
        isSingleLine = true
        isClickable = true
        isFocusable = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 9, 12, 1, TypedValue.COMPLEX_UNIT_SP)
        setTextColor(palette.text)
        kit.selectableBackground(this, borderless = true)
        contentDescription = label
        minWidth = kit.dp(40)
        val horizontal = kit.dp(2)
        setPadding(horizontal, 0, horizontal, 0)
        layoutParams = LayoutParams(kit.dp(44), kit.dp(40))
        setOnClickListener { onClick() }
        column.addView(this)
        buttons.add(this)
    }

}
