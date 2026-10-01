package io.github.supermonster003.autojs6.plugin.three.shell.terminal

/**
 * Pure-data view of the metadata reported through `IPluginInfoProvider.getInfo()` (and, from
 * roadmap P2.4 on, `ITerminalPlugin.getInfo()` / `getCapabilities()`).
 *
 * Android-specific lookups (package version, localized strings, raw resources, the packaged
 * native ABIs) happen in [threeShellTerminalPluginRuntimeInfo]; this class keeps the mapping
 * itself testable on the JVM.
 */
data class ThreeShellTerminalPluginRuntimeInfo(
    val name: String,
    val description: String,
    val instruction: String?,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
    /** ABIs whose pty libraries are packaged in the installed APK set (roadmap D14), never null. */
    val supportedAbis: Array<String>,
) {
    val author: String get() = ThreeShellTerminalPlugin.AUTHOR
    val id: String get() = ThreeShellTerminalPlugin.ID
    val engine: String get() = ThreeShellTerminalPlugin.ENGINE
    val variant: String get() = ThreeShellTerminalPlugin.VARIANT

    val requiresHostVersion: Long get() = ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION

    override fun equals(other: Any?): Boolean = other is ThreeShellTerminalPluginRuntimeInfo &&
        name == other.name && description == other.description && instruction == other.instruction &&
        versionName == other.versionName && versionCode == other.versionCode && versionDate == other.versionDate &&
        supportedAbis.contentEquals(other.supportedAbis)

    override fun hashCode(): Int = listOf(name, description, instruction, versionName, versionCode, versionDate).hashCode() * 31 +
        supportedAbis.contentHashCode()
}
