package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.google.android.material.appbar.MaterialToolbar
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.SessionAssembly
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.ShellQuoting
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalKeySequences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSettingsActions
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator.Resolution
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.TerminalNodeSetup
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.service.SessionNotifications
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.storage.StorageAccess
import jackpal.androidterm.emulatorview.ColorScheme
import org.autojs.plugin.terminal.api.TerminalContract
import org.autojs.plugin.terminal.api.TerminalErrorCodes
import kotlin.math.max

/**
 * The terminal screen (roadmap P3.1, ported from the host): a pty-backed `/system/bin/sh` rendered
 * by the jackpal emulator view, a key bar, npm helpers, and two banners that explain why `node` may
 * be unavailable (D17) or why the requested directory could not be entered (D18). Sessions live in
 * [TerminalSessionManager] and survive this screen; the start goes through [SessionAssembly] so the
 * screen and the host Binder resolve directories and the Node.js toolchain identically.
 *
 * zh-CN: 终端界面 (P3.1, 自宿主迁入): jackpal 模拟器视图渲染的 pty `/system/bin/sh`, 按键栏, npm 辅助动作,
 * 以及说明 `node` 为何不可用 (D17) 或目录为何无法进入 (D18) 的两条横幅. 会话常驻 [TerminalSessionManager],
 * 启动经 [SessionAssembly], 与宿主 Binder 以同一方式解析目录与 Node.js 工具链.
 */
class TerminalActivity : HostAppearanceActivity() {

    private lateinit var root: LinearLayout
    private lateinit var statusBarSpacer: View
    private lateinit var toolbar: MaterialToolbar
    private lateinit var terminalContainer: FrameLayout
    private lateinit var keyBar: TerminalToolbarView
    private lateinit var terminalView: TerminalEmulatorView
    private lateinit var nodeBanner: NodeBanner
    private lateinit var storageBanner: StorageBanner
    private lateinit var npmDialogs: TerminalNpmDialogs

    private val preferences: TerminalPreferences by lazy { TerminalPreferences(this) }
    private val settingsActions: TerminalSettingsActions by lazy { TerminalSettingsActions(this) }

    private var textSelection: TerminalTextSelection? = null
    private var session: TerminalSessionManager.Session? = null
    private var sessionAttached = false
    private var subtitleDirectory: String? = null
    private var nodeResolution: Resolution? = null
    private var progressDialog: AlertDialog? = null

    /** Incremented by every (re)start so a slow plan cannot attach after a newer request. zh-CN: 每次 (重新) 启动递增, 使迟到的计划不会覆盖更新的请求. */
    private var startGeneration = 0

    private val sessionListener: () -> Unit = {
        runOnUiThread { if (!isDestroyed && session?.pty?.isProcessAlive == false) onSessionEnded() }
    }

