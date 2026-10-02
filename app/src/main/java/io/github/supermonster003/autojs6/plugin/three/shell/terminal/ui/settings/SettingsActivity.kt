package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.EntryCaller
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSettingsActions
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.storage.StorageAccess
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.Appearance
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.BackgroundWork
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.ExternalIntents
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.HostAppearance
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalSettingsDialogs
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.update.AppUpdateCoordinator
import org.autojs.plugin.terminal.api.TerminalActions
import java.util.Locale

/**
 * The standalone settings page (roadmap D29, P5.1; standalone settings specification): the
 * appearance group (language, night mode, theme color, launcher icon, each defaulting to "follow
 * AutoJs6"), the terminal group (text size, npm registry, ignore-scripts, Node.js integration,
 * environment probe, all-files access, clear data) and the information group (update check,
 * version history, About). Every choice dialog follows choose-then-confirm; a change applies at
 * once, appearance changes by recreating the screen. The host's plugin center reaches this screen
 * through `org.autojs.plugin.TERMINAL_SETTINGS` behind the plugin permission, the terminal menu
 * starts it explicitly; the same caller rule as the terminal entry applies ([EntryCaller]).
 *
 * zh-CN: 独立设置页 (D29, P5.1; 独立设置页规范): 外观组 (语言, 夜间模式, 主题色, 启动器图标, 默认均跟随 AutoJs6),
 * 终端组 (字号, npm 镜像源, 忽略安装脚本, Node.js 集成, 环境探测, 全部文件访问, 清除数据) 与信息组 (检查更新,
 * 版本历史, 关于). 选择对话框一律先选后确定, 更改立即生效, 外观更改通过重建界面生效. 宿主插件中心经受插件权限
 * 保护的 `org.autojs.plugin.TERMINAL_SETTINGS` 进入, 终端菜单显式启动; 调用方规则与终端入口相同 ([EntryCaller]).
 */
class SettingsActivity : HostAppearanceActivity() {

    internal lateinit var scaffold: Scaffold
        private set

    internal lateinit var updates: AppUpdateCoordinator
        private set

    internal lateinit var appearancePreferences: AppearancePreferences
        private set

    /** The last choice / confirmation dialog, exposed for instrumentation and the recreate guard. */
    internal var prompt: AlertDialog? = null
        private set

    internal val rows = linkedMapOf<String, SettingRow>()

