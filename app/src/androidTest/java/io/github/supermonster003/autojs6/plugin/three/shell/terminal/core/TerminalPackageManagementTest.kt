package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.os.Binder
import android.os.SystemClock
import android.os.Looper
import android.system.Os
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder.TerminalPluginBinder
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.TerminalNodeEnvironment
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** P2.5 uses scratch projects only, including clear-data. No user's terminal home is deleted. */
@RunWith(AndroidJUnit4::class)
class TerminalPackageManagementTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = TerminalPreferences(context)

    @Test
    fun settingsValidateBeforeSavingAndExportToNewSessions() = withSavedPreferences {
        val actions = TerminalSettingsActions(context)
        actions.setRegistry(TerminalPreferences.REGISTRY_CUSTOM, " HTTPS://MIRROR.EXAMPLE.COM/npm ")
        assertEquals("https://mirror.example.com/npm/", actions.registrySummary)
        assertThrows(IllegalArgumentException::class.java) { actions.setRegistry(TerminalPreferences.REGISTRY_CUSTOM, "http://unsafe.example.com") }
        assertEquals(TerminalPreferences.REGISTRY_CUSTOM, preferences.npmRegistryChoice)
        assertEquals("https://mirror.example.com/npm/", preferences.npmRegistryCustomUrl)
        actions.setRegistry(TerminalPreferences.REGISTRY_NPMMIRROR)
        actions.setIgnoreScripts(true)
        val env = TerminalNodeEnvironment.build(TerminalPaths.of(context), preferences.nodeEnvironmentOptions())
        assertEquals(TerminalNodeEnvironment.NPMMIRROR_REGISTRY, env["npm_config_registry"])
        assertEquals(env["npm_config_registry"], env["COREPACK_NPM_REGISTRY"])
        assertEquals("true", env["npm_config_ignore_scripts"])
        actions.setRegistry(TerminalPreferences.REGISTRY_NPMJS)
        assertEquals(TerminalNodeEnvironment.DEFAULT_REGISTRY, actions.registrySummary)
        assertFalse(TerminalNodeEnvironment.build(TerminalPaths.of(context), preferences.nodeEnvironmentOptions()).containsKey("npm_config_registry"))
        actions.setNodeIntegration(false)
        assertEquals(NodeCliLocator.Resolution.Unavailable.IntegrationDisabled, NodeCliLocator.resolve(context))
    }

    @Test
    fun clearDataStopsLiveAndPendingSessionsWithoutFollowingLinks() {
        val scratch = File(context.cacheDir, "p25-clear-${System.nanoTime()}").apply { check(mkdirs()) }
        val paths = TerminalPaths(File(scratch, "terminal")).ensureLayout()
        val outside = File(scratch, "external-project").apply { check(mkdirs()) }
        val sentinel = File(outside, "keep.txt").apply { writeText("keep") }
        Os.symlink(outside.path, File(paths.home, "linked-project").path)
        Os.symlink(File(scratch, "missing").path, File(paths.home, "dangling").path)
        File(paths.home, "remove.txt").writeText("remove")
        File(paths.bin, "remove-tool").writeText("remove")
        val saved = context.getSharedPreferences(TerminalPreferences.FILE_NAME, 0).all
        val oldPlan = SessionAssembly.plan(context, null)
        val live = onMain { TerminalSessionManager.create(context, paths.home.path).also { it.pty.updateSize(80, 24) } }
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val planned = CountDownLatch(1)
        val router = TerminalPluginBinder(context, object : CallerGuard {
            override fun enforceHost(): Int = Binder.getCallingUid()
        }) { ctx, cwd, prefs, generation ->
            entered.countDown()
            try {
                check(release.await(15, TimeUnit.SECONDS))
                SessionAssembly.plan(ctx, cwd, prefs, dataGeneration = generation)
            } finally {
                planned.countDown()
            }
        }
        try {
            await { live.pty.pid > 0 }
            val pending = JsonParser.parseString(router.openSession("{}" )).asJsonObject["id"].asString
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val result = AtomicReference<Result<Int>>()
            val cleared = CountDownLatch(1)
            TerminalSettingsActions(context, paths).clearData { result.set(it); cleared.countDown() }
            assertTrue(cleared.await(10, TimeUnit.SECONDS))
            assertEquals(2, result.get().getOrThrow())
            assertNotNull(live.exitCode)
            assertFalse(File("/proc/${live.pty.pid}").exists())
            assertEquals("keep", sentinel.readText())
            assertTrue(paths.home.listFiles()!!.isEmpty())
            assertTrue(paths.bin.listFiles()!!.isEmpty())
            assertEquals(TerminalPaths.DEFAULT_PROFILE, paths.profile.readText())
            assertEquals(saved, context.getSharedPreferences(TerminalPreferences.FILE_NAME, 0).all)
            assertThrows(CancellationException::class.java) { onMain { SessionAssembly.start(context, oldPlan) } }
            release.countDown()
            assertTrue(planned.await(5, TimeUnit.SECONDS))
            assertNull(TerminalSessionManager.get(pending))
            assertEquals("[]", router.listSessions())
            Log.i(TAG, "clear: closed=2, shell reaped, pending cancelled, external project and preferences preserved")
        } finally {
            release.countDown()
            router.closeAllSessions()
            router.close()
            onMain { TerminalSessionManager.close(live.id) }
            TerminalDataCleaner.deleteTree(scratch)
        }
    }

    @Test
    fun aClearFailureReturnsToTheMainThreadAndAllowsFutureSessions() {
        val root = File(context.cacheDir, "p25-bad-root-${System.nanoTime()}").apply { writeText("keep") }
        val completed = CountDownLatch(1)
        val failed = AtomicReference<Result<Int>>()
        val callbackOnMain = AtomicReference<Boolean>()
        try {
            TerminalSettingsActions(context, TerminalPaths(root)).clearData {
                failed.set(it)
                callbackOnMain.set(Looper.myLooper() == Looper.getMainLooper())
                completed.countDown()
            }
            assertTrue(completed.await(10, TimeUnit.SECONDS))
            assertTrue(failed.get().isFailure)
            assertTrue(callbackOnMain.get())
            TerminalDataLifecycle.checkTicket(TerminalDataLifecycle.ticket())
            assertEquals("keep", root.readText())
        } finally {
            root.delete()
        }
    }

    @Test
    fun clearDataAlsoWaitsForAShellAlreadyInItsCloseGracePeriod() {
        val scratch = File(context.cacheDir, "p25-closing-${System.nanoTime()}").apply { check(mkdirs()) }
        val paths = TerminalPaths(File(scratch, "terminal")).ensureLayout()
        val session = onMain {
            TerminalSessionManager.create(context, paths.home.path, argv = listOf(
                "/system/bin/sh", "-c", "trap '' HUP; printf '__%s__\\n' WAITING; while :; do sleep 1; done",
            )).also { it.pty.updateSize(80, 24) }
        }
        val completed = CountDownLatch(1)
        val reapedAtCompletion = AtomicReference<Boolean>()
        val outcome = AtomicReference<Result<Int>>()
        try {
            await { onMain { session.pty.transcriptText.contains("__WAITING__") } }
            onMain {
                TerminalSessionManager.close(session.id)
                assertNull(TerminalSessionManager.get(session.id))
                TerminalSettingsActions(context, paths).clearData {
                    outcome.set(it)
                    reapedAtCompletion.set(session.exitCode != null)
                    completed.countDown()
                }
            }
            assertTrue(completed.await(10, TimeUnit.SECONDS))
            assertEquals(0, outcome.get().getOrThrow())
            assertTrue("a removed session's shell must exit before files are reset", reapedAtCompletion.get())
            assertTrue(paths.profile.isFile)
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
            await { session.exitCode != null }
            TerminalDataCleaner.deleteTree(scratch)
        }
    }

    @Test
    fun npmMirrorInstallAndProjectScriptsWorkInARealSession() = withSavedPreferences {
        assumeTrue("Node.js Runtime not installed", NodeCliLocator.discover(context) != null)
        assumeTrue("npmmirror is unreachable", runCatching {
            Socket().use { it.connect(InetSocketAddress("registry.npmmirror.com", 443), 5_000) }
            true
        }.getOrDefault(false))
        val project = File(context.cacheDir, "p25-npm-${System.nanoTime()}").apply { check(mkdirs()) }
        val actions = TerminalSettingsActions(context)
        actions.setNodeIntegration(true)
        actions.setRegistry(TerminalPreferences.REGISTRY_NPMMIRROR)
        actions.setIgnoreScripts(true)
        File(project, "package.json").writeText("""{
          "name":"terminal-p25-test","version":"1.0.0","private":true,
          "scripts":{"verify":"node verify.js","postinstall":"node postinstall.js"},
          "dependencies":{"is-number":"7.0.0"}
        }""")
        File(project, "verify.js").writeText("if (!require('is-number')(42)) process.exit(9); console.log('project script passed');")
        File(project, "postinstall.js").writeText("require('fs').writeFileSync('install-script-ran', 'unexpected');")
        assertEquals(listOf("verify", "postinstall"), TerminalNpmActions.scripts(project)!!.map { it.name })
        val plan = SessionAssembly.plan(context, project.path, preferences)
        assertTrue(plan.nodeResolution.toString(), plan.nodeResolution is NodeCliLocator.Resolution.Available)
        assertEquals(TerminalNodeEnvironment.NPMMIRROR_REGISTRY, plan.environment["npm_config_registry"])
        val session = onMain {
            SessionAssembly.start(context, plan, extraEnvironment = mapOf("npm_config_cache" to File(project, "npm-cache").path))
                .also { it.pty.updateSize(120, 60) }
        }
        try {
            await { onMain { session.pty.transcriptText.contains("$") } }
            onMain { session.pty.write(TerminalNpmActions.npmInstall() + " --no-audit --no-fund --fetch-retries=0 --fetch-timeout=45000; printf '__%s_%s__\\n' INSTALL \"\$?\"\n") }
            await(120_000) { onMain { Regex("__INSTALL_\\d+__").containsMatchIn(session.pty.transcriptText) } }
            assertEquals(0, exitStatus(session, "INSTALL"))
            assertTrue(File(project, "node_modules/is-number/package.json").isFile)
            assertFalse("ignore-scripts must prevent the postinstall hook", File(project, "install-script-ran").exists())
            onMain { session.pty.write(TerminalNpmActions.npmRunScript("verify") + "; printf '__%s_%s__\\n' RUN \"\$?\"\n") }
            await { onMain { Regex("__RUN_\\d+__").containsMatchIn(session.pty.transcriptText) } }
            assertEquals(0, exitStatus(session, "RUN"))
            Log.i(TAG, "npmmirror: fresh cache, is-number@7.0.0 installed, npm run verify exit=0, postinstall skipped")
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
            await { session.exitCode != null }
            TerminalDataCleaner.deleteTree(project)
        }
    }

    private fun exitStatus(session: TerminalSessionManager.Session, label: String): Int =
        onMain { Regex("__${label}_(\\d+)__").find(session.pty.transcriptText)!!.groupValues[1].toInt() }

    private fun withSavedPreferences(block: () -> Unit) {
        val prefs = context.getSharedPreferences(TerminalPreferences.FILE_NAME, 0)
        val keys = listOf(TerminalPreferences.KEY_NPM_REGISTRY, TerminalPreferences.KEY_NPM_REGISTRY_CUSTOM_URL,
            TerminalPreferences.KEY_NPM_IGNORE_SCRIPTS, TerminalPreferences.KEY_NODE_INTEGRATION_ENABLED)
        val saved = prefs.all
        try {
            block()
        } finally {
            prefs.edit().apply {
                keys.forEach { key ->
                    when (val value = saved[key]) {
                        is String -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> remove(key)
                    }
                }
            }.commit()
            NodeCliLocator.invalidate()
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { result.set(runCatching(block)) }
        return result.get().getOrThrow()
    }

    private fun await(timeout: Long = 15_000, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (!predicate() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
        assertTrue("timed out", predicate())
    }

    private companion object { const val TAG = "TerminalPackageManagement" }
}
