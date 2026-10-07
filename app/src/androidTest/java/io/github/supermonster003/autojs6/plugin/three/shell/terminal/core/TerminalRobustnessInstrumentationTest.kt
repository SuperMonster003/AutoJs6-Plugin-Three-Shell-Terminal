package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder.OutputSubscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/** P6 regression probes. Only sessions and files created by this suite are removed. */
@RunWith(AndroidJUnit4::class)
class TerminalRobustnessInstrumentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val handler = Handler(Looper.getMainLooper())

    @Test
    fun cancellingAnIdleReadDoesNotWaitForTheProducerToClose() {
        val pipe = ParcelFileDescriptor.createPipe()
        val descriptor = pipe[0].fileDescriptor
        val input = PtyIo(pipe[0]).input
        val started = CountDownLatch(1)
        val result = AtomicReference<Int>()
        val reader = thread(name = "P6-idle-reader") {
            started.countDown()
            result.set(input.read())
        }
        try {
            assertTrue(started.await(2, TimeUnit.SECONDS))
            SystemClock.sleep(150)
            assertTrue("the producer remains open and has supplied no data", reader.isAlive)
            input.close()
            reader.join(2_000)
            assertFalse("cancellation releases an idle reader", reader.isAlive)
            assertEquals(-1, result.get())
            assertFalse("the owned descriptor closes after the reader has returned", descriptor.valid())
        } finally {
            input.close()
            pipe.forEach { it.close() }
            reader.join(2_000)
        }
    }

    @Test
    fun oneHundredCreateCloseCyclesReturnPtyDescriptorsAndThreadsToBaseline() {
        // Warm up framework singletons/notification channels before measuring resources owned by ptys.
        repeat(5) { cycle(it) }
        await("warmup workers finish") { terminalWorkers() == 0 }
        val before = descriptors()
        val baseline = before.size
        repeat(100) { cycle(it) }
        await("all terminal workers finish") { terminalWorkers() == 0 }
        await("file descriptors return to the warmed baseline") { descriptors().size <= baseline }
        val after = descriptors()
        assertEquals("no pty descriptor may survive a closed session", before.count { it == "/dev/ptmx" }, after.count { it == "/dev/ptmx" })
        Log.i(TAG, "cycles=100 fdBefore=$baseline fdAfter=${after.size} terminalWorkers=${terminalWorkers()}")
    }

    @Test
    fun naturalExitsAndUnattachedSessionsReleaseTheirPtys() {
        val baseline = descriptors().count { it == "/dev/ptmx" }
        repeat(20) { index ->
            val session = if (index % 2 == 0) create() else onMain {
                TerminalSessionManager.create(context, TerminalPaths.of(context).home.path,
                    argv = listOf("/system/bin/sh", "-c", "exit 0"))
            }
            try {
                if (index % 2 == 0) onMain { session.pty.write("exit\r") }
                await("natural exit") { session.exitCode != null }
                await("natural EOF closes the pty") { descriptors().count { it == "/dev/ptmx" } == baseline }
            } finally { close(session) }
        }
        await("natural-exit workers finish") { terminalWorkers() == 0 }
        Log.i(TAG, "naturalAndUnattachedExits=20 remainingPtys=$baseline terminalWorkers=0")
    }

    @Test
    fun oneHundredMiBAndAnUnreadSubscriberDoNotBlockTheUiOrAnotherSubscriber() {
        val file = File.createTempFile("p6-output-", ".txt", context.cacheDir)
        val line = ("x".repeat(127) + "\n").toByteArray()
        val block = ByteArray(1024 * 1024) { line[it % line.size] }
        file.outputStream().buffered().use { out -> repeat(100) { out.write(block) } }
        val session = create()
        val dropped = AtomicLong()
        val received = AtomicLong()
        val fastDropped = AtomicLong()
        val slow = OutputSubscription("p6-slow", session.id, Process.myPid(), onOverflow = { dropped.addAndGet(it) }, onClosed = {})
        val fast = OutputSubscription("p6-fast", session.id, Process.myPid(), onOverflow = { fastDropped.addAndGet(it) }, onClosed = {})
        val reader = thread(name = "P6-count-output", isDaemon = true) {
            ParcelFileDescriptor.AutoCloseInputStream(fast.readEnd).use { input ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    received.addAndGet(count.toLong())
                }
            }
        }
        var heartbeatAt = SystemClock.uptimeMillis()
        val worstHeartbeat = AtomicLong()
        val pulse = object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                worstHeartbeat.updateAndGet { maxOf(it, now - heartbeatAt) }
                heartbeatAt = now
                handler.postDelayed(this, 20)
            }
        }
        var failure: Throwable? = null
        try {
            onMain { slow.attach(session.pty, false); fast.attach(session.pty, false) }
            slow.start()
            fast.start()
            handler.post(pulse)
            onMain { session.pty.write("cat '${file.path}'; printf '__P6_%s__\\n' DONE\r") }
            await("100 MiB completes", 180_000) { onMain { session.pty.transcriptText.contains("__P6_DONE__") } }
            await("fast subscriber receives the full output") { received.get() >= file.length() }
            assertTrue("the stalled subscriber must report dropped bytes", dropped.get() > 0)
            assertEquals("the draining subscriber must not inherit another subscriber's overflow", 0, fastDropped.get())
            assertTrue("main thread must continue processing input (max=${worstHeartbeat.get()} ms)", worstHeartbeat.get() < 2_000)
            onMain { session.pty.write("yes\r") }
            SystemClock.sleep(1_000)
            onMain { session.pty.write(byteArrayOf(3), 0, 1) }
            onMain { session.pty.write("printf '__P6_%s__\\n' INTERRUPTED\r") }
            await("Ctrl+C interrupts yes") { onMain { session.pty.transcriptText.contains("__P6_INTERRUPTED__") } }
            Log.i(TAG, "catBytes=${file.length()} received=${received.get()} stalledDropped=${dropped.get()} mainThreadMaxMs=${worstHeartbeat.get()} yes=interruptible")
        } catch (error: Throwable) {
            failure = error
        } finally {
            // A latch signalled in the reader's finally does not mean the thread has exited yet.
            // Join it, and always clean the shell/file even if any cleanup assertion fails.
            fun cleanup(action: () -> Unit) {
                try { action() } catch (error: Throwable) {
                    val original = failure
                    if (original == null) failure = error else original.addSuppressed(error)
                }
            }
            cleanup { handler.removeCallbacks(pulse) }
            cleanup { slow.abort() }
            cleanup { slow.readEnd.close() }
            cleanup { fast.abort() }
            cleanup { reader.join(5_000); assertFalse("the reader must stop after cancellation", reader.isAlive) }
            cleanup { close(session) }
            cleanup { check(file.delete()) }
        }
        failure?.let { throw it }
    }

    @Test
    fun ptyWindowSizeTracksEveryResizeWithoutRestartingTheShell() {
        val session = create()
        try {
            val pid = session.pty.pid
            for ((columns, rows) in listOf(80 to 24, 43 to 17, 120 to 12, 60 to 40, 80 to 24)) {
                onMain {
                    session.pty.updateSize(columns, rows)
                    // mksh updates these from TIOCGWINSZ after SIGWINCH, including API 24 without stty.
                    session.pty.write("printf '__P6_SIZE_%s__\\n' \"\$LINES \$COLUMNS\"\r")
                }
                await("kernel pty size $columns x $rows") { onMain { session.pty.transcriptText.contains("__P6_SIZE_$rows $columns" + "__") } }
                assertEquals(pid, session.pty.pid)
            }
            Log.i(TAG, "ptyResize=80x24,43x17,120x12,60x40,80x24 samePid=true")
        } finally { close(session) }
    }

    @Test
    fun warmedSessionsReachTheFirstPromptWithinThePerformanceBudget() {
        val samples = ArrayList<Long>()
        repeat(11) { index ->
            val start = SystemClock.elapsedRealtimeNanos()
            val session = create()
            val millis = TimeUnit.NANOSECONDS.toMillis(SystemClock.elapsedRealtimeNanos() - start)
            try { if (index > 0) samples.add(millis) } finally { close(session) }
        }
        // Device speed is recorded, not asserted on shared CI runners. API 35 hardware is the release gate.
        Log.i(TAG, "firstPromptMs=$samples medianMs=${samples.sorted()[samples.size / 2]} maxMs=${samples.max()}")
        assertTrue("even slow runners must create sessions within the bounded startup timeout", samples.max() < 15_000)
    }

    private fun cycle(index: Int) {
        val session = onMain {
            TerminalSessionManager.create(context, TerminalPaths.of(context).home.path).also {
                if (index % 3 != 0) it.pty.updateSize(80, 24)
            }
        }
        try {
            if (index % 3 == 2) await("child starts") { session.pty.pid > 0 }
        } finally { close(session) }
    }

    private fun create(): TerminalSessionManager.Session {
        val session = onMain {
            TerminalSessionManager.create(context, TerminalPaths.of(context).home.path).also { it.pty.updateSize(80, 24) }
        }
        try {
            await("first shell prompt") { onMain { session.pty.transcriptText.contains("$") } }
            return session
        } catch (failure: Throwable) { close(session); throw failure }
    }

    private fun close(session: TerminalSessionManager.Session) {
        onMain { TerminalSessionManager.close(session.id) }
        await("shell reaped") { session.exitCode != null }
        val pid = session.pty.pid
        if (pid > 0) assertFalse("closed child must not survive", File("/proc/$pid").exists())
    }

    private fun descriptors(): List<String> = File("/proc/self/fd").listFiles().orEmpty().mapNotNull {
        runCatching { android.system.Os.readlink(it.path) }.getOrNull()
    }

    private fun terminalWorkers() = Thread.getAllStackTraces().keys.count {
        it.isAlive && (it.name.startsWith("TerminalPtySession-") || it.name.startsWith("TermSession"))
    }

    private fun await(what: String, timeoutMillis: Long = 15_000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) {
                Thread.getAllStackTraces().filter { it.key.name.startsWith("TermSession") }.forEach { (worker, stack) ->
                    Log.e(TAG, "${worker.name}: ${stack.joinToString("; ")}")
                }
                "Timed out: $what"
            }
            SystemClock.sleep(5)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }

    private companion object { const val TAG = "ThreeShellP6" }
}
