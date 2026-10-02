package io.github.supermonster003.autojs6.plugin.three.shell.terminal.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Release tag comparison behind the update check (roadmap P5.2, AGENTS.md 12). */
class AppVersionPolicyTest {

    @Test
    fun stableVersionsCompareNumericallyWithAnOptionalPrefix() {
        assertTrue(AppVersionPolicy.isNewer("v1.0.1", "1.0.0"))
        assertTrue(AppVersionPolicy.isNewer("1.10.0", "v1.9.9"))
        assertTrue(AppVersionPolicy.isNewer("2.0", "1.99.99"))
        assertFalse(AppVersionPolicy.isNewer("1.0.0", "1.0.0"))
        assertFalse(AppVersionPolicy.isNewer("v0.9.9", "1.0.0"))
        // No integer overflow: lengths decide first.
        assertTrue(AppVersionPolicy.isNewer("1.0.99999999999999999999", "1.0.9"))
    }

    @Test
    fun preReleasesSortBelowTheirReleaseAndMetadataIsIgnored() {
        assertTrue(AppVersionPolicy.isNewer("1.1.0", "1.1.0-beta.2"))
        assertTrue(AppVersionPolicy.isNewer("1.1.0-beta.2", "1.1.0-beta.1"))
        assertTrue(AppVersionPolicy.isNewer("1.1.0-beta.10", "1.1.0-beta.9"))
        assertTrue(AppVersionPolicy.isNewer("1.1.0-rc.1", "1.1.0-beta.9"))
        assertTrue(AppVersionPolicy.isNewer("1.1.0-alpha.1", "1.1.0-alpha"))
        assertFalse(AppVersionPolicy.isNewer("1.0.0+build.7", "1.0.0"))
        assertTrue(AppVersionPolicy.isIgnored("v1.2.0+linux", "1.2.0"))
    }

    @Test
    fun malformedTextIsNeitherNewerNorIgnored() {
        listOf(null, "", "1", "1.", "01.2.3", "1.2.3-01", "1.2.3-", "latest", "1.2.3.4", "x".repeat(129)).forEach { text ->
            assertNull("$text must not parse", AppVersionPolicy.parse(text))
            assertFalse(AppVersionPolicy.isNewer(text, "1.0.0"))
            assertFalse(AppVersionPolicy.isIgnored(text, "1.0.0"))
            assertFalse(AppVersionPolicy.isNewer("9.9.9", text))
        }
        assertNotNull(AppVersionPolicy.parse(" v1.0.0 "))
        assertEquals(listOf("1", "2", "0"), AppVersionPolicy.parse("1.2")!!.parts)
    }

    @Test
    fun ignoringMatchesTheExactVersionOnly() {
        assertTrue(AppVersionPolicy.isIgnored("v1.0.1", "1.0.1"))
        assertFalse(AppVersionPolicy.isIgnored("v1.0.2", "1.0.1"))
        assertFalse(AppVersionPolicy.isIgnored("1.0.1-rc.1", "1.0.1"))
    }

}
