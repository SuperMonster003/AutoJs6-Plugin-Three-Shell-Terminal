package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.content.SharedPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliTrust
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
        assertEquals("node_trusted_signers", TerminalPreferences.KEY_NODE_TRUSTED_SIGNERS)
        assertEquals(listOf("appearance_language", "appearance_dark_mode", "appearance_color"), TerminalPreferences.APPEARANCE_KEYS)
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

    @Test
    fun trustedNodeSignersKeepOrderDropMalformedValuesAndStayBounded() {
        val store = MemoryPreferences()
        val preferences = TerminalPreferences(store)
        assertEquals(emptyList<NodeCliTrust.UserTrust>(), preferences.trustedNodeSigners)
        val entries = (1..NodeCliTrust.UserTrust.MAX_ENTRIES + 2).map { NodeCliTrust.UserTrust.of("com.example.node$it", listOf("%064x".format(it)))!! }
        entries.forEach(preferences::addTrustedNodeSigners)
        assertEquals(entries.takeLast(NodeCliTrust.UserTrust.MAX_ENTRIES), preferences.trustedNodeSigners)
        // Re-confirming moves an entry to the newest position without duplicating it.
        preferences.addTrustedNodeSigners(entries[3])
        assertEquals(entries[3], preferences.trustedNodeSigners.last())
        assertEquals(NodeCliTrust.UserTrust.MAX_ENTRIES, preferences.trustedNodeSigners.size)
        preferences.removeTrustedNodeSigners(entries[3])
        assertEquals(false, entries[3] in preferences.trustedNodeSigners)
        store.values[TerminalPreferences.KEY_NODE_TRUSTED_SIGNERS] = "garbage\ncom.example.ok|${"e".repeat(64)}\n|x"
        assertEquals(listOf(NodeCliTrust.UserTrust.of("com.example.ok", listOf("e".repeat(64)))), preferences.trustedNodeSigners)
        preferences.removeTrustedNodeSigners(preferences.trustedNodeSigners.single())
        assertEquals(false, TerminalPreferences.KEY_NODE_TRUSTED_SIGNERS in store.values)
    }

    /** Just enough SharedPreferences for the string-valued keys under test. */
    private class MemoryPreferences : SharedPreferences {
        val values = LinkedHashMap<String, Any?>()
        override fun getAll(): MutableMap<String, *> = values
        override fun getString(key: String, defValue: String?): String? = values[key] as String? ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String, defValue: Int): Int = values[key] as Int? ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as Long? ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as Float? ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as Boolean? ?: defValue
        override fun contains(key: String): Boolean = key in values
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = LinkedHashMap<String, Any?>()
            private val removed = HashSet<String>()
            override fun putString(key: String, value: String?) = apply { pending[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = apply { pending[key] = values }
            override fun putInt(key: String, value: Int) = apply { pending[key] = value }
            override fun putLong(key: String, value: Long) = apply { pending[key] = value }
            override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
            override fun remove(key: String) = apply { removed.add(key) }
            override fun clear() = apply { removed.addAll(values.keys) }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                removed.forEach(values::remove)
                values.putAll(pending)
            }
        }
    }

}
