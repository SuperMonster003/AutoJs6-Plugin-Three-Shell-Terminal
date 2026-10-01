package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Small component factory shared by the terminal screens: dp arithmetic, palette-tinted dialogs
 * (24 dp corners, neutral surface, accent text buttons, choose-then-confirm selectors), toasts and
 * the clipboard / share / browse intents the host utilities used to provide.
 * zh-CN: 终端界面共用的小型组件工厂: dp 换算, 按调色板着色的对话框 (24 dp 圆角, 中性表面, 强调色文字按钮,
 * 先选后确定的选择器), toast 以及宿主工具类曾提供的剪贴板 / 分享 / 浏览器 Intent.
 */
internal class UiKit(val context: Context, val palette: TerminalPalette) {

    fun dp(value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

    fun dpF(value: Float): Float = value * context.resources.displayMetrics.density

    fun string(@StringRes resource: Int, vararg args: Any): String = context.getString(resource, *args)

    fun roundedFill(fill: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dpF(radiusDp.toFloat())
    }

    fun tinted(drawable: Drawable, color: Int): Drawable =
        DrawableCompat.wrap(drawable.mutate()).also { DrawableCompat.setTint(it, color) }

    /** The theme's selectable background with the accent ripple. */
    fun selectableBackground(view: View, borderless: Boolean = false) {
        val value = TypedValue()
        val attribute = if (borderless) android.R.attr.selectableItemBackgroundBorderless else android.R.attr.selectableItemBackground
        if (view.context.theme.resolveAttribute(attribute, value, true)) {
            view.background = AppCompatResources.getDrawable(view.context, value.resourceId)?.mutate()?.also {
                (it as? RippleDrawable)?.setColor(ColorStateList.valueOf(palette.accentRipple))
            }
        }
    }

    fun controlTintList(): ColorStateList = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_focused),
            intArrayOf(),
        ),
        intArrayOf(ColorPolicy.withAlpha(palette.muted, 0x66), palette.accent, palette.accent, palette.muted),
    )

    fun tintEditText(editText: EditText) {
        tintTextHandles(editText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            editText.textCursorDrawable?.let { editText.textCursorDrawable = tinted(it, palette.accent) }
        }
    }

    /** Selection highlight and handles of any selectable text view (API 29+ exposes the handles). */
    fun tintTextHandles(view: TextView) {
        view.highlightColor = ColorPolicy.withAlpha(palette.accent, 0x55)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.textSelectHandle?.let { view.setTextSelectHandle(tinted(it, palette.accent)) }
            view.textSelectHandleLeft?.let { view.setTextSelectHandleLeft(tinted(it, palette.accent)) }
            view.textSelectHandleRight?.let { view.setTextSelectHandleRight(tinted(it, palette.accent)) }
        }
    }

    /** Recursively applies the palette to framework and Material controls that were not built here. */
    fun applyThemeToControls(root: View) {
        when (root) {
            is CompoundButton -> root.buttonTintList = controlTintList()
            is CheckedTextView -> root.checkMarkTintList = controlTintList()
            is EditText -> tintEditText(root)
            is CircularProgressIndicator -> {
                root.setIndicatorColor(palette.accent)
                root.trackColor = ColorPolicy.withAlpha(palette.accent, 0x33)
            }
            is ProgressBar -> {
                root.progressTintList = ColorStateList.valueOf(palette.accent)
                root.indeterminateTintList = ColorStateList.valueOf(palette.accent)
            }
        }
        if (root is TextView) {
            root.setLinkTextColor(palette.accent)
            root.highlightColor = ColorPolicy.withAlpha(palette.accent, 0x55)
        }
        if (root is ViewGroup) for (index in 0 until root.childCount) applyThemeToControls(root.getChildAt(index))
    }

    fun tintDialogButtons(dialog: AlertDialog, destructive: Boolean = false) {
        dialog.listView?.let(::applyThemeToControls)
        for (which in listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL)) {
            dialog.getButton(which)?.apply {
                isAllCaps = false
                minHeight = dp(TOUCH_TARGET)
                minWidth = dp(TOUCH_TARGET)
                val active = if (destructive && which == AlertDialog.BUTTON_POSITIVE) palette.danger else palette.accent
                setTextColor(
                    ColorStateList(
                        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                        intArrayOf(ColorPolicy.withAlpha(palette.muted, 0x66), active),
                    ),
                )
                if (this is MaterialButton) rippleColor = ColorStateList.valueOf(palette.accentRipple)
            }
        }
    }

    // ---- dialogs ----------------------------------------------------------------------------

    fun materialDialog(): MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(context)
        .setBackground(roundedFill(palette.surface, DIALOG_RADIUS))

    /** Plain message; [neutral] adds a third action such as "Copy". */
    fun messageDialog(title: CharSequence?, message: CharSequence?, neutral: Pair<CharSequence, () -> Unit>? = null): AlertDialog {
        val builder = materialDialog().setTitle(title).setMessage(message).setPositiveButton(android.R.string.ok, null)
        neutral?.let { (label, action) -> builder.setNeutralButton(label) { _, _ -> action() } }
        return builder.show().also { tintDialogButtons(it) }
    }

    /** Confirmation; a destructive positive action is colored with the danger tone. */
    fun confirmDialog(
        title: CharSequence?,
        message: CharSequence?,
        positive: CharSequence,
        destructive: Boolean = false,
        onPositive: () -> Unit,
    ): AlertDialog = materialDialog()
        .setTitle(title)
        .setMessage(message)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(positive) { _, _ -> onPositive() }
        .show().also { tintDialogButtons(it, destructive) }

    /** Non-cancelable indeterminate progress with one line of text. */
    fun progressDialog(message: CharSequence): AlertDialog {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPaddingRelative(dp(24), dp(20), dp(24), dp(20))
            addView(
                CircularProgressIndicator(context).apply {
                    isIndeterminate = true
                    indicatorSize = dp(32)
                    trackThickness = dp(3)
                    setIndicatorColor(palette.accent)
                    trackColor = ColorPolicy.withAlpha(palette.accent, 0x33)
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(20) },
            )
            addView(
                TextView(context).apply {
                    text = message
                    textSize = 16f
                    setTextColor(palette.text)
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
        }
        return materialDialog().setView(row).setCancelable(false).show()
    }

    /**
     * One text field; [validate] returns an error to show inline or null to accept. Only the
     * positive action submits; cancel, back and outside taps discard the draft.
     */
    fun inputDialog(
        title: CharSequence,
        hint: CharSequence?,
        value: CharSequence? = null,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        validate: (String) -> CharSequence? = { null },
        onSubmit: (String) -> Unit,
    ): AlertDialog {
        val edit = TextInputEditText(context).apply {
            setText(value)
            this.inputType = inputType
            isSingleLine = true
            textSize = 16f
            setTextColor(palette.text)
            setHintTextColor(palette.muted)
            tintEditText(this)
        }
        val layout = TextInputLayout(context).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxStrokeColorStateList(
                ColorStateList(arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()), intArrayOf(palette.accent, palette.outline)),
            )
            boxStrokeErrorColor = ColorStateList.valueOf(palette.danger)
            setErrorTextColor(ColorStateList.valueOf(palette.danger))
            defaultHintTextColor = ColorStateList.valueOf(palette.muted)
            hintTextColor = ColorStateList.valueOf(palette.accent)
            this.hint = hint
            addView(edit)
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(dp(24), dp(8), dp(24), 0)
            addView(layout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        val dialog = materialDialog().setTitle(title).setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            tintDialogButtons(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val text = edit.text?.toString().orEmpty()
                val problem = validate(text)
                if (problem != null) {
                    layout.error = problem
                } else {
                    dialog.dismiss()
                    onSubmit(text)
                }
            }
            edit.requestFocus()
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        return dialog
    }

    /** Action list (npm scripts, package managers): tapping a row performs it at once. */
    fun actionListDialog(title: CharSequence, message: CharSequence?, labels: List<CharSequence>, onSelect: (Int) -> Unit): AlertDialog {
        val builder = materialDialog().setTitle(title)
        if (message == null) {
            builder.setAdapter(ChoiceAdapter(this, labels, radio = false)) { _, index -> onSelect(index) }
        } else {
            // setMessage and a list cannot share an AlertDialog; render the explanation as a header of the list.
            val list = ListView(context).apply {
                divider = null
                addHeaderView(
                    TextView(context).apply {
                        text = message
                        textSize = 14f
                        setTextColor(palette.muted)
                        setLineSpacing(0f, 1.15f)
                        setPaddingRelative(dp(24), dp(4), dp(24), dp(12))
                    },
                    null,
                    false,
                )
                adapter = ChoiceAdapter(this@UiKit, labels, radio = false)
            }
            builder.setView(list)
            val dialog = builder.setNegativeButton(android.R.string.cancel, null).show().also { tintDialogButtons(it) }
            list.setOnItemClickListener { _, _, position, _ ->
                val index = position - list.headerViewsCount
                if (index >= 0) {
                    dialog.dismiss()
                    onSelect(index)
                }
            }
            return dialog
        }
        return builder.setNegativeButton(android.R.string.cancel, null).show().also { tintDialogButtons(it) }
    }

    /**
     * Settings selector with the choose-then-confirm semantics of the standalone settings
     * specification (section 8): the row only moves the draft, OK applies it once.
     */
    fun confirmedChoiceDialog(title: CharSequence, labels: List<CharSequence>, checked: Int, onConfirm: (Int) -> Unit): AlertDialog {
        var draft = checked
        val dialog = materialDialog().setTitle(title)
            .setSingleChoiceItems(ChoiceAdapter(this, labels, radio = true), checked) { _, index -> draft = index }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm(draft) }
            .show()
        tintDialogButtons(dialog)
        val metrics = context.resources.displayMetrics
        val width = minOf(dp(560), metrics.widthPixels - dp(48))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.decorView?.post {
            val maximum = (metrics.heightPixels * 0.85f).toInt()
            if ((dialog.window?.decorView?.height ?: 0) > maximum) dialog.window?.setLayout(width, maximum)
        }
        dialog.listView?.post { dialog.listView?.setSelection(0) }
        return dialog
    }

    /** Rows of the choice dialogs: a 56 / 72 dp minimum, wrapping labels and the palette radio indicator. */
    private class ChoiceRow(context: Context, radio: Boolean) : LinearLayout(context), android.widget.Checkable {
        val indicator: MaterialRadioButton? = if (radio) MaterialRadioButton(context).apply {
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            minimumWidth = 0
            minimumHeight = 0
        } else null
        val label = TextView(context).apply {
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            textSize = 16f
            setLineSpacing(0f, 1.08f)
        }

        init {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            indicator?.let(::addView)
            addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }

        override fun isChecked() = indicator?.isChecked == true
        override fun setChecked(value: Boolean) {
            indicator?.isChecked = value
        }

        override fun toggle() {
            isChecked = !isChecked
        }
    }

    private class ChoiceAdapter(private val kit: UiKit, labels: List<CharSequence>, private val radio: Boolean) :
        ArrayAdapter<CharSequence>(kit.context, android.R.layout.simple_list_item_1, labels) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView as? ChoiceRow) ?: ChoiceRow(kit.context, radio)
            row.layoutParams = android.widget.AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val label = getItem(position)
            row.minimumHeight = kit.dp(if (label?.contains('\n') == true) 72 else 56)
            row.setPaddingRelative(kit.dp(24), kit.dp(12), kit.dp(24), kit.dp(12))
            row.indicator?.let { indicator ->
                indicator.layoutParams = LinearLayout.LayoutParams(kit.dp(32), kit.dp(32)).apply { marginEnd = kit.dp(8) }
                indicator.buttonTintList = kit.controlTintList()
            }
            row.label.text = label
            row.label.setTextColor(kit.palette.text)
            row.contentDescription = label
            row.isChecked = (parent as? ListView)?.isItemChecked(position) == true
            kit.selectableBackground(row)
            return row
        }
    }

    // ---- feedback and intents ---------------------------------------------------------------

    fun toast(@StringRes resource: Int, long: Boolean = false) = toast(context.getString(resource), long)

    fun toast(text: CharSequence, long: Boolean = false) {
        Toast.makeText(context.applicationContext, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    companion object {

        const val TOUCH_TARGET = 48
        const val DIALOG_RADIUS = 24

        /** The screen's kit when [context] belongs to a [HostAppearanceActivity]; otherwise one from the current appearance. */
        fun of(context: Context): UiKit {
            var current: Context? = context
            while (current != null) {
                if (current is HostAppearanceActivity) return current.kit
                current = (current as? ContextWrapper)?.baseContext?.takeIf { it !== current }
            }
            val appearance = Appearance.resolve(context)
            val palette = TerminalPalette.resolve(context, appearance)
            return UiKit(materialContext(context, appearance), palette)
        }

        /** Material widgets need a Material theme; application and configuration contexts carry a platform one. */
        private fun materialContext(context: Context, appearance: Appearance): Context {
            val attributes = context.theme.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorPrimaryVariant))
            val material = attributes.hasValue(0)
            attributes.recycle()
            return if (material) context else ContextThemeWrapper(appearance.wrap(context), R.style.Theme_ThreeShellTerminal)
        }

    }

}

/** Clipboard access without logging the content (AGENTS.md 15). */
internal object Clipboard {

    fun set(context: Context, text: CharSequence) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        manager.setPrimaryClip(ClipData.newPlainText("text", text))
    }

    fun get(context: Context): CharSequence? {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        return manager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)
    }

}

/** Outgoing intents that must never crash the terminal when no handler exists. */
internal object ExternalIntents {

    fun shareText(context: Context, text: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        return startSafely(context, Intent.createChooser(send, null))
    }

    fun browse(context: Context, url: String): Boolean =
        startSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun startSafely(context: Context, intent: Intent): Boolean {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

}

/** Blocking work off the main thread with the result delivered on it; the owner checks its own lifecycle. */
internal object BackgroundWork {

    private val executor: ExecutorService = Executors.newCachedThreadPool { runnable -> Thread(runnable, "TerminalUi").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())

    fun <T> run(work: () -> T, onDone: (Result<T>) -> Unit) {
        executor.execute {
            val result = runCatching(work)
            main.post { onDone(result) }
        }
    }

    fun post(action: () -> Unit) {
        main.post(action)
    }

}
