package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.text.InputType
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalNpmActions
import java.io.File

/**
 * Menu-driven npm helpers that type ready-made commands into the terminal; the command text comes
 * from `core/TerminalNpmActions`, this class only owns the dialogs (roadmap P2.5 / P3.1).
 *
 * @param typeCommand      writes one command line (without the trailing newline) and executes it
 * @param currentDirectory the shell's current working directory, when known
 * zh-CN: 由菜单驱动的 npm 辅助动作; 命令文本来自 `core/TerminalNpmActions`, 本类只负责对话框.
 */
internal class TerminalNpmDialogs(
    private val kit: UiKit,
    private val typeCommand: (String) -> Unit,
    private val currentDirectory: () -> File?,
) {

    fun npmInit() = typeCommand(TerminalNpmActions.npmInit())

    fun npmInstall() = typeCommand(TerminalNpmActions.npmInstall())

    fun npmInstallPackage() {
        kit.inputDialog(
            title = kit.string(R.string.terminal_npm_install_package),
            hint = kit.string(R.string.terminal_package_name_hint),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
        ) { input ->
            runCatching { TerminalNpmActions.npmInstallPackage(input) }.getOrNull()?.let(typeCommand)
        }
    }

    fun npmRunScript() {
        val scripts = TerminalNpmActions.scripts(currentDirectory())
        if (scripts == null) {
            kit.toast(R.string.terminal_no_package_json)
            return
        }
        if (scripts.isEmpty()) {
            kit.toast(R.string.terminal_no_npm_scripts)
            return
        }
        kit.actionListDialog(kit.string(R.string.terminal_npm_run_script), null, scripts.map { "${it.name}\n${it.command}" }) { index ->
            typeCommand(TerminalNpmActions.npmRunScript(scripts[index].name))
        }
    }

    fun otherPackageManagers() {
        val managers = TerminalNpmActions.otherPackageManagers
        kit.actionListDialog(
            kit.string(R.string.terminal_other_package_managers),
            kit.string(R.string.terminal_other_package_managers_content),
            managers.map { it.versionCommand },
        ) { index -> typeCommand(managers[index].versionCommand) }
    }

    fun searchNpm() {
        kit.inputDialog(
            title = kit.string(R.string.terminal_search_npm),
            hint = kit.string(R.string.terminal_package_name_hint),
        ) { query ->
            if (!ExternalIntents.browse(kit.context, TerminalNpmActions.searchUrl(query))) kit.toast(R.string.terminal_error_occurred)
        }
    }

}
