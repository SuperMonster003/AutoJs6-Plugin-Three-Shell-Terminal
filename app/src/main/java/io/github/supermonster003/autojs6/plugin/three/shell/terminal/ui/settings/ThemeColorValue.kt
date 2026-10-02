package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import java.util.Locale

/**
 * Pure input and value policy of the theme color picker (standalone settings specification,
 * section 8): the 16 shared presets, `#RRGGBB` / `RRGGBB` / `rgb(r, g, b)` parsing and the
 * readable text color over a swatch. A draft never touches preferences or the process theme.
 * zh-CN: 主题色选择器的纯输入 / 取值策略 (独立设置页规范第 8 节): 16 个共用预设, `#RRGGBB` / `RRGGBB` /
 * `rgb(r, g, b)` 解析, 以及色块上可读的文字颜色; 草稿不改动偏好或进程主题.
 */
internal object ThemeColorValue {

    val PRESETS: List<Int> = listOf(
        0xFFFFDEAD.toInt(), 0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(),
        0xFF673AB7.toInt(), 0xFF3F51B5.toInt(), 0xFF2196F3.toInt(), 0xFF03A9F4.toInt(),
        0xFF00BCD4.toInt(), 0xFF009688.toInt(), 0xFF4CAF50.toInt(), 0xFF8BC34A.toInt(),
        0xFFFF9800.toInt(), 0xFFFF5722.toInt(), 0xFF795548.toInt(), 0xFF607D8B.toInt(),
    )

    private val HEX = Regex("#?([0-9a-fA-F]{6})")
    private val RGB = Regex("rgb\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*\\)", RegexOption.IGNORE_CASE)
    private const val OPAQUE = 0xFF000000.toInt()

    fun parse(input: String): Int? {
        val text = input.trim()
        HEX.matchEntire(text)?.let { return it.groupValues[1].toInt(16) or OPAQUE }
        val channels = RGB.matchEntire(text)?.groupValues?.drop(1)?.map(String::toInt) ?: return null
        if (channels.any { it !in 0..255 }) return null
        return OPAQUE or (channels[0] shl 16) or (channels[1] shl 8) or channels[2]
    }

    fun hex(color: Int): String = String.format(Locale.ROOT, "#%06X", color and 0xFFFFFF)

    fun rgb(color: Int): String = "rgb(${color ushr 16 and 255}, ${color ushr 8 and 255}, ${color and 255})"

    /** Black or white, whichever reads better over [color] (WCAG relative luminance). */
    fun onColor(color: Int): Int {
        fun linear(value: Int): Double = (value / 255.0).let { if (it <= 0.04045) it / 12.92 else Math.pow((it + 0.055) / 1.055, 2.4) }
        val luminance = 0.2126 * linear(color ushr 16 and 255) + 0.7152 * linear(color ushr 8 and 255) + 0.0722 * linear(color and 255)
        return if ((luminance + 0.05) / 0.05 >= 1.05 / (luminance + 0.05)) OPAQUE else 0xFFFFFFFF.toInt()
    }

}
