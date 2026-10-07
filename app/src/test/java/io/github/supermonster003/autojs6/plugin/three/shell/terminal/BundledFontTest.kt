package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * The bundled terminal font (roadmap P8) matches its recorded provenance: the resource and the license carry the
 * recorded SHA-256, the third-party notices name the font digest, and the legal assets task packages the license.
 */
class BundledFontTest {

    private val root: File = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }

    @Test
    fun fontAndLicenseMatchTheirProvenance() {
        val provenance = JsonParser.parseString(File(root, "vendor/fonts/jetbrains-mono/provenance.json").readText()).asJsonObject
        val resource = File(root, provenance["resource"].asString)
        assertEquals("app/src/main/res/font/jetbrains_mono_nl_regular.ttf", provenance["resource"].asString)
        assertEquals(provenance["resourceSha256"].asString, sha256(resource))
        assertEquals(provenance["licenseSha256"].asString, sha256(File(root, "vendor/fonts/jetbrains-mono/OFL.txt")))
        assertEquals("OFL-1.1", provenance["license"].asString)
        val notices = File(root, "THIRD_PARTY_NOTICES.md").readText()
        assertTrue(notices.contains(provenance["resourceSha256"].asString))
        assertTrue(notices.contains(provenance["licenseSha256"].asString))
        val gradle = File(root, "app/build.gradle.kts").readText()
        assertTrue("the license must be bundled for the About screen", gradle.contains("vendor/fonts/jetbrains-mono") && gradle.contains("legal/fonts/jetbrains-mono"))
    }

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it.toInt() and 0xff) }

}
