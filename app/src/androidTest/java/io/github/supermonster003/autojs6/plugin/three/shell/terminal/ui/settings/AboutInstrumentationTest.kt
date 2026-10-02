package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.update.AppUpdateCoordinator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.update.AppUpdateSettings
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.update.ReleaseInfo
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.update.ReleaseInfoCodec
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.update.UpdateFailure
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.update.UpdateResult
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.update.UpdateSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Roadmap P5.2 device evidence: the About screen shows the installed version, every bundled
 * document opens offline with its expected content, and the manual update check drives the result
 * dialog (open release / history / ignore) from an injected source without touching the network.
 * Results are logged under [TAG].
 */
@RunWith(AndroidJUnit4::class)
class AboutInstrumentationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    private val versionName = packageInfo.versionName.orEmpty()

    @Before
    fun resetUpdateState() {
        context.getSharedPreferences(AppUpdateCoordinator.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit()
        AppUpdateCoordinator.sourceOverride = null
    }

    @After
    fun restore() {
        AppUpdateCoordinator.sourceOverride = null
        context.getSharedPreferences(AppUpdateCoordinator.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit()
        finishAll()
    }

    @Test
    fun aboutShowsTheInstalledVersionAndOpensTheBundledHistory() {
        val about = launch(Intent(context, AboutActivity::class.java)) as AboutActivity
        val version = onMain { about.scaffold.content.findViewWithTag<ViewGroup>(AboutActivity.TAG_VERSION).children.filterIsInstance<TextView>().last().text.toString() }
        Log.i(TAG, "about version block: ${version.replace('\n', ' ')}")
        assertTrue(version.contains(versionName))
        assertTrue(version.contains(PackageInfoCompat.getLongVersionCode(packageInfo).toString()))
        assertEquals(context.getString(R.string.plugin_author), onMain { about.scaffold.content.findViewWithTag<ViewGroup>(AboutActivity.TAG_AUTHOR).children.filterIsInstance<TextView>().last().text.toString() })
        onMain { about.scaffold.content.findViewWithTag<View>(AboutActivity.TAG_HISTORY).performClick() }
        val history = awaitValue { onMain { resumed<ReleaseHistoryActivity>() } }
        val text = awaitDocument(history)
        Log.i(TAG, "history document: ${text.length} chars, locale ${context.resources.configuration.locales[0].toLanguageTag()}")
        assertTrue("history names the installed version", text.contains("v$versionName"))
        assertTrue(text.contains(context.getString(R.string.plugin_description).take(8)) || text.contains("v$versionName"))
    }

    @Test
    fun everyBundledLegalDocumentOpensOffline() {
        val markers = mapOf(
            BundledDocument.LICENSE to "Mozilla Public License",
            BundledDocument.NOTICES to "Third-party notices",
            BundledDocument.JACKPAL_LICENSE to "Apache License",
            BundledDocument.JACKPAL_NOTICE to "Android Open Source Project",
        )
        markers.forEach { (document, marker) ->
            val screen = launch(ReleaseHistoryActivity.intent(context, document)) as ReleaseHistoryActivity
            assertEquals(context.getString(document.title), onMain { screen.scaffold.toolbar.title.toString() })
            val text = awaitDocument(screen)
            Log.i(TAG, "${document.key}: ${text.length} chars")
            assertTrue("${document.key} contains '$marker'", text.contains(marker))
            onMain { screen.finish() }
            await { onMain { resumed<ReleaseHistoryActivity>() == null } }
        }
    }

    @Test
    fun theUpdateCheckPresentsAnInjectedReleaseAndRemembersTheIgnoredVersion() {
        val tag = "v99.0.0"
        val release = ReleaseInfo(tag, "https://github.com/${ReleaseInfoCodec.REPOSITORY}/releases/tag/$tag", "# $tag\n\n* `Feature` injected")
        AppUpdateCoordinator.sourceOverride = UpdateSource { UpdateResult.Success(release) }
        val about = launch(Intent(context, AboutActivity::class.java)) as AboutActivity
        onMain { about.scaffold.content.findViewWithTag<View>(AboutActivity.TAG_UPDATE).performClick() }
        var dialog = awaitValue { onMain { about.updates.dialog?.takeIf { it.isShowing && it.getButton(AlertDialog.BUTTON_POSITIVE)?.visibility == View.VISIBLE } } }
        onMain {
            assertEquals(context.getString(R.string.update_open_release), dialog.getButton(AlertDialog.BUTTON_POSITIVE).text.toString())
            assertEquals(context.getString(R.string.release_history_title), dialog.getButton(AlertDialog.BUTTON_NEUTRAL).text.toString())
            assertEquals(context.getString(R.string.update_ignore), dialog.getButton(AlertDialog.BUTTON_NEGATIVE).text.toString())
            assertTrue(about.hasUnconfirmedDialogForTest())
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        }
        await { onMain { !about.updates.isShowingDialog } }
        assertEquals(setOf(tag), AppUpdateSettings(context).ignored)
        // Within the reuse window the cached answer is presented without another fetch; the action now un-ignores.
        AppUpdateCoordinator.sourceOverride = UpdateSource { throw AssertionError("must not fetch again within 12 hours") }
        onMain { about.scaffold.content.findViewWithTag<View>(AboutActivity.TAG_UPDATE).performClick() }
        dialog = awaitValue { onMain { about.updates.dialog?.takeIf { it.isShowing && it.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility == View.VISIBLE } } }
        onMain {
            assertEquals(context.getString(R.string.update_unignore), dialog.getButton(AlertDialog.BUTTON_NEGATIVE).text.toString())
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        }
        await { onMain { !about.updates.isShowingDialog } }
        assertTrue(AppUpdateSettings(context).ignored.isEmpty())
        // A failed fetch only toasts: no dialog stays behind.
        context.getSharedPreferences(AppUpdateCoordinator.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit()
        AppUpdateCoordinator.sourceOverride = UpdateSource { UpdateResult.Failure(UpdateFailure.HTTP) }
        onMain { about.scaffold.content.findViewWithTag<View>(AboutActivity.TAG_UPDATE).performClick() }
        await { onMain { about.updates.dialog == null && !about.updates.isShowingDialog } }
        assertNull(onMain { about.updates.dialog })
        assertNotEquals("", versionName)
        assertFalse(onMain { about.isFinishing })
        Log.i(TAG, "update check: injected $tag presented, ignored and un-ignored, failure left no dialog")
    }

    private fun launch(intent: Intent) = instrumentation.startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun awaitDocument(screen: ReleaseHistoryActivity): String {
        val loading = context.getString(R.string.document_loading)
        return awaitValue {
            onMain {
                val view = screen.scaffold.content.findViewWithTag<TextView>(ReleaseHistoryActivity.TAG_DOCUMENT)
                check(view != null) { "the document view reported an error" }
                view.text.toString().takeIf { it != loading }
            }
        }
    }

    private inline fun <reified T : android.app.Activity> resumed(): T? = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<T>().singleOrNull()

    private fun finishAll() {
        onMain {
            for (stage in listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED, Stage.STARTED)) {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage).forEach { if (!it.isFinishing) it.finish() }
            }
        }
        SystemClock.sleep(300)
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "About state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T : Any> awaitValue(supplier: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (true) {
            supplier()?.let { return it }
            check(SystemClock.uptimeMillis() < deadline) { "About state did not settle" }
            SystemClock.sleep(50)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { value.set(runCatching(block)) }
        return value.get().getOrThrow()
    }

    private companion object {
        const val TAG = "ThreeShellTerminalAbout"
    }

}
