package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.common.api.IPluginInfoProvider
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder.TerminalPluginBinder
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.terminal.api.ITerminalPlugin
import org.autojs.plugin.terminal.api.TerminalCapabilityKeys
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Verifies the host-facing activation and discovery contract against the installed APK: the Wake
 * Activity, the INFO service (with a real `getInfo()` round trip reporting the packaged ABIs), the
 * `org.autojs.plugin.TERMINAL` service with the placeholder Binder of roadmap P0, and the vendored pty
 * libraries being loadable on the device.
 */
@RunWith(AndroidJUnit4::class)
class ThreeShellTerminalPluginContractTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val packageName: String
        get() = context.packageName

    @Test
    fun wakeActivityFollowsTheHostActivationContract() {
        val applicationInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val wakeActivity = applicationInfo.metaData?.getString(WAKE_ACTIVITY_META_DATA)
        assertEquals(".WakeActivity", wakeActivity)
        assertEquals(context.getString(R.string.plugin_author), applicationInfo.metaData?.getString(AUTHOR_META_DATA))
        assertEquals(ThreeShellTerminalPlugin.NATIVE_PAGE_ALIGNMENT, applicationInfo.metaData?.getInt(NATIVE_PAGE_ALIGNMENT_META_DATA, -1))

        val component = ComponentName(packageName, packageName + wakeActivity)
        val activityInfo = context.packageManager.getActivityInfo(component, 0)
        assertTrue("Wake Activity must be exported", activityInfo.exported)
        assertTrue("Wake Activity must be enabled", activityInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, activityInfo.permission)
        assertEquals(android.R.style.Theme_NoDisplay, activityInfo.theme)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH != 0)

        val wakeIntent = Intent(WAKE_ACTION).addCategory(Intent.CATEGORY_DEFAULT).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentActivities(wakeIntent, 0)
        assertEquals("The WAKE action must resolve to exactly one activity", 1, matches.size)
        assertEquals(component.className, matches.single().activityInfo.name)
    }

    @Test
    fun exactlyOneLauncherAliasIsEnabled() {
        // Roadmap P5.3: the app drawer shows exactly one of the four icon aliases, all pointing at the private LauncherActivity.
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentActivities(launcherIntent, 0)
        assertEquals("Exactly one launcher alias is expected: ${matches.map { it.activityInfo.name }}", 1, matches.size)
        val entry = matches.single().activityInfo
        assertTrue(entry.name, entry.name.startsWith("$packageName.launcher.") && entry.name.endsWith("IconAlias"))
        assertEquals("$packageName.ui.LauncherActivity", entry.targetActivity)
    }

    @Test
    fun infoServiceIsDiscoverableAndReportsPluginInfo() {
        val serviceInfo = discoverSingleService(
            ThreeShellTerminalPlugin.INFO_ACTION,
            ThreeShellTerminalPluginInfoService::class.java.name,
        )
        assertEquals(packageName, serviceInfo.processName)

        withBoundService(serviceInfo) { binder ->
            assertEquals(IPluginInfoProvider.DESCRIPTOR, binder.interfaceDescriptor)
            val info = IPluginInfoProvider.Stub.asInterface(binder).info
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val expectedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            assertEquals("3-Shell Terminal", info.name)
            assertEquals(context.getString(R.string.app_name), info.name)
            assertEquals(context.getString(R.string.plugin_description), info.description)
            assertTrue("instruction must be read from the raw resource", info.instruction?.isNotBlank() == true)
            assertEquals(ThreeShellTerminalPlugin.AUTHOR, info.author)
            assertEquals(context.getString(R.string.plugin_author), info.author)
            assertEquals(ThreeShellTerminalPlugin.ID, info.id)
            assertEquals(context.getString(R.string.plugin_id), info.id)
            assertEquals(ThreeShellTerminalPlugin.ENGINE, info.engine)
            assertEquals(context.getString(R.string.plugin_engine), info.engine)
            assertEquals(ThreeShellTerminalPlugin.VARIANT, info.variant)
            assertEquals(context.getString(R.string.plugin_variant), info.variant)
            assertEquals(packageInfo.versionName, info.versionName)
            assertEquals(expectedVersionCode, info.versionCode)
            assertEquals(context.getString(R.string.plugin_version_date), info.versionDate)
            assertTrue(info.versionDate?.isNotBlank() == true)
            // The packaged pty libraries decide the reported ABIs (roadmap D14): a split APK reports its own
            // ABI, the universal APK all four; the running process must be covered either way.
            val supportedAbis = requireNotNull(info.supportedAbis) { "supportedAbis must not be null" }
            assertTrue("supportedAbis must not be empty", supportedAbis.isNotEmpty())
            assertArrayEquals(NativeLibraryInventory.supportedAbis(context), supportedAbis)
            assertTrue("every reported ABI must be known", supportedAbis.all { it in NativeLibraryInventory.knownAbis })
            val processAbis = if (android.os.Process.is64Bit()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
            assertTrue("the running process ABI must be packaged", processAbis.any { it in supportedAbis })
            assertCapabilities(requireNotNull(info.capabilities))
        }
    }

    @Test
    fun terminalServiceAnswersTheContractBinder() {
        val serviceInfo = discoverSingleService(
            ThreeShellTerminalPlugin.SERVICE_ACTION,
            ThreeShellTerminalPluginService::class.java.name,
        )
        assertEquals(packageName, serviceInfo.processName)

        withBoundService(serviceInfo) { binder ->
            assertEquals(ThreeShellTerminalPlugin.SERVICE_DESCRIPTOR, binder.interfaceDescriptor)
            assertTrue(binder.isBinderAlive)
            assertTrue(binder.pingBinder())
            // Since roadmap P2.4 the service hands out the ITerminalPlugin.Stub router; the guard itself is
            // covered by TerminalBinderContractTest (the instrumentation runs under the plugin uid, not the host's).
            assertTrue(binder.queryLocalInterface(ThreeShellTerminalPlugin.SERVICE_DESCRIPTOR) is TerminalPluginBinder)
            assertTrue(ITerminalPlugin.Stub.asInterface(binder) is TerminalPluginBinder)
        }
    }

    @Test
    fun vendoredPtyLibrariesLoadOnTheDevice() {
        // libjackpal-termexec2 (TermExec JNI) and libjackpal-androidterm5 (Exec JNI) must be packaged for the
        // running ABI and pass the loader's page-size checks (16 KB devices reject 4 KB aligned libraries).
        // `useLegacyPackaging = false` keeps the libraries uncompressed inside the APK, so they are mapped
        // from there rather than extracted to nativeLibraryDir; loading is the only reliable check.
        System.loadLibrary("jackpal-termexec2")
        System.loadLibrary("jackpal-androidterm5")
        val applicationInfo = context.applicationInfo
        val apks = listOfNotNull(applicationInfo.sourceDir) + applicationInfo.splitSourceDirs.orEmpty()
        val processAbi = (if (android.os.Process.is64Bit()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS)
            .first { it in NativeLibraryInventory.knownAbis }
        val packaged = apks.flatMap { apk ->
            java.util.zip.ZipFile(apk).use { zip -> zip.entries().asSequence().map { it.name }.filter { it.startsWith("lib/$processAbi/") }.toList() }
        }
        assertEquals(NativeLibraryInventory.libraryNames.map { "lib/$processAbi/$it" }.toSet(), packaged.toSet())
    }

    @Test
    fun noExportedContentProviderIsRegistered() {
        // AndroidX libraries merge the non-exported androidx.startup InitializationProvider; the plugin itself
        // declares no provider, so nothing outside the package may reach one.
        val packageInfo = context.packageManager.getPackageInfo(packageName, PackageManager.GET_PROVIDERS)
        val exported = packageInfo.providers.orEmpty().filter { it.exported }
        assertTrue("P0 registers no exported content provider, found ${exported.map { it.name }}", exported.isEmpty())
    }

    private fun assertCapabilities(capabilities: Bundle) {
        assertEquals(ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
        // Roadmap P2.4: the contract version the host validates, the implemented features, the ceilings and the Node CLI state.
        assertEquals(
            setOf(
                PluginCapabilityKeys.REQUIRES_HOST_VERSION,
                TerminalCapabilityKeys.CONTRACT_VERSION,
                TerminalCapabilityKeys.FEATURES_KEY,
                TerminalCapabilityKeys.MAX_SESSIONS,
                TerminalCapabilityKeys.MAX_SUBSCRIPTIONS,
                TerminalCapabilityKeys.NODE_CLI,
            ),
            capabilities.keySet(),
        )
        assertEquals(TerminalContract.CONTRACT_VERSION, capabilities.getInt(TerminalCapabilityKeys.CONTRACT_VERSION))
        assertArrayEquals(ThreeShellTerminalPlugin.FEATURES.toTypedArray(), capabilities.getStringArray(TerminalCapabilityKeys.FEATURES_KEY))
        assertEquals(TerminalContract.MAX_SESSIONS, capabilities.getInt(TerminalCapabilityKeys.MAX_SESSIONS))
        assertEquals(TerminalContract.MAX_SUBSCRIPTIONS_PER_SESSION, capabilities.getInt(TerminalCapabilityKeys.MAX_SUBSCRIPTIONS))
        assertTrue(TerminalContract.isNodeCliState(capabilities.getString(TerminalCapabilityKeys.NODE_CLI)))
    }

    private fun discoverSingleService(action: String, expectedClassName: String): ServiceInfo {
        val discoveryIntent = Intent(action)
            .addCategory(ThreeShellTerminalPlugin.SERVICE_CATEGORY)
            .setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(discoveryIntent, PackageManager.GET_META_DATA)
        assertEquals("The discovery contract for $action must resolve exactly one service", 1, matches.size)

        val serviceInfo = matches.single().serviceInfo
        assertEquals(packageName, serviceInfo.packageName)
        assertEquals(expectedClassName, serviceInfo.name)
        assertTrue("$expectedClassName must be exported", serviceInfo.exported)
        assertTrue("$expectedClassName must be enabled", serviceInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, serviceInfo.permission)
        val requiresHostVersion = requireNotNull(serviceInfo.metaData) { "requiresHostVersion meta-data is missing" }
            .getInt(REQUIRES_HOST_VERSION_META_DATA)
        assertEquals(ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION, requiresHostVersion.toLong())
        return serviceInfo
    }

    private fun withBoundService(serviceInfo: ServiceInfo, block: (IBinder) -> Unit) {
        val binderReference = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binderReference.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit

            override fun onNullBinding(name: ComponentName) {
                connected.countDown()
            }
        }

        val explicitIntent = Intent().setComponent(ComponentName(serviceInfo.packageName, serviceInfo.name))
        assertTrue("bindService returned false", context.bindService(explicitIntent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("Timed out waiting for the Binder service", connected.await(10, TimeUnit.SECONDS))
            val binder = binderReference.get()
            assertNotNull("The service returned a null Binder", binder)
            block(binder)
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val WAKE_ACTION = "org.autojs.plugin.action.WAKE"
        const val WAKE_ACTIVITY_META_DATA = "org.autojs.plugin.WAKE_ACTIVITY"
        const val AUTHOR_META_DATA = "org.autojs.plugin.info.AUTHOR"
        const val NATIVE_PAGE_ALIGNMENT_META_DATA = "org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"
        const val REQUIRES_HOST_VERSION_META_DATA = "requiresHostVersion"
    }
}
