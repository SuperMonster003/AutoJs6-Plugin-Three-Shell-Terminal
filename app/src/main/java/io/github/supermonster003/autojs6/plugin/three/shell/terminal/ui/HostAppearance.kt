package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import org.autojs.plugin.common.api.AutoJs6HostSettingsContract as C
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * The appearance snapshot AutoJs6 publishes through its versioned settings provider
 * (`AutoJs6HostSettingsContract`, `common-plugin-api`): resolved language tag, active night mode
 * and the two theme colors. Reading goes through an unstable provider client off the main thread;
 * anything malformed, foreign or outdated decodes to null so the caller falls back to the system
 * (standalone settings specification, section 7). Nothing is ever written back to the host.
 *
 * zh-CN: AutoJs6 经版本化设置 provider 发布的外观快照 (语言, 夜间模式, 主题色). 在工作线程经不稳定 provider
 * 客户端读取; 格式错误, 来源不符或协议不符时解码为 null, 调用方回退到系统值 (独立设置页规范第 7 节). 从不写回宿主.
 */
internal data class HostAppearance(val language: String, val dark: Boolean, val primary: Int, val accent: Int) {

    companion object {

        /** Last successful read of this process; null until the first read completes or when the host is unavailable. */
        @Volatile
        var cached: HostAppearance? = null

        val worker: Executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "HostAppearance").apply { isDaemon = true } }

        /** Blocking provider call; returns null when the host is missing, disabled, unreachable or answers something else. */
        fun read(context: Context): HostAppearance? = runCatching {
            context.applicationContext.contentResolver.acquireUnstableContentProviderClient(Uri.parse(C.CONTENT_URI))?.use { client ->
                client.call(C.METHOD_GET_SETTINGS, null, null)?.let(::decode)
            }
        }.getOrNull()

        /** Strict decoding: protocol version, host package, value types and a sane BCP 47 tag. */
        fun decode(value: Bundle): HostAppearance? = runCatching {
            require(!value.hasFileDescriptors())
            require(value.getInt(C.KEY_PROTOCOL_VERSION) == C.PROTOCOL_VERSION)
            require(value.getString(C.KEY_HOST_PACKAGE_NAME) == C.HOST_PACKAGE_NAME)
            @Suppress("DEPRECATION")
            val typesValid = value.get(C.KEY_DARK_MODE_ACTIVE) is Boolean &&
                value.get(C.KEY_THEME_COLOR_PRIMARY) is Int &&
                value.get(C.KEY_THEME_COLOR_ACCENT) is Int
            require(typesValid)
            val tag = requireNotNull(value.getString(C.KEY_RESOLVED_LANGUAGE_TAG))
            require(isLanguageTag(tag))
            HostAppearance(tag, value.getBoolean(C.KEY_DARK_MODE_ACTIVE), value.getInt(C.KEY_THEME_COLOR_PRIMARY), value.getInt(C.KEY_THEME_COLOR_ACCENT))
        }.getOrNull()

        private val LANGUAGE_TAG = Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")

        fun isLanguageTag(tag: String): Boolean =
            tag.length in 2..80 && LANGUAGE_TAG.matches(tag) && Locale.forLanguageTag(tag).language.isNotBlank()

    }

}

/**
 * The appearance a screen actually uses: the plugin's own [AppearancePreferences] layered over the
 * host snapshot when available, otherwise over the system language and night mode with the agreed
 * no-host theme color (`#FFDEAD`). A fresh install follows AutoJs6 for all three (roadmap P5.1).
 *
 * zh-CN: 界面实际使用的外观: 插件自身的 [AppearancePreferences] 叠加在宿主快照之上, 无宿主时叠加在系统语言,
 * 夜间模式与约定的无宿主主题色 (`#FFDEAD`) 之上; 全新安装时三项均跟随 AutoJs6 (P5.1).
 */
internal data class Appearance(val language: String, val dark: Boolean, val primarySeed: Int, val accentSeed: Int) {

    /** A context whose resources speak [language] in the [dark] or light night mode. */
    fun wrap(context: Context): Context = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply {
            val locale = Locale.forLanguageTag(language)
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        },
    )

    companion object {

        /** The no-host fallback theme color shared by the standalone plugins (`theme_color_default`). */
        const val DEFAULT_COLOR = 0xFFFFDEAD.toInt()

        fun resolve(context: Context, host: HostAppearance? = HostAppearance.cached): Appearance {
            val configuration = context.resources.configuration
            val systemLanguage = configuration.locales[0].toLanguageTag()
            val systemDark = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            return AppearancePreferences.read(context).resolve(host, systemLanguage, systemDark)
        }

        /** Pure resolution without plugin preferences (tests, defaults): host values win, the system fills the gaps. */
        fun resolve(host: HostAppearance?, systemLanguage: String, systemDark: Boolean): Appearance =
            AppearancePreferences().resolve(host, systemLanguage, systemDark)

    }

}
