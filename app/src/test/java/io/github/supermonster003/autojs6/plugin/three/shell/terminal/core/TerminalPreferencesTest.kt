package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.TerminalNodeEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The frozen preference vocabulary (roadmap D29) and the pure registry mapping behind `npmRegistry`. */
class TerminalPreferencesTest {

    @Test
    fun keysAndChoicesAreFrozen() {
        assertEquals("terminal", TerminalPreferences.FILE_NAME)
        assertEquals("terminal_text_size", TerminalPreferences.KEY_TEXT_SIZE)
        assertEquals("npm_registry", TerminalPreferences.KEY_NPM_REGISTRY)
        assertEquals("npm_registry_custom_url", TerminalPreferences.KEY_NPM_REGISTRY_CUSTOM_URL)
        assertEquals("npm_ignore_scripts", TerminalPreferences.KEY_NPM_IGNORE_SCRIPTS)
        assertEquals("node_integration_enabled", TerminalPreferences.KEY_NODE_INTEGRATION_ENABLED)
        assertEquals("notification_permission_requested", TerminalPreferences.KEY_NOTIFICATION_PERMISSION_REQUESTED)
        assertEquals(
            listOf("manager_status_collapsed", "manager_controls_collapsed", "manager_sessions_collapsed", "manager_settings_collapsed"),
            TerminalPreferences.MANAGER_SECTION_KEYS,
        )
        assertEquals(listOf("npmjs", "npmmirror", "custom"), TerminalPreferences.REGISTRY_CHOICES)
    }

    @Test
    fun registryChoiceMapsToTheExportedRegistryOrNull() {
        assertNull(TerminalPreferences.resolveRegistry(null, null))
        assertNull(TerminalPreferences.resolveRegistry(TerminalPreferences.REGISTRY_NPMJS, "https://ignored.example.com/"))
        assertNull(TerminalPreferences.resolveRegistry("unknown", "https://ignored.example.com/"))
        assertEquals(TerminalNodeEnvironment.NPMMIRROR_REGISTRY, TerminalPreferences.resolveRegistry(TerminalPreferences.REGISTRY_NPMMIRROR, null))
        assertEquals("https://registry.example.com/", TerminalPreferences.resolveRegistry(TerminalPreferences.REGISTRY_CUSTOM, " https://registry.example.com "))
        assertNull(TerminalPreferences.resolveRegistry(TerminalPreferences.REGISTRY_CUSTOM, "http://registry.example.com/"))
        assertNull(TerminalPreferences.resolveRegistry(TerminalPreferences.REGISTRY_CUSTOM, null))
    }

}
