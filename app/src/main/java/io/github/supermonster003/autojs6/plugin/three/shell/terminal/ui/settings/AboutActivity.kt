package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.pm.PackageInfoCompat
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.UiKit
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.update.AppUpdateCoordinator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.update.ReleaseInfoCodec

/**
 * Identity, developer and licenses of the plugin (roadmap D31, P5.2): version with build number and
 * build date, the bundled documents (history, license, third-party notices), the jackpal
 * Android-Terminal-Emulator license and notice (Apache-2.0) and the AutoJs6 plugin API artifacts
 * (MPL 2.0). Every document opens offline; only "Source code" and "Developer page" leave the plugin.
 * zh-CN: 插件的身份, 开发者与许可证 (D31, P5.2): 带构建号与构建日期的版本, 内置文档 (版本历史, 许可证,
 * 第三方声明), jackpal Android-Terminal-Emulator 的许可证与声明 (Apache-2.0) 以及 AutoJs6 插件 API 构件
 * (MPL 2.0); 全部文档离线打开, 只有 "源码" 与 "开发者主页" 离开插件.
 */
class AboutActivity : HostAppearanceActivity() {

    internal lateinit var scaffold: Scaffold
        private set

    /** The manual update check (AGENTS.md 12); its dialogs keep the screen from being recreated underneath. */
    internal lateinit var updates: AppUpdateCoordinator
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scaffold = buildScaffold(getString(R.string.about_title))
        val page = scaffold.content
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName.orEmpty()
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        updates = AppUpdateCoordinator(this, versionName)
        with(kit) {
            page.addView(identityBlock())
            page.addView(hairline(0))
            page.addView(
                settingRow(getString(R.string.update_check), getString(R.string.update_installed, versionName), R.drawable.ic_settings_update, TAG_UPDATE) {
                    updates.check()
                }.view,
            )
            link(R.string.release_history_title, R.drawable.ic_settings_history, TAG_HISTORY) { open(BundledDocument.HISTORY) }
            link(R.string.settings_source, R.drawable.ic_settings_code, TAG_SOURCE) { AppUpdateCoordinator.openPage(this@AboutActivity, ReleaseInfoCodec.SOURCE) }
            link(R.string.about_developer_page, R.drawable.ic_settings_person, TAG_DEVELOPER) { AppUpdateCoordinator.openPage(this@AboutActivity, DEVELOPER_PAGE) }
            link(R.string.settings_license, R.drawable.ic_settings_license, TAG_LICENSE) { open(BundledDocument.LICENSE) }
            link(R.string.settings_notices, R.drawable.ic_settings_license, TAG_NOTICES) { open(BundledDocument.NOTICES) }
            page.addView(hairline(0))
            page.addView(sectionHeader(getString(R.string.about_third_party)))
            page.addView(
                settingRow(getString(R.string.about_component_jackpal), getString(R.string.about_component_jackpal_summary), R.drawable.ic_settings_license, TAG_JACKPAL) {
                    actionListDialog(
                        getString(R.string.about_component_jackpal),
                        null,
                        listOf(getString(R.string.about_jackpal_license), getString(R.string.about_jackpal_notice)),
                    ) { index -> open(if (index == 0) BundledDocument.JACKPAL_LICENSE else BundledDocument.JACKPAL_NOTICE) }
                }.view,
            )
            page.addView(
                settingRow(getString(R.string.about_component_host_api), getString(R.string.about_component_host_api_summary), R.drawable.ic_settings_license, TAG_HOST_API) {
                    open(BundledDocument.NOTICES)
                }.view,
            )
            page.addView(
                settingRow(getString(R.string.about_component_libraries), getString(R.string.about_component_libraries_summary), R.drawable.ic_settings_license, TAG_LIBRARIES) {
                    open(BundledDocument.NOTICES)
                }.view,
            )
            page.addView(hairline(0))
            page.addView(
                infoBlock(
                    getString(R.string.about_version),
                    getString(R.string.about_version_value, versionName, versionCode, getString(R.string.plugin_version_date)),
                    TAG_VERSION,
                ),
            )
            page.addView(infoBlock(getString(R.string.about_developer), getString(R.string.plugin_author), TAG_AUTHOR))
            page.addView(infoBlock(getString(R.string.settings_license), getString(R.string.about_license_name), TAG_LICENSE_NAME))
        }
        setContentView(scaffold.root)
    }

    /** Icon in a rounded outline (the transparent glyph shows the page behind it), name and description. */
    private fun UiKit.identityBlock(): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPaddingRelative(dp(SettingsMetrics.SCREEN_MARGIN), dp(SettingsMetrics.SECTION_TOP), dp(SettingsMetrics.SCREEN_MARGIN), dp(SettingsMetrics.SECTION_TOP))
        addView(
            ImageView(context).apply {
                setImageResource(R.mipmap.ic_launcher)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                tag = TAG_ICON
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    cornerRadius = dpF(UiKit.DIALOG_RADIUS.toFloat())
                    setStroke(dp(1), palette.outline)
                }
                clipToOutline = true
            },
            LinearLayout.LayoutParams(dp(ICON_SIZE), dp(ICON_SIZE)),
        )
        addView(
            text(getString(R.string.app_name), SettingsMetrics.TEXT_PAGE_TITLE, medium = true).apply {
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPaddingRelative(0, dp(SettingsMetrics.CHEVRON_GAP), 0, dp(SettingsMetrics.TITLE_SUMMARY_GAP))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isAccessibilityHeading = true
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
        addView(
            text(getString(R.string.plugin_description), SettingsMetrics.TEXT_SUMMARY, palette.muted).apply {
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
    }

    private fun UiKit.link(@StringRes title: Int, @DrawableRes icon: Int, tag: String, action: () -> Unit) {
        scaffold.content.addView(settingRow(getString(title), null, icon, tag, onClick = action).view)
    }

    private fun open(document: BundledDocument) {
        startActivity(ReleaseHistoryActivity.intent(this, document))
    }

    override fun hasUnconfirmedDialog(): Boolean = ::updates.isInitialized && updates.isShowingDialog

    /** Instrumentation access to the protected recreate guard. */
    internal fun hasUnconfirmedDialogForTest(): Boolean = hasUnconfirmedDialog()

    override fun onDestroy() {
        if (::updates.isInitialized) updates.close()
        super.onDestroy()
    }

    companion object {

        const val DEVELOPER_PAGE = "https://github.com/SuperMonster003"

        private const val ICON_SIZE = 88

        const val TAG_ICON = "about-icon"
        const val TAG_UPDATE = "about-update"
        const val TAG_HISTORY = "about-history"
        const val TAG_SOURCE = "about-source"
        const val TAG_DEVELOPER = "about-developer"
        const val TAG_LICENSE = "about-license"
        const val TAG_NOTICES = "about-notices"
        const val TAG_JACKPAL = "about-jackpal"
        const val TAG_HOST_API = "about-host-api"
        const val TAG_LIBRARIES = "about-libraries"
        const val TAG_VERSION = "about-version"
        const val TAG_AUTHOR = "about-author"
        const val TAG_LICENSE_NAME = "about-license-name"

    }

}
