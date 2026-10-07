package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.graphics.Typeface
import android.os.SystemClock
import android.text.Selection
import android.text.Spannable
import android.util.TypedValue
import android.view.ActionMode
import android.view.InputDevice
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import jackpal.androidterm.emulatorview.TerminalSelectionSnapshot

/**
 * Native selection handles on a frozen transcript, restored to the live terminal on dismissal;
 * ported from the host terminal. The snapshot keeps logical lines, so a copied range matches what
 * the shell printed even while new output arrives underneath.
 * zh-CN: 在冻结的转录文本上使用原生选择手柄, 关闭后回到实时终端 (自宿主迁入); 快照保留逻辑行, 新输出不影响已选范围.
 */
internal class TerminalTextSelection(private val terminal: TerminalEmulatorView, private val onClosed: () -> Unit) {

    private val context = terminal.context
    private val kit = UiKit.of(context)
    private val parent = terminal.parent as FrameLayout
    private val scroll = ScrollView(context)
    private var actionMode: ActionMode? = null
    private var dismissed = false
    private var selectionText: TextView? = null
    private var selectionSnapshot: TerminalSelectionSnapshot? = null

    fun show(x: Float, y: Float) {
        val session = terminal.termSession ?: return dismiss()
        if (!session.isRunning) return dismiss()
        val palette = kit.palette
        val snapshot = TerminalSelectionSnapshot.capture(session, terminal.visibleColumns, terminal.transcriptTopLine)
        val text = AppCompatTextView(context).apply {
            id = R.id.terminal_selection
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, terminal.textSizeSp.toFloat())
            setTextColor(palette.terminalForeground)
            setBackgroundColor(palette.terminalBackground)
            highlightColor = ColorPolicy.withAlpha(palette.accent, 0x4D)
            includeFontPadding = false
            setHorizontallyScrolling(true)
            setTextIsSelectable(true)
            setText(snapshot.text, TextView.BufferType.SPANNABLE)
            kit.tintTextHandles(this)
            customSelectionActionModeCallback = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    actionMode = mode
                    menu.clear()
                    menu.add(Menu.NONE, android.R.id.copy, 0, android.R.string.copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    menu.add(Menu.NONE, android.R.id.selectAll, 1, android.R.string.selectAll).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = perform(item.itemId)

                override fun onDestroyActionMode(mode: ActionMode) {
                    actionMode = null
                    post { dismiss() }
                }
            }
        }
        selectionText = text
        selectionSnapshot = snapshot
        scroll.setBackgroundColor(palette.terminalBackground)
        scroll.isFillViewport = true
        scroll.addView(text, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        parent.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        text.post {
            if (dismissed) return@post
            val layout = text.layout ?: return@post
            val top = layout.getLineTop(snapshot.topLine.coerceAtMost(layout.lineCount - 1))
            scroll.scrollTo(0, top)
            text.requestFocus()
            val now = SystemClock.uptimeMillis()
            val localY = (y + scroll.scrollY).coerceIn(0f, (text.height - 1).toFloat())
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(now, now, action, x, localY, 0)
                // The editor shows handles and its toolbar only for touchscreen input; obtain() reports a bare pointer.
                // zh-CN: 编辑器只为触屏输入显示手柄与工具栏; obtain() 生成的事件来源仅为通用指针.
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                text.dispatchTouchEvent(event)
                event.recycle()
                // End the synthetic long press with an UP so Android shows its handles and toolbar.
                // zh-CN: 以 UP 事件结束合成的长按, 让系统显示手柄与工具栏.
                if (action == MotionEvent.ACTION_DOWN) text.performLongClick()
            }
        }
    }

    /** True while Android shows the selection handles and the copy / select-all toolbar. */
    internal val isActionModeShown: Boolean get() = actionMode != null

    /**
     * Runs one toolbar action ([android.R.id.copy] or [android.R.id.selectAll]); the action mode's callback and the
     * instrumentation share this path, so a test exercises the same copy as a tap on the toolbar.
     * zh-CN: 执行一个工具栏动作; 选择模式回调与 instrumentation 共用此路径.
     */
    internal fun perform(itemId: Int): Boolean {
        val text = selectionText ?: return false
        val snapshot = selectionSnapshot ?: return false
        return when (itemId) {
            android.R.id.copy -> {
                Clipboard.copyAndReport(context, snapshot.selectedText(text.selectionStart, text.selectionEnd))
                actionMode?.finish() ?: dismiss()
                true
            }
            android.R.id.selectAll -> {
                Selection.setSelection(text.text as Spannable, 0, text.length())
                actionMode?.invalidate()
                true
            }
            else -> false
        }
    }

    fun dismiss() {
        if (dismissed) return
        dismissed = true
        actionMode?.finish()
        parent.removeView(scroll)
        terminal.requestFocus()
        onClosed()
    }

}
