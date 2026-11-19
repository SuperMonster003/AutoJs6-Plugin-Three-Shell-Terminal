package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.content.res.AssetManager
import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R

/**
 * The legal documents bundled into the APK (AGENTS.md 12, roadmap D31): the plugin license, the
 * third-party notices, the upstream jackpal license and notice and the license of the bundled
 * terminal font (roadmap P8), copied by the `bundleLegalAssets` Gradle task. The [key] travels in
 * the Intent extra of [DocumentActivity]. The release history is not one of them: it has its own
 * page ([ReleaseHistoryActivity], plugin-center P21.8).
 * zh-CN: 打包进 APK 的法律文档 (AGENTS.md 12, D31): 插件许可证, 第三方声明, 由 `bundleLegalAssets` 任务复制的
 * 上游 jackpal 许可证与声明, 以及内置终端字体的许可证 (P8); [key] 作为 [DocumentActivity] 的 Intent extra 传递.
 * 发行历史不在其中, 它有自己的页面 ([ReleaseHistoryActivity], plugin-center P21.8).
 */
internal enum class BundledDocument(val key: String, val assetPath: String, @param:StringRes val title: Int, val markdown: Boolean) {
    LICENSE("license", "legal/LICENSE", R.string.settings_license, false),
    NOTICES("notices", "legal/THIRD_PARTY_NOTICES.md", R.string.settings_notices, true),
    JACKPAL_LICENSE("jackpal-license", "legal/jackpal/LICENSE", R.string.about_jackpal_license, false),
    JACKPAL_NOTICE("jackpal-notice", "legal/jackpal/NOTICE", R.string.about_jackpal_notice, false),
    FONT_LICENSE("font-license", "legal/fonts/jetbrains-mono/OFL.txt", R.string.about_font_license, false);

    companion object {

        /** Reading is bounded to 1 MiB. */
        const val MAX_DOCUMENT_BYTES = 1024 * 1024

        /** The document named by [key]; null for a missing or unknown key (the screen then closes). */
        fun of(key: String?): BundledDocument? = entries.firstOrNull { it.key == key }

        fun read(assets: AssetManager, path: String): String = assets.open(path).use { stream ->
            val bytes = stream.readBytes()
            require(bytes.size <= MAX_DOCUMENT_BYTES) { "bundled document too large: $path" }
            bytes.toString(Charsets.UTF_8)
        }

    }
}
