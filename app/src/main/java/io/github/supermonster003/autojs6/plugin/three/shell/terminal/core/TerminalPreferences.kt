package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.content.Context
import android.content.SharedPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliTrust
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.TerminalNodeEnvironment

/**
 * Persisted terminal settings of the plugin, stored in its own `SharedPreferences` file (plugin
 * roadmap D29): text size, npm registry choice with an optional custom URL, the npm
 * `ignore-scripts` switch and the Node.js integration switch. The value vocabulary is frozen so the
 * settings page (P5) and the Binder environment document read the same keys.
 *
 * zh-CN: 插件自有 `SharedPreferences` 中持久化的终端设置 (路线图 D29): 字号, npm 镜像源选择与自定义 URL,
 * npm `ignore-scripts` 开关, Node.js 集成开关. 取值词汇固定, 使设置页 (P5) 与 Binder 环境文档读取同一组键.
 */
class TerminalPreferences(private val preferences: SharedPreferences) {

    constructor(context: Context) : this(context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE))

    var textSizeSp: Int
        get() = preferences.getInt(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE_SP).coerceIn(MIN_TEXT_SIZE_SP, MAX_TEXT_SIZE_SP)
        set(value) = preferences.edit().putInt(KEY_TEXT_SIZE, value.coerceIn(MIN_TEXT_SIZE_SP, MAX_TEXT_SIZE_SP)).apply()

    /**
     * Terminal colors (roadmap P8), one of [COLOR_SCHEMES]: follow the light / dark appearance, always dark,
     * always light, or tinted by the theme color. Unknown stored values read as [COLOR_SCHEME_FOLLOW].
     * zh-CN: 终端配色 (P8), [COLOR_SCHEMES] 之一: 跟随浅色 / 深色外观, 始终深色, 始终浅色, 或按主题色着色.
     */
    var colorScheme: String
        get() = preferences.getString(KEY_COLOR_SCHEME, null)?.takeIf { it in COLOR_SCHEMES } ?: COLOR_SCHEME_FOLLOW
        set(value) {
            require(value in COLOR_SCHEMES) { "Unknown color scheme: $value" }
            preferences.edit().putString(KEY_COLOR_SCHEME, value).apply()
        }

    /** Terminal font (roadmap P8), one of [FONTS]; unknown stored values read as [FONT_SYSTEM]. zh-CN: 终端字体 (P8). */
    var font: String
        get() = preferences.getString(KEY_FONT, null)?.takeIf { it in FONTS } ?: FONT_SYSTEM
        set(value) {
            require(value in FONTS) { "Unknown font: $value" }
            preferences.edit().putString(KEY_FONT, value).apply()
        }

    /** One of [REGISTRY_CHOICES]; unknown stored values read as [REGISTRY_NPMJS]. zh-CN: [REGISTRY_CHOICES] 之一; 未知值按 [REGISTRY_NPMJS] 处理. */
    var npmRegistryChoice: String
        get() = preferences.getString(KEY_NPM_REGISTRY, null)?.takeIf { it in REGISTRY_CHOICES } ?: REGISTRY_NPMJS
        set(value) {
            require(value in REGISTRY_CHOICES) { "Unknown npm registry choice: $value" }
            preferences.edit().putString(KEY_NPM_REGISTRY, value).apply()
        }

    var npmRegistryCustomUrl: String?
        get() = preferences.getString(KEY_NPM_REGISTRY_CUSTOM_URL, null)?.trim()?.takeIf { it.isNotEmpty() }
        set(value) = preferences.edit().putString(KEY_NPM_REGISTRY_CUSTOM_URL, value?.trim()?.takeIf { it.isNotEmpty() }).apply()

    /**
     * Registry URL for npm / corepack, or null when npm's own default applies (also when the custom
     * URL is missing or not `https://`).
     * zh-CN: npm / corepack 的 registry URL; 使用 npm 自身默认值时为 null (自定义 URL 缺失或非 `https://` 时亦然).
     */
    val npmRegistry: String?
        get() = resolveRegistry(npmRegistryChoice, npmRegistryCustomUrl)

    /** Validate before atomically committing the choice and URL; a rejected custom URL changes nothing. */
    fun setRegistry(choice: String, customUrl: String? = null) {
        require(choice in REGISTRY_CHOICES) { "Unknown npm registry choice" }
        val normalized = if (choice == REGISTRY_CUSTOM) {
            requireNotNull(TerminalNodeEnvironment.sanitizeRegistry(customUrl)) { "Custom registry must be a valid HTTPS URL" }
        } else null
        preferences.edit().apply {
            putString(KEY_NPM_REGISTRY, choice)
            if (normalized != null) putString(KEY_NPM_REGISTRY_CUSTOM_URL, normalized)
        }.apply()
    }

    var npmIgnoreScripts: Boolean
        get() = preferences.getBoolean(KEY_NPM_IGNORE_SCRIPTS, false)
        set(value) = preferences.edit().putBoolean(KEY_NPM_IGNORE_SCRIPTS, value).apply()

    /** False switches the Node.js Runtime integration off even when the plugin is installed. zh-CN: 为 false 时即使已安装 Node.js 运行时插件也不接入. */
    var nodeIntegrationEnabled: Boolean
        get() = preferences.getBoolean(KEY_NODE_INTEGRATION_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_NODE_INTEGRATION_ENABLED, value).apply()

    /**
     * Whether a command that ran without an interactive shell (`keepOpen: false`) posts a notice when it ends
     * (roadmap P8); on by default.
     * zh-CN: 不带交互 shell 运行的命令 (`keepOpen: false`) 结束时是否发出通知 (P8); 默认开启.
     */
    var commandNotificationsEnabled: Boolean
        get() = preferences.getBoolean(KEY_COMMAND_NOTIFICATIONS, true)
        set(value) = preferences.edit().putBoolean(KEY_COMMAND_NOTIFICATIONS, value).apply()

    /**
     * Signer sets the user confirmed for third-party Node.js Runtime builds (roadmap P8, D34 Q3), oldest
     * first; malformed stored values are ignored.
     * zh-CN: 用户为第三方 Node.js Runtime 构建确认过的签名集合 (P8, D34 Q3), 旧的在前; 忽略格式错误的存储值.
     */
    val trustedNodeSigners: List<NodeCliTrust.UserTrust>
        get() = preferences.getString(KEY_NODE_TRUSTED_SIGNERS, null).orEmpty()
            .split('\n').mapNotNull { NodeCliTrust.UserTrust.decode(it.trim()) }.distinct()
            .take(NodeCliTrust.UserTrust.MAX_ENTRIES)

    /**
     * Records [trust]; the oldest confirmation makes room once [NodeCliTrust.UserTrust.MAX_ENTRIES] are stored.
     * zh-CN: 记录 [trust]; 达到上限时移除最旧的确认.
     */
    fun addTrustedNodeSigners(trust: NodeCliTrust.UserTrust) {
        val kept = (trustedNodeSigners - trust).takeLast(NodeCliTrust.UserTrust.MAX_ENTRIES - 1) + trust
        writeTrustedNodeSigners(kept)
    }

    fun removeTrustedNodeSigners(trust: NodeCliTrust.UserTrust) = writeTrustedNodeSigners(trustedNodeSigners - trust)

    private fun writeTrustedNodeSigners(values: List<NodeCliTrust.UserTrust>) {
        preferences.edit().apply {
            if (values.isEmpty()) remove(KEY_NODE_TRUSTED_SIGNERS) else putString(KEY_NODE_TRUSTED_SIGNERS, values.joinToString("\n") { it.encode() })
        }.apply()
    }

    /**
     * Whether the terminal screen already asked for `POST_NOTIFICATIONS` (API 33+, roadmap D15); the
     * request is made once, a refusal only hides the session notification.
     * zh-CN: 终端界面是否已请求过通知权限 (API 33+, D15); 只请求一次, 拒绝仅隐藏会话通知.
     */
    var notificationPermissionRequested: Boolean
        get() = preferences.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
        set(value) = preferences.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, value).apply()

    /**
     * Collapsed state of one session manager section (roadmap P3.2; the host kept per-group levels under
     * its own keys), [key] being one of [MANAGER_SECTION_KEYS]; every section starts expanded.
     * zh-CN: 会话管理器某一分组的收起状态 (P3.2), [key] 取自 [MANAGER_SECTION_KEYS]; 默认全部展开.
     */
    fun isManagerSectionCollapsed(key: String): Boolean {
        require(key in MANAGER_SECTION_KEYS) { "Unknown manager section: $key" }
        return preferences.getBoolean(key, false)
    }

    fun setManagerSectionCollapsed(key: String, collapsed: Boolean) {
        require(key in MANAGER_SECTION_KEYS) { "Unknown manager section: $key" }
        preferences.edit().putBoolean(key, collapsed).apply()
    }

    fun nodeEnvironmentOptions() = TerminalNodeEnvironment.Options(
        registry = npmRegistry,
        ignoreScripts = npmIgnoreScripts,
    )

    companion object {

        const val FILE_NAME = "terminal"

        const val KEY_TEXT_SIZE = "terminal_text_size"
        const val KEY_NPM_REGISTRY = "npm_registry"
        const val KEY_NPM_REGISTRY_CUSTOM_URL = "npm_registry_custom_url"
        const val KEY_NPM_IGNORE_SCRIPTS = "npm_ignore_scripts"
        const val KEY_NODE_INTEGRATION_ENABLED = "node_integration_enabled"
        const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        const val KEY_NODE_TRUSTED_SIGNERS = "node_trusted_signers"
        const val KEY_COMMAND_NOTIFICATIONS = "command_completion_notifications"
        const val KEY_COLOR_SCHEME = "terminal_color_scheme"
        const val KEY_FONT = "terminal_font"

        const val COLOR_SCHEME_FOLLOW = "follow"
        const val COLOR_SCHEME_DARK = "dark"
        const val COLOR_SCHEME_LIGHT = "light"
        const val COLOR_SCHEME_THEME = "theme"
        val COLOR_SCHEMES: List<String> = listOf(COLOR_SCHEME_FOLLOW, COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT, COLOR_SCHEME_THEME)

        const val FONT_SYSTEM = "system"
        const val FONT_JETBRAINS_MONO = "jetbrains-mono"
        val FONTS: List<String> = listOf(FONT_SYSTEM, FONT_JETBRAINS_MONO)

        /** Appearance choices of the settings page (roadmap P5.1), read by `ui.AppearancePreferences`. */
        const val KEY_APPEARANCE_LANGUAGE = "appearance_language"
        const val KEY_APPEARANCE_DARK_MODE = "appearance_dark_mode"
        const val KEY_APPEARANCE_COLOR = "appearance_color"
        val APPEARANCE_KEYS: List<String> = listOf(KEY_APPEARANCE_LANGUAGE, KEY_APPEARANCE_DARK_MODE, KEY_APPEARANCE_COLOR)

        const val KEY_MANAGER_STATUS_COLLAPSED = "manager_status_collapsed"
        const val KEY_MANAGER_CONTROLS_COLLAPSED = "manager_controls_collapsed"
        const val KEY_MANAGER_SESSIONS_COLLAPSED = "manager_sessions_collapsed"
        const val KEY_MANAGER_SETTINGS_COLLAPSED = "manager_settings_collapsed"
        val MANAGER_SECTION_KEYS: List<String> = listOf(
            KEY_MANAGER_STATUS_COLLAPSED, KEY_MANAGER_CONTROLS_COLLAPSED, KEY_MANAGER_SESSIONS_COLLAPSED, KEY_MANAGER_SETTINGS_COLLAPSED,
        )

        const val REGISTRY_NPMJS = "npmjs"
        const val REGISTRY_NPMMIRROR = "npmmirror"
        const val REGISTRY_CUSTOM = "custom"
        val REGISTRY_CHOICES: List<String> = listOf(REGISTRY_NPMJS, REGISTRY_NPMMIRROR, REGISTRY_CUSTOM)

        const val DEFAULT_TEXT_SIZE_SP = 12
        const val MIN_TEXT_SIZE_SP = 6
        const val MAX_TEXT_SIZE_SP = 40

        /**
         * Pure mapping of a registry choice plus custom URL to the exported registry, or null for
         * npm's default.
         * zh-CN: 镜像源选择 + 自定义 URL 到导出 registry 的纯映射; 使用 npm 默认值时为 null.
         */
        @JvmStatic
        fun resolveRegistry(choice: String?, customUrl: String?): String? = when (choice) {
            REGISTRY_NPMMIRROR -> TerminalNodeEnvironment.NPMMIRROR_REGISTRY
            REGISTRY_CUSTOM -> TerminalNodeEnvironment.sanitizeRegistry(customUrl)
            else -> null
        }

    }

}
