package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.R
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.ShellQuoting
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import org.autojs.plugin.terminal.api.TerminalContract
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class TerminalCommandInstrumentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun editorSurvivesActivityRecreationAndActionsStayVisibleWithTheKeyboard() {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val before = TerminalSessionManager.activeSessions.map { it.id }.toSet()
        val draft = "# draft\n" + "printf '%s\\n' '中文'\n".repeat(20)
        val scenario = ActivityScenario.launch<TerminalActivity>(TerminalActivity.intent(context)
            .putExtra(TerminalContract.EXTRA_NEW_SESSION, true))
        try {
            await { TerminalSessionManager.activeSessions.any { it.id !in before } }
            scenario.onActivity { it.editCommand(draft) }
            scenario.recreate()
            await {
                var ready = false
                scenario.onActivity { ready = it.commandDialogForTest?.isShowing == true }
                ready
            }
            scenario.onActivity { activity ->
                val edit = activity.commandDialogForTest!!.findViewById<EditText>(R.id.terminal_command_editor)!!
                assertEquals(draft, edit.text.toString())
                edit.requestFocus()
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(edit, 0)
            }
            // A cold emulator can take longer than a fixed pause to bring up its first keyboard.
            await {
                var shown = false
                scenario.onActivity { shown = (ViewCompat.getRootWindowInsets(it.window.decorView)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0) > 0 }
                shown
            }
            SystemClock.sleep(600)
            scenario.onActivity { activity ->
                val host = activity.window.decorView
                val insets = ViewCompat.getRootWindowInsets(host)!!
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                assertTrue("keyboard must be visible", ime > 0)
                val keyboardTop = maxOf(host.height, activity.resources.displayMetrics.heightPixels) - ime
                val dialog = activity.commandDialogForTest!!
                val card = IntArray(2).also { dialog.window!!.decorView.getLocationOnScreen(it) }[1]
                val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                assertTrue("dialog starts under the status bar: top=$card, status bar=$statusBar", card >= statusBar)
                for (which in listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL)) {
                    val button = dialog.getButton(which)
                    val bounds = Rect()
                    assertTrue(button.getGlobalVisibleRect(bounds))
                    assertEquals(button.height, bounds.height())
                    assertTrue("button overlaps keyboard: $bounds, keyboard top=$keyboardTop", bounds.bottom <= keyboardTop)
                }
                // The editor itself must keep room for a few lines above the keyboard, not just the buttons.
                val edit = dialog.findViewById<EditText>(R.id.terminal_command_editor)!!
                val editor = Rect()
                assertTrue("editor is hidden", edit.getGlobalVisibleRect(editor))
                assertTrue("editor shows less than 3 lines: $editor, line height=${edit.lineHeight}", editor.height() >= edit.lineHeight * 3)
                assertTrue("editor overlaps keyboard: $editor, keyboard top=$keyboardTop", editor.bottom <= keyboardTop)
            }
            screenshot("command-editor-keyboard")
        } finally {
            scenario.close()
            onMain { TerminalSessionManager.activeSessions.filter { it.id !in before }.forEach { TerminalSessionManager.close(it.id) } }
        }
    }

    @Test
    fun menuAndImePasteAreReviewedBeforeExecutionAndTheCommandCanBeReadBack() {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val paths = TerminalPaths.of(context).ensureLayout()
        val target = File(paths.tmp, "command-editor-test.txt").also { it.delete() }
        val body = "#!/system/bin/sh\n# 中文 comments, emoji \uD83D\uDE42, and a long path\n" +
            "# /data/example/${"long-directory/".repeat(7)}\nprintf '%s\\n' \"complete text\"\n"
        val command = "cat << 'EOF' > ${ShellQuoting.quote(target.path)}\n${body}EOF\nprintf '%s\\n' 'command finished'"
        val before = TerminalSessionManager.activeSessions.map { it.id }.toSet()
        val activity = instrumentation.startActivitySync(TerminalActivity.intent(context, paths.home.path)
            .putExtra(TerminalContract.EXTRA_NEW_SESSION, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as TerminalActivity
        try {
            await { onMain { activity.findViewById<TerminalEmulatorView>(R.id.terminal)?.termSession != null } }
            val original = TerminalSessionManager.activeSessions.single { it.id !in before }
            onMain {
                val connection = activity.findViewById<TerminalEmulatorView>(R.id.terminal).onCreateInputConnection(EditorInfo())!!
                connection.commitText("\n", 1)
                assertNull(activity.commandDialogForTest)
            }
            // Menu paste and cancellation must not submit even the first line.
            onMain {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("test", command))
                selectMenu(activity, R.id.action_paste)
                val dialog = requireNotNull(activity.commandDialogForTest)
                assertEquals(command, dialog.findViewById<EditText>(R.id.terminal_command_editor)?.text.toString())
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
            }
            assertFalse(target.exists())
            assertEquals(before.size + 1, TerminalSessionManager.activeSessions.size)

            // Keyboard clipboard commits use the same complete editor instead of character-at-a-time injection.
            onMain {
                val view = activity.findViewById<TerminalEmulatorView>(R.id.terminal)
                val connection = requireNotNull(view.onCreateInputConnection(EditorInfo()))
                assertTrue(if (Build.VERSION.SDK_INT >= 33) connection.commitText(command, 1, null) else connection.commitText(command, 1))
                assertEquals(command, activity.commandDialogForTest?.findViewById<EditText>(R.id.terminal_command_editor)?.text.toString())
            }
            screenshot("command-editor")
            onMain { activity.commandDialogForTest!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick() }
            await { TerminalSessionManager.activeSessions.any { it.submittedCommand == command } }
            val executed = TerminalSessionManager.activeSessions.single { it.submittedCommand == command }
            await { onMain { executed.pty.transcriptText.contains("command finished") && executed.pty.transcriptText.contains(": 0") } }
            assertArrayEquals(body.toByteArray(Charsets.UTF_8), target.readBytes())
            assertTrue(original.isAlive)
            screenshot("command-output")
            onMain {
                selectMenu(activity, R.id.action_view_command)
                assertEquals(command, activity.commandDialogForTest?.findViewById<TextView>(R.id.terminal_command_record)?.text.toString())
            }
            screenshot("command-record")
        } finally {
            onMain {
                activity.finish()
                TerminalSessionManager.activeSessions.filter { it.id !in before }.forEach { TerminalSessionManager.close(it.id) }
            }
            target.delete()
        }
    }

    private fun selectMenu(activity: TerminalActivity, id: Int) {
        val menu = PopupMenu(activity, activity.findViewById(R.id.terminal)).menu
        activity.menuInflater.inflate(R.menu.menu_terminal, menu)
        activity.onOptionsItemSelected(menu.findItem(id))
    }

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(200)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun await(condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 20000
        while (!condition()) {
            assertTrue("timed out", SystemClock.uptimeMillis() < end)
            SystemClock.sleep(30)
        }
    }

    private fun <T> onMain(action: () -> T): T {
        val value = AtomicReference<T>()
        instrumentation.runOnMainSync { value.set(action()) }
        return value.get()
    }
}
