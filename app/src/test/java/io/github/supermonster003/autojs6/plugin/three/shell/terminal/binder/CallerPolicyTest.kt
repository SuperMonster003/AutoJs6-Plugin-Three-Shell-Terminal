package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure rule behind `HostCallerGuard`: same uid as the installed host, host version, equal non-empty signer sets. */
class CallerPolicyTest {

    private val signer = setOf("a".repeat(64))
    private val hostUid = 10123
    private val packages = setOf(ThreeShellTerminalPlugin.HOST_PACKAGE_NAME)
    private val version = ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION

    @Test
    fun theInstalledSameSignerHostOfTheRequiredVersionIsAllowed() {
        assertTrue(CallerPolicy.allowed(hostUid, packages, hostUid, version, signer, signer))
        assertTrue("a newer host is fine", CallerPolicy.allowed(hostUid, packages, hostUid, version + 100, signer, signer))
        assertTrue("shared-uid siblings do not matter as long as the host is among them", CallerPolicy.allowed(hostUid, packages + "org.example.sibling", hostUid, version, signer, signer))
    }

    @Test
    fun everythingElseIsRefused() {
        assertFalse("another uid", CallerPolicy.allowed(hostUid + 1, packages, hostUid, version, signer, signer))
        assertFalse("host not installed", CallerPolicy.allowed(hostUid, packages, null, 0L, emptySet(), signer))
        assertFalse("uid without the host package", CallerPolicy.allowed(hostUid, setOf("org.example.other"), hostUid, version, signer, signer))
        assertFalse("host too old", CallerPolicy.allowed(hostUid, packages, hostUid, version - 1, signer, signer))
        assertFalse("different signer", CallerPolicy.allowed(hostUid, packages, hostUid, version, setOf("b".repeat(64)), signer))
        assertFalse("extra signer on one side", CallerPolicy.allowed(hostUid, packages, hostUid, version, signer + "b".repeat(64), signer))
        assertFalse("empty signer sets never match", CallerPolicy.allowed(hostUid, packages, hostUid, version, emptySet(), emptySet()))
    }

}
