package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.content.res.AssetManager
import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import java.io.IOException
import java.util.Locale

/**
 * The documents bundled into the APK (AGENTS.md 12, roadmap D31): the localized changelog, the
 * plugin license, the third-party notices and the upstream jackpal license and notice copied by
 * the `bundleLegalAssets` Gradle task. The [key] travels in the Intent extra of the document screen.
 * zh-CN: 打包进 APK 的文档 (AGENTS.md 12, D31): 本地化更新日志, 插件许可证, 第三方声明以及由
 * `bundleLegalAssets` 任务复制的上游 jackpal 许可证与声明; [key] 作为文档界面的 Intent extra 传递.
 */
internal enum class BundledDocument(val key: String, val assetPath: String?, @param:StringRes val title: Int, val markdown: Boolean) {
    HISTORY("history", null, R.string.release_history_title, true),
    LICENSE("license", "legal/LICENSE", R.string.settings_license, false),
    NOTICES("notices", "legal/THIRD_PARTY_NOTICES.md", R.string.settings_notices, true),
    JACKPAL_LICENSE("jackpal-license", "legal/jackpal/LICENSE", R.string.about_jackpal_license, false),
    JACKPAL_NOTICE("jackpal-notice", "legal/jackpal/NOTICE", R.string.about_jackpal_notice, false);

    companion object {
        fun of(key: String?): BundledDocument = entries.firstOrNull { it.key == key } ?: HISTORY
    }
}

/**
 * Locale to bundled changelog mapping (AGENTS.md 12): `doc/CHANGELOG-{tag}.md` for the ten
 * supported languages, English as the fallback. Reading is bounded to 1 MiB.
 * zh-CN: 语言到内置更新日志的映射 (AGENTS.md 12): 十种语言各取 `doc/CHANGELOG-{tag}.md`, 回退英文; 读取上限 1 MiB.
 */
internal object ReleaseHistory {

    const val MAX_DOCUMENT_BYTES = 1024 * 1024

    private val PLAIN_LANGUAGES = setOf("en", "ar", "es", "fr", "ja", "ko", "ru")

    fun languageCode(locale: Locale): String = when {
        locale.language == "zh" && (locale.country == "HK" || locale.country == "MO") -> "zh-Hant-HK"
        locale.language == "zh" && (locale.country == "TW" || locale.script == "Hant") -> "zh-Hant-TW"
        locale.language == "zh" -> "zh-Hans"
        locale.language in PLAIN_LANGUAGES -> locale.language
        else -> "en"
    }

    fun candidates(locale: Locale): List<String> =
        listOf("doc/CHANGELOG-${languageCode(locale)}.md", "doc/CHANGELOG-en.md").distinct()

    /** The first candidate that reads as non-blank text; null when none does. */
    fun load(locale: Locale, read: (String) -> String): String? {
        for (path in candidates(locale)) {
            try {
                read(path).takeIf { it.isNotBlank() }?.let { return it }
            } catch (_: IOException) {
                // Try the next candidate; the English document is always bundled.
            }
        }
        return null
    }

    fun read(assets: AssetManager, path: String): String = assets.open(path).use { stream ->
        val bytes = stream.readBytes()
        require(bytes.size <= MAX_DOCUMENT_BYTES) { "bundled document too large: $path" }
        bytes.toString(Charsets.UTF_8)
    }

}
