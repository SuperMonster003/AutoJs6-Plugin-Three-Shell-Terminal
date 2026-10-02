package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalPalette

/**
 * Minimal offline Markdown for the bundled changelogs, notices and release notes: headings, the
 * `######` date lines, bullets with inline-code category badges, simple tables and horizontal rules.
 * No links, images or HTML are interpreted, so a document can never load anything.
 * zh-CN: 内置更新日志, 声明与发行说明用的最小离线 Markdown 渲染: 标题, `######` 日期行, 带行内代码标签的
 * 列表项, 简单表格与分隔线; 不解释链接, 图片与 HTML, 文档不可能加载任何外部内容.
 */
internal object DocumentText {

    private val RULE = Regex("[ *_-]{3,}")
    private val TABLE_SEPARATOR = Regex(":?-{3,}:?")

    fun render(text: String, palette: TerminalPalette): CharSequence = SpannableStringBuilder().apply {
        var previousBlank = true
        text.lineSequence().forEach { raw ->
            val line = raw.trimEnd()
            if (RULE.matches(line)) return@forEach
            if (line.isBlank()) {
                if (!previousBlank) append('\n')
                previousBlank = true
                return@forEach
            }
            previousBlank = false
            val level = line.takeWhile { it == '#' }.length
            val start = length
            val trimmed = line.trimStart()
            when {
                level in 1..5 -> {
                    append(line.drop(level).trimStart()).append('\n')
                    setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(RelativeSizeSpan(if (level == 1) 1.3f else 1.12f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(palette.text), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                level == 6 -> {
                    append(line.drop(level).trimStart()).append('\n')
                    setSpan(ForegroundColorSpan(palette.muted), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(RelativeSizeSpan(0.9f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                    inline(trimmed.drop(2), palette)
                    append('\n')
                    setSpan(BulletSpan(24, palette.muted), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                trimmed.startsWith("|") -> {
                    val cells = trimmed.trim('|').split('|').map { it.trim() }
                    if (cells.all { TABLE_SEPARATOR.matches(it) }) {
                        previousBlank = true
                        return@forEach
                    }
                    cells.forEachIndexed { index, cell ->
                        if (index > 0) append("  ·  ")
                        inline(cell, palette)
                    }
                    append('\n')
                    setSpan(BulletSpan(24, palette.muted), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else -> {
                    inline(line, palette)
                    append('\n')
                }
            }
        }
    }

    /** `code` segments become medium-weight accent labels (category tags such as `Feature`). */
    private fun SpannableStringBuilder.inline(value: String, palette: TerminalPalette) {
        val parts = value.split('`')
        parts.forEachIndexed { index, part ->
            val start = length
            append(part)
            if (index % 2 == 1 && part.isNotEmpty()) {
                setSpan(ForegroundColorSpan(palette.accent), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(TypefaceSpan("sans-serif-medium"), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

}