    private val preferences by lazy { TerminalPreferences(this) }
    private val settingsActions by lazy { TerminalSettingsActions(this) }
    private var progress: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val caller = EntryCaller.identify(this)
        if (!EntryCaller.accepts(this, caller)) {
            Log.w(TAG, "refused ${TerminalActions.OPEN_SETTINGS} from ${caller ?: "an unnamed caller"}")
            finish()
            return
        }
        appearancePreferences = AppearancePreferences.read(this)
        updates = AppUpdateCoordinator(this, packageManager.getPackageInfo(packageName, 0).versionName.orEmpty())
        scaffold = buildScaffold(getString(R.string.terminal_settings))
        buildPage(scaffold.content)
        setContentView(scaffold.root)
    }

    override fun onResume() {
        super.onResume()
        if (::scaffold.isInitialized) refreshSummaries()
    }

    override fun hasUnconfirmedDialog(): Boolean =
        prompt?.isShowing == true || progress?.isShowing == true || (::updates.isInitialized && updates.isShowingDialog)

    /** Instrumentation access to the protected recreate guard. */
    internal fun hasUnconfirmedDialogForTest(): Boolean = hasUnconfirmedDialog()

    override fun onDestroy() {
        prompt?.dismiss()
        prompt = null
        progress?.dismiss()
        progress = null
        if (::updates.isInitialized) updates.close()
        super.onDestroy()
    }

    // region Page

    private fun buildPage(page: LinearLayout) = with(kit) {
        val current = requireNotNull(appearance)
        page.addView(sectionHeader(getString(R.string.app_settings_appearance)))
        add(page, KEY_LANGUAGE, settingRow(getString(R.string.app_settings_language), languageSummary(current), R.drawable.ic_settings_language, KEY_LANGUAGE) { chooseLanguage() })
        add(page, KEY_DARK_MODE, settingRow(getString(R.string.app_settings_dark_mode), darkModeSummary(current), R.drawable.ic_settings_night, KEY_DARK_MODE) { chooseDarkMode() })
        val color = settingRow(getString(R.string.app_settings_theme_color), colorSummary(current), R.drawable.ic_settings_theme, KEY_COLOR) { chooseColor() }
        color.view.addView(
            View(this@SettingsActivity).apply {
                tag = TAG_COLOR_DOT
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(palette.primary)
                    setStroke(dp(1), palette.outline)
                }
            },
            color.view.childCount - 1,
            LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginStart = dp(SettingsMetrics.ROW_PADDING) },
        )
        add(page, KEY_COLOR, color)
        add(page, KEY_LAUNCHER_ICON, settingRow(getString(R.string.launcher_icon_title), launcherIconSummary(), R.drawable.ic_settings_launcher, KEY_LAUNCHER_ICON) { chooseLauncherIcon() }, divider = false)

        page.addView(sectionHeader(getString(R.string.terminal_title)))
        add(page, KEY_TEXT_SIZE, settingRow(getString(R.string.terminal_text_size), textSizeSummary(), R.drawable.ic_settings_text_size, KEY_TEXT_SIZE) {
            TerminalSettingsDialogs.showTextSize(kit, preferences, onChanged = { rows.getValue(KEY_TEXT_SIZE).setSummary(textSizeSummary()) }, onPrompt = { prompt = it })
        })
        add(page, KEY_REGISTRY, settingRow(getString(R.string.terminal_npm_registry), registrySummary(), R.drawable.ic_settings_registry, KEY_REGISTRY) {
            TerminalSettingsDialogs.showNpmRegistry(kit, preferences, settingsActions, onChanged = { rows.getValue(KEY_REGISTRY).setSummary(registrySummary()) }, onPrompt = { prompt = it })
        })
        add(page, KEY_IGNORE_SCRIPTS, switchRow(getString(R.string.terminal_npm_ignore_scripts), getString(R.string.terminal_npm_ignore_scripts_summary), R.drawable.ic_settings_shield, preferences.npmIgnoreScripts, KEY_IGNORE_SCRIPTS) { enabled ->
            settingsActions.setIgnoreScripts(enabled)
        })
        add(page, KEY_NODE_INTEGRATION, switchRow(getString(R.string.settings_node_integration), getString(R.string.settings_node_integration_summary), R.drawable.ic_settings_node, preferences.nodeIntegrationEnabled, KEY_NODE_INTEGRATION) { enabled ->
            settingsActions.setNodeIntegration(enabled)
            if (enabled) toast(R.string.terminal_node_enabled_for_new_sessions)
        })
        add(page, KEY_NODE_PROBE, settingRow(getString(R.string.terminal_node_probe), getString(R.string.settings_node_probe_summary), R.drawable.ic_settings_probe, KEY_NODE_PROBE) {
            startActivity(Intent(this@SettingsActivity, NodeProbeActivity::class.java))
        })
        add(page, KEY_STORAGE, settingRow(getString(R.string.settings_all_files_access), storageSummary(), R.drawable.ic_settings_storage, KEY_STORAGE) { openStorageAccess() })
        add(page, KEY_CLEAR_DATA, settingRow(getString(R.string.settings_clear_data), getString(R.string.settings_clear_data_summary), R.drawable.ic_settings_delete, KEY_CLEAR_DATA, chevron = false, titleColor = palette.danger) { confirmClearData() }, divider = false)

        page.addView(sectionHeader(getString(R.string.settings_section_information)))
        add(page, KEY_UPDATE, settingRow(getString(R.string.update_check), getString(R.string.update_installed, packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()), R.drawable.ic_settings_update, KEY_UPDATE) { updates.check() })
        add(page, KEY_HISTORY, settingRow(getString(R.string.release_history_title), null, R.drawable.ic_settings_history, KEY_HISTORY) {
            startActivity(ReleaseHistoryActivity.intent(this@SettingsActivity, BundledDocument.HISTORY))
        })
        add(page, KEY_ABOUT, settingRow(getString(R.string.about_title), getString(R.string.about_summary), R.drawable.ic_settings_info, KEY_ABOUT) {
            startActivity(Intent(this@SettingsActivity, AboutActivity::class.java))
        }, divider = false)
    }

    private fun add(page: LinearLayout, key: String, row: SettingRow, divider: Boolean = true) {
        rows[key] = row
        page.addView(row.view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        if (divider) page.addView(kit.hairline())
    }

    /** Values that other screens or the system may have changed while this one was away. */
    private fun refreshSummaries() {
        rows[KEY_TEXT_SIZE]?.setSummary(textSizeSummary())
        rows[KEY_REGISTRY]?.setSummary(registrySummary())
        rows[KEY_IGNORE_SCRIPTS]?.setChecked(preferences.npmIgnoreScripts)
        rows[KEY_NODE_INTEGRATION]?.setChecked(preferences.nodeIntegrationEnabled)
        rows[KEY_STORAGE]?.setSummary(storageSummary())
        rows[KEY_LAUNCHER_ICON]?.setSummary(launcherIconSummary())
    }

    // endregion

    // region Summaries

    private fun languageSummary(current: Appearance): String {
        if (appearancePreferences.language == AppearancePreferences.HOST) {
            val display = Locale.forLanguageTag(current.language).getDisplayName(resources.configuration.locales[0])
            return getString(R.string.app_settings_follow_autojs6_summary, display)
        }
        return getString(LANGUAGE_LABELS[AppearancePreferences.LANGUAGES.indexOf(appearancePreferences.language)])
    }

    private fun darkModeSummary(current: Appearance): String {
        if (appearancePreferences.darkMode == AppearancePreferences.HOST) {
            return getString(R.string.app_settings_follow_autojs6_summary, getString(if (current.dark) R.string.app_settings_always_dark else R.string.app_settings_always_light))
        }
        return getString(MODE_LABELS[AppearancePreferences.MODES.indexOf(appearancePreferences.darkMode)])
    }

    private fun colorSummary(current: Appearance): String =
        appearancePreferences.color?.let(ThemeColorValue::hex)
            ?: getString(R.string.app_settings_follow_autojs6_summary, ThemeColorValue.hex(current.primarySeed))

    private fun launcherIconSummary(): String = getString(LAUNCHER_ICON_LABELS[LauncherIcons.current(this).ordinal])

    private fun textSizeSummary(): String = "${preferences.textSizeSp} sp"

    private fun registrySummary(): String = when (preferences.npmRegistryChoice) {
        TerminalPreferences.REGISTRY_NPMMIRROR -> getString(R.string.terminal_npm_registry_npmmirror)
        TerminalPreferences.REGISTRY_CUSTOM -> preferences.npmRegistryCustomUrl ?: getString(R.string.terminal_npm_registry_custom)
        else -> getString(R.string.terminal_npm_registry_npmjs)
    }

    private fun storageSummary(): String = when (StorageAccess.state(this)) {
        StorageAccess.State.GRANTED -> getString(R.string.settings_all_files_access_granted)
        StorageAccess.State.NOT_APPLICABLE -> getString(R.string.settings_all_files_access_not_applicable)
        else -> getString(R.string.settings_all_files_access_denied)
    }

    // endregion

    // region Actions

    private fun chooseLanguage() {
        val index = AppearancePreferences.LANGUAGES.indexOf(appearancePreferences.language).coerceAtLeast(0)
        prompt = kit.confirmedChoiceDialog(getString(R.string.app_settings_language), LANGUAGE_LABELS.map(::getString), index) {
            saveAppearance(appearancePreferences.copy(language = AppearancePreferences.LANGUAGES[it]))
        }
    }

    private fun chooseDarkMode() {
        val index = AppearancePreferences.MODES.indexOf(appearancePreferences.darkMode).coerceAtLeast(0)
        prompt = kit.confirmedChoiceDialog(getString(R.string.app_settings_dark_mode), MODE_LABELS.map(::getString), index) {
            saveAppearance(appearancePreferences.copy(darkMode = AppearancePreferences.MODES[it]))
        }
    }

    private fun chooseColor() {
        val hostColor = (HostAppearance.cached?.primary ?: Appearance.DEFAULT_COLOR) or 0xFF000000.toInt()
        prompt = ThemeColorChooser.show(this, appearancePreferences.color, hostColor) { color ->
            saveAppearance(appearancePreferences.copy(color = color))
        }
    }

    /** Persists the choice and rebuilds this screen; other screens follow on their next resume. */
    internal fun saveAppearance(value: AppearancePreferences) {
        value.write(this)
        appearancePreferences = value
        recreate()
    }

    private fun chooseLauncherIcon() {
        val labels = LauncherIconMode.entries.map { mode ->
            val title = getString(LAUNCHER_ICON_LABELS[mode.ordinal])
            val note = when (mode) {
                LauncherIconMode.AUTO -> getString(R.string.launcher_icon_auto_note)
                LauncherIconMode.TRANSPARENT -> getString(R.string.launcher_icon_transparent_note)
                else -> null
            }
            if (note == null) title else SpannableString("$title\n$note").apply {
                setSpan(RelativeSizeSpan(14f / 16f), title.length + 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        prompt = kit.confirmedChoiceDialog(getString(R.string.launcher_icon_title), labels, LauncherIcons.current(this).ordinal) { index ->
            val app = applicationContext
            val mode = LauncherIconMode.entries[index]
            BackgroundWork.run({ LauncherIcons.select(app, mode) }) { result ->
                if (isFinishing || isDestroyed) return@run
                result.onSuccess { kit.toast(R.string.launcher_icon_applied_note, long = true) }
                    .onFailure { kit.toast(R.string.launcher_icon_failed) }
                rows[KEY_LAUNCHER_ICON]?.setSummary(launcherIconSummary())
            }
        }
    }

    private fun openStorageAccess() {
        when (StorageAccess.state(this)) {
            StorageAccess.State.NOT_APPLICABLE -> kit.toast(R.string.settings_all_files_access_not_applicable)
            StorageAccess.State.DENIED_LEGACY -> requestPermissions(StorageAccess.legacyPermissionsToRequest(), REQUEST_STORAGE)
            else -> {
                val intents = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) StorageAccess.resolvableAllFilesAccessIntents(this) else emptyList()
                val target = intents.firstOrNull() ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                if (!ExternalIntents.startSafely(this, target)) kit.toast(R.string.terminal_storage_settings_unavailable)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE) rows[KEY_STORAGE]?.setSummary(storageSummary())
    }

    private fun confirmClearData() {
        val root = TerminalPaths.of(this).root.path
        prompt = kit.confirmDialog(
            getString(R.string.settings_clear_data),
            getString(R.string.settings_clear_data_confirm, root),
            getString(R.string.settings_clear_action),
            destructive = true,
        ) {
            progress = kit.progressDialog(getString(R.string.settings_clearing))
            settingsActions.clearData { result ->
                progress?.dismiss()
                progress = null
                if (isFinishing || isDestroyed) return@clearData
                result.onSuccess { kit.toast(R.string.settings_data_cleared) }
                    .onFailure { kit.toast(R.string.terminal_error_occurred, long = true) }
            }
        }
    }

    private fun toast(@StringRes resource: Int) = kit.toast(resource)

    // endregion

    companion object {

        private const val TAG = "ThreeShellTerminalSettings"
        private const val REQUEST_STORAGE = 41

        const val KEY_LANGUAGE = "appearance-language"
        const val KEY_DARK_MODE = "appearance-dark"
        const val KEY_COLOR = "appearance-color"
        const val KEY_LAUNCHER_ICON = "launcher-icon"
        const val KEY_TEXT_SIZE = "terminal-text-size"
        const val KEY_REGISTRY = "terminal-registry"
        const val KEY_IGNORE_SCRIPTS = "terminal-ignore-scripts"
        const val KEY_NODE_INTEGRATION = "terminal-node-integration"
        const val KEY_NODE_PROBE = "terminal-node-probe"
        const val KEY_STORAGE = "terminal-storage"
        const val KEY_CLEAR_DATA = "terminal-clear-data"
        const val KEY_UPDATE = "info-update"
        const val KEY_HISTORY = "info-history"
        const val KEY_ABOUT = "info-about"
        const val TAG_COLOR_DOT = "appearance-color-dot"

        /** Picker labels in the order of [AppearancePreferences.LANGUAGES]. */
        internal val LANGUAGE_LABELS: List<Int> = listOf(
            R.string.app_settings_follow_autojs6, R.string.app_settings_follow_system,
            R.string.app_language_zh_hans, R.string.app_language_zh_hant_hk, R.string.app_language_zh_hant_tw, R.string.app_language_en,
            R.string.app_language_fr, R.string.app_language_es, R.string.app_language_ja, R.string.app_language_ko, R.string.app_language_ru, R.string.app_language_ar,
        )

        /** Picker labels in the order of [AppearancePreferences.MODES]. */
        internal val MODE_LABELS: List<Int> = listOf(
            R.string.app_settings_follow_autojs6, R.string.app_settings_follow_system, R.string.app_settings_always_light, R.string.app_settings_always_dark,
        )

        /** Labels in the order of [LauncherIconMode.entries]. */
        internal val LAUNCHER_ICON_LABELS: List<Int> = listOf(
            R.string.launcher_icon_light, R.string.launcher_icon_dark, R.string.launcher_icon_auto, R.string.launcher_icon_transparent,
        )

        fun intent(context: android.content.Context): Intent = Intent(context, SettingsActivity::class.java)

    }

}
