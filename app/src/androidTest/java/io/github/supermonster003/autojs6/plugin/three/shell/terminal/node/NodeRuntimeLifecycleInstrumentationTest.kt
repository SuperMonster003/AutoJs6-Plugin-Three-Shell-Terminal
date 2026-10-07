package io.github.supermonster003.autojs6.plugin.three.shell.terminal.node

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.SessionAssembly
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPtySession
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/** Explicitly coordinated only on a disposable emulator; never uninstalls packages itself. */
@RunWith(AndroidJUnit4::class)
class NodeRuntimeLifecycleInstrumentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun runningNodeSurvivesReplacementAndRemovalWhileNewSessionsRediscoverTheRuntime() {
        assumeTrue("requires a disposable-device coordinator", InstrumentationRegistry.getArguments().getString("p6NodeLifecycle") == "true")
        val directory = File(context.cacheDir, "p6-node-lifecycle").apply { check(mkdirs() || isDirectory) }
        val stateFile = File(directory, "state")
        val signalFile = File(directory, "signal")
        signalFile.delete()
        val plan = SessionAssembly.plan(context, null, refreshNode = true)
        val before = plan.nodeResolution as NodeCliLocator.Resolution.Available
        val observed = AtomicLong()
        val session = onMain {
            SessionAssembly.start(context, plan, command = "node -e \"setInterval(()=>process.stdout.write('alive\\n'),100)\"", keepOpen = false).also {
                it.pty.addOutputTap(TerminalPtySession.OutputTap { _, _, count -> observed.addAndGet(count.toLong()) })
                it.pty.updateSize(80, 24)
            }
        }
        try {
            await("node started") { observed.get() > 0 }
            for (stage in listOf("replace", "remove", "restore")) {
                stateFile.writeText("ready-$stage")
                await("coordinator completed $stage", 180_000) { signalFile.takeIf { it.isFile }?.readText()?.trim() == stage }
                val count = observed.get()
                await("existing node still emits after $stage") { observed.get() > count + 20 }
                assertTrue("running node must survive $stage", session.isAlive)
                val refreshed = SessionAssembly.plan(context, null)
                if (stage == "remove") {
                    assertEquals(NodeCliLocator.Resolution.Unavailable.PluginMissing, refreshed.nodeResolution)
                    assertTrue(refreshed.environment.isEmpty())
                    assertFalse(File(refreshed.paths.bin, "node").exists())
                    val plain = onMain { SessionAssembly.start(context, refreshed).also { it.pty.updateSize(80, 24) } }
                    try {
                        onMain { plain.pty.write("printf '__%s__\\n' PLAIN\r") }
                        await("new plain shell works") { onMain { plain.pty.transcriptText.contains("__PLAIN__") } }
                    } finally { onMain { TerminalSessionManager.close(plain.id) } }
                } else {
                    val current = refreshed.nodeResolution as NodeCliLocator.Resolution.Available
                    val link = File(refreshed.paths.bin, "node")
                    assertEquals(current.launcher.executable.canonicalPath, link.canonicalPath)
                    if (stage == "replace") assertTrue("replacement must refresh the installation path", before.launcher.executable.path != current.launcher.executable.path)
                }
                Log.i("ThreeShellP6", "runtimeLifecycle=$stage existingNode=alive newSession=${refreshed.nodeResolution.contractState}")
            }
            stateFile.writeText("passed")
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
            signalFile.delete()
        }
    }

    private fun await(what: String, timeout: Long = 15_000, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (!condition()) { check(SystemClock.elapsedRealtime() < deadline) { "Timed out: $what" }; SystemClock.sleep(50) }
    }

    private fun <T> onMain(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { result.set(runCatching(block)) }
        return result.get().getOrThrow()
    }
}
