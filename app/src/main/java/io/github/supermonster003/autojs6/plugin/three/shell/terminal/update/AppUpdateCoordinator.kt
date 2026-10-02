package io.github.supermonster003.autojs6.plugin.three.shell.terminal.update

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.ExternalIntents
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.UiKit
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings.BundledDocument
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings.DocumentText
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings.ReleaseHistoryActivity
import java.util.concurrent.Executors

/**
 * The manual "Check for updates" flow (AGENTS.md 12, roadmap P5.2): a cancelable progress dialog
 * with a hard timeout, one toast per failure, a 12-hour reuse of the last answer, and a result
 * dialog whose neutral action opens the bundled history, whose positive action opens the release
 * page in the browser and whose negative action ignores (or un-ignores) that version. Nothing runs
 * automatically and nothing is downloaded.
 * zh-CN: 手动 "检查更新" 流程 (AGENTS.md 12, P5.2): 可取消且带硬超时的进度对话框, 失败仅一条 toast, 结果复用
 * 12 小时, 结果对话框的中性按钮打开内置版本历史, 肯定按钮用浏览器打开发行页, 否定按钮忽略 (或取消忽略) 该版本;
 * 没有自动检查, 不下载任何文件.
 */
internal class AppUpdateCoordinator(private val activity: HostAppearanceActivity, private val installed: String) {

    private val preferences = activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "AppUpdate").apply { isDaemon = true } }
    private var pending: UpdateCancellation? = null
    private var generation = 0
    val settings = AppUpdateSettings(activity)

    internal var dialog: AlertDialog? = null
        private set

    /** True while the progress or the result dialog is up; the owner then keeps the screen from recreating. */
    val isShowingDialog: Boolean get() = dialog?.isShowing == true

    fun check() {
        if (pending != null) return
        val cached = preferences.getString(KEY_RELEASE, null)
        val parsed = if (cached.isNullOrEmpty()) null else runCatching { ReleaseInfoCodec.decode(cached) }.getOrNull()
        val last = if (preferences.contains(KEY_CHECKED) && (cached == "" || parsed != null)) preferences.getLong(KEY_CHECKED, 0) else null
        if (!UpdateSchedulePolicy.fetchDue(last, System.currentTimeMillis())) {
            present(parsed)
            return
        }
        val call = UpdateCancellation()
        pending = call
        val expected = ++generation
        dialog = activity.kit.materialDialog()
            .setMessage(R.string.update_checking)
            .setNegativeButton(android.R.string.cancel) { _, _ -> cancel() }
            .setOnCancelListener { cancel() }
            .show()
            .also { activity.kit.tintDialogButtons(it) }
        val timeout = Runnable {
            if (pending === call) {
                cancel()
                toast(R.string.update_failed)
            }
        }
        main.postDelayed(timeout, TIMEOUT_MS)
        worker.execute {
            val result = runCatching { (sourceOverride ?: AppUpdateRepository()).fetchLatest(call) }
                .getOrElse { UpdateResult.Failure(UpdateFailure.NETWORK) }
            main.post {
                if (generation != expected || call.cancelled || activity.isFinishing || activity.isDestroyed) return@post
                main.removeCallbacks(timeout)
                pending = null
                dialog?.dismiss()
                dialog = null
                when (result) {
                    is UpdateResult.Success -> {
                        val encoded = runCatching { result.release?.let(ReleaseInfoCodec::encode).orEmpty() }
                        if (encoded.isFailure) {
                            toast(R.string.update_failed)
                        } else {
                            preferences.edit().putString(KEY_RELEASE, encoded.getOrThrow()).putLong(KEY_CHECKED, System.currentTimeMillis()).apply()
                            present(result.release)
                        }
                    }
                    is UpdateResult.Failure -> toast(R.string.update_failed)
                }
            }
        }
    }

    private fun present(release: ReleaseInfo?) {
        if (release == null) {
            toast(R.string.update_no_release)
            return
        }
        if (!AppVersionPolicy.isNewer(release.tag, installed)) {
            toast(R.string.update_current)
            return
        }
        val ignored = settings.ignored.any { AppVersionPolicy.isIgnored(release.tag, it) }
        dialog?.dismiss()
        val kit = activity.kit
        val message = SpannableStringBuilder(activity.getString(R.string.update_installed, installed))
        if (release.notes.isNotBlank()) message.append("\n\n").append(DocumentText.render(release.notes, kit.palette))
        dialog = kit.materialDialog()
            .setTitle(activity.getString(R.string.update_available, release.tag))
            .setMessage(message)
            .setPositiveButton(R.string.update_open_release) { _, _ ->
                if (ReleaseInfoCodec.validUrl(release.url, release.tag)) openPage(activity, release.url)
            }
            .setNeutralButton(R.string.release_history_title) { _, _ ->
                activity.startActivity(ReleaseHistoryActivity.intent(activity, BundledDocument.HISTORY))
            }
            .setNegativeButton(if (ignored) R.string.update_unignore else R.string.update_ignore) { _, _ ->
                if (ignored) settings.unignore(listOf(release.tag)) else settings.ignore(release.tag)
            }
            .show()
            .also { kit.tintDialogButtons(it) }
    }

    fun cancel() {
        generation++
        pending?.cancel()
        pending = null
        main.removeCallbacksAndMessages(null)
        dialog?.dismiss()
        dialog = null
    }

    fun close() {
        cancel()
        worker.shutdownNow()
    }

    private fun toast(@StringRes resource: Int) = activity.kit.toast(resource, long = true)

    companion object {

        const val PREFERENCES = "updates"
        const val KEY_RELEASE = "release"
        const val KEY_CHECKED = "checked"
        const val TIMEOUT_MS = 25_000L

        /** Instrumentation hook: replaces the GitHub fetch for the current process. */
        @Volatile
        internal var sourceOverride: UpdateSource? = null

        fun openPage(context: Context, url: String) {
            if (!ExternalIntents.browse(context, url)) UiKit.of(context).toast(R.string.settings_open_failed, long = true)
        }

    }

}

/** The ignored release tags; invalid saved values are dropped on read. zh-CN: 已忽略的发行 tag; 读取时丢弃无效值. */
internal class AppUpdateSettings(context: Context) {

    private val preferences = context.getSharedPreferences(AppUpdateCoordinator.PREFERENCES, Context.MODE_PRIVATE)

    val ignored: Set<String>
        get() = preferences.getStringSet(KEY_IGNORED, emptySet()).orEmpty().filter { AppVersionPolicy.parse(it) != null }.toSet()

    fun ignore(tag: String) {
        preferences.edit().putStringSet(KEY_IGNORED, ignored + tag).apply()
    }

    fun unignore(tags: Collection<String>) {
        val next = ignored.filterNot { saved -> tags.any { AppVersionPolicy.isIgnored(saved, it) } }.toSet()
        preferences.edit().putStringSet(KEY_IGNORED, next).apply()
    }

    private companion object {
        const val KEY_IGNORED = "ignoredTags"
    }

}
