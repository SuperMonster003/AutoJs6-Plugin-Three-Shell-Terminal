package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator.Resolution

/**
 * Plain-text report of a Node.js launcher resolution for the "Details" dialog and the P5 settings
 * probe; ported from the host developer option. Deliberately English and untranslated: it is a
 * diagnostic the user copies into an issue, so every line must read the same everywhere.
 * zh-CN: Node.js 启动器解析结果的纯文本报告 (自宿主开发者选项迁入), 供 "详情" 对话框与 P5 设置页探测使用;
 * 有意保持英文不翻译, 便于用户原样复制到 issue.
 */
internal object NodeProbeReport {

    private const val LOGCAT_HINT = "adb logcat -b all -d | grep -E \"avc: .*(nodexe|three.shell.terminal)\""

    fun render(resolution: Resolution): String = buildString {
        when (resolution) {
            is Resolution.Available -> {
                appendLine("Result: available")
                appendLauncher(resolution.launcher)
                appendLine("Probe: ${resolution.probe.summary()}")
            }
            is Resolution.Unavailable.IntegrationDisabled -> appendLine("Result: Node.js integration disabled in the plugin settings")
            is Resolution.Unavailable.PluginMissing -> appendLine("Result: plugin missing")
            is Resolution.Unavailable.PluginNotTrusted -> {
                appendLine("Result: plugin signer not trusted")
                appendLine("Plugin: ${resolution.packageName}")
                appendLine("Signers: ${resolution.signers.joinToString(", ").ifEmpty { "(none)" }}")
            }
            is Resolution.Unavailable.PluginTooOld -> {
                appendLine("Result: plugin has no terminal launcher")
                appendLine("Plugin: ${resolution.packageName}")
                appendLine("Reason: ${resolution.reason}")
            }
            is Resolution.Unavailable.ExecutableMissing -> {
                appendLine("Result: launcher missing")
                appendLine("Plugin: ${resolution.packageName}")
                appendLine("ABI: ${resolution.abi}")
                appendLine("Expected: ${resolution.executableName}")
            }
            is Resolution.Unavailable.SetupFailed -> {
                appendLine("Result: npm / corepack setup failed")
                appendLauncher(resolution.launcher)
                appendLine("Error: ${resolution.message}")
            }
            is Resolution.Unavailable.ExecDenied -> {
                appendLine("Result: launcher could not run")
                appendLauncher(resolution.launcher)
                appendLine("Probe: ${resolution.probe.summary()}")
                resolution.probe.stderr.trim().takeIf { it.isNotEmpty() }?.let { appendLine("stderr: ${it.take(600)}") }
                appendLine()
                appendLine("Hint: $LOGCAT_HINT")
            }
        }
    }.trimEnd()

    private fun StringBuilder.appendLauncher(launcher: NodeCliLocator.Launcher) {
        appendLine("Plugin: ${launcher.packageName} (${launcher.versionCode})")
        appendLine("Launcher: ${launcher.executable.path}")
        appendLine("Commands: ${launcher.descriptor.commands.joinToString(", ")}")
        launcher.descriptor.npmVersion?.let { appendLine("npm: $it") }
        launcher.descriptor.corepackVersion?.let { appendLine("corepack: $it") }
    }

}
