package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import jackpal.androidterm.emulatorview.TerminalTypeface

/**
 * The terminal font (roadmap P8): the system monospace face or the bundled JetBrains Mono NL (SIL Open Font
 * License 1.1; the variant without ligatures, so every character keeps its own cell). The emulator renderer,
 * the selection overlay and the command output screen all draw with the same face.
 * zh-CN: 终端字体 (P8): 系统等宽字体或内置的 JetBrains Mono NL (SIL OFL 1.1; 无连字变体, 每个字符保持各自的单元格).
 * 模拟器渲染器, 选择层与命令输出页使用同一字体.
 */
internal object TerminalFonts {

    fun typeface(context: Context, font: String = TerminalPreferences(context).font): Typeface = when (font) {
        TerminalPreferences.FONT_JETBRAINS_MONO -> runCatching { ResourcesCompat.getFont(context, R.font.jetbrains_mono_nl_regular) }.getOrNull() ?: Typeface.MONOSPACE
        else -> Typeface.MONOSPACE
    }

    /** Makes the stored choice the renderer default; true when it changed. zh-CN: 把保存的选择设为渲染器默认字体; 变化时返回 true. */
    fun apply(context: Context): Boolean {
        val next = typeface(context)
        if (TerminalTypeface.get() === next) return false
        TerminalTypeface.set(next)
        return true
    }

}
