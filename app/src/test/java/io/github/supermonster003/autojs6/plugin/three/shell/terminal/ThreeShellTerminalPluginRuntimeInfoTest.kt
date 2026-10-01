package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.nodejs.api.NodeJsPluginActions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ThreeShellTerminalPluginRuntimeInfoTest {

    @Test
    fun `runtime fields are assembled without losing the plugin identity`() {
        val info = ThreeShellTerminalPluginRuntimeInfo(
            name = "3-Shell Terminal",
            description = "Multi-session terminal running the system shell in a pty, with background sessions, a key bar and Node.js commands",
            instruction = "# 3-Shell Terminal",
            versionName = "1.0.0",
            versionCode = 3L,
            versionDate = "Oct 1, 2026",
            supportedAbis = arrayOf("arm64-v8a"),
        )

        assertEquals("3-Shell Terminal", info.name)
        assertEquals("Multi-session terminal running the system shell in a pty, with background sessions, a key bar and Node.js commands", info.description)
        assertEquals("# 3-Shell Terminal", info.instruction)
        assertEquals("SuperMonster003", info.author)
        assertEquals("three-shell-terminal", info.id)
        assertEquals("terminal", info.engine)
        assertEquals("default", info.variant)
        assertEquals("1.0.0", info.versionName)
        assertEquals(3L, info.versionCode)
        assertEquals("Oct 1, 2026", info.versionDate)
        assertArrayEquals(arrayOf("arm64-v8a"), info.supportedAbis)
        assertEquals(5303L, info.requiresHostVersion)
        assertEquals(ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION, info.requiresHostVersion)
    }

    @Test
    fun `equality compares the packaged ABIs by content`() {
        fun info(vararg abis: String) = ThreeShellTerminalPluginRuntimeInfo("n", "d", null, "1.0.0", 1L, "Oct 1, 2026", arrayOf(*abis))
        assertEquals(info("x86_64", "x86"), info("x86_64", "x86"))
        assertEquals(info("x86_64", "x86").hashCode(), info("x86_64", "x86").hashCode())
        assertTrue(info("x86_64") != info("x86"))
        assertTrue(info() != info("x86"))
    }

    @Test
    fun `identity constants follow the host discovery contract`() {
        assertEquals("io.github.supermonster003.autojs6.plugin.three.shell.terminal", ThreeShellTerminalPlugin.PACKAGE_NAME)
        assertEquals("org.autojs.autojs6", ThreeShellTerminalPlugin.HOST_PACKAGE_NAME)
        assertEquals("io.github.supermonster003.autojs6.plugin.nodejs", ThreeShellTerminalPlugin.NODEJS_PACKAGE_NAME)
        assertEquals("org.autojs.plugin.nodejs.RUNTIME", ThreeShellTerminalPlugin.NODEJS_SERVICE_ACTION)
        assertEquals(NodeJsPluginActions.RUNTIME, ThreeShellTerminalPlugin.NODEJS_SERVICE_ACTION)
        assertEquals("three-shell-terminal", ThreeShellTerminalPlugin.ID)
        assertEquals("terminal", ThreeShellTerminalPlugin.ENGINE)
        assertEquals("default", ThreeShellTerminalPlugin.VARIANT)
        assertEquals("SuperMonster003", ThreeShellTerminalPlugin.AUTHOR)
        assertEquals("org.autojs.plugin.TERMINAL", ThreeShellTerminalPlugin.SERVICE_ACTION)
        assertEquals("terminal", ThreeShellTerminalPlugin.SERVICE_CATEGORY)
        assertEquals("org.autojs.plugin.INFO", ThreeShellTerminalPlugin.INFO_ACTION)
        assertEquals(PluginActions.INFO, ThreeShellTerminalPlugin.INFO_ACTION)
        assertEquals("org.autojs.plugin.terminal.api.ITerminalPlugin", ThreeShellTerminalPlugin.SERVICE_DESCRIPTOR)
        assertEquals(16384, ThreeShellTerminalPlugin.NATIVE_PAGE_ALIGNMENT)
    }

    @Test
    fun `identity constants match the values the documentation and build publish`() {
        val root = findProjectRoot()
        val common = Files.readString(root.resolve(".readme/common.json"))
        assertTrue(common.contains("\"plugin_application_id\": \"${ThreeShellTerminalPlugin.PACKAGE_NAME}\""))
        assertTrue(common.contains("\"plugin_id\": \"${ThreeShellTerminalPlugin.ID}\""))
        assertTrue(common.contains("\"plugin_engine\": \"${ThreeShellTerminalPlugin.ENGINE}\""))
        assertTrue(common.contains("\"plugin_variant\": \"${ThreeShellTerminalPlugin.VARIANT}\""))
        assertTrue(common.contains("\"plugin_service_action\": \"${ThreeShellTerminalPlugin.SERVICE_ACTION}\""))
        assertTrue(common.contains("\"plugin_service_category\": \"${ThreeShellTerminalPlugin.SERVICE_CATEGORY}\""))
        assertTrue(common.contains("\"plugin_aidl_interface\": \"${ThreeShellTerminalPlugin.SERVICE_DESCRIPTOR}\""))
        assertTrue(common.contains("\"required_host_version_code\": \"${ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION}\""))

        val build = Files.readString(root.resolve("app/build.gradle.kts"))
        // app/build.gradle.kts binds the application id once and reuses it for namespace and applicationId.
        assertTrue(build.contains("val globalApplicationId = \"${ThreeShellTerminalPlugin.PACKAGE_NAME}\""))
        assertTrue(build.contains("applicationId = globalApplicationId"))
        assertTrue(build.contains("\"plugin_id\", \"${ThreeShellTerminalPlugin.ID}\""))
        assertTrue(build.contains("\"plugin_engine\", \"${ThreeShellTerminalPlugin.ENGINE}\""))
        assertTrue(build.contains("\"plugin_variant\", \"${ThreeShellTerminalPlugin.VARIANT}\""))
        assertTrue(build.contains("\"plugin_author\", \"${ThreeShellTerminalPlugin.AUTHOR}\""))
        // The ABI list and the library names drive both the release verifier and the runtime inventory.
        assertTrue(build.contains("val nativeAbis = listOf(${NativeLibraryInventory.knownAbis.joinToString { "\"$it\"" }})"))
        assertTrue(build.contains("val nativeLibraryNames = listOf(${NativeLibraryInventory.libraryNames.joinToString { "\"$it\"" }})"))

        val settings = Files.readString(root.resolve("settings.gradle.kts"))
        assertTrue(settings.contains("rootProject.name = \"autojs6-plugin-three-shell-terminal\""))
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
