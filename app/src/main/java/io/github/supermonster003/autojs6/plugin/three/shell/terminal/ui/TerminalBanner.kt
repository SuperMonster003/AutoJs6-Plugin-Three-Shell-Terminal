package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R

/**
 * Binding of `include_terminal_banner.xml`: one explanation line, up to two accent text actions
 * and a close button. The Node.js and storage banners share it.
 * zh-CN: `include_terminal_banner.xml` 的绑定: 一行说明, 至多两个强调色文字动作与一个关闭按钮; Node.js 与存储横幅共用.
 */
internal class TerminalBanner(val root: View, kit: UiKit) {

    private val text: TextView = root.findViewById(R.id.banner_text)
    private val action: TextView = root.findViewById(R.id.banner_action)
    private val secondaryAction: TextView = root.findViewById(R.id.banner_secondary_action)
    private val close: ImageView = root.findViewById(R.id.banner_close)

    /** Set by the owner when the user dismisses the banner; it then stays hidden for this screen. */
    var dismissed: Boolean = false
        private set

    var onDismissed: (() -> Unit)? = null

    init {
        val palette = kit.palette
        root.setBackgroundColor(palette.surface)
        text.setTextColor(palette.muted)
        for (button in listOf(action, secondaryAction)) {
            button.setTextColor(palette.accent)
            kit.selectableBackground(button, borderless = true)
        }
        close.imageTintList = android.content.res.ColorStateList.valueOf(palette.text)
        kit.selectableBackground(close, borderless = true)
        close.setOnClickListener {
            dismissed = true
            root.isVisible = false
            onDismissed?.invoke()
        }
    }

    val isShowing: Boolean get() = root.isVisible

    /**
     * Shows the banner unless the user dismissed it on this screen.
     *
     * @param primary   label and handler of the trailing action, null for none
     * @param secondary label and handler of the action before it, null for none
     */
    fun show(message: CharSequence, primary: Pair<CharSequence, () -> Unit>? = null, secondary: Pair<CharSequence, () -> Unit>? = null) {
        if (dismissed) return
        text.text = message
        bind(action, primary)
        bind(secondaryAction, secondary)
        root.isVisible = true
    }

    fun hide() {
        root.isVisible = false
    }

    /** Lets a later reason show the banner again (for example after a new session started). */
    fun resetDismissal() {
        dismissed = false
    }

    private fun bind(button: TextView, action: Pair<CharSequence, () -> Unit>?) {
        if (action == null) {
            button.isVisible = false
            button.setOnClickListener(null)
        } else {
            button.text = action.first
            button.setOnClickListener { action.second() }
            button.isVisible = true
        }
    }

}
