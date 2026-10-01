package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.text.InputType
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSettingsActions
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.TerminalNodeEnvironment

/**
 * The two settings selectors reachable from the terminal menu (text size, npm registry), with the
 * choose-then-confirm semantics of the standalone settings specification; the P5.1 settings page
 * reuses them. A custom registry URL is validated before anything is saved.
 * zh-CN: 终端菜单可达的两个设置选择器 (字号, npm 镜像源), 先选后确定; P5.1 设置页复用. 自定义 URL 保存前先校验.
 */
internal object TerminalSettingsDialogs {

    val TEXT_SIZES_SP: IntArray = intArrayOf(8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32)

    fun showTextSize(kit: UiKit, preferences: TerminalPreferences, onChanged: (Int) -> Unit) {
        val current = preferences.textSizeSp
        val selected = TEXT_SIZES_SP.indexOfFirst { it >= current }.takeIf { it >= 0 } ?: TEXT_SIZES_SP.lastIndex
        kit.confirmedChoiceDialog(kit.string(R.string.terminal_text_size), TEXT_SIZES_SP.map { "$it sp" }, selected) { index ->
            val size = TEXT_SIZES_SP[index]
            preferences.textSizeSp = size
            onChanged(size)
        }
    }

    fun showNpmRegistry(kit: UiKit, preferences: TerminalPreferences, actions: TerminalSettingsActions, onChanged: () -> Unit = {}) {
        val choices = TerminalPreferences.REGISTRY_CHOICES
        val labels = listOf(
            kit.string(R.string.terminal_npm_registry_npmjs),
            kit.string(R.string.terminal_npm_registry_npmmirror),
            kit.string(R.string.terminal_npm_registry_custom),
        )
        val current = choices.indexOf(preferences.npmRegistryChoice).coerceAtLeast(0)
        kit.confirmedChoiceDialog(kit.string(R.string.terminal_npm_registry), labels, current) { index ->
            val choice = choices[index]
            if (choice == TerminalPreferences.REGISTRY_CUSTOM) {
                promptCustomRegistry(kit, preferences, actions, onChanged)
            } else {
                actions.setRegistry(choice)
                onChanged()
            }
        }
    }

    private fun promptCustomRegistry(kit: UiKit, preferences: TerminalPreferences, actions: TerminalSettingsActions, onChanged: () -> Unit) {
        kit.inputDialog(
            title = kit.string(R.string.terminal_npm_registry_custom_url),
            hint = kit.string(R.string.terminal_npm_registry_custom_url_hint),
            value = preferences.npmRegistryCustomUrl,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            validate = { input -> if (TerminalNodeEnvironment.sanitizeRegistry(input) == null) kit.string(R.string.terminal_npm_registry_https_only) else null },
        ) { input ->
            actions.setRegistry(TerminalPreferences.REGISTRY_CUSTOM, input)
            onChanged()
        }
    }

}
