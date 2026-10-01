package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.storage.StorageAccess
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalActivity.Companion.subtitleView
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P3.1 device evidence for the terminal screen: switching between sessions keeps shell
 * state and unsubmitted input (ported from the host), the subtitle shows the tilde path and copies
 * the real directory, leaving the screen keeps the shell running (D15), and the storage banner
 * appears for a shared-storage directory without the grant and leaves once it is granted (D18).
 * The banner case changes the plugin's own grant through the shell (`appops set` from API 30,
 * `pm grant` below) and leaves it in place, because withdrawing either grant kills the process. Results are logged under [TAG].
 */
@RunWith(AndroidJUnit4::class)
class TerminalActivityInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    /**
     * API 33+: the screen asks for `POST_NOTIFICATIONS` once when it starts its first session; the system
     * dialog would keep the Activity paused and `startActivitySync` waiting, so the tests grant it up front
     * (the user-facing request is covered by the manual evidence in `docs/dev/p3-ui-evidence.md`).
     */
    @Before
    fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun switchingSessionsPreservesShellStateAndUnsubmittedInput() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val first = onMain { TerminalSessionManager.create(context, paths.home.path) }
        val second = onMain { TerminalSessionManager.create(context, paths.tmp.path) }
        var activity: TerminalActivity? = null
        try {
            activity = startTerminal(TerminalActivity.intent(context).putExtra(TerminalContract.EXTRA_SESSION_ID, first.id))
            val terminal = activity
            await { onMain { terminal.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === first.pty } }
            onMain { first.pty.write("AJ6_SESSION=first; printf '__AJ6_%s__\\n' \"\$AJ6_SESSION\"\r") }
            await { onMain { first.pty.transcriptText.contains("__AJ6_first__") } }
            // Keep an unfinished command in the shell's own line editor across view replacement.
            onMain { first.pty.write("printf '__DRAFT_%s__\\n' preserved") }
            await { onMain { first.pty.transcriptText.contains("preserved") } }
            onMain { TerminalActivity.launchSession(terminal, second.id) }
            await { onMain { terminal.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === second.pty } }
            onMain { second.pty.write("AJ6_SESSION=second; printf '__AJ6_%s__\\n' \"\$AJ6_SESSION\"\r") }
            await { onMain { second.pty.transcriptText.contains("__AJ6_second__") } }
            onMain { TerminalActivity.launchSession(terminal, first.id) }
            await { onMain { terminal.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === first.pty } }
            onMain {
                assertFalse(first.pty.transcriptText.contains("__AJ6_second__"))
                first.pty.write("\r")
            }
            await { onMain { first.pty.transcriptText.contains("__DRAFT_preserved__") } }
            onMain {
                TerminalSessionManager.close(first.id)
                assertFalse(first.pty.isProcessAlive)
                assertTrue(second.pty.isProcessAlive)
            }
            await { first.pty.exitCode != null }
            Log.i(TAG, "switching: first ${first.id} kept its draft and variable across two switches on API ${Build.VERSION.SDK_INT}")
        } finally {
            onMain {
                activity?.finish()
                TerminalSessionManager.close(first.id)
                TerminalSessionManager.close(second.id)
            }
        }
    }

    @Test
    fun subtitleShowsTheTildePathAndCopiesTheRealDirectory() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val session = onMain { TerminalSessionManager.create(context, paths.home.path) }
        var activity: TerminalActivity? = null
        try {
            activity = startTerminal(TerminalActivity.intent(context).putExtra(TerminalContract.EXTRA_SESSION_ID, session.id))
            val terminal = activity
            await { onMain { terminal.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === session.pty } }
            await { onMain { session.pty.transcriptText.contains("$") } }
            await { onMain { terminal.findViewById<Toolbar>(R.id.toolbar).subtitle?.toString() == "~" } }
            val copiedHome = onMain {
                val toolbar = terminal.findViewById<Toolbar>(R.id.toolbar)
                val subtitle = toolbar.subtitleView()
                assertNotNull("subtitle view must be reachable once a subtitle is shown", subtitle)
                subtitle!!.performClick()
                Clipboard.get(terminal).toString()
            }
            // The clipboard gets the shell's real directory (procfs), never the "~" label; where /data/user/0 is a
            // symlink to /data/data the real path is spelled differently from the configured one.
            assertSameDirectory(paths.home, File(copiedHome))
            assertEquals(session.pty.currentDirectory()!!.path, copiedHome)
            // Also copy the real current directory after cd, rather than a stale or abbreviated label.
            onMain { session.pty.write("cd '${paths.tmp.path}'\r") }
            await { session.pty.currentDirectory()?.let { sameDirectory(paths.tmp, it) } == true }
            val (copiedTmp, subtitleTmp) = onMain {
                val toolbar = terminal.findViewById<Toolbar>(R.id.toolbar)
                toolbar.subtitleView()!!.performClick()
                Clipboard.get(terminal).toString() to toolbar.subtitle.toString()
            }
            assertSameDirectory(paths.tmp, File(copiedTmp))
            assertEquals(session.pty.currentDirectory()!!.path, copiedTmp)
            // tmp is not below home, so the subtitle shows the full real path instead of a tilde form.
            assertFalse(subtitleTmp.startsWith("~"))
            assertSameDirectory(paths.tmp, File(subtitleTmp))
            Log.i(TAG, "subtitle: '~' then '$subtitleTmp'; clipboard '$copiedHome' then '$copiedTmp' (configured tmp ${paths.tmp.path})")
        } finally {
            onMain {
                activity?.finish()
                TerminalSessionManager.close(session.id)
            }
        }
    }

    @Test
    fun leavingTheScreenKeepsTheSessionRunningAndReopeningRestoresIt() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val before = TerminalSessionManager.activeSessions.map { it.id }.toSet()
        var activity: TerminalActivity? = null
        var reopened: TerminalActivity? = null
        var created: TerminalSessionManager.Session? = null
        try {
            activity = startTerminal(TerminalActivity.intent(context).putExtra(TerminalContract.EXTRA_NEW_SESSION, true))
            val terminal = activity
            await { onMain { terminal.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession != null } }
            val session = TerminalSessionManager.activeSessions.single { it.id !in before }
            created = session
            await { session.pty.currentDirectory()?.canonicalPath == paths.home.canonicalPath }
            onMain { session.pty.write("AJ6_KEPT=yes\r") }
            onMain { terminal.navigateBack() }
            await { terminal.isDestroyed || terminal.isFinishing }
            SystemClock.sleep(300)
            assertTrue("the shell must survive the screen", session.pty.isProcessAlive)
            assertTrue(TerminalSessionManager.activeSessions.any { it.id == session.id })
            // Without extras the screen reopens the most recent live session instead of starting a new one.
            reopened = startTerminal(TerminalActivity.intent(context))
            val second = reopened
            await { onMain { second.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession === session.pty } }
            onMain { session.pty.write("printf '__KEPT_%s__\\n' \"\$AJ6_KEPT\"\r") }
            await { onMain { session.pty.transcriptText.contains("__KEPT_yes__") } }
            assertEquals(before.size + 1, TerminalSessionManager.activeSessions.size)
            Log.i(TAG, "leaving: session ${session.id} survived finish() and was restored with its variables")
        } finally {
            onMain {
                activity?.takeUnless { it.isDestroyed }?.finish()
                reopened?.takeUnless { it.isDestroyed }?.finish()
                created?.let { TerminalSessionManager.close(it.id) }
            }
        }
    }

    @Test
    fun storageBannerAppearsWithoutTheGrantAndReentersTheDirectoryOnceGranted() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val target = File(Environment.getExternalStorageDirectory(), "Download")
        val initiallyGranted = StorageAccess.state(context).isGranted
        Log.i(TAG, "banner: api=${Build.VERSION.SDK_INT} state=${StorageAccess.state(context)} target=${target.path}")
        // Neither grant can be withdrawn from inside the process: revoking the runtime permissions kills the app,
        // and so does `appops set ... MANAGE_EXTERNAL_STORAGE default` on API 30+ (HyperOS 2 / API 35 crashed the
        // instrumentation). An ungranted start therefore has to be prepared from adb before the run.
        val before = TerminalSessionManager.activeSessions.map { it.id }.toSet()
        var activity: TerminalActivity? = null
        var created: TerminalSessionManager.Session? = null
        var granted = StorageAccess.state(context).isGranted
        try {
            activity = startTerminal(TerminalActivity.intent(context, target.path).putExtra(TerminalContract.EXTRA_NEW_SESSION, true))
            val terminal = activity
            await { onMain { terminal.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession != null } }
            val session = TerminalSessionManager.activeSessions.single { it.id !in before }
            created = session
            val banner = onMain { terminal.findViewById<View>(R.id.storage_banner) }
            if (granted) {
                // Grant already present (API < 30 after `pm grant`, API 30+ after `appops set ... allow`): the
                // directory is simply entered, no banner.
                await { session.pty.currentDirectory()?.canonicalPath == target.canonicalPath }
                onMain { assertEquals(View.GONE, banner.visibility) }
                Log.i(TAG, "banner: grant present, entered ${target.path} without a banner; withdraw it from adb to repeat the ungranted case")
                return
            }
            await { session.pty.currentDirectory()?.canonicalPath == paths.home.canonicalPath }
            await { onMain { banner.visibility == View.VISIBLE } }
            onMain {
                val message = banner.findViewById<TextView>(R.id.banner_text).text.toString()
                assertTrue("banner must name the requested directory: $message", message.contains(target.path))
                assertEquals(terminal.getString(R.string.terminal_action_grant), banner.findViewById<TextView>(R.id.banner_action).text.toString())
                assertEquals(terminal.getString(R.string.terminal_storage_permission_required, target.path), message)
            }
            Log.i(TAG, "banner: shown for ${target.path} while ${StorageAccess.state(context)}")

            grantStorage()
            granted = true
            await { StorageAccess.state(context).isGranted }
            // Returning from the system settings re-renders the banner: the action becomes "Re-enter directory".
            instrumentation.runOnMainSync {
                instrumentation.callActivityOnPause(terminal)
                instrumentation.callActivityOnResume(terminal)
            }
            await { onMain { banner.findViewById<TextView>(R.id.banner_action).text.toString() == terminal.getString(R.string.terminal_action_reenter_directory) } }
            onMain { banner.findViewById<View>(R.id.banner_action).performClick() }
            await { onMain { banner.visibility == View.GONE } }
            await { session.pty.currentDirectory()?.canonicalPath == target.canonicalPath }
            await { onMain { terminal.findViewById<Toolbar>(R.id.toolbar).subtitle?.toString() == paths.toTildePath(target.path) } }
            Log.i(TAG, "banner: grant via shell, re-entered ${session.pty.currentDirectory()?.path}, banner gone")
        } finally {
            onMain {
                activity?.takeUnless { it.isDestroyed }?.finish()
                created?.let { TerminalSessionManager.close(it.id) }
            }
            if (granted && !initiallyGranted) {
                val reset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    "appops set ${context.packageName} MANAGE_EXTERNAL_STORAGE default"
                } else {
                    StorageAccess.LEGACY_PERMISSIONS.joinToString("; ") { "pm revoke ${context.packageName} $it" }
                }
                Log.i(TAG, "banner: storage grant left in place (withdrawing it kills the process); reset from adb with `$reset`")
            }
        }
    }

    /** Equal after symlink resolution, or the same (device, inode) where canonicalization leaves `/data/user/0` alone. */
    private fun sameDirectory(expected: File, actual: File): Boolean {
        if (expected.canonicalPath == actual.canonicalPath) return true
        val left = runCatching { android.system.Os.stat(expected.path) }.getOrNull() ?: return false
        val right = runCatching { android.system.Os.stat(actual.path) }.getOrNull() ?: return false
        return left.st_dev == right.st_dev && left.st_ino == right.st_ino
    }

    private fun assertSameDirectory(expected: File, actual: File) {
        assertTrue("expected ${expected.path} (${expected.canonicalPath}) but was ${actual.path} (${actual.canonicalPath})", sameDirectory(expected, actual))
    }

    private fun grantStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            shell("appops set ${context.packageName} MANAGE_EXTERNAL_STORAGE allow")
        } else {
            StorageAccess.LEGACY_PERMISSIONS.forEach { shell("pm grant ${context.packageName} $it") }
        }
    }

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { String(it.readBytes()) }.also {
            Log.i(TAG, "shell: $command -> ${it.trim()}")
        }
    }

    private fun startTerminal(intent: Intent): TerminalActivity =
        instrumentation.startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as TerminalActivity

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "Terminal state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }

    private companion object {
        const val TAG = "ThreeShellTerminalUi"
    }

}
