package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalEnvironment
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import jackpal.androidterm.emulatorview.EmulatorView
import jackpal.androidterm.emulatorview.TermSession

/**
 * [EmulatorView] with pinch-to-zoom text size (persisted in [TerminalPreferences]) and the terminal
 * defaults of the plugin; ported from the host terminal (roadmap P3.1).
 * zh-CN: 支持捏合缩放字号 (持久化到 [TerminalPreferences]) 并应用插件终端默认设置的 [EmulatorView]; 自宿主终端迁入.
 */
class TerminalEmulatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : EmulatorView(context, attrs) {

    private val preferences = TerminalPreferences(context)

    var textSizeSp: Int = preferences.textSizeSp
        set(value) {
            val clamped = value.coerceIn(TerminalPreferences.MIN_TEXT_SIZE_SP, TerminalPreferences.MAX_TEXT_SIZE_SP)
            if (field == clamped) return
            field = clamped
            if (termSession != null) {
                setTextSize(clamped)
            }
            preferences.textSizeSp = clamped
            onTextSizeChanged?.invoke(clamped)
        }

    var onTextSizeChanged: ((Int) -> Unit)? = null

    /** Long press at view coordinates; the Activity opens the frozen-transcript selection. zh-CN: 长按坐标, 由 Activity 打开冻结转录的选择层. */
    var onSelectionRequested: ((Float, Float) -> Unit)? = null

    val transcriptTopLine: Int get() = computeVerticalScrollOffset()

    private var scaleAccumulator = 1f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            scaleAccumulator = 1f
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleAccumulator *= detector.scaleFactor
            when {
                scaleAccumulator >= ZOOM_STEP -> {
                    textSizeSp += 1
                    scaleAccumulator = 1f
                }
                scaleAccumulator <= 1f / ZOOM_STEP -> {
                    textSizeSp -= 1
                    scaleAccumulator = 1f
                }
            }
            return true
        }
    })

    init {
        setDensity(resources.displayMetrics)
        setUseCookedIME(false)
    }

    override fun attachSession(session: TermSession) {
        super.attachSession(session)
        // The key listener that owns the term type and the text paints only exist once a session is attached.
        // zh-CN: 持有终端类型的按键监听器与文本画笔在会话附加后才存在.
        setTermType(TerminalEnvironment.DEFAULT_TERM)
        setTextSize(textSizeSp)
    }

    // Taps are dispatched by the base class's GestureDetector (onSingleTapUp shows the keyboard), long presses by
    // onLongPress; only the pinch is intercepted here, so no synthetic performClick() is involved.
    // zh-CN: 点按由基类的 GestureDetector 分发, 长按走 onLongPress; 这里只截获双指缩放, 不涉及 performClick().
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (scaleDetector.isInProgress) {
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onLongPress(event: MotionEvent) {
        onSelectionRequested?.invoke(event.x, event.y)
    }

    companion object {
        private const val ZOOM_STEP = 1.12f
    }

}
