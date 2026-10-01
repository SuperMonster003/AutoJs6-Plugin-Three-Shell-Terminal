package io.github.supermonster003.autojs6.plugin.three.shell.terminal.storage

import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import org.autojs.plugin.terminal.api.TerminalErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P2.2 device evidence: the storage state follows the plugin's own grant, a shared-storage
 * directory falls back to `$HOME` with `STORAGE_PERMISSION_REQUIRED` while the grant is missing and
 * is entered by a real shell once it is present, private directories never need the grant, and the
 * "All files access" intents resolve on API 30+. Run once without and once with the grant
 * (`pm grant` / `pm revoke` below API 30, `appops set <pkg> MANAGE_EXTERNAL_STORAGE allow|default`
 * from API 30); results are logged under [TAG].
 */
@RunWith(AndroidJUnit4::class)
class StorageAccessInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun stateFollowsThePluginsOwnGrant() {
        val expectedGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            StorageAccess.LEGACY_PERMISSIONS.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        }
        val state = StorageAccess.state(context)
        Log.i(TAG, "state: api=${Build.VERSION.SDK_INT} state=$state platformGranted=$expectedGranted external=${Environment.getExternalStorageDirectory().path}")
        assertEquals(expectedGranted, state.isGranted)
        val expectedDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) StorageAccess.State.DENIED_ALL_FILES else StorageAccess.State.DENIED_LEGACY
        if (!expectedGranted) assertEquals(expectedDenied, state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertTrue("API 30+ must resolve an all-files-access intent", StorageAccess.resolvableAllFilesAccessIntents(context).isNotEmpty())
            assertEquals(0, StorageAccess.legacyPermissionsToRequest().size)
        } else {
            assertTrue(StorageAccess.allFilesAccessIntents(context.packageName).isEmpty())
            assertEquals(2, StorageAccess.legacyPermissionsToRequest().size)
        }
    }

    @Test
    fun sharedDirectoryFallsBackWithoutTheGrantAndIsEnteredWithIt() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val target = File(Environment.getExternalStorageDirectory(), "Download")
        val state = StorageAccess.state(context)
        val resolved = StorageAccess.resolveDirectory(context, target.path, paths.home)
        Log.i(TAG, "shared: api=${Build.VERSION.SDK_INT} state=$state requested=${target.path} -> ${resolved.directory.path} reason=${resolved.fallbackReason}")
        assertTrue(StorageAccess.isSharedStorage(context, target.path))
        if (!state.isGranted) {
            assertEquals(paths.home, resolved.directory)
            assertEquals(TerminalErrorCodes.STORAGE_PERMISSION_REQUIRED, resolved.fallbackReason)
            assertTrue(resolved.fellBack)
            return
        }
        if (!target.isDirectory) assertTrue("cannot create ${target.path}", target.mkdirs())
        assertNull(resolved.fallbackReason)
        assertEquals(File(StorageAccess.normalize(target.path)!!), resolved.directory)
        val session = onMain { TerminalSessionManager.create(context, resolved.directory.path).also { it.pty.updateSize(80, 24) } }
        try {
            await { session.pty.currentDirectory()?.canonicalPath == target.canonicalPath }
            Log.i(TAG, "shared: shell ${session.pty.pid} entered ${session.pty.currentDirectory()?.path}")
        } finally {
            onMain { TerminalSessionManager.close(session.id) }
        }
    }

    @Test
    fun privateDirectoriesResolveWithoutTheGrant() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val home = paths.home
        assertFalse(StorageAccess.isSharedStorage(context, home.path))
        assertFalse(StorageAccess.isSharedStorage(context, context.filesDir.path))
        context.getExternalFilesDir(null)?.let { own ->
            assertFalse("own external files dir must not need the grant: ${own.path}", StorageAccess.isSharedStorage(context, own.path))
        }
        assertTrue(StorageAccess.isSharedStorage(context, "/sdcard"))
        assertTrue(StorageAccess.isSharedStorage(context, Environment.getExternalStorageDirectory().path))

        val same = StorageAccess.resolveDirectory(context, home.path, home)
        assertEquals(home, same.directory)
        assertNull(same.fallbackReason)

        val tmp = StorageAccess.resolveDirectory(context, paths.tmp.path + "/", home)
        assertEquals(paths.tmp, tmp.directory)
        assertNull(tmp.fallbackReason)

        val missing = StorageAccess.resolveDirectory(context, File(context.filesDir, "does-not-exist").path, home)
        assertEquals(home, missing.directory)
        assertEquals(TerminalErrorCodes.DIRECTORY_INACCESSIBLE, missing.fallbackReason)

        val file = File(paths.tmp, "not-a-directory.txt").apply { writeText("x") }
        val notDirectory = StorageAccess.resolveDirectory(context, file.path, home)
        assertEquals(home, notDirectory.directory)
        assertEquals(TerminalErrorCodes.DIRECTORY_INACCESSIBLE, notDirectory.fallbackReason)
        file.delete()

        val relative = StorageAccess.resolveDirectory(context, "Scripts", home)
        assertEquals(home, relative.directory)
        assertEquals(TerminalErrorCodes.DIRECTORY_INACCESSIBLE, relative.fallbackReason)

        val unreadable = StorageAccess.resolveDirectory(context, "/data/data", home)
        assertEquals(home, unreadable.directory)
        assertEquals(TerminalErrorCodes.DIRECTORY_INACCESSIBLE, unreadable.fallbackReason)

        val none = StorageAccess.resolveDirectory(context, null, home)
        assertEquals(home, none.directory)
        assertNull(none.fallbackReason)
        assertFalse(none.fellBack)
        Log.i(TAG, "private: home / tmp resolved, missing / file / relative / unreadable fell back with DIRECTORY_INACCESSIBLE")
    }

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
        const val TAG = "ThreeShellStorage"
    }

}
