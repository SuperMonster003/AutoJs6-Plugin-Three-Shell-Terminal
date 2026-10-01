package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import com.google.android.material.switchmaterial.SwitchMaterial
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSettingsActions
import java.util.Locale

/**
 * The session manager (roadmap P3.2, ported from the host's connection-manager style dialog):
 * four collapsible sections (status, controls, sessions, settings) over the process-wide
 * [TerminalSessionManager]. It listens to the same registry the Binder's `onSessionsChanged`
 * uses, so sessions created or closed by the host appear immediately. Opening a session starts
 * a terminal Activity on top of the caller (`preserveManager`), and Back returns to this dialog.
 *
 * zh-CN: 会话管理器 (P3.2, 自宿主的连接管理器样式迁入): 状态 / 控制 / 会话 / 设置四个可收起的分组,
 * 基于进程级 [TerminalSessionManager]; 与 Binder `onSessionsChanged` 监听同一注册表, 宿主创建或关闭的会话
 * 即时出现. 打开会话会在调用者之上启动终端 Activity, 返回键回到本对话框.
 */
internal class TerminalManagerDialog private constructor(
    private val activity: HostAppearanceActivity,
    private val onTextSizeChanged: (Int) -> Unit,
    private val onDismissed: () -> Unit,
) {

    private val kit: UiKit = activity.kit
    private val palette: TerminalPalette = kit.palette
    private val preferences = TerminalPreferences(activity)
    private val settings = TerminalSettingsActions(activity)
    private val handler = Handler(Looper.getMainLooper())

    private val stateValue = valueText()
    private val countValue = valueText()
    private val newSession = actionRow(R.id.manager_new_session, R.string.terminal_new_session) { createSession() }
    private val closeAll = actionRow(R.id.manager_close_all, R.string.terminal_close_all_sessions) {
        TerminalSessionManager.closeAll()
        refresh()
    }
    private val sessionList = LinearLayout(activity).apply {
        id = R.id.manager_session_list
        orientation = LinearLayout.VERTICAL
    }
    private val noSessions = TextView(activity).apply {
        id = R.id.manager_no_sessions
        text = kit.string(R.string.terminal_no_sessions)
        textSize = 14f
        setTextColor(palette.muted)
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = kit.dp(56)
        setPaddingRelative(kit.dp(24), 0, kit.dp(24), 0)
    }
    private val textSizeSummary = summaryText()
    private val registrySummary = summaryText()
    private val ignoreScripts = SwitchMaterial(activity).apply {
        id = R.id.manager_npm_ignore_scripts
        isClickable = false
        isFocusable = false
    }
    private var applyingState = false
    private var creating = false

    private val dialog: AlertDialog
    private val sessionListener: () -> Unit = { handler.post { if (dialog.isShowing) refresh() } }

    /**
     * Periodic refresh while showing: the uptime column moves, and a session's PID / directory only
     * become known shortly after the registry announced it.
     * zh-CN: 显示期间定时刷新: 运行时长在走, 且会话的 PID / 目录在注册表通知后片刻才可知.
     */
    private val tick = object : Runnable {
        override fun run() {
            if (!dialog.isShowing) return
            refresh()
            handler.postDelayed(this, TICK_MILLIS)
        }
    }

    init {
        val content = LinearLayout(activity).apply {
            id = R.id.manager_root
            orientation = LinearLayout.VERTICAL
            setPadding(0, kit.dp(4), 0, kit.dp(8))
        }
        content.addView(Section(R.id.manager_status, R.string.terminal_status, TerminalPreferences.KEY_MANAGER_STATUS_COLLAPSED).apply {
            body.addView(statusRow(R.string.terminal_state, stateValue))
            body.addView(statusRow(R.string.terminal_sessions, countValue))
        }.view)
        content.addView(Section(R.id.manager_controls, R.string.terminal_controls, TerminalPreferences.KEY_MANAGER_CONTROLS_COLLAPSED).apply {
            body.addView(newSession)
            body.addView(closeAll)
        }.view)
        content.addView(Section(R.id.manager_sessions, R.string.terminal_sessions, TerminalPreferences.KEY_MANAGER_SESSIONS_COLLAPSED).apply {
            body.addView(noSessions)
            body.addView(sessionList)
        }.view)
        content.addView(Section(R.id.manager_settings, R.string.terminal_settings, TerminalPreferences.KEY_MANAGER_SETTINGS_COLLAPSED).apply {
            body.addView(settingRow(R.id.manager_text_size, R.string.terminal_text_size, textSizeSummary) {
                TerminalSettingsDialogs.showTextSize(kit, preferences) { size ->
                    onTextSizeChanged(size)
                    refresh()
                }
            })
            body.addView(settingRow(R.id.manager_npm_registry, R.string.terminal_npm_registry, registrySummary) {
                TerminalSettingsDialogs.showNpmRegistry(kit, preferences, settings) { refresh() }
            })
            body.addView(switchRow(R.string.terminal_npm_ignore_scripts, R.string.terminal_npm_ignore_scripts_summary, ignoreScripts) { checked ->
                settings.setIgnoreScripts(checked)
            })
        }.view)
        kit.applyThemeToControls(content)
        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = true
            addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        dialog = kit.materialDialog()
            .setTitle(R.string.terminal_manager)
            .setView(scroll)
            .setPositiveButton(R.string.terminal_action_close, null)
            .create()
        dialog.setOnShowListener {
            kit.tintDialogButtons(dialog)
            TerminalSessionManager.addListener(sessionListener)
            refresh()
            handler.removeCallbacks(tick)
            handler.postDelayed(tick, TICK_MILLIS)
        }
        dialog.setOnDismissListener {
            handler.removeCallbacks(tick)
            TerminalSessionManager.removeListener(sessionListener)
            onDismissed()
        }
    }

    private fun show(): AlertDialog {
        dialog.show()
        val metrics = activity.resources.displayMetrics
        val width = minOf(kit.dp(560), metrics.widthPixels - kit.dp(48))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.decorView?.post {
            val maximum = (metrics.heightPixels * 0.85f).toInt()
            if ((dialog.window?.decorView?.height ?: 0) > maximum) dialog.window?.setLayout(width, maximum)
        }
        return dialog
    }

    /** Re-reads the registry and the preferences; called on show, on every registry change and after own actions. */
    fun refresh() {
        val sessions = TerminalSessionManager.activeSessions
        stateValue.text = kit.string(if (sessions.isEmpty()) R.string.terminal_stopped else R.string.terminal_running)
        countValue.text = formatCount(sessions.size)
        closeAll.isVisible = sessions.isNotEmpty()
        val now = System.currentTimeMillis()
        sessionList.removeAllViews()
        sessions.forEach { sessionList.addView(sessionRow(it, now)) }
        noSessions.isVisible = sessions.isEmpty()
        textSizeSummary.text = formatTextSize(preferences.textSizeSp)
        registrySummary.text = registryLabel()
        applyingState = true
        ignoreScripts.isChecked = preferences.npmIgnoreScripts
        applyingState = false
    }

    private fun registryLabel(): String = when (preferences.npmRegistryChoice) {
        TerminalPreferences.REGISTRY_NPMMIRROR -> kit.string(R.string.terminal_npm_registry_npmmirror)
        TerminalPreferences.REGISTRY_CUSTOM -> preferences.npmRegistry ?: kit.string(R.string.terminal_npm_registry_custom)
        else -> kit.string(R.string.terminal_npm_registry_npmjs)
    }

    // region Actions

    /**
     * New session in `$HOME` (the plugin has no host working directory; the entry protocol passes one
     * explicitly). The output is read from the start so the terminal shows the prompt when it opens.
     * zh-CN: 在 `$HOME` 新建会话 (插件没有宿主工作目录的概念, 入口协议会显式传目录); 从一开始就读取输出.
     */
    private fun createSession() {
        if (creating) return
        creating = true
        setEnabled(newSession, false)
        SessionStarter.start(activity, preferences, requestedDirectory = null, stillWanted = { dialog.isShowing }) { result ->
            creating = false
            setEnabled(newSession, true)
            result.onSuccess { started ->
                started.session.pty.updateSize(DEFAULT_COLUMNS, DEFAULT_ROWS)
                openSession(started.session.id)
            }
            result.onFailure { error -> kit.toast(error.message ?: kit.string(R.string.terminal_error_occurred), long = true) }
            refresh()
        }
    }

    private fun openSession(id: String) {
        if (TerminalSessionManager.get(id)?.pty?.isProcessAlive != true) return refresh()
        TerminalActivity.launchSession(activity, id, preserveManager = true)
    }

    private fun closeSession(id: String) {
        val session = TerminalSessionManager.get(id) ?: return refresh()
        val pty = session.pty
        if (!pty.isProcessAlive || !pty.hasLiveChildren()) {
            TerminalSessionManager.close(id)
            refresh()
            return
        }
        kit.confirmDialog(
            kit.string(R.string.terminal_session_title, id),
            kit.string(R.string.terminal_confirm_close_running),
            kit.string(R.string.terminal_close_session),
            destructive = true,
        ) {
            TerminalSessionManager.close(id)
            refresh()
        }
    }

    private fun showSession(id: String) {
        val session = TerminalSessionManager.get(id)?.takeIf { it.pty.isProcessAlive } ?: return refresh()
        val directory = session.pty.currentDirectory()?.path ?: session.initialDirectory
        val info = kit.string(R.string.terminal_working_directory) + ": " + directory + "\n" +
            "PID: " + session.pty.pid + "\n" +
            kit.string(R.string.terminal_uptime) + ": " + ElapsedTime.format(System.currentTimeMillis() - session.createdAt)
        val details = kit.materialDialog()
            .setTitle(kit.string(R.string.terminal_session_title, id))
            .setMessage(info)
            .setNeutralButton(R.string.terminal_action_copy) { _, _ ->
                Clipboard.set(activity, info)
                kit.toast(R.string.terminal_copied_to_clipboard)
            }
            .setNegativeButton(R.string.terminal_close_session) { _, _ -> closeSession(id) }
            .setPositiveButton(R.string.terminal_action_open) { _, _ -> openSession(id) }
            .show()
        kit.tintDialogButtons(details)
    }

    // endregion

    // region Rows

    private fun valueText(): TextView = TextView(activity).apply {
        textSize = 14f
        setTextColor(palette.text)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_END
    }

    private fun summaryText(): TextView = TextView(activity).apply {
        textSize = 13f
        setTextColor(palette.muted)
    }

    private fun statusRow(@StringRes label: Int, value: TextView): View = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = kit.dp(40)
        setPaddingRelative(kit.dp(24), 0, kit.dp(24), 0)
        addView(TextView(activity).apply {
            text = kit.string(label)
            textSize = 14f
            setTextColor(palette.muted)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(value, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun actionRow(@IdRes id: Int, @StringRes title: Int, onClick: () -> Unit): TextView = TextView(activity).apply {
        this.id = id
        text = kit.string(title)
        textSize = 16f
        setTextColor(palette.accent)
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = kit.dp(56)
        setPaddingRelative(kit.dp(24), 0, kit.dp(24), 0)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        kit.selectableBackground(this)
        setOnClickListener { onClick() }
    }

    private fun setEnabled(row: View, enabled: Boolean) {
        row.isEnabled = enabled
        row.alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    private fun settingRow(@IdRes id: Int, @StringRes title: Int, summary: TextView, onClick: () -> Unit): View = LinearLayout(activity).apply {
        this.id = id
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = kit.dp(72)
        setPaddingRelative(kit.dp(24), kit.dp(8), kit.dp(24), kit.dp(8))
        addView(TextView(activity).apply {
            text = kit.string(title)
            textSize = 16f
            setTextColor(palette.text)
        })
        addView(summary)
        kit.selectableBackground(this)
        setOnClickListener { onClick() }
    }

    private fun switchRow(@StringRes title: Int, @StringRes summary: Int, switch: SwitchMaterial, onChanged: (Boolean) -> Unit): View =
        LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = kit.dp(72)
            setPaddingRelative(kit.dp(24), kit.dp(8), kit.dp(16), kit.dp(8))
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(activity).apply {
                    text = kit.string(title)
                    textSize = 16f
                    setTextColor(palette.text)
                })
                addView(TextView(activity).apply {
                    text = kit.string(summary)
                    textSize = 13f
                    setTextColor(palette.muted)
                })
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(switch, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = kit.dp(16)
            })
            switch.setOnCheckedChangeListener { _, checked -> if (!applyingState) onChanged(checked) }
            kit.selectableBackground(this)
            setOnClickListener { switch.isChecked = !switch.isChecked }
        }

    private fun sessionRow(session: TerminalSessionManager.Session, now: Long): View {
        val directory = session.pty.currentDirectory()?.path ?: session.initialDirectory
        val row = LinearLayout(activity).apply {
            id = R.id.manager_session_row
            tag = session.id
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = kit.dp(72)
            setPaddingRelative(kit.dp(24), kit.dp(8), kit.dp(8), kit.dp(8))
            kit.selectableBackground(this)
            setOnClickListener { showSession(session.id) }
        }
        row.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(activity).apply {
                id = R.id.manager_session_title
                text = kit.string(R.string.terminal_session_title, session.id)
                textSize = 16f
                setTextColor(palette.text)
            })
            addView(TextView(activity).apply {
                id = R.id.manager_session_summary
                text = formatSessionSummary(session, directory, now)
                textSize = 13f
                setTextColor(palette.muted)
            })
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(activity).apply {
            id = R.id.manager_session_open
            text = kit.string(R.string.terminal_action_open)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(palette.accent)
            gravity = Gravity.CENTER
            minimumHeight = kit.dp(UiKit.TOUCH_TARGET)
            minWidth = kit.dp(UiKit.TOUCH_TARGET)
            setPaddingRelative(kit.dp(12), 0, kit.dp(12), 0)
            kit.selectableBackground(this, borderless = true)
            setOnClickListener { openSession(session.id) }
        })
        row.addView(ImageView(activity).apply {
            id = R.id.manager_session_close
            setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_close_24dp)?.let { kit.tinted(it, palette.muted) })
            contentDescription = kit.string(R.string.terminal_close_session)
            scaleType = ImageView.ScaleType.CENTER
            kit.selectableBackground(this, borderless = true)
            setOnClickListener { closeSession(session.id) }
        }, LinearLayout.LayoutParams(kit.dp(UiKit.TOUCH_TARGET), kit.dp(UiKit.TOUCH_TARGET)))
        return row
    }

    /** A collapsible group: header with the title and a chevron, body below; the state persists per section. */
    private inner class Section(@IdRes id: Int, @StringRes title: Int, private val key: String) {

        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

        private val chevron = ImageView(activity).apply {
            setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_expand_more_24dp)?.let { kit.tinted(it, palette.muted) })
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val view: LinearLayout = LinearLayout(activity).apply {
            this.id = id
            orientation = LinearLayout.VERTICAL
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = kit.dp(UiKit.TOUCH_TARGET)
                setPaddingRelative(kit.dp(24), 0, kit.dp(16), 0)
                addView(TextView(activity).apply {
                    text = kit.string(title)
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setTextColor(palette.accent)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(chevron, LinearLayout.LayoutParams(kit.dp(24), kit.dp(24)))
                kit.selectableBackground(this)
                setOnClickListener {
                    preferences.setManagerSectionCollapsed(key, !preferences.isManagerSectionCollapsed(key))
                    apply()
                }
            })
            addView(body)
        }

        fun apply() {
            val collapsed = preferences.isManagerSectionCollapsed(key)
            body.isVisible = !collapsed
            chevron.rotation = if (collapsed) 0f else 180f
        }

        init {
            apply()
        }

    }

    // endregion

    companion object {

        private const val DEFAULT_COLUMNS = 80
        private const val DEFAULT_ROWS = 24
        private const val DISABLED_ALPHA = 0.38f
        private const val TICK_MILLIS = 1_000L

        /** Session count in the status section. */
        internal fun formatCount(count: Int): String = String.format(Locale.getDefault(), "%d", count)

        /** Text size summary (`12 sp`); the unit is a technical token, not translated. */
        internal fun formatTextSize(sp: Int): String = String.format(Locale.getDefault(), "%d sp", sp)

        /** Second line of a session row: tilde path, PID and uptime; PID and `|` are technical tokens. */
        internal fun formatSessionSummary(session: TerminalSessionManager.Session, directory: String, now: Long): String =
            session.paths.toTildePath(directory) + "\n" + String.format(Locale.getDefault(), "PID: %d | %s", session.pty.pid, ElapsedTime.format(now - session.createdAt))

        /**
         * Shows the manager over [activity]; [onTextSizeChanged] lets a terminal underneath follow the
         * text size setting, [onDismissed] lets a dialog-only host Activity finish with it.
         * zh-CN: 在 [activity] 之上显示管理器; [onTextSizeChanged] 让下方终端跟随字号, [onDismissed] 让仅承载对话框的 Activity 随之结束.
         */
        fun show(activity: HostAppearanceActivity, onTextSizeChanged: (Int) -> Unit = {}, onDismissed: () -> Unit = {}): AlertDialog =
            TerminalManagerDialog(activity, onTextSizeChanged, onDismissed).show()

    }

}
