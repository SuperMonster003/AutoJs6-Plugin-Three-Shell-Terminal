package io.github.supermonster003.autojs6.plugin.three.shell.terminal.node

import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliTrust.Signers
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliTrust.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The D17 signer rule: official set, own key, anything else, and multi-signed or rotated APKs. */
class NodeCliTrustTest {

    private val official = "31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213"
    private val own = "a".repeat(64)
    private val other = "b".repeat(64)
    private val ownSigners = setOf(own)

    @Test
    fun theOfficialDigestIsTheHostsPluginTrustManagerValue() {
        assertEquals(setOf(official), NodeCliTrust.OFFICIAL_SHA_256)
        assertEquals(64, official.length)
    }

    @Test
    fun singleSignersAreClassified() {
        assertEquals(Verdict.Official, NodeCliTrust.evaluate(Signers(listOf(official)), ownSigners))
        assertEquals(Verdict.Self, NodeCliTrust.evaluate(Signers(listOf(own)), ownSigners))
        assertEquals(Verdict.Untrusted(listOf(other)), NodeCliTrust.evaluate(Signers(listOf(other)), ownSigners))
        assertEquals(Verdict.Untrusted(emptyList()), NodeCliTrust.evaluate(Signers(emptyList()), ownSigners))
        assertEquals("a development build whose own key equals the official key is official", Verdict.Official, NodeCliTrust.evaluate(Signers(listOf(official)), setOf(official)))
    }

    @Test
    fun digestsAreComparedCaseInsensitivelyAndWithoutColons() {
        assertEquals(Verdict.Official, NodeCliTrust.evaluate(Signers(listOf(official.uppercase())), ownSigners))
        val colon = official.chunked(2).joinToString(":")
        assertEquals(Verdict.Official, NodeCliTrust.evaluate(Signers(listOf(colon)), ownSigners))
        assertEquals(Verdict.Self, NodeCliTrust.evaluate(Signers(listOf(own)), setOf(own.uppercase())))
    }

    @Test
    fun multiSignedApksNeedEveryCurrentSignerTrusted() {
        assertEquals(Verdict.Official, NodeCliTrust.evaluate(Signers(listOf(official, official)), ownSigners))
        assertEquals(Verdict.Self, NodeCliTrust.evaluate(Signers(listOf(official, own)), ownSigners))
        assertEquals(Verdict.Self, NodeCliTrust.evaluate(Signers(listOf(own, own)), ownSigners))
        assertEquals(Verdict.Untrusted(listOf(official, other)), NodeCliTrust.evaluate(Signers(listOf(official, other)), ownSigners))
        assertEquals(Verdict.Untrusted(listOf(own, other)), NodeCliTrust.evaluate(Signers(listOf(own, other)), ownSigners))
    }

    @Test
    fun rotationLineageCountsForSingleSigners() {
        val rotatedFromOfficial = Signers(current = listOf(other), lineage = listOf(official, other))
        assertEquals(Verdict.Official, NodeCliTrust.evaluate(rotatedFromOfficial, ownSigners))
        val rotatedFromOwn = Signers(current = listOf(other), lineage = listOf(own, other))
        assertEquals(Verdict.Self, NodeCliTrust.evaluate(rotatedFromOwn, ownSigners))
        val unknownLineage = Signers(current = listOf(other), lineage = listOf("c".repeat(64), other))
        val verdict = NodeCliTrust.evaluate(unknownLineage, ownSigners)
        assertTrue(verdict is Verdict.Untrusted)
        assertEquals(listOf(other, "c".repeat(64)), (verdict as Verdict.Untrusted).signers)
        assertFalse(verdict.isTrusted)
        assertTrue(Verdict.Official.isTrusted)
        assertTrue(Verdict.Self.isTrusted)
    }

    @Test
    fun sha256RendersLowercaseHex() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", NodeCliTrust.sha256(ByteArray(0)))
    }

}
