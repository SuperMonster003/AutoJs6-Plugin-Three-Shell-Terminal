package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.content.Context
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences

/**
 * The plugin's own appearance choices (standalone settings specification, sections 6 and 7; roadmap
 * P5.1): language, night mode and theme color, each defaulting to "follow AutoJs6". They are
 * layered on top of the host snapshot in [resolve]: an explicit choice wins, "follow AutoJs6" takes
 * the host value and falls back to the system, "follow system" ignores the host. Stored in the
 * `terminal` preferences file next to the terminal settings; nothing is ever written to the host.
 *
 * zh-CN: 插件自身的外观选择 (独立设置页规范第 6, 7 节; P5.1): 语言, 夜间模式与主题色, 默认均为 "跟随 AutoJs6".
 * [resolve] 把它们叠加在宿主快照之上: 显式选择优先, "跟随 AutoJs6" 取宿主值并回退系统, "跟随系统" 忽略宿主.
 * 与终端设置同存于 `terminal` 偏好文件; 从不写回宿主.
 */
internal data class AppearancePreferences(
    val language: String = HOST,
    val darkMode: String = HOST,
    val color: Int? = null,
) {

    fun resolve(host: HostAppearance?, systemLanguage: String, systemDark: Boolean): Appearance {
        val dark = when (darkMode) {
            LIGHT -> false
            DARK -> true
            SYSTEM -> systemDark
            else -> host?.dark ?: systemDark
        }
        val tag = when (language) {
            HOST -> host?.language ?: systemLanguage
            SYSTEM -> systemLanguage
            else -> language
        }
        val primary = color ?: host?.primary ?: Appearance.DEFAULT_COLOR
        val accent = color ?: host?.accent ?: host?.primary ?: Appearance.DEFAULT_COLOR
        return Appearance(tag, dark, primary or OPAQUE, accent or OPAQUE)
    }

    /** True when every choice follows AutoJs6 (the state a fresh install is in). */
    val followsHost: Boolean get() = language == HOST && darkMode == HOST && color == null

    fun write(context: Context) {
        preferences(context).edit()
            .putString(TerminalPreferences.KEY_APPEARANCE_LANGUAGE, language)
            .putString(TerminalPreferences.KEY_APPEARANCE_DARK_MODE, darkMode)
            .apply { if (color == null) remove(TerminalPreferences.KEY_APPEARANCE_COLOR) else putInt(TerminalPreferences.KEY_APPEARANCE_COLOR, color) }
            .apply()
    }

    companion object {

        const val HOST = "host"
        const val SYSTEM = "system"
        const val LIGHT = "light"
        const val DARK = "dark"

        /** Stored language values in the order of the picker: follow AutoJs6, follow system, then the ten bundled languages. */
        val LANGUAGES: List<String> = listOf(HOST, SYSTEM, "zh-Hans", "zh-Hant-HK", "zh-Hant-TW", "en", "fr", "es", "ja", "ko", "ru", "ar")

        /** Stored night mode values in the order of the picker. */
        val MODES: List<String> = listOf(HOST, SYSTEM, LIGHT, DARK)

        private const val OPAQUE = 0xFF000000.toInt()

        /** Pure constructor from stored values; unknown or missing values fall back to "follow AutoJs6". */
        fun of(language: String?, darkMode: String?, color: Int?): AppearancePreferences = AppearancePreferences(
            language = language?.takeIf { it in LANGUAGES } ?: HOST,
            darkMode = darkMode?.takeIf { it in MODES } ?: HOST,
            color = color?.let { it or OPAQUE },
        )

        fun read(context: Context): AppearancePreferences {
            val preferences = preferences(context)
            return of(
                preferences.getString(TerminalPreferences.KEY_APPEARANCE_LANGUAGE, null),
                preferences.getString(TerminalPreferences.KEY_APPEARANCE_DARK_MODE, null),
                if (preferences.contains(TerminalPreferences.KEY_APPEARANCE_COLOR)) preferences.getInt(TerminalPreferences.KEY_APPEARANCE_COLOR, 0) else null,
            )
        }

        private fun preferences(context: Context) =
            context.applicationContext.getSharedPreferences(TerminalPreferences.FILE_NAME, Context.MODE_PRIVATE)

    }

}
