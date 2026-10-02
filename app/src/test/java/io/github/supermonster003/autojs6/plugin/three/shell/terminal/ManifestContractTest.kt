package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Keeps `AndroidManifest.xml` and [ThreeShellTerminalPlugin] from drifting apart: the host discovers the
 * plugin through the manifest, while the services and tests use the Kotlin constants.
 */
class ManifestContractTest {

    private val manifest: Element by lazy {
        val path = findProjectRoot().resolve("app/src/main/AndroidManifest.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        factory.newDocumentBuilder().parse(path.toFile()).documentElement
    }

    @Test
    fun `manifest declares the P0 permissions and queries the host and the Node runtime plugin`() {
        val permissions = manifest.children("uses-permission").map { it.androidAttribute("name") to it.androidAttributeOrNull("maxSdkVersion") }
        assertEquals(
            listOf(
                PLUGIN_PERMISSION to null,
                "android.permission.READ_EXTERNAL_STORAGE" to "32",
                "android.permission.WRITE_EXTERNAL_STORAGE" to "29",
                "android.permission.MANAGE_EXTERNAL_STORAGE" to null,
                "android.permission.FOREGROUND_SERVICE" to null,
                "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" to null,
                "android.permission.POST_NOTIFICATIONS" to null,
                "android.permission.INTERNET" to null,
            ),
            permissions,
        )

        val queries = manifest.child("queries")
        assertEquals(
            listOf(ThreeShellTerminalPlugin.HOST_PACKAGE_NAME, ThreeShellTerminalPlugin.NODEJS_PACKAGE_NAME),
            queries.children("package").map { it.androidAttribute("name") },
        )
        val intent = queries.children("intent").single()
        assertEquals(ThreeShellTerminalPlugin.NODEJS_SERVICE_ACTION, intent.child("action").androidAttribute("name"))
    }

    @Test
    fun `the vendored jackpal libraries are the only manifest merger overrides`() {
        val usesSdk = manifest.child("uses-sdk")
        assertEquals(
            "jackpal.androidterm,jackpal.androidterm.emulatorview,jackpal.androidterm.libtermexec",
            usesSdk.getAttributeNS(TOOLS_NAMESPACE, "overrideLibrary"),
        )
        assertNull(usesSdk.androidAttributeOrNull("minSdkVersion"))
    }

    @Test
    fun `application metadata points at the wake activity, the author string and the 16 KB alignment`() {
        val application = manifest.child("application")
        assertEquals("false", application.androidAttribute("allowBackup"))
        assertEquals("@string/app_name", application.androidAttribute("label"))
        assertEquals("@mipmap/ic_launcher_system", application.androidAttribute("icon"))
        assertEquals("@mipmap/ic_launcher_system", application.androidAttribute("roundIcon"))
        assertEquals("@style/Theme.ThreeShellTerminal", application.androidAttribute("theme"))
        assertEquals("@xml/data_extraction_rules", application.androidAttribute("dataExtractionRules"))
        assertEquals("@xml/locales_config", application.androidAttribute("localeConfig"))

        val metaData = application.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(".WakeActivity", metaData["org.autojs.plugin.WAKE_ACTIVITY"])
        assertEquals("@string/plugin_author", metaData["org.autojs.plugin.info.AUTHOR"])
        assertEquals(ThreeShellTerminalPlugin.NATIVE_PAGE_ALIGNMENT.toString(), metaData["org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"])
        assertEquals("16384", metaData["org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"])
    }

    @Test
    fun `the wake activity follows the activation contract`() {
        val wake = manifest.child("application").children("activity").single { it.androidAttribute("name") == ".WakeActivity" }
        assertEquals("true", wake.androidAttribute("exported"))
        assertEquals("true", wake.androidAttribute("excludeFromRecents"))
        assertEquals("true", wake.androidAttribute("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.androidAttribute("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.androidAttribute("theme"))
        val wakeFilter = wake.child("intent-filter")
        assertEquals(listOf("org.autojs.plugin.action.WAKE"), wakeFilter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.DEFAULT"), wakeFilter.children("category").map { it.androidAttribute("name") })
        assertTrue(manifest.child("application").children("provider").isEmpty())
    }

    @Test
    fun `the launcher icon aliases carry MAIN LAUNCHER, default to automatic and target the forwarder`() {
        val aliases = manifest.child("application").children("activity-alias")
        assertEquals(
            listOf(".launcher.AdaptiveLightIconAlias", ".launcher.AdaptiveDarkIconAlias", ".launcher.AdaptiveAutoIconAlias", ".launcher.TransparentIconAlias"),
            aliases.map { it.androidAttribute("name") },
        )
        val icons = mapOf(
            ".launcher.AdaptiveLightIconAlias" to "@mipmap/ic_launcher_system_light",
            ".launcher.AdaptiveDarkIconAlias" to "@mipmap/ic_launcher_system",
            ".launcher.AdaptiveAutoIconAlias" to "@mipmap/ic_launcher_system_auto",
            ".launcher.TransparentIconAlias" to "@mipmap/ic_launcher",
        )
        aliases.forEach { alias ->
            val name = alias.androidAttribute("name")
            assertEquals("true", alias.androidAttribute("exported"))
            assertNull("aliases need no caller permission", alias.androidAttributeOrNull("permission"))
            assertEquals(".ui.LauncherActivity", alias.androidAttribute("targetActivity"))
            assertEquals(icons.getValue(name), alias.androidAttribute("icon"))
            assertEquals(icons.getValue(name), alias.androidAttribute("roundIcon"))
            assertEquals("only the automatic icon is enabled by default", (name == ".launcher.AdaptiveAutoIconAlias").toString(), alias.androidAttribute("enabled"))
            val filter = alias.child("intent-filter")
            assertEquals(listOf("android.intent.action.MAIN"), filter.children("action").map { it.androidAttribute("name") })
            assertEquals(listOf("android.intent.category.LAUNCHER"), filter.children("category").map { it.androidAttribute("name") })
        }
        // MAIN / LAUNCHER live only on the aliases.
        manifest.child("application").children("activity").forEach { activity ->
            activity.children("intent-filter").forEach { filter ->
                assertTrue(activity.androidAttribute("name"), filter.children("category").none { it.androidAttribute("name") == "android.intent.category.LAUNCHER" })
            }
        }
        val receiver = manifest.child("application").children("receiver").single()
        assertEquals(".ui.settings.LauncherIconUpdateReceiver", receiver.androidAttribute("name"))
        assertEquals("false", receiver.androidAttribute("exported"))
        assertEquals(listOf("android.intent.action.MY_PACKAGE_REPLACED"), receiver.child("intent-filter").children("action").map { it.androidAttribute("name") })
    }

    @Test
    fun `the terminal screen stays private to the plugin`() {
        val activities = manifest.child("application").children("activity").map { it.androidAttribute("name") }
        assertEquals(listOf(".WakeActivity", ".ThreeShellTerminalEntryActivity", ".ui.LauncherActivity", ".ui.TerminalActivity", ".ui.TerminalManagerActivity"), activities)
        val terminal = manifest.child("application").children("activity").single { it.androidAttribute("name") == ".ui.TerminalActivity" }
        assertEquals("false", terminal.androidAttribute("exported"))
        assertNull("the terminal screen needs no caller permission because it is not exported", terminal.androidAttributeOrNull("permission"))
        assertEquals("standard", terminal.androidAttribute("launchMode"))
        assertEquals("io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalActivity", terminal.androidAttribute("taskAffinity"))
        assertEquals("@style/Theme.ThreeShellTerminal.Terminal", terminal.androidAttribute("theme"))
        assertEquals("adjustResize|stateVisible", terminal.androidAttribute("windowSoftInputMode"))
        assertTrue("the terminal screen must not be reachable through an intent filter", terminal.children("intent-filter").isEmpty())
    }

    @Test
    fun `the terminal entry activity follows the host protocol`() {
        val entry = manifest.child("application").children("activity").single { it.androidAttribute("name") == ".ThreeShellTerminalEntryActivity" }
        assertEquals("true", entry.androidAttribute("exported"))
        assertEquals(PLUGIN_PERMISSION, entry.androidAttribute("permission"))
        assertEquals("true", entry.androidAttribute("excludeFromRecents"))
        assertEquals("@android:style/Theme.NoDisplay", entry.androidAttribute("theme"))
        assertNull("the entry joins the caller's task", entry.androidAttributeOrNull("taskAffinity"))
        assertNull(entry.androidAttributeOrNull("launchMode"))
        val filter = entry.child("intent-filter")
        assertEquals(listOf(ThreeShellTerminalPlugin.OPEN_TERMINAL_ACTION), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("org.autojs.plugin.TERMINAL_OPEN"), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.DEFAULT"), filter.children("category").map { it.androidAttribute("name") })
        assertTrue("the host resolves exactly one TERMINAL_OPEN activity", manifest.child("application").children("activity").count { activity ->
            activity.children("intent-filter").any { f -> f.children("action").any { it.androidAttribute("name") == ThreeShellTerminalPlugin.OPEN_TERMINAL_ACTION } }
        } == 1)
    }

    @Test
    fun `the launcher activity is a private forwarder in the terminal task`() {
        val launcher = manifest.child("application").children("activity").single { it.androidAttribute("name") == ".ui.LauncherActivity" }
        assertEquals("false", launcher.androidAttribute("exported"))
        assertNull(launcher.androidAttributeOrNull("permission"))
        assertEquals("@android:style/Theme.NoDisplay", launcher.androidAttribute("theme"))
        val terminal = manifest.child("application").children("activity").single { it.androidAttribute("name") == ".ui.TerminalActivity" }
        assertEquals("the home screen task must be the terminal task", terminal.androidAttribute("taskAffinity"), launcher.androidAttribute("taskAffinity"))
        assertNull("excluding the task root would hide the whole terminal task from recents", launcher.androidAttributeOrNull("excludeFromRecents"))
        assertTrue("MAIN / LAUNCHER arrive with the icon aliases of P5.3", launcher.children("intent-filter").isEmpty())
    }

    @Test
    fun `the session manager activity is a private transparent dialog host`() {
        val manager = manifest.child("application").children("activity").single { it.androidAttribute("name") == ".ui.TerminalManagerActivity" }
        assertEquals("false", manager.androidAttribute("exported"))
        assertNull(manager.androidAttributeOrNull("permission"))
        assertEquals("true", manager.androidAttribute("excludeFromRecents"))
        assertEquals("singleTop", manager.androidAttribute("launchMode"))
        assertEquals("@style/Theme.ThreeShellTerminal.Transparent", manager.androidAttribute("theme"))
        assertNull("the manager host keeps the package task affinity", manager.androidAttributeOrNull("taskAffinity"))
        assertTrue(manager.children("intent-filter").isEmpty())
    }

    @Test
    fun `info service and terminal service match the identity constants`() {
        val services = manifest.child("application").children("service").associateBy { it.androidAttribute("name") }
        assertEquals(setOf(".ThreeShellTerminalPluginInfoService", ".ThreeShellTerminalPluginService", SESSION_SERVICE), services.keys)

        val info = services.getValue(".ThreeShellTerminalPluginInfoService")
        assertDiscoveryContract(info, ThreeShellTerminalPlugin.INFO_ACTION)
        assertNull(info.androidAttributeOrNull("process"))

        val terminal = services.getValue(".ThreeShellTerminalPluginService")
        assertDiscoveryContract(terminal, ThreeShellTerminalPlugin.SERVICE_ACTION)
        assertNull(terminal.androidAttributeOrNull("process"))
    }

    @Test
    fun `the session service is a private special-use foreground service`() {
        val service = manifest.child("application").children("service").single { it.androidAttribute("name") == SESSION_SERVICE }
        assertEquals("false", service.androidAttribute("exported"))
        assertEquals("true", service.androidAttribute("enabled"))
        assertEquals("specialUse", service.androidAttribute("foregroundServiceType"))
        assertNull(service.androidAttributeOrNull("permission"))
        assertNull(service.androidAttributeOrNull("process"))
        assertTrue(service.children("intent-filter").isEmpty())
        assertTrue(service.children("meta-data").isEmpty())
        val property = service.child("property")
        assertEquals("android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE", property.androidAttribute("name"))
        val subtype = property.androidAttribute("value")
        assertTrue("the special-use subtype must describe the plugin's terminal sessions", subtype.contains("3-Shell Terminal") && subtype.contains("shell"))
        assertFalse("the special-use subtype must not describe the host", subtype.contains("AutoJs6"))
    }

    @Test
    fun `only discovery, activation, the host entry and the launcher aliases are exported`() {
        val expected = mapOf(
            ".WakeActivity" to PLUGIN_PERMISSION,
            ".ThreeShellTerminalEntryActivity" to PLUGIN_PERMISSION,
            ".launcher.AdaptiveLightIconAlias" to null,
            ".launcher.AdaptiveDarkIconAlias" to null,
            ".launcher.AdaptiveAutoIconAlias" to null,
            ".launcher.TransparentIconAlias" to null,
            ".ThreeShellTerminalPluginInfoService" to PLUGIN_PERMISSION,
            ".ThreeShellTerminalPluginService" to PLUGIN_PERMISSION,
        )
        val components = listOf("activity", "activity-alias", "service", "receiver", "provider")
            .flatMap { manifest.child("application").children(it) }
        val exported = components.filter { it.androidAttribute("exported") == "true" }
        assertEquals(expected, exported.associate { it.androidAttribute("name") to it.androidAttributeOrNull("permission") })
        components.filterNot { it in exported }.forEach { assertEquals("false", it.androidAttribute("exported")) }
    }

    private fun assertDiscoveryContract(service: Element, action: String) {
        assertEquals("true", service.androidAttribute("exported"))
        assertEquals("true", service.androidAttribute("enabled"))
        assertEquals(PLUGIN_PERMISSION, service.androidAttribute("permission"))
        val filter = service.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf(ThreeShellTerminalPlugin.SERVICE_CATEGORY), filter.children("category").map { it.androidAttribute("name") })
        val metaData = service.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION.toString(), metaData["requiresHostVersion"])
    }

    private fun Element.children(tag: String): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length)
            .map { nodes.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName == tag }
    }

    private fun Element.child(tag: String): Element = children(tag).single()

    private fun Element.androidAttribute(name: String): String =
        androidAttributeOrNull(name) ?: error("Missing android:$name on <$tagName>")

    private fun Element.androidAttributeOrNull(name: String): String? =
        if (hasAttributeNS(ANDROID_NAMESPACE, name)) getAttributeNS(ANDROID_NAMESPACE, name) else null

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val TOOLS_NAMESPACE = "http://schemas.android.com/tools"
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val SESSION_SERVICE = ".service.ThreeShellTerminalSessionService"
    }
}