    private val legacyStoragePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        storageBanner.refresh()
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun hasUnconfirmedDialog(): Boolean = progressDialog?.isShowing == true || textSelection != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)
        root = findViewById(R.id.root)
        statusBarSpacer = findViewById(R.id.status_bar_spacer)
        toolbar = findViewById(R.id.toolbar)
        terminalContainer = findViewById(R.id.terminal_container)
        keyBar = findViewById(R.id.key_bar)
        terminalView = findViewById(R.id.terminal)
        applyPalette()
        setUpToolbar()
        applyWindowInsets()

        npmDialogs = TerminalNpmDialogs(kit, ::typeCommand) { session?.pty?.currentDirectory() }
        nodeBanner = NodeBanner(this, TerminalBanner(findViewById(R.id.node_banner), kit), settingsActions) {
            refreshNodeAvailability(refresh = true)
        }
        storageBanner = StorageBanner(
            this,
            TerminalBanner(findViewById(R.id.storage_banner), kit),
            requestLegacyPermissions = { legacyStoragePermissions.launch(StorageAccess.legacyPermissionsToRequest()) },
            onReenter = ::changeDirectory,
        )
        setUpKeyBar()
        setUpBackHandling()
        TerminalSessionManager.addListener(sessionListener)
        openSession(intent, savedInstanceState?.getString(STATE_SESSION_ID))
    }

    private fun applyPalette() {
        root.setBackgroundColor(palette.surface)
        statusBarSpacer.setBackgroundColor(palette.surface)
        terminalContainer.setBackgroundColor(palette.terminalBackground)
        keyBar.setBackgroundColor(palette.surface)
        toolbar.setBackgroundColor(palette.surface)
        toolbar.setTitleTextColor(palette.text)
        toolbar.setSubtitleTextColor(palette.muted)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        toolbar.title = getString(R.string.terminal_title)
        toolbar.navigationIcon = androidx.appcompat.content.res.AppCompatResources
            .getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material)?.let { kit.tinted(it, palette.text) }
        toolbar.setNavigationContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description)
        toolbar.setNavigationOnClickListener { leave() }
        toolbar.overflowIcon = toolbar.overflowIcon?.let { kit.tinted(it, palette.text) }
    }

    /**
     * Edge to edge: the spacer takes the status bar, the root pads for the side bars and for the
     * larger of navigation bar and keyboard, so the key bar always sits right above the IME.
     * zh-CN: 全面屏布局: 占位视图承接状态栏, 根布局为侧边栏与导航栏 / 键盘中较高者留白, 按键栏始终贴在输入法上方.
     */
    private fun applyWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            statusBarSpacer.layoutParams?.let { params ->
                if (params.height != bars.top) {
                    params.height = bars.top
                    statusBarSpacer.layoutParams = params
                }
            }
            view.setPadding(bars.left, 0, bars.right, max(bars.bottom, ime.bottom))
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    // region Session lifecycle

    private fun openSession(request: Intent, restoredId: String? = null) {
        startGeneration++
        dismissProgress()
        val explicitId = restoredId ?: request.getStringExtra(TerminalContract.EXTRA_SESSION_ID)
        val command = request.getStringExtra(TerminalContract.EXTRA_COMMAND)?.takeIf { it.isNotBlank() }
        val wantsNew = request.getBooleanExtra(TerminalContract.EXTRA_NEW_SESSION, false) || (explicitId == null && command != null)
        val restored = explicitId?.let(TerminalSessionManager::get)?.takeIf { it.pty.isProcessAlive }
            ?: if (explicitId == null && !wantsNew) TerminalSessionManager.activeSessions.lastOrNull() else null
        if (explicitId != null && restored == null) {
            kit.toast(R.string.terminal_session_ended)
            if (session == null) finish()
            return
        }
        if (restored == null) {
            startSession(request.getStringExtra(TerminalContract.EXTRA_DIRECTORY)?.takeIf { it.isNotBlank() }, command)
        } else {
            attachSession(restored)
            updateSubtitle(restored.pty.currentDirectory()?.path ?: restored.initialDirectory)
            if (restoredId == null && explicitId == null) {
                request.getStringExtra(TerminalContract.EXTRA_DIRECTORY)?.takeIf { it.isNotBlank() }?.let(::changeDirectory)
            }
            refreshNodeAvailability(refresh = false)
        }
    }

    /**
     * Plans the session off the main thread ([SessionAssembly.plan]: directory per D18, Node.js
     * toolchain per D17) and starts it on the main thread. The archive extraction (first run after
     * a Node.js Runtime install or update) shows a progress dialog.
     * zh-CN: 在工作线程规划会话 (D18 目录, D17 工具链), 在主线程启动; 资产解压 (插件安装或更新后的首次) 显示进度.
     */
    private fun startSession(requestedDirectory: String?, command: String?) {
        val generation = ++startGeneration
        requestNotificationPermissionOnce()
        val app = applicationContext
        val prefs = preferences
        BackgroundWork.run({
            val resolution = NodeCliLocator.resolve(app, integrationEnabled = prefs.nodeIntegrationEnabled)
            TerminalNodeSetup.needsInstall(TerminalPaths.of(app), resolution)
        }) { probe ->
            if (generation != startGeneration || isFinishing || isDestroyed) return@run
            if (probe.getOrDefault(false)) progressDialog = kit.progressDialog(getString(R.string.terminal_preparing_npm))
            BackgroundWork.run({ SessionAssembly.plan(app, requestedDirectory, prefs) }) { planned ->
                if (generation != startGeneration || isFinishing || isDestroyed) {
                    dismissProgress()
                    return@run
                }
                dismissProgress()
                val started = planned.mapCatching { plan -> plan to SessionAssembly.start(this, plan, command = command) }
                started.onFailure { error ->
                    kit.toast(error.message ?: getString(R.string.terminal_error_occurred), long = true)
                    if (session == null) finish()
                }
                started.onSuccess { (plan, created) ->
                    nodeResolution = plan.nodeResolution
                    attachSession(created)
                    updateSubtitle(plan.directory.directory.path)
                    nodeBanner.render(plan.nodeResolution)
                    renderDirectoryFallback(plan.directory)
                    invalidateOptionsMenu()
                }
            }
        }
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    /** API 33+: ask for the notification permission once; a refusal only hides the session notification (D15). */
    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (SessionNotifications.isPermissionGranted(this) || preferences.notificationPermissionRequested) return
        preferences.notificationPermissionRequested = true
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun renderDirectoryFallback(resolved: StorageAccess.Resolved) {
        when (resolved.fallbackReason) {
            null -> storageBanner.hide()
            TerminalErrorCodes.STORAGE_PERMISSION_REQUIRED -> storageBanner.showPermissionRequired(resolved.requested.orEmpty())
            else -> {
                storageBanner.hide()
                kit.toast(getString(R.string.terminal_directory_inaccessible, resolved.requested.orEmpty()), long = true)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openSession(intent)
    }

    /**
     * Types a `cd` into the running shell (used when the terminal is reopened for a directory);
     * a directory the plugin cannot enter shows the banner or toast instead of a failing command.
     * zh-CN: 向运行中的 shell 键入 `cd`; 无法进入的目录改为显示横幅或提示, 而不是键入注定失败的命令.
     */
    private fun changeDirectory(requested: String) {
        val current = session ?: return
        val pty = current.pty
        if (!pty.isProcessAlive) return
        val resolved = StorageAccess.resolveDirectory(this, requested, current.paths.home)
        if (resolved.fellBack) {
            renderDirectoryFallback(resolved)
            return
        }
        storageBanner.hide()
        pty.clearModifiers()
        pty.write("cd " + ShellQuoting.quote(resolved.directory.path) + TerminalKeySequences.ENTER)
        updateSubtitle(resolved.directory.path)
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing) return
        session?.let(::attachSession)
        terminalView.textSizeSp = preferences.textSizeSp
        terminalView.onResume()
        refreshSubtitle()
        storageBanner.refresh()
    }

    override fun onPause() {
        textSelection?.dismiss()
        terminalView.onPause()
        // A foreground terminal may attach the same PTY while this screen stays underneath it.
        // zh-CN: 前台的另一个终端界面可能附加同一个 PTY, 本界面退到后台时先解除附加.
        detachSession()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SESSION_ID, session?.id)
    }

    /**
     * Leaving the screen never ends the session: it stays in [TerminalSessionManager] under the
     * foreground service until "Close session" or the shell's own exit.
     * zh-CN: 离开界面不会结束会话: 它由前台服务保持在 [TerminalSessionManager] 中, 直到 "关闭会话" 或 shell 自行退出.
     */
    override fun onDestroy() {
        startGeneration++
        dismissProgress()
        TerminalSessionManager.removeListener(sessionListener)
        detachSession()
        super.onDestroy()
    }

    private fun detachSession() {
        if (!sessionAttached) return
        sessionAttached = false
        session?.pty?.let {
            it.onProcessExited = null
            it.onInputAfterExit = null
            it.onModifiersChanged = null
            it.setUpdateCallback(null)
        }
    }

    private fun attachSession(newSession: TerminalSessionManager.Session) {
        if (session?.id == newSession.id && sessionAttached) return
        detachSession()
        terminalView.onPause()
        textSelection?.dismiss()
        val parent = terminalView.parent as ViewGroup
        val index = parent.indexOfChild(terminalView)
        val params = terminalView.layoutParams
        parent.removeView(terminalView)
        terminalView = TerminalEmulatorView(this).apply { id = R.id.terminal }
        parent.addView(terminalView, index, params)
        session = newSession
        sessionAttached = true
        val pty = newSession.pty
        val view = terminalView
        val scheme = ColorScheme(palette.terminalForeground, palette.terminalBackground)
        pty.setColorScheme(scheme)
        view.attachSession(pty)
        // The view allocates its paints in attachSession, so the scheme has to follow it.
        // zh-CN: 视图在 attachSession 中才创建画笔, 配色必须在其后设置.
        view.setColorScheme(scheme)
        view.setExtGestureListener(object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                showSoftKeyboard(view)
                return false
            }
        })
        view.onResume()
        view.onSelectionRequested = { x, y ->
            textSelection?.dismiss()
            val selection = TerminalTextSelection(view) { textSelection = null }
            textSelection = selection
            selection.show(x, y)
        }
        pty.onProcessExited = { onSessionEnded() }
        pty.onInputAfterExit = { finish() }
        pty.onModifiersChanged = { ctrl, alt -> keyBar.setModifiers(ctrl, alt) }
        keyBar.setModifiers(pty.ctrlArmed, pty.altArmed)
        if (pty.exitCode != null) {
            onSessionEnded()
        } else {
            view.postDelayed({ if (!isDestroyed) refreshSubtitle() }, SUBTITLE_REFRESH_DELAY_MILLIS)
            view.post { if (!isDestroyed) view.requestFocus() }
        }
    }

    private fun onSessionEnded() {
        subtitleDirectory = null
        toolbar.subtitle = getString(R.string.terminal_session_ended)
        keyBar.setModifiers(ctrl = false, alt = false)
    }

    // endregion

    // region Key bar, back and subtitle

    private fun setUpKeyBar() {
        keyBar.listener = object : TerminalToolbarView.Listener {
            override fun onKey(key: TerminalKeySequences.Key) {
                session?.pty?.sendKey(key)
            }

            override fun onText(text: String) {
                session?.pty?.write(text)
            }

            override fun onToggleCtrl() {
                showSoftKeyboard(terminalView)
                session?.pty?.toggleCtrl()
            }

            override fun onToggleAlt() {
                showSoftKeyboard(terminalView)
                session?.pty?.toggleAlt()
            }
        }
    }

    private fun setUpBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (textSelection != null) textSelection?.dismiss() else leave()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        leave()
        return true
    }

    override fun navigateBack() = leave()

    /**
     * Back / Up: the session keeps running in the background; a toast says so while a child process is busy.
     * zh-CN: 返回 / 向上: 会话在后台继续运行; 有子进程忙碌时以 toast 说明.
     */
    private fun leave() {
        val pty = session?.pty
        if (pty != null && pty.isProcessAlive && pty.hasLiveChildren()) {
            kit.toast(R.string.terminal_session_kept_running)
        }
        finish()
    }

    /** Menu "Close session": ends the shell (after confirmation while a child process runs) and leaves. zh-CN: 菜单 "关闭会话": 结束 shell (有子进程运行时先确认) 并离开. */
    private fun requestCloseSession() {
        val current = session
        val pty = current?.pty
        if (current == null || pty == null || !pty.isProcessAlive || !pty.hasLiveChildren()) {
            current?.let { TerminalSessionManager.close(it.id) }
            finish()
            return
        }
        kit.confirmDialog(
            getString(R.string.terminal_close_session),
            getString(R.string.terminal_confirm_close_running),
            getString(R.string.terminal_close_session),
            destructive = true,
        ) {
            TerminalSessionManager.close(current.id)
            finish()
        }
    }

    private fun updateSubtitle(directory: String) {
        val paths = session?.paths ?: return
        subtitleDirectory = directory
        toolbar.subtitle = paths.toTildePath(directory)
        toolbar.subtitleView()?.setOnClickListener {
            val current = session?.pty?.takeIf { it.isProcessAlive }?.currentDirectory()?.path ?: subtitleDirectory
            if (current != null) {
                updateSubtitle(current)
                copyToClipboard(current)
            }
        }
    }

    /**
     * Shows where the shell actually is: the requested directory may have been replaced by `$HOME`
     * (D18), and the user may have changed directories since.
     * zh-CN: 显示 shell 实际所在目录: 请求目录可能已被 `$HOME` 取代 (D18), 用户也可能已切换目录.
     */
    private fun refreshSubtitle() {
        val pty = session?.pty ?: return
        if (!pty.isProcessAlive) return
        val current = pty.currentDirectory()?.path ?: return
        updateSubtitle(current)
    }

    // endregion

    // region Node availability

    /**
     * For restored sessions and after the integration switch changed: the environment of a running
     * shell cannot change, so this only refreshes the banner and the menu state.
     * zh-CN: 用于恢复的会话与集成开关变化之后: 运行中 shell 的环境无法更改, 这里只刷新横幅与菜单状态.
     */
    private fun refreshNodeAvailability(refresh: Boolean) {
        val app = applicationContext
        val enabled = preferences.nodeIntegrationEnabled
        BackgroundWork.run({ NodeCliLocator.resolve(app, refresh, enabled) }) { result ->
            if (isFinishing || isDestroyed) return@run
            val resolution = result.getOrElse { Resolution.Unavailable.PluginMissing }
            nodeResolution = resolution
            nodeBanner.render(resolution)
            invalidateOptionsMenu()
        }
    }

    // endregion

    // region Menu

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_terminal, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_npm_ignore_scripts)?.isChecked = preferences.npmIgnoreScripts
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId in NPM_MENU_ITEMS && !nodeBanner.ensureAvailable(nodeResolution)) {
            return true
        }
        when (item.itemId) {
            R.id.action_npm_init -> npmDialogs.npmInit()
            R.id.action_npm_install -> npmDialogs.npmInstall()
            R.id.action_npm_install_package -> npmDialogs.npmInstallPackage()
            R.id.action_npm_run_script -> npmDialogs.npmRunScript()
            R.id.action_other_package_managers -> npmDialogs.otherPackageManagers()
            R.id.action_search_npm -> npmDialogs.searchNpm()
            R.id.action_copy_transcript -> copyToClipboard(session?.pty?.transcriptText)
            R.id.action_paste -> paste()
            R.id.action_share_transcript -> ExternalIntents.shareText(this, session?.pty?.transcriptText.orEmpty())
            R.id.action_text_size -> TerminalSettingsDialogs.showTextSize(kit, preferences) { terminalView.textSizeSp = it }
            R.id.action_npm_registry -> TerminalSettingsDialogs.showNpmRegistry(kit, preferences, settingsActions)
            R.id.action_npm_ignore_scripts -> {
                settingsActions.setIgnoreScripts(!item.isChecked)
                invalidateOptionsMenu()
            }
            R.id.action_node_probe -> nodeBanner.showProbeDetails(nodeResolution ?: Resolution.Unavailable.PluginMissing)
            R.id.action_show_keyboard -> showSoftKeyboard()
            R.id.action_clear -> clearScreen()
            R.id.action_tips -> showTips()
            R.id.action_new_session -> openSession(intent(this).putExtra(TerminalContract.EXTRA_NEW_SESSION, true))
            R.id.action_close_session -> requestCloseSession()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // endregion

    private fun typeCommand(command: String) {
        val pty = session?.pty ?: return
        pty.clearModifiers()
        pty.write(command + TerminalKeySequences.ENTER)
    }

    private fun paste() {
        val text = Clipboard.get(this)?.toString().orEmpty()
        if (text.isEmpty()) return
        session?.pty?.write(text)
    }

    private fun copyToClipboard(text: String?) {
        val value = text?.takeIf { it.isNotEmpty() } ?: return
        Clipboard.set(this, value)
        kit.toast(R.string.terminal_copied_to_clipboard)
    }

    private fun clearScreen() {
        val pty = session?.pty ?: return
        pty.reset()
        if (pty.isProcessAlive) {
            TerminalKeySequences.control('l')?.let { pty.write(it.toString()) }
        }
    }

    private fun showSoftKeyboard(view: View = terminalView) {
        view.requestFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(view, 0)
    }

    private fun showTips() {
        val content = getString(R.string.terminal_tips_content, TerminalPaths.of(this).home.path) + "\n\n" + getString(R.string.terminal_input_tips)
        kit.messageDialog(getString(R.string.terminal_tips), content)
    }

    companion object {

        private const val STATE_SESSION_ID = "session_id"
        private const val SUBTITLE_REFRESH_DELAY_MILLIS = 800L

        private val NPM_MENU_ITEMS = listOf(
            R.id.action_npm_init,
            R.id.action_npm_install,
            R.id.action_npm_install_package,
            R.id.action_npm_run_script,
            R.id.action_other_package_managers,
        )

        /** The toolbar's subtitle TextView, once a subtitle has been set. zh-CN: 已设置副标题后的副标题 TextView. */
        internal fun Toolbar.subtitleView(): TextView? {
            val subtitle = subtitle?.toString() ?: return null
            return children.filterIsInstance<TextView>().firstOrNull { it.text?.toString() == subtitle }
        }

        /**
         * Explicit intent of this screen; the extras use the `TerminalContract` names so the entry
         * Activity (roadmap P3.3) forwards the host's extras unchanged.
         * zh-CN: 本界面的显式 Intent; extras 使用 `TerminalContract` 键名, 使入口 Activity (P3.3) 可原样转发宿主的 extras.
         */
        @JvmStatic
        @JvmOverloads
        fun intent(context: Context, directory: String? = null): Intent = Intent(context, TerminalActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            directory?.takeIf { it.isNotBlank() }?.let { putExtra(TerminalContract.EXTRA_DIRECTORY, it) }
        }

        fun launchNew(context: Context) {
            ExternalIntents.startSafely(context, intent(context).putExtra(TerminalContract.EXTRA_NEW_SESSION, true))
        }

        /**
         * Brings [id] to the front; [preserveManager] keeps the caller and its dialog underneath a
         * distinct terminal Activity instead of reusing the current one (roadmap P3.2).
         * zh-CN: 把会话 [id] 带到前台; [preserveManager] 时在独立终端 Activity 下保留调用页面及其对话框 (P3.2).
         */
        fun launchSession(context: Context, id: String, preserveManager: Boolean = false) {
            val intent = intent(context).putExtra(TerminalContract.EXTRA_SESSION_ID, id)
            if (preserveManager) intent.flags = intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP.inv()
            ExternalIntents.startSafely(context, intent)
        }

        @JvmStatic
        @JvmOverloads
        fun launch(context: Context, directory: String? = null) {
            ExternalIntents.startSafely(context, intent(context, directory))
        }

    }

}
