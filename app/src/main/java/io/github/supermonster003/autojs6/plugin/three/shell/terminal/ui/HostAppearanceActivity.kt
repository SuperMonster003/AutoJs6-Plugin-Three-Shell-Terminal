package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

/**
 * Base of every screen of the plugin: the night mode is fixed on the AppCompat delegate and the
 * locale-wrapped base context is attached before any resource is read, so language, night mode and
 * the theme color follow AutoJs6 (standalone settings specification, sections 6 and 7). The host
 * snapshot is refreshed off the main thread on every resume; when the effective appearance changed
 * the screen is recreated, unless a subclass reports an unconfirmed dialog or user interaction.
 *
 * zh-CN: 插件全部界面的基类: 在读取任何资源之前先在 AppCompat delegate 上固定夜间模式并附着带语言的基础上下文,
 * 使语言, 夜间模式与主题色跟随 AutoJs6 (独立设置页规范第 6, 7 节). 每次 onResume 在工作线程刷新宿主快照,
 * 有效外观变化时重建界面 (子类报告未确认对话框或用户正在交互时除外).
 */
abstract class HostAppearanceActivity : AppCompatActivity() {

    private var applied: Appearance? = null
    private lateinit var systemContext: Context
    private var appearanceGeneration = 0
    private var interacted = false

    internal val appearance: Appearance? get() = applied

    /** Runtime palette of this screen, resolved once from the attached appearance. */
    internal val palette: TerminalPalette by lazy { TerminalPalette.resolve(this, applied) }

    internal val kit: UiKit by lazy { UiKit(this, palette) }

    /** Subclasses return true while a dialog holds an unconfirmed draft; the screen is then not recreated under it. */
    protected open fun hasUnconfirmedDialog(): Boolean = false

    override fun onUserInteraction() {
        interacted = true
        super.onUserInteraction()
    }

    override fun attachBaseContext(newBase: Context) {
        systemContext = newBase
        val resolved = Appearance.resolve(newBase)
        applied = resolved
        delegate.localNightMode = if (resolved.dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        super.attachBaseContext(resolved.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarAppearance()
    }

    /**
     * System bars: the status bar stays transparent behind the screen's own spacer, the navigation
     * bar takes the surface color (below API 26 a dark color, because light icons cannot be requested there).
     */
    @Suppress("DEPRECATION")
    protected open fun applySystemBarAppearance() {
        val dark = palette.isDark
        // PhoneWindow.getInsetsController() on Android 13 dereferences its decor directly; materialize it first.
        val decor = window.decorView
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) palette.surface else 0xFF121212.toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (dark) 0 else light, light)
        } else {
            decor.systemUiVisibility = if (dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        }
    }

    override fun onResume() {
        super.onResume()
        interacted = false
        if (!hasUnconfirmedDialog() && applied != Appearance.resolve(systemContext)) {
            recreate()
            return
        }
        val expected = ++appearanceGeneration
        HostAppearance.worker.execute {
            val next = HostAppearance.read(applicationContext)
            runOnUiThread {
                if (expected != appearanceGeneration || isFinishing || isDestroyed) return@runOnUiThread
                HostAppearance.cached = next
                if (!interacted && !hasUnconfirmedDialog() && applied != Appearance.resolve(systemContext, next)) recreate()
            }
        }
    }

    override fun onPause() {
        appearanceGeneration++
        super.onPause()
    }

    /** Toolbar navigation and the system back gesture both land here. */
    open fun navigateBack() {
        finish()
    }

    /** True when the attached base context is in night mode (for callers without a palette yet). */
    protected fun isNightMode(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

}
