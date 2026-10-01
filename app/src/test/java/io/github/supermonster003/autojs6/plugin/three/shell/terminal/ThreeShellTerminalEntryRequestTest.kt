package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure rules of the `TERMINAL_OPEN` entry: extras validation and the caller policy behind the manifest permission. */
class ThreeShellTerminalEntryRequestTest {

    @Test
    fun theHostRequestsPassThroughWithTheirExtras() {
        assertEquals(EntryRequest(null, null, false, null, false), EntryRequest.of(null, null, false, null, false))
        assertEquals(EntryRequest("/sdcard/scripts", null, false, null, false), EntryRequest.of("/sdcard/scripts", null, false, null, false))
        assertEquals(EntryRequest("/sdcard/scripts", null, true, null, false), EntryRequest.of("/sdcard/scripts", null, true, null, false))
        assertEquals(EntryRequest(null, "7", false, null, false), EntryRequest.of(null, "7", false, null, false))
        assertEquals(EntryRequest("/sdcard/p", null, false, "npm test", false), EntryRequest.of("/sdcard/p", null, false, "npm test", false))
        assertEquals(EntryRequest(null, null, false, null, true), EntryRequest.of(null, null, false, null, true))
    }

    @Test
    fun blankStringsCountAsAbsent() {
        assertEquals(EntryRequest(null, null, false, null, false), EntryRequest.of("  ", " ", false, "", false))
        assertEquals("a blank sessionId does not clash with newSession", EntryRequest(null, null, true, null, false), EntryRequest.of(null, " ", true, "", false))
    }

    @Test
    fun oversizedValuesAreRefused() {
        val longestPath = "/" + "a".repeat(TerminalContract.MAX_PATH_BYTES - 1)
        assertEquals(longestPath, EntryRequest.of(longestPath, null, false, null, false)?.directory)
        assertNull(EntryRequest.of(longestPath + "a", null, false, null, false))
        assertNull("size is measured in UTF-8 bytes", EntryRequest.of("/" + "中".repeat(TerminalContract.MAX_PATH_BYTES / 3 + 1), null, false, null, false))
        val longestCommand = "x".repeat(TerminalContract.MAX_COMMAND_BYTES)
        assertEquals(longestCommand, EntryRequest.of(null, null, false, longestCommand, false)?.command)
        assertNull(EntryRequest.of(null, null, false, longestCommand + "x", false))
    }

    @Test
    fun contradictoryCombinationsAreRefusedLikeTheHostDoes() {
        assertNull("manager with sessionId", EntryRequest.of(null, "7", false, null, true))
        assertNull("manager with newSession", EntryRequest.of(null, null, true, null, true))
        assertNull("manager with command", EntryRequest.of(null, null, false, "ls", true))
        assertNull("sessionId with newSession", EntryRequest.of(null, "7", true, null, false))
        assertNull("sessionId with command", EntryRequest.of(null, "7", false, "ls", false))
        assertEquals("manager with a directory is tolerated", true, EntryRequest.of("/sdcard", null, false, null, true)?.manager)
    }

    @Test
    fun flagsAcceptBooleansAndTheirSpelling() {
        assertTrue(EntryRequest.flag(true))
        assertFalse(EntryRequest.flag(false))
        assertTrue(EntryRequest.flag("true"))
        assertTrue(EntryRequest.flag("TRUE"))
        assertFalse(EntryRequest.flag("yes"))
        assertFalse(EntryRequest.flag(1))
        assertFalse(EntryRequest.flag(null))
    }

    @Test
    fun theCallerPolicyTrustsTheManifestPermissionForUnnamedCallersAndChecksNamedOnes() {
        val self = ThreeShellTerminalPlugin.PACKAGE_NAME
        val host = ThreeShellTerminalPlugin.HOST_PACKAGE_NAME
        val signer = setOf("a".repeat(64))
        assertTrue("plain startActivity below API 34 names nobody; Android already enforced the permission", EntryCallerPolicy.accepts(null, self, false, emptySet(), signer))
        assertTrue("the plugin itself", EntryCallerPolicy.accepts(self, self, false, emptySet(), signer))
        assertTrue("the same-signer host holding the permission", EntryCallerPolicy.accepts(host, self, true, signer, signer))
        assertFalse("a named caller without the permission", EntryCallerPolicy.accepts(host, self, false, signer, signer))
        assertFalse("a different signer", EntryCallerPolicy.accepts(host, self, true, setOf("b".repeat(64)), signer))
        assertFalse("an extra signer on one side", EntryCallerPolicy.accepts(host, self, true, signer + "b".repeat(64), signer))
        assertFalse("unknown plugin signers never match", EntryCallerPolicy.accepts(host, self, true, emptySet(), emptySet()))
    }

}
