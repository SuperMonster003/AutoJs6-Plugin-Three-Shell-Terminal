package io.github.supermonster003.autojs6.plugin.three.shell.terminal.node

import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator.Resolution
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The settings short-circuit and the contract vocabulary of every resolution outcome. */
class NodeCliLocatorTest {

    private val launcher = NodeCliLocator.Launcher(
        packageName = NodeCliLocator.OFFICIAL_PLUGIN_PACKAGE,
        versionCode = 220,
        nativeLibraryDir = "/data/app/plugin/lib/arm64",
        executable = File("/data/app/plugin/lib/arm64/libnodexe.so"),
        descriptor = NodeCliMetadata.Descriptor("libnodexe.so", listOf("node"), "nodejs/cli/x.bin", "0".repeat(64), "lib/node_modules", null, null, null, null),
    )
    private val probe = NodeCliProbe.Result(0, "v24.21.0\n", "", 10, null, false)

    @Test
    fun aDisabledIntegrationShortCircuitsBeforeDiscovery() {
        var discovered = false
        val gated = NodeCliLocator.gate(integrationEnabled = false) { discovered = true; null }
        assertEquals(NodeCliLocator.Gate.Stop(Resolution.Unavailable.IntegrationDisabled), gated)
        assertFalse("discovery must not run while the switch is off", discovered)
    }

    @Test
    fun anEnabledIntegrationWithoutAPluginIsMissing() {
        var discovered = false
        val gated = NodeCliLocator.gate(integrationEnabled = true) { discovered = true; null }
        assertEquals(NodeCliLocator.Gate.Stop(Resolution.Unavailable.PluginMissing), gated)
        assertTrue(discovered)
    }

    @Test
    fun everyOutcomeMapsOntoADistinctContractState() {
        val outcomes = listOf(
            Resolution.Available(launcher, probe) to TerminalContract.NODE_CLI_AVAILABLE,
            Resolution.Unavailable.IntegrationDisabled to TerminalContract.NODE_CLI_DISABLED,
            Resolution.Unavailable.PluginMissing to TerminalContract.NODE_CLI_PLUGIN_MISSING,
            Resolution.Unavailable.PluginNotTrusted("x", listOf("b".repeat(64))) to TerminalContract.NODE_CLI_PLUGIN_UNTRUSTED,
            Resolution.Unavailable.PluginTooOld("x", "schema not declared") to TerminalContract.NODE_CLI_PLUGIN_TOO_OLD,
            Resolution.Unavailable.ExecutableMissing("x", "arm64-v8a", "libnodexe.so") to TerminalContract.NODE_CLI_EXECUTABLE_MISSING,
            Resolution.Unavailable.ExecDenied(launcher, probe.copy(exitCode = 126)) to TerminalContract.NODE_CLI_EXEC_DENIED,
            Resolution.Unavailable.SetupFailed(launcher, "digest mismatch") to TerminalContract.NODE_CLI_SETUP_FAILED,
        )
        outcomes.forEach { (resolution, expected) ->
            assertEquals(resolution.toString(), expected, resolution.contractState)
            assertTrue(expected, TerminalContract.isNodeCliState(resolution.contractState))
        }
        assertEquals("every contract state is produced by exactly one outcome", TerminalContract.NODE_CLI_STATES.toSet(), outcomes.map { it.second }.toSet())
        assertEquals(TerminalContract.NODE_CLI_STATES.size, outcomes.size)
    }

}
