package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import org.junit.Assert.assertEquals
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
    fun `the wake activity is the only activity and follows the activation contract`() {
        val wake = manifest.child("application").children("activity").single()
        assertEquals(".WakeActivity", wake.androidAttribute("name"))
        assertEquals("true", wake.androidAttribute("exported"))
        assertEquals("true", wake.androidAttribute("excludeFromRecents"))
        assertEquals("true", wake.androidAttribute("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.androidAttribute("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.androidAttribute("theme"))
        val wakeFilter = wake.child("intent-filter")
        assertEquals(listOf("org.autojs.plugin.action.WAKE"), wakeFilter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.DEFAULT"), wakeFilter.children("category").map { it.androidAttribute("name") })
        assertTrue(manifest.child("application").children("activity-alias").isEmpty())
        assertTrue(manifest.child("application").children("receiver").isEmpty())
        assertTrue(manifest.child("application").children("provider").isEmpty())
    }

    @Test
    fun `info service and terminal service match the identity constants`() {
        val services = manifest.child("application").children("service").associateBy { it.androidAttribute("name") }
        assertEquals(setOf(".ThreeShellTerminalPluginInfoService", ".ThreeShellTerminalPluginService"), services.keys)

        val info = services.getValue(".ThreeShellTerminalPluginInfoService")
        assertDiscoveryContract(info, ThreeShellTerminalPlugin.INFO_ACTION)
        assertNull(info.androidAttributeOrNull("process"))

        val terminal = services.getValue(".ThreeShellTerminalPluginService")
        assertDiscoveryContract(terminal, ThreeShellTerminalPlugin.SERVICE_ACTION)
        assertNull(terminal.androidAttributeOrNull("process"))
    }

    @Test
    fun `only discovery and activation components are exported`() {
        val expected = mapOf(
            ".WakeActivity" to PLUGIN_PERMISSION,
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
    }
}
