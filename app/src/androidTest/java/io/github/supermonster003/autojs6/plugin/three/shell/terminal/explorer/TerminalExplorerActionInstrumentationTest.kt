package io.github.supermonster003.autojs6.plugin.three.shell.terminal.explorer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalExplorerActionService
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalExplorerActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalEmulatorView
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.explorer.api.ExplorerActionCapabilityKeys
import org.autojs.plugin.explorer.api.ExplorerActionCatalogKeys
import org.autojs.plugin.explorer.api.ExplorerActionIntentExtras
import org.autojs.plugin.explorer.api.ExplorerActionPluginIds
import org.autojs.plugin.explorer.api.ExplorerActionTargetKeys
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenValues
import org.autojs.plugin.explorer.api.IExplorerActionPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P8.1 device evidence for the file manager's "Open in terminal" Explorer Action (appendix D
 * Q10): the discovery service answers the identity and the protocol v23 catalog the host's registry
 * accepts, the adb shell cannot start the exported Activity, a same-signer request (the plugin's own
 * uid stands in for the host) opens the terminal in the selected directory in its own task, and
 * requests outside the declared shape open nothing. Results are logged under [TAG].
 */
@RunWith(AndroidJUnit4::class)
class TerminalExplorerActionInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val activity = ComponentName(context, ThreeShellTerminalExplorerActivity::class.java)

    @Before
    fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun theDiscoveryServiceAnswersTheExplorerIdentityAndTheDirectoryCatalog() {
        val services = context.packageManager.queryIntentServices(
            Intent(ThreeShellTerminalPlugin.EXPLORER_ACTION).setPackage(context.packageName),
            PackageManager.GET_META_DATA,
        )
        val serviceInfo = services.single().serviceInfo
        assertEquals(ThreeShellTerminalExplorerActionService::class.java.name, serviceInfo.name)
        assertTrue(serviceInfo.exported)
        assertEquals(ThreeShellTerminalPlugin.PLUGIN_PERMISSION, serviceInfo.permission)
        assertNull("the plugin center keeps reading requiresHostVersion from the INFO service", serviceInfo.metaData?.get("requiresHostVersion"))

        withBoundService(ComponentName(serviceInfo.packageName, serviceInfo.name)) { binder ->
            assertEquals(IExplorerActionPlugin.DESCRIPTOR, binder.interfaceDescriptor)
            val plugin = IExplorerActionPlugin.Stub.asInterface(binder)

            val info = plugin.info
            assertEquals(ExplorerActionPluginIds.ENGINE, info.engine)
            assertEquals(ThreeShellTerminalPlugin.ID, info.id)
            assertEquals(context.getString(R.string.app_name), info.name)
            assertTrue("the packaged ABIs are reported", info.supportedAbis?.isNotEmpty() == true)
            val capabilities = requireNotNull(info.capabilities)
            assertEquals(ThreeShellTerminalPlugin.EXPLORER_REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
            assertEquals(23, capabilities.getInt(ExplorerActionCapabilityKeys.PROTOCOL_VERSION))
            assertEquals(
                "only the two capabilities the registry reads; no Node.js state is computed for this identity",
                setOf(PluginCapabilityKeys.REQUIRES_HOST_VERSION, ExplorerActionCapabilityKeys.PROTOCOL_VERSION),
                capabilities.keySet(),
            )

            val catalog = plugin.actionCatalog
            assertEquals(23, catalog.getInt(ExplorerActionCatalogKeys.PROTOCOL_VERSION))
            val action = catalogActions(catalog).single()
            assertEquals(ThreeShellTerminalPlugin.EXPLORER_DIRECTORY_ACTION_ID, action.getString(ExplorerActionCatalogKeys.ID))
            assertEquals(ThreeShellTerminalExplorerActivity::class.java.name, action.getString(ExplorerActionCatalogKeys.ACTIVITY_CLASS_NAME))
            assertEquals(ExplorerActionValues.TARGET_DIRECTORY, action.getInt(ExplorerActionCatalogKeys.TARGET_KIND))
            assertEquals(ExplorerActionValues.CARDINALITY_SINGLE, action.getInt(ExplorerActionCatalogKeys.CARDINALITY))
            assertEquals(ExplorerActionValues.ACCESS_READ_ONLY, action.getInt(ExplorerActionCatalogKeys.ACCESS_MODE))
            assertEquals(ExplorerActionValues.PLACEMENT_OVERFLOW, action.getInt(ExplorerActionCatalogKeys.PLACEMENT))
            assertEquals(ExplorerActionValues.PRESENTATION_ACTIVITY, action.getInt(ExplorerActionCatalogKeys.PRESENTATION))
            assertTrue(action.getBoolean(ExplorerActionCatalogKeys.FILESYSTEM_PATH))
            assertFalse("no output, matcher or session capability is declared", listOf(
                ExplorerActionCatalogKeys.OUTPUT_MODE, ExplorerActionCatalogKeys.MIME_TYPES, ExplorerActionCatalogKeys.EXTENSIONS,
                ExplorerActionCatalogKeys.FILE_NAME_SUFFIXES, ExplorerActionCatalogKeys.PROBE_FOR_ACTION_ID,
                ExplorerActionCatalogKeys.READ_SIBLINGS, ExplorerActionCatalogKeys.PLAYBACK_PROGRESS,
                ExplorerActionCatalogKeys.RELATED_FILE_SUFFIXES,
            ).any(action::containsKey))

            // The label the host resolves by name exists in this package and is the translated string.
            val labelName = requireNotNull(action.getString(ExplorerActionCatalogKeys.LABEL_RESOURCE_NAME))
            val labelId = context.resources.getIdentifier(labelName, "string", context.packageName)
            assertEquals(R.string.terminal_explorer_open_in_terminal, labelId)
            assertEquals(context.getString(R.string.terminal_explorer_open_in_terminal), context.getString(labelId))

            // The archive transactions belong to other catalog shapes and refuse without a session.
            val pipe = ParcelFileDescriptor.createPipe()
            try {
                val refused = plugin.openArchiveV11(pipe[0], Bundle())
                assertEquals(ExplorerArchiveOpenValues.ERROR_UNRECOVERABLE, refused.getInt(ExplorerArchiveOpenKeys.ERROR_CODE))
                assertNull(refused.getBinder(ExplorerArchiveOpenKeys.SESSION_BINDER))
                assertNull(plugin.openArchive(pipe[0], Bundle()))
                assertNull(plugin.getHostFileInfo(pipe[0], Bundle()))
            } finally {
                pipe.forEach { runCatching { it.close() } }
            }
            Log.i(TAG, "service: identity ${info.engine} requires ${ThreeShellTerminalPlugin.EXPLORER_REQUIRED_HOST_VERSION}, catalog v23 with one directory action on API ${Build.VERSION.SDK_INT}")
        }
    }

    @Test
    fun aCallerWithoutThePluginPermissionIsRefusedByAndroid() {
        val before = sessionIds()
        shell("logcat -c")
        val output = shell(
            "am start -a ${ThreeShellTerminalPlugin.EXPLORER_EXECUTE_ACTION} -c android.intent.category.DEFAULT -n ${activity.flattenToString()}",
        )
        SystemClock.sleep(1_500)
        val denial = (output + shell("logcat -d -s ActivityManager:W ActivityTaskManager:W")).lineSequence()
            .firstOrNull { it.contains("Permission Denial") && it.contains(ThreeShellTerminalPlugin.EXPLORER_EXECUTE_ACTION) }
        assertTrue("Android must refuse the shell uid before the activity runs: $output", denial != null)
        assertTrue(denial!!, denial.contains("uid=2000") && denial.contains("requires ${ThreeShellTerminalPlugin.PLUGIN_PERMISSION}"))
        onMain { assertNull(resumedTerminal()) }
        assertEquals(before, sessionIds())
        Log.i(TAG, "activity: shell caller refused by Android on API ${Build.VERSION.SDK_INT}")
    }

    @Test
    fun aSameSignerRequestOpensTheTerminalInTheSelectedDirectory() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val directory = File(paths.tmp, "explorer-action").apply { mkdirs() }
        val before = sessionIds()
        var terminal: TerminalActivity? = null
        try {
            instrumentation.startActivitySync(executeIntent(directory.canonicalPath))
            val screen = awaitValue { onMain { resumedTerminal() } }
            terminal = screen
            // The most recent session changes into the directory, or a new session starts there (the former host menu item).
            val session = awaitValue {
                onMain {
                    val attached = screen.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession
                    TerminalSessionManager.activeSessions.firstOrNull { it.pty === attached }
                }
            }
            await { session.pty.pid > 0 && session.pty.currentDirectory()?.let { sameDirectory(directory, it) } == true }
            onMain {
                assertTrue("the terminal opened from the file manager owns its task", screen.isTaskRoot)
                assertFalse("the forwarder never shows", resumedForwarder())
            }
            onMain { screen.onBackPressedDispatcher.onBackPressed() }
            await { screen.isFinishing || screen.isDestroyed }
            SystemClock.sleep(300)
            assertTrue("the session outlives the screen", session.pty.isProcessAlive)
            Log.i(TAG, "activity: ${if (session.id in before) "reused" else "started"} ${session.id} in the selected directory on API ${Build.VERSION.SDK_INT}")
        } finally {
            onMain {
                terminal?.takeUnless { it.isDestroyed }?.finish()
                TerminalSessionManager.activeSessions.filter { it.id !in before }.forEach { TerminalSessionManager.close(it.id) }
            }
        }
    }

    @Test
    fun requestsOutsideTheDeclaredShapeOpenNothing() {
        val paths = TerminalPaths.of(context).ensureLayout()
        val before = sessionIds()
        val path = paths.tmp.canonicalPath
        listOf(
            "protocol 22" to executeIntent(path).putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, 22),
            "another action id" to executeIntent(path).putExtra(ExplorerActionIntentExtras.ACTION_ID, "three-shell-terminal.other"),
            "a file target" to executeIntent(path, kind = ExplorerActionValues.TARGET_FILE),
            "a relative path" to executeIntent("tmp/explorer-action"),
            "a parent segment" to executeIntent("$path/../.."),
            "no path" to executeIntent(null),
        ).forEach { (label, intent) ->
            instrumentation.startActivitySync(intent)
            SystemClock.sleep(800)
            onMain { assertNull("$label must not open the terminal", resumedTerminal()) }
            assertEquals("$label must not start a session", before, sessionIds())
        }
        Log.i(TAG, "activity: six malformed requests opened nothing on API ${Build.VERSION.SDK_INT}")
    }

    private fun executeIntent(path: String?, kind: Int = ExplorerActionValues.TARGET_DIRECTORY): Intent =
        Intent(ThreeShellTerminalPlugin.EXPLORER_EXECUTE_ACTION)
            .setComponent(activity)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(ExplorerActionIntentExtras.ACTION_ID, ThreeShellTerminalPlugin.EXPLORER_DIRECTORY_ACTION_ID)
            .putExtra(ExplorerActionIntentExtras.PROTOCOL_VERSION, ThreeShellTerminalPlugin.EXPLORER_PROTOCOL_VERSION)
            .putParcelableArrayListExtra(
                ExplorerActionIntentExtras.TARGETS,
                arrayListOf(
                    Bundle().apply {
                        putString(ExplorerActionTargetKeys.ID, "target")
                        putInt(ExplorerActionTargetKeys.KIND, kind)
                        path?.let { putString(ExplorerActionTargetKeys.FILESYSTEM_PATH, it) }
                    },
                ),
            )

    private fun catalogActions(catalog: Bundle): List<Bundle> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            catalog.getParcelableArrayList(ExplorerActionCatalogKeys.ACTIONS, Bundle::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            catalog.getParcelableArrayList<Bundle>(ExplorerActionCatalogKeys.ACTIONS).orEmpty()
        }

    private fun withBoundService(component: ComponentName, block: (IBinder) -> Unit) {
        val binderReference = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binderReference.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        assertTrue(context.bindService(Intent().setComponent(component), connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("Timed out waiting for the Explorer Action service", connected.await(10, TimeUnit.SECONDS))
            block(assertNotNullAndGet(binderReference.get()))
        } finally {
            context.unbindService(connection)
        }
    }

    private fun assertNotNullAndGet(binder: IBinder?): IBinder {
        assertNotNull("The service returned a null Binder", binder)
        return binder!!
    }

    private fun sessionIds(): Set<String> = TerminalSessionManager.activeSessions.map { it.id }.toSet()

    private fun resumedTerminal() = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<TerminalActivity>().singleOrNull()

    private fun resumedForwarder() = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).any { it is ThreeShellTerminalExplorerActivity }

    private fun sameDirectory(expected: File, actual: File): Boolean {
        if (expected.canonicalPath == actual.canonicalPath) return true
        val left = runCatching { android.system.Os.stat(expected.path) }.getOrNull() ?: return false
        val right = runCatching { android.system.Os.stat(actual.path) }.getOrNull() ?: return false
        return left.st_dev == right.st_dev && left.st_ino == right.st_ino
    }

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { String(it.readBytes()) }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "Terminal state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T : Any> awaitValue(supplier: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (true) {
            supplier()?.let { return it }
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
        const val TAG = "ThreeShellTerminalExplorer"
    }

}
