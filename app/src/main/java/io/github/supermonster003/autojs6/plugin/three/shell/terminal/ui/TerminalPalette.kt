package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.TonalPalette
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** WCAG 2 contrast arithmetic on packed ARGB ints; no Android types so the JVM tests can run it. */
internal object ColorPolicy {

    const val MINIMUM_TEXT_CONTRAST = 4.5
    private const val OPAQUE_BLACK = 0xFF000000.toInt()
    private const val OPAQUE_WHITE = 0xFFFFFFFF.toInt()

    fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val value = (color shr shift and 0xFF) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun contrastRatio(first: Int, second: Int): Double {
        val lighter = max(luminance(first), luminance(second))
        val darker = min(luminance(first), luminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** The higher-contrast opaque foreground for a filled control. */
    fun onFilledColor(background: Int): Int =
        if (contrastRatio(OPAQUE_BLACK, background) >= contrastRatio(OPAQUE_WHITE, background)) OPAQUE_BLACK else OPAQUE_WHITE

    /** Keeps the hue and moves toward black or white only as far as needed for 4.5:1 text on [background]. */
    fun readableAccent(color: Int, background: Int): Int {
        val opaque = color or OPAQUE_BLACK
        if (contrastRatio(opaque, background) >= MINIMUM_TEXT_CONTRAST) return opaque
        fun toward(target: Int): Int? {
            if (contrastRatio(target, background) < MINIMUM_TEXT_CONTRAST) return null
            var low = 0.0
            var high = 1.0
            repeat(18) {
                val middle = (low + high) / 2.0
                if (contrastRatio(blend(opaque, target, middle), background) >= MINIMUM_TEXT_CONTRAST) high = middle else low = middle
            }
            return blend(opaque, target, high)
        }
        val source = luminance(opaque)
        return listOfNotNull(toward(OPAQUE_BLACK), toward(OPAQUE_WHITE)).minByOrNull { abs(luminance(it) - source) }
            ?: onFilledColor(background)
    }

    fun withAlpha(color: Int, alpha: Int): Int = color and 0xFFFFFF or (alpha.coerceIn(0, 255) shl 24)

    /** Opaque linear blend; [ratio] 0 keeps [first], 1 yields [second]. */
    fun blend(first: Int, second: Int, ratio: Double): Int {
        fun channel(shift: Int): Int {
            val start = first shr shift and 0xFF
            val end = second shr shift and 0xFF
            return (start + (end - start) * ratio).toInt().coerceIn(0, 255)
        }
        return OPAQUE_BLACK or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

}

/**
 * The HCT accent policy every standalone plugin shares (standalone settings specification, section
 * 6): chroma below 4 collapses to 0, otherwise it is raised to at least 48 and capped at 96; the
 * light primary / onPrimary tones are 40 / 100, the dark ones 80 / 20.
 * zh-CN: 全部独立插件共用的 HCT 强调色规则 (独立设置页规范第 6 节).
 */
internal object ThemeAccentRoles {

    data class Roles(val primary: Int, val onPrimary: Int)

    @SuppressLint("RestrictedApi") // The pinned Material dependency embeds Material Color Utilities.
    fun fromSeed(seed: Int, dark: Boolean): Roles {
        val source = Hct.fromInt(seed or 0xFF000000.toInt())
        val chroma = if (source.chroma < 4.0) 0.0 else max(source.chroma, 48.0).coerceAtMost(96.0)
        val tones = TonalPalette.fromHueAndChroma(source.hue, chroma)
        return Roles(
            primary = tones.tone(if (dark) 80 else 40),
            onPrimary = tones.tone(if (dark) 20 else 100),
        )
    }

}

/**
 * The terminal's own foreground / background pair per color scheme (roadmap P8). The theme-tinted
 * scheme keeps the hue of the theme color at low chroma (neutral for gray seeds) and stays far above
 * 7:1 in both modes.
 * zh-CN: 各配色方案下终端自身的前景 / 背景 (P8). 主题色方案保留主题色色相并取低彩度 (灰色种子为中性), 两种模式下对比度都远高于 7:1.
 */
internal object TerminalSchemeColors {

    const val THEME_CHROMA = 16.0

    data class Pair(val background: Int, val foreground: Int)

    @SuppressLint("RestrictedApi")
    fun theme(seed: Int, dark: Boolean): Pair {
        val source = Hct.fromInt(seed or 0xFF000000.toInt())
        val chroma = if (source.chroma < 4.0) 0.0 else THEME_CHROMA
        val background = TonalPalette.fromHueAndChroma(source.hue, chroma).tone(if (dark) 10 else 96)
        val foreground = TonalPalette.fromHueAndChroma(source.hue, chroma / 2).tone(if (dark) 92 else 14)
        return Pair(background, foreground)
    }

}

/**
 * Runtime colors of the terminal screens: neutral surfaces from `colors.xml` in the requested night
 * mode plus an accent derived from the followed or chosen theme color and pushed to 4.5:1 against
 * every surface it is drawn on. The emulator keeps its own foreground / background pair, chosen by
 * the terminal color scheme ([scheme], roadmap P8).
 * zh-CN: 终端界面的运行时配色: 中性表面来自对应夜间模式的 `colors.xml`, 强调色由跟随或自选的主题色派生并保证
 * 对每个承载表面都达到 4.5:1; 模拟器保留自己的前景 / 背景.
 */
internal data class TerminalPalette(
    val primary: Int,
    val onPrimary: Int,
    val accent: Int,
    val onAccent: Int,
    val background: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val outline: Int,
    val divider: Int,
    val text: Int,
    val muted: Int,
    val danger: Int,
    val terminalBackground: Int,
    val terminalForeground: Int,
    val isDark: Boolean,
    val scheme: String = TerminalPreferences.COLOR_SCHEME_FOLLOW,
) {

    /** Translucent accent fill of the armed Ctrl / Alt keys and tonal controls. */
    val accentTone: Int get() = ColorPolicy.withAlpha(accent, TONE_ALPHA)

    val accentRipple: Int get() = ColorPolicy.withAlpha(accent, 0x2E)

    companion object {

        const val TONE_ALPHA = 0x30

        /** Below this relative luminance black text reads worse than white text (equal contrast at about 0.18). */
        private const val DARK_LUMINANCE = 0.18

        private data class Key(val seed: Int, val accentSeed: Int, val dark: Boolean, val scheme: String)

        private val cache = ConcurrentHashMap<Key, TerminalPalette>()
        private val neutrals = ConcurrentHashMap<Boolean, IntArray>()

        fun isDark(context: Context, appearance: Appearance?): Boolean = appearance?.dark
            ?: (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)

        fun resolve(context: Context, appearance: Appearance?): TerminalPalette {
            val dark = isDark(context, appearance)
            val seed = (appearance?.primarySeed ?: Appearance.DEFAULT_COLOR) or 0xFF000000.toInt()
            val accentSeed = (appearance?.accentSeed ?: seed) or 0xFF000000.toInt()
            val scheme = TerminalPreferences(context).colorScheme
            return cache.getOrPut(Key(seed, accentSeed, dark, scheme)) {
                val terminal = when (scheme) {
                    TerminalPreferences.COLOR_SCHEME_DARK -> neutralColors(context, true).let { TerminalSchemeColors.Pair(it[8], it[9]) }
                    TerminalPreferences.COLOR_SCHEME_LIGHT -> neutralColors(context, false).let { TerminalSchemeColors.Pair(it[8], it[9]) }
                    TerminalPreferences.COLOR_SCHEME_THEME -> TerminalSchemeColors.theme(seed, dark)
                    else -> null
                }
                build(neutralColors(context, dark), seed, dark, accentSeed, terminal, scheme)
            }
        }

        /** Static neutrals of the requested night mode, read once per mode from the resources. */
        private fun neutralColors(context: Context, dark: Boolean): IntArray = neutrals.getOrPut(dark) {
            val current = context.resources.configuration
            val matches = (current.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) == dark
            val resources = if (matches) context else context.createConfigurationContext(
                Configuration(current).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                },
            )
            intArrayOf(
                R.color.window_background, R.color.surface, R.color.surface_variant, R.color.outline, R.color.divider,
                R.color.text_color_primary, R.color.text_color_secondary, R.color.status_danger,
                R.color.terminal_background, R.color.terminal_foreground,
            ).map(resources::getColor).toIntArray()
        }

        /**
         * Pure construction from the ten neutrals (JVM tests feed the specification values directly); [terminal]
         * replaces the neutral terminal pair for a non-following color scheme.
         */
        internal fun build(
            neutral: IntArray,
            seed: Int,
            dark: Boolean,
            accentSeed: Int = seed,
            terminal: TerminalSchemeColors.Pair? = null,
            scheme: String = TerminalPreferences.COLOR_SCHEME_FOLLOW,
        ): TerminalPalette {
            val roles = ThemeAccentRoles.fromSeed(seed, dark)
            val accentRoles = ThemeAccentRoles.fromSeed(accentSeed, dark)
            val background = neutral[0]
            val surface = neutral[1]
            val surfaceVariant = neutral[2]
            val terminalBackground = terminal?.background ?: neutral[8]
            var accent = ColorPolicy.readableAccent(accentRoles.primary, background)
            // An always-dark terminal in a light appearance (or the reverse) cannot share one 4.5:1 accent with the
            // surfaces; the accent is drawn on the surfaces (key bar, banners), so only a same-side terminal counts.
            val terminalOnSameSide = terminal == null || (ColorPolicy.luminance(terminalBackground) < DARK_LUMINANCE) == dark
            repeat(8) {
                for (reference in listOfNotNull(
                    background, surface, surfaceVariant, terminalBackground.takeIf { terminalOnSameSide },
                    ColorPolicy.blend(background, accent, TONE_ALPHA / 255.0),
                    ColorPolicy.blend(surface, accent, TONE_ALPHA / 255.0),
                )) {
                    accent = ColorPolicy.readableAccent(accent, reference)
                }
            }
            return TerminalPalette(
                primary = roles.primary,
                onPrimary = roles.onPrimary,
                accent = accent,
                onAccent = ColorPolicy.onFilledColor(accent),
                background = background,
                surface = surface,
                surfaceVariant = surfaceVariant,
                outline = neutral[3],
                divider = neutral[4],
                text = neutral[5],
                muted = neutral[6],
                danger = neutral[7],
                terminalBackground = terminalBackground,
                terminalForeground = terminal?.foreground ?: neutral[9],
                isDark = dark,
                scheme = scheme,
            )
        }

    }

}
