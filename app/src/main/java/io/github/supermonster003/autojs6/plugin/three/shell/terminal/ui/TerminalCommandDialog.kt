package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.text.InputType
import android.app.Activity
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalCommand
import jackpal.androidterm.emulatorview.TerminalTypeface

/** A wrapped, selectable command editor. Only an explicit action sends any text to a shell. */
internal object TerminalCommandDialog {
    fun show(kit: UiKit, value: String, onPaste: (String) -> Boolean, onRun: (String) -> Unit): AlertDialog {
        val context = kit.context
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(kit.dp(24), kit.dp(8), kit.dp(24), 0)
        }
        content.addView(TextView(context).apply {
            text = context.getString(R.string.terminal_command_hint)
            setTextColor(kit.palette.muted)
            textSize = 13f
        })
        val edit = EditText(context).apply {
            id = R.id.terminal_command_editor
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_NONE
            typeface = TerminalTypeface.get()
            textSize = 14f
            gravity = Gravity.TOP or Gravity.START
            textDirection = View.TEXT_DIRECTION_LTR
            setHorizontallyScrolling(false)
            minLines = 6
            maxLines = 14
            setTextColor(kit.palette.text)
            setText(value)
            setSelection(0)
            // Drafts live only in this dialog; do not put command contents into Activity saved state.
            isSaveEnabled = false
            kit.tintEditText(this)
        }
        content.addView(edit, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val scroll = ScrollView(context).apply { addView(content) }
        // This dialog places and sizes its own window. The builder's default background insets (80dp above and
        // below the card) would only shrink the visible card inside it: on a short screen with the keyboard up
        // they left no room for the editor and clipped the buttons.
        val dialog = kit.materialDialog().setBackgroundInsetTop(0).setBackgroundInsetBottom(0)
            .setTitle(R.string.terminal_command_editor)
            .setView(scroll).setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.terminal_command_paste, null)
            .setPositiveButton(R.string.terminal_command_run, null).create()
        dialog.setOnShowListener {
            kit.tintDialogButtons(dialog)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            val window = dialog.window!!
            val decor = window.decorView
            val host = (context as Activity).window.decorView
            val resize = ViewTreeObserver.OnGlobalLayoutListener {
                val insets = ViewCompat.getRootWindowInsets(host)
                val bars = insets?.getInsets(WindowInsetsCompat.Type.systemBars())
                val ime = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                val screenHeight = maxOf(context.resources.displayMetrics.heightPixels, host.height)
                val available = screenHeight - maxOf(ime, bars?.bottom ?: 0) - (bars?.top ?: 0)
                // The card wraps its content, at most from 16dp below the status bar to 32dp above the keyboard or navigation bar.
                val maxHeight = (available - kit.dp(48)).coerceAtLeast(kit.dp(160)).coerceAtMost(kit.dp(600))
                val width = (context.resources.displayMetrics.widthPixels - kit.dp(32)).coerceAtMost(kit.dp(560))
                // A floating window is laid out below the status bar, so the offset already starts there.
                val y = kit.dp(16)
                val gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                val attributes = window.attributes
                if (attributes.height != ViewGroup.LayoutParams.WRAP_CONTENT || attributes.width != width || attributes.y != y || attributes.gravity != gravity) {
                    window.attributes = attributes.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        this.width = width
                        this.y = y
                        this.gravity = gravity
                    }
                    return@OnGlobalLayoutListener
                }
                val panel = decor.findViewById<View>(androidx.appcompat.R.id.parentPanel) ?: return@OnGlobalLayoutListener
                // AlertDialogLayout shrinks the buttons before the editor when space runs out; count their full height.
                val buttons = decor.findViewById<ViewGroup>(androidx.appcompat.R.id.buttonPanel)
                val squeezed = buttons?.getChildAt(0)?.let { buttons.paddingTop + it.height + buttons.paddingBottom - buttons.height }?.coerceAtLeast(0) ?: 0
                val visible = Rect().let { if (scroll.getLocalVisibleRect(it)) it.height() else 0 }
                val chrome = panel.height - visible + squeezed
                val height = minOf(content.height, maxHeight - chrome).coerceAtLeast(kit.dp(48))
                if (scroll.layoutParams.height != height) scroll.layoutParams = scroll.layoutParams.apply { this.height = height }
            }
            decor.viewTreeObserver.addOnGlobalLayoutListener(resize)
            host.viewTreeObserver.addOnGlobalLayoutListener(resize)
            dialog.setOnDismissListener {
                decor.viewTreeObserver.removeOnGlobalLayoutListener(resize)
                host.viewTreeObserver.removeOnGlobalLayoutListener(resize)
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                if (onPaste(edit.text.toString())) dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val command = TerminalCommand.normalize(edit.text.toString())
                if (!TerminalCommand.isValid(command)) {
                    edit.error = context.getString(R.string.terminal_command_invalid)
                } else {
                    onRun(command)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
        return dialog
    }

    fun view(kit: UiKit, command: String): AlertDialog {
        val text = TextView(kit.context).apply {
            id = R.id.terminal_command_record
            this.text = command
            typeface = TerminalTypeface.get()
            textSize = 14f
            textDirection = View.TEXT_DIRECTION_LTR
            setTextColor(kit.palette.text)
            setTextIsSelectable(true)
            setPadding(kit.dp(24), kit.dp(12), kit.dp(24), kit.dp(12))
            kit.tintTextHandles(this)
        }
        val scroll = ScrollView(kit.context).apply { addView(text) }
        return kit.materialDialog().setTitle(R.string.terminal_command_view).setView(scroll)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(android.R.string.copy) { _, _ -> Clipboard.copyAndReport(kit.context, command) }
            .show().also { kit.tintDialogButtons(it) }
    }
}
