package io.github.supermonster003.autojs6.plugin.three.shell.terminal.explorer

import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import org.autojs.plugin.explorer.api.ExplorerActionPluginActions
import org.autojs.plugin.explorer.api.ExplorerActionProtocol
import org.autojs.plugin.explorer.api.ExplorerActionValues
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Freezes the "Open in terminal" Explorer Action (roadmap P8.1, appendix D Q10): the only catalog
 * shape for which the host discloses a directory path, and the request rule of its Activity.
 */
class TerminalExplorerActionTest {

    private val directory = ExplorerDirectoryRequest.Target(ExplorerActionValues.TARGET_DIRECTORY, "/storage/emulated/0/Scripts")

    @Test
    fun `the catalog declares the narrow protocol v23 directory shape`() {
        val spec = TerminalExplorerAction.spec
        assertEquals(23, spec.protocolVersion)
        assertTrue("the host must support the declared protocol", spec.protocolVersion <= ExplorerActionProtocol.MAX_SUPPORTED_VERSION)
        assertEquals("three-shell-terminal.open-directory", spec.id)
        assertTrue(Regex("[a-z0-9][a-z0-9._-]*").matches(spec.id))
        assertEquals("terminal_explorer_open_in_terminal", spec.labelResourceName)
        assertEquals("Open in terminal", spec.labelFallback)
        assertEquals("io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalExplorerActivity", spec.activityClassName)
        assertEquals(ExplorerActionValues.PRESENTATION_ACTIVITY, spec.presentation)
        assertEquals(ExplorerActionValues.TARGET_DIRECTORY, spec.targetKind)
        assertEquals(ExplorerActionValues.CARDINALITY_SINGLE, spec.cardinality)
        assertEquals(ExplorerActionValues.ACCESS_READ_ONLY, spec.accessMode)
        assertEquals(ExplorerActionValues.PLACEMENT_OVERFLOW, spec.placement)
        assertTrue(spec.filesystemPath)
        assertEquals(5320L, ThreeShellTerminalPlugin.EXPLORER_REQUIRED_HOST_VERSION)
        assertTrue(
            "the directory action needs a newer host than the terminal contract",
            ThreeShellTerminalPlugin.EXPLORER_REQUIRED_HOST_VERSION > ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION,
        )
    }

    @Test
    fun `the menu title resource exists in every locale and is kept from resource shrinking`() {
        val res = findProjectRoot().resolve("app/src/main/res")
        val folders = Files.list(res).use { stream ->
            stream.filter { Files.isRegularFile(it.resolve("strings.xml")) }.map { it.fileName.toString() }.toList()
        }
        assertEquals(11, folders.size)
        folders.forEach { folder ->
            val text = String(Files.readAllBytes(res.resolve("$folder/strings.xml")), Charsets.UTF_8)
            assertTrue("$folder lacks the Explorer Action title", text.contains("<string name=\"${TerminalExplorerAction.LABEL_RESOURCE_NAME}\">"))
        }
        val keep = String(Files.readAllBytes(res.resolve("raw/keep_explorer_action_label.xml")), Charsets.UTF_8)
        assertTrue(keep.contains("tools:keep=\"@string/${TerminalExplorerAction.LABEL_RESOURCE_NAME}\""))
    }

    @Test
    fun `a well-formed request opens its directory`() {
        assertEquals("/storage/emulated/0/Scripts", request(targets = listOf(directory))?.directory)
        assertEquals("/", request(targets = listOf(directory.copy(filesystemPath = "/")))?.directory)
        assertEquals("/data/user/0/x/中文 dir", request(targets = listOf(directory.copy(filesystemPath = "/data/user/0/x/中文 dir")))?.directory)
    }

    @Test
    fun `requests outside the declared action are refused`() {
        assertNull(request(action = "android.intent.action.VIEW"))
        assertNull(request(action = null))
        assertNull(request(actionId = "three-shell-terminal.other"))
        assertNull(request(actionId = null))
        assertNull(request(protocolVersion = 22))
        assertNull(request(protocolVersion = 24))
        assertNull(request(protocolVersion = 0))
    }

    @Test
    fun `only one directory target with a path is accepted`() {
        assertNull(request(targets = null))
        assertNull(request(targets = emptyList()))
        assertNull(request(targets = listOf(directory, directory)))
        assertNull(request(targets = listOf(directory.copy(kind = ExplorerActionValues.TARGET_FILE))))
        assertNull(request(targets = listOf(directory.copy(kind = null))))
        assertNull(request(targets = listOf(directory.copy(filesystemPath = null))))
    }

    @Test
    fun `paths must be absolute, normalized and within both ceilings`() {
        listOf("", "relative/dir", "~/dir", "/a/../b", "/a/./b", "/..", "/a\u0000b", "/a\nb").forEach { path ->
            assertFalse("accepted '$path'", ExplorerDirectoryRequest.isAcceptablePath(path))
        }
        assertTrue(ExplorerDirectoryRequest.isAcceptablePath("/a/..b/c.d"))
        val longest = "/" + "a".repeat(ExplorerActionProtocol.MAX_FILESYSTEM_PATH_LENGTH - 1)
        assertTrue(ExplorerDirectoryRequest.isAcceptablePath(longest))
        assertFalse(ExplorerDirectoryRequest.isAcceptablePath(longest + "a"))
        assertFalse(
            "UTF-8 size is bounded by the terminal path ceiling",
            ExplorerDirectoryRequest.isAcceptablePath("/" + "中".repeat(TerminalContract.MAX_PATH_BYTES / 3 + 1)),
        )
    }

    private fun request(
        action: String? = ExplorerActionPluginActions.EXECUTE,
        actionId: String? = ThreeShellTerminalPlugin.EXPLORER_DIRECTORY_ACTION_ID,
        protocolVersion: Int = 23,
        targets: List<ExplorerDirectoryRequest.Target>? = listOf(directory),
    ) = ExplorerDirectoryRequest.of(action, actionId, protocolVersion, targets)

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) && Files.isDirectory(it.resolve("app")) }

}
