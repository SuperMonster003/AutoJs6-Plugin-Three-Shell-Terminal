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
    fun aUserConfirmationCoversExactlyTheConfirmedCurrentSigners() {
        val confirmed = setOf(NodeCliTrust.fingerprint(listOf(other)))
        assertEquals(Verdict.UserTrusted(listOf(other)), NodeCliTrust.evaluate(Signers(listOf(other)), ownSigners, confirmed))
        assertEquals(Verdict.UserTrusted(listOf(other)), NodeCliTrust.evaluate(Signers(listOf(other.uppercase())), ownSigners, confirmed))
        assertTrue(NodeCliTrust.evaluate(Signers(listOf(other)), ownSigners, confirmed).isTrusted)
        val elsewhere = "c".repeat(64)
        assertEquals(Verdict.Untrusted(listOf(elsewhere)), NodeCliTrust.evaluate(Signers(listOf(elsewhere)), ownSigners, confirmed))
        // A confirmed older key of the lineage does not cover a rotated current key.
        val rotated = Signers(current = listOf(elsewhere), lineage = listOf(other, elsewhere))
        val verdict = NodeCliTrust.evaluate(rotated, ownSigners, confirmed)
        assertEquals(Verdict.Untrusted(listOf(elsewhere, other), listOf(elsewhere)), verdict)
    }

    @Test
    fun officialAndOwnKeysTakePrecedenceOverUserConfirmations() {
        val confirmedEverything = setOf(official, own, other).map { NodeCliTrust.fingerprint(listOf(it)) }.toSet()
        assertEquals(Verdict.Official, NodeCliTrust.evaluate(Signers(listOf(official)), ownSigners, confirmedEverything))
        assertEquals(Verdict.Self, NodeCliTrust.evaluate(Signers(listOf(own)), ownSigners, confirmedEverything))
    }

    @Test
    fun multiSignedConfirmationsAreOrderIndependentButNeverPartial() {
        val second = "d".repeat(64)
        val confirmed = setOf(NodeCliTrust.fingerprint(listOf(second, other)))
        assertEquals(Verdict.UserTrusted(listOf(other, second)), NodeCliTrust.evaluate(Signers(listOf(other, second)), ownSigners, confirmed))
        assertEquals(Verdict.Untrusted(listOf(other, second)), NodeCliTrust.evaluate(Signers(listOf(other, second)), ownSigners, setOf(NodeCliTrust.fingerprint(listOf(other)))))
        assertEquals(NodeCliTrust.fingerprint(listOf(second, other)), NodeCliTrust.fingerprint(listOf(other.uppercase(), second, other)))
    }

    @Test
    fun userTrustRecordsAreValidatedAndRoundTrip() {
        val trust = NodeCliTrust.UserTrust.of("com.example.node", listOf(other.uppercase(), "a1".repeat(32)))!!
        assertEquals(listOf("a1".repeat(32), other), trust.digests)
        assertEquals("com.example.node|${"a1".repeat(32)},$other", trust.encode())
        assertEquals(trust, NodeCliTrust.UserTrust.decode(trust.encode()))
        val colon = other.chunked(2).joinToString(":")
        assertEquals(NodeCliTrust.UserTrust.of("com.example.node", listOf(other)), NodeCliTrust.UserTrust.of("com.example.node", listOf(colon)))
        assertEquals(null, NodeCliTrust.UserTrust.of("node", listOf(other)))
        assertEquals(null, NodeCliTrust.UserTrust.of("com.example.node|x", listOf(other)))
        assertEquals(null, NodeCliTrust.UserTrust.of("com.example.node", emptyList()))
        assertEquals(null, NodeCliTrust.UserTrust.of("com.example.node", listOf("xyz")))
        assertEquals(null, NodeCliTrust.UserTrust.of("com.example.node", (0 until 5).map { "%064x".format(it) }))
        assertEquals(null, NodeCliTrust.UserTrust.decode("com.example.node"))
        assertEquals(null, NodeCliTrust.UserTrust.decode("|$other"))
    }

    @Test
    fun sha256RendersLowercaseHex() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", NodeCliTrust.sha256(ByteArray(0)))
    }

}
