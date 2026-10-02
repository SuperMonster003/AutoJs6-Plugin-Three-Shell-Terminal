package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.util.Locale

/** Locale to bundled changelog mapping with the English fallback (AGENTS.md 12). */
class ReleaseHistoryTest {

    @Test
    fun everySupportedLanguageMapsToItsOwnDocumentAndOthersFallBackToEnglish() {
        assertEquals("zh-Hans", ReleaseHistory.languageCode(Locale.SIMPLIFIED_CHINESE))
        assertEquals("zh-Hans", ReleaseHistory.languageCode(Locale.forLanguageTag("zh-Hans-SG")))
        assertEquals("zh-Hant-HK", ReleaseHistory.languageCode(Locale.forLanguageTag("zh-HK")))
        assertEquals("zh-Hant-HK", ReleaseHistory.languageCode(Locale.forLanguageTag("zh-Hant-MO")))
        assertEquals("zh-Hant-TW", ReleaseHistory.languageCode(Locale.TRADITIONAL_CHINESE))
        assertEquals("zh-Hant-TW", ReleaseHistory.languageCode(Locale.forLanguageTag("zh-Hant")))
        listOf("en", "fr", "es", "ja", "ko", "ru", "ar").forEach { language ->
            assertEquals(language, ReleaseHistory.languageCode(Locale.forLanguageTag(language)))
            assertEquals(language, ReleaseHistory.languageCode(Locale.forLanguageTag("$language-XX")))
        }
        assertEquals("en", ReleaseHistory.languageCode(Locale.GERMANY))
        assertEquals("en", ReleaseHistory.languageCode(Locale.ROOT))
    }

    @Test
    fun candidatesListTheLocaleDocumentThenEnglishWithoutDuplicates() {
        assertEquals(listOf("doc/CHANGELOG-ja.md", "doc/CHANGELOG-en.md"), ReleaseHistory.candidates(Locale.JAPAN))
        assertEquals(listOf("doc/CHANGELOG-en.md"), ReleaseHistory.candidates(Locale.US))
        assertEquals(listOf("doc/CHANGELOG-en.md"), ReleaseHistory.candidates(Locale.ITALY))
    }

    @Test
    fun loadSkipsMissingOrBlankDocumentsAndReturnsNullWhenNoneReads() {
        val assets = mapOf("doc/CHANGELOG-en.md" to "# en", "doc/CHANGELOG-fr.md" to "  \n")
        fun read(path: String): String = assets[path] ?: throw IOException(path)
        assertEquals("# en", ReleaseHistory.load(Locale.FRANCE, ::read))
        assertEquals("# en", ReleaseHistory.load(Locale.KOREA, ::read))
        assertEquals("# en", ReleaseHistory.load(Locale.US, ::read))
        assertNull(ReleaseHistory.load(Locale.US) { throw IOException(it) })
    }

    @Test
    fun documentKeysResolveWithHistoryAsTheDefault() {
        BundledDocument.entries.forEach { assertEquals(it, BundledDocument.of(it.key)) }
        assertEquals(BundledDocument.HISTORY, BundledDocument.of(null))
        assertEquals(BundledDocument.HISTORY, BundledDocument.of("unknown"))
        assertEquals(
            listOf(null, "legal/LICENSE", "legal/THIRD_PARTY_NOTICES.md", "legal/jackpal/LICENSE", "legal/jackpal/NOTICE"),
            BundledDocument.entries.map { it.assetPath },
        )
    }

}
