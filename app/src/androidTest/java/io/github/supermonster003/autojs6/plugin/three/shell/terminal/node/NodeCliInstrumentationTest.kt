package io.github.supermonster003.autojs6.plugin.three.shell.terminal.node

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.SessionAssembly
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator.Resolution
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P2.3 device evidence. With the Node.js Runtime plugin installed: its signer passes the D17
 * rule, the session plan installs the npm / corepack archive and links `usr/bin`, `node`, `npm`
 * and `corepack` answer inside a real session, and (online) `npm install` fetches a package from
 * the registry that `node` then runs. Without it: the plan reports `plugin-missing` and the session
 * is a plain shell. The settings switch short-circuits on every device. Results are logged under [TAG].
 *
 * Output markers are built with `printf '__%s__' NAME` so the echoed command line never contains the
 * marker the test waits for; only executed output does.
 */
@RunWith(AndroidJUnit4::class)
class NodeCliInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = TerminalPreferences(context)
    private val runtimeInstalled: Boolean get() = NodeCliLocator.discover(context) != null

    @Test
    fun theInstalledRuntimeIsTrustedAndResolvable() {
        assumeTrue("Node.js Runtime plugin not installed on this device", runtimeInstalled)
        val candidate = NodeCliLocator.discover(context)!!
        val signers = NodeCliTrust.signersOf(context, candidate.packageName)!!
        val own = NodeCliTrust.signersOf(context, context.packageName)!!
        val verdict = NodeCliTrust.verdict(context, candidate.packageName)
        Log.i(TAG, "trust: api=${Build.VERSION.SDK_INT} package=${candidate.packageName} version=${candidate.versionCode} signers=${signers.current} lineage=${signers.lineage} own=${own.current} verdict=${verdict.javaClass.simpleName}")
        assertTrue("the official runtime must be trusted: $verdict", verdict.isTrusted)
        assertTrue(signers.current.isNotEmpty() && signers.current.all { it.length == 64 })

        val resolution = NodeCliLocator.resolve(context, refresh = true, integrationEnabled = true)
        Log.i(TAG, "resolve: ${resolution.contractState} $resolution")
        assertTrue("expected Available, got $resolution", resolution is Resolution.Available)
        assertEquals(TerminalContract.NODE_CLI_AVAILABLE, resolution.contractState)
        assertEquals(resolution, NodeCliLocator.cachedOrNull())
    }

    @Test
    fun theIntegrationSwitchShortCircuitsWithoutTouchingTheRuntime() {
        val before = preferences.nodeIntegrationEnabled
        try {
            preferences.nodeIntegrationEnabled = false
            val resolution = NodeCliLocator.resolve(context, refresh = true)
            assertEquals(Resolution.Unavailable.IntegrationDisabled, resolution)
            assertEquals(TerminalContract.NODE_CLI_DISABLED, resolution.contractState)
            val plan = SessionAssembly.plan(context, null, preferences)
            assertEquals(Resolution.Unavailable.IntegrationDisabled, plan.nodeResolution)
            assertTrue(plan.environment.isEmpty())
            assertFalse(File(plan.paths.bin, "node").exists())
            Log.i(TAG, "switch off: ${plan.nodeResolution.contractState}, environment empty, usr/bin/node absent")
        } finally {
            preferences.nodeIntegrationEnabled = before
        }
    }

    @Test
    fun nodeNpmAndCorepackAnswerInsideASession() {
        assumeTrue("Node.js Runtime plugin not installed on this device", runtimeInstalled)
        preferences.nodeIntegrationEnabled = true
        val plan = SessionAssembly.plan(context, null, preferences, refreshNode = true)
        Log.i(TAG, "plan: node=${plan.nodeResolution.contractState} installedNow=${plan.node.installedNow} env=${plan.environment.keys}")
        assertTrue("expected Available, got ${plan.nodeResolution}", plan.nodeResolution is Resolution.Available)
        assertEquals(plan.paths.nodeCliRoot.path, plan.environment[TerminalNodeEnvironment.CLI_ROOT_VARIABLE])
        listOf("node", "npm", "npx", "corepack").forEach { assertTrue("usr/bin/$it must be linked", File(plan.paths.bin, it).exists()) }
        val session = onMain { SessionAssembly.start(context, plan).also { it.pty.updateSize(120, 40) } }
        try {
            await { onMain { session.pty.transcriptText.contains("$") } }
            onMain { session.pty.write("printf '__NODE_%s__\\n' \"\$(node --version)\"; printf '__NPM_%s__\\n' \"\$(npm --version)\"; printf '__COREPACK_%s__\\n' \"\$(corepack --version)\"; printf '__%s__\\n' DONE\r") }
            await(120_000) { onMain { session.pty.transcriptText.contains("__DONE__") } }
            val transcript = onMain { session.pty.transcriptText }.trimEnd()
            val node = Regex("__NODE_(v\\d+\\.\\d+\\.\\d+)__").find(transcript)?.groupValues?.get(1)
            val npm = Regex("__NPM_(\\d+\\.\\d+\\.\\d+)__").find(transcript)?.groupValues?.get(1)
            val corepack = Regex("__COREPACK_(\\d+\\.\\d+\\.\\d+)__").find(transcript)?.groupValues?.get(1)
            val descriptor = (plan.nodeResolution as Resolution.Available).launcher.descriptor
            Log.i(TAG, "versions: node=$node npm=$npm corepack=$corepack (declared npm=${descriptor.npmVersion} corepack=${descriptor.corepackVersion})")
            assertTrue("node version missing in: ${transcript.takeLast(400)}", node != null)
            assertTrue("npm version missing in: ${transcript.takeLast(400)}", npm != null)
            assertTrue("corepack version missing in: ${transcript.takeLast(400)}", corepack != null)
            descriptor.npmVersion?.let { assertEquals(it, npm) }
            descriptor.corepackVersion?.let { assertEquals(it, corepack) }
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
        }
    }

    /**
     * Registry round trip when online: `npm install cowsay` into a scratch project, `node` runs the
     * package, and `npx --yes cowsay hi` fetches it into the npx cache. The bin shim itself cannot
     * run: `npm_config_bin_links=false` creates no `.bin` entries (and Android would not execute a
     * script written under app data anyway, see the host's `docs/nodejs/TERMINAL.md`), so `npx`
     * ends with the shell's "not found" status 127. That status is pinned here as the documented
     * limitation; a change means the documentation must change too.
     */
    @Test
    fun npmInstallsFromTheRegistryAndNodeRunsThePackage() {
        assumeTrue("Node.js Runtime plugin not installed on this device", runtimeInstalled)
        assumeTrue("device is offline", isOnline())
        preferences.nodeIntegrationEnabled = true
        val plan = SessionAssembly.plan(context, null, preferences)
        assumeTrue("Node.js unavailable: ${plan.nodeResolution}", plan.nodeResolution is Resolution.Available)
        val project = File(plan.paths.home, "p23-npm-${System.currentTimeMillis()}").apply { check(mkdirs()) }
        val session = onMain { SessionAssembly.start(context, plan).also { it.pty.updateSize(120, 60) } }
        try {
            await { onMain { session.pty.transcriptText.contains("$") } }
            onMain { session.pty.write("cd '${project.path}' && npm install --no-audit --no-fund --loglevel=error cowsay; printf '__%s_%s__\\n' INSTALL_EXIT \"\$?\"\r") }
            await(300_000) { onMain { Regex("__INSTALL_EXIT_\\d+__").containsMatchIn(session.pty.transcriptText) } }
            val installExit = exitStatus(session, "INSTALL_EXIT")
            val installed = File(project, "node_modules/cowsay/package.json").isFile
            Log.i(TAG, "npm install: exit=$installExit installed=$installed registry=${plan.environment["npm_config_registry"] ?: "default"}")
            assertEquals("npm install cowsay must succeed: ${tail(session)}", 0, installExit)
            assertTrue("node_modules/cowsay must exist after the install", installed)

            onMain { session.pty.write("node node_modules/cowsay/cli.js hi; printf '__%s_%s__\\n' RUN_EXIT \"\$?\"\r") }
            await(60_000) { onMain { Regex("__RUN_EXIT_\\d+__").containsMatchIn(session.pty.transcriptText) } }
            val runExit = exitStatus(session, "RUN_EXIT")
            val cow = onMain { session.pty.transcriptText }.contains("< hi >")
            Log.i(TAG, "node run: exit=$runExit cow=$cow")
            assertEquals("node must run the installed package: ${tail(session)}", 0, runExit)
            assertTrue("cowsay output missing: ${tail(session)}", cow)

            onMain { session.pty.write("npx --yes cowsay hi; printf '__%s_%s__\\n' NPX_EXIT \"\$?\"\r") }
            await(300_000) { onMain { Regex("__NPX_EXIT_\\d+__").containsMatchIn(session.pty.transcriptText) } }
            val npxExit = exitStatus(session, "NPX_EXIT")
            val npxCache = File(plan.paths.npmCache, "_npx")
            val fetched = npxCache.walkTopDown().any { it.name == "package.json" && it.parentFile?.name == "cowsay" }
            Log.i(TAG, "npx: exit=$npxExit fetchedIntoCache=$fetched cache=${npxCache.path} tail=${tail(session, 240).replace("\n", " | ")}")
            assertTrue("npx must fetch cowsay into ${npxCache.path}", fetched)
            assertEquals("npx bin shims are not executable on Android (documented limitation)", 127, npxExit)
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
            project.deleteRecursively()
        }
    }

    @Test
    fun withoutTheRuntimeTheSessionIsAPlainShell() {
        assumeTrue("Node.js Runtime plugin is installed on this device", !runtimeInstalled)
        preferences.nodeIntegrationEnabled = true
        val plan = SessionAssembly.plan(context, null, preferences, refreshNode = true)
        assertEquals(Resolution.Unavailable.PluginMissing, plan.nodeResolution)
        assertEquals(TerminalContract.NODE_CLI_PLUGIN_MISSING, plan.nodeResolution.contractState)
        assertTrue(plan.environment.isEmpty())
        assertNull(NodeCliLocator.discover(context))
        val paths = TerminalPaths.of(context)
        assertFalse(File(paths.bin, "node").exists())
        val session = onMain { SessionAssembly.start(context, plan).also { it.pty.updateSize(80, 24) } }
        try {
            await { onMain { session.pty.transcriptText.contains("$") } }
            onMain { session.pty.write("printf '__NODE_%s__\\n' \"\$(command -v node || echo none)\"; printf '__CLIROOT_%s__\\n' \"\${AUTOJS6_NODE_CLI_ROOT:-unset}\"; printf '__%s__\\n' DONE\r") }
            await { onMain { session.pty.transcriptText.contains("__DONE__") } }
            val transcript = onMain { session.pty.transcriptText }.trimEnd()
            assertTrue("node must not resolve: ${transcript.takeLast(300)}", transcript.contains("__NODE_none__"))
            assertTrue("no Node variables may be exported: ${transcript.takeLast(300)}", transcript.contains("__CLIROOT_unset__"))
            Log.i(TAG, "plain shell: plugin-missing, node=none, AUTOJS6_NODE_CLI_ROOT unset")
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
        }
    }

    /** A TCP connect to the npm registry; the plugin holds INTERNET but not ACCESS_NETWORK_STATE. */
    private fun isOnline(): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress("registry.npmjs.org", 443), 5_000) }
        true
    }.getOrDefault(false)

    private fun exitStatus(session: TerminalSessionManager.Session, marker: String): Int =
        Regex("__${marker}_(\\d+)__").find(onMain { session.pty.transcriptText })!!.groupValues[1].toInt()

    private fun tail(session: TerminalSessionManager.Session, chars: Int = 600): String =
        onMain { session.pty.transcriptText }.trimEnd().takeLast(chars)

    private fun await(timeoutMillis: Long = 15_000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "Terminal state did not settle within $timeoutMillis ms" }
            SystemClock.sleep(100)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }

    private companion object {
        const val TAG = "ThreeShellNode"
    }

}
