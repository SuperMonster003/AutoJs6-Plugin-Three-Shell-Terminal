package io.github.supermonster003.autojs6.plugin.three.shell.terminal.spike

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import jackpal.androidterm.PtyBridge
import org.autojs.plugin.nodejs.api.NodeJsPluginCapabilityKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Roadmap P0.2 spike: proves on the device, under the plugin's own uid, the three facts the terminal
 * depends on before any host code is migrated. Results are logged under [TAG] so they can be collected
 * with `adb logcat -s ThreeShellSpike` into `docs/dev/p0-spike-evidence.md`.
 *
 * 1. pty: open `/dev/ptmx`, fork `/system/bin/sh` through the vendored libtermexec, read its output,
 *    reap exit code 7.
 * 2. shared storage: `cd /sdcard` behaves according to the plugin's own storage grant (not the host's).
 * 3. Node.js launcher: the Node.js Runtime plugin's `NODE_CLI_*` manifest contract is readable, its
 *    signer is the official one (or ours), and `libnodexe.so --version` executes from this process.
 */
@RunWith(AndroidJUnit4::class)
class P0SpikeTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun ptyRunsTheSystemShellAndReportsItsExitCode() {
        val startedAt = System.nanoTime()
        val ptmx = ParcelFileDescriptor.open(File("/dev/ptmx"), ParcelFileDescriptor.MODE_READ_WRITE)
        val cmd = "/system/bin/sh"
        val argv = arrayOf(cmd, "-c", "echo spike-\$\$; pwd; id; exit 7")
        val envp = arrayOf("PATH=/system/bin:/system/xbin", "HOME=${context.filesDir.path}", "TERM=xterm-256color")
        val pid = PtyBridge.createSubprocess(ptmx, cmd, argv, envp)
        assertTrue("fork failed", pid > 0)
        PtyBridge.setWindowSize(ptmx.fd, 24, 80, 0, 0)
        PtyBridge.setUtf8Mode(ptmx.fd, true)

        val output = StringBuilder()
        val reader = Thread {
            runCatching {
                ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(ptmx.fileDescriptor)).use { input ->
                    val buffer = ByteArray(4096)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        synchronized(output) { output.append(String(buffer, 0, read)) }
                    }
                }
            }
        }.apply { isDaemon = true; start() }
        val exitCode = PtyBridge.waitFor(pid)
        reader.join(TimeUnit.SECONDS.toMillis(5))
        ptmx.close()
        val text = synchronized(output) { output.toString() }
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        Log.i(TAG, "pty: pid=$pid exit=$exitCode elapsed=${elapsedMs}ms uid=${android.os.Process.myUid()} api=${Build.VERSION.SDK_INT} abi=${Build.SUPPORTED_ABIS.first()}")
        Log.i(TAG, "pty output: ${text.replace("\r", "").trim().replace("\n", " | ")}")

        assertEquals("the shell must exit with the requested code", 7, exitCode)
        assertTrue("pid echo missing in: $text", text.contains("spike-$pid"))
        assertTrue("id must show the plugin uid, got: $text", text.contains("uid=${android.os.Process.myUid()}"))
        assertTrue("the child must run under an app SELinux domain: $text", text.contains("untrusted_app") || text.contains("u:r:"))
    }

    @Test
    fun sharedStorageAccessFollowsThePluginsOwnGrant() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        val target = Environment.getExternalStorageDirectory().path
        val result = runShell("cd '$target' && ls -1 | head -n 3 && echo __OK__", timeoutSeconds = 20)
        Log.i(TAG, "storage: api=${Build.VERSION.SDK_INT} granted=$granted target=$target exit=${result.exitCode} output=${result.output.lines().joinToString(" | ")}")

        val hostGranted = runCatching {
            context.packageManager.getApplicationInfo(ThreeShellTerminalPlugin.HOST_PACKAGE_NAME, 0)
            true
        }.getOrDefault(false)
        Log.i(TAG, "storage: host installed=$hostGranted (its grants do not apply to this uid)")

        // Listing `/sdcard` itself is allowed on API < 30 even without the permission (world-executable dir),
        // so the probe writes a file as the decisive check on every API level.
        val probeFile = File(target, "three-shell-spike-${System.currentTimeMillis()}.txt")
        val write = runShell("echo spike > '${probeFile.path}' && rm -f '${probeFile.path}' && echo __WRITE_OK__", timeoutSeconds = 20)
        Log.i(TAG, "storage: write exit=${write.exitCode} output=${write.output.lines().joinToString(" | ")}")
        if (granted) {
            assertTrue("granted but listing failed: ${result.output}", result.output.contains("__OK__"))
            assertTrue("granted but write failed: ${write.output}", write.output.contains("__WRITE_OK__"))
        } else {
            assertTrue("not granted but write succeeded: ${write.output}", !write.output.contains("__WRITE_OK__"))
            val denied = write.output.contains("Permission denied", ignoreCase = true) || write.output.contains("Operation not permitted", ignoreCase = true)
            assertTrue("expected a permission failure (STORAGE_PERMISSION_REQUIRED), got: ${write.output}", denied)
        }
    }

    @Test
    fun nodeLauncherOfTheRuntimePluginExecutesFromThisProcess() {
        val pm = context.packageManager
        val query = Intent(ThreeShellTerminalPlugin.NODEJS_SERVICE_ACTION).setPackage(ThreeShellTerminalPlugin.NODEJS_PACKAGE_NAME)
        @Suppress("DEPRECATION")
        val services = pm.queryIntentServices(query, PackageManager.GET_META_DATA)
        assumeTrue("Node.js Runtime plugin not installed on this device", services.isNotEmpty())
        val serviceInfo = services.single().serviceInfo
        val meta = requireNotNull(serviceInfo.metaData) { "RUNTIME service carries no meta-data" }
        val schema = meta.get(NodeJsPluginCapabilityKeys.NODE_CLI_SCHEMA)?.toString()
        val executableName = meta.getString(NodeJsPluginCapabilityKeys.NODE_CLI_EXECUTABLE)
        val commands = meta.getString(NodeJsPluginCapabilityKeys.NODE_CLI_COMMANDS)
        val archive = meta.getString(NodeJsPluginCapabilityKeys.NODE_CLI_ARCHIVE)
        val archiveSha256 = meta.getString(NodeJsPluginCapabilityKeys.NODE_CLI_ARCHIVE_SHA256)
        val packageInfo = pm.getPackageInfo(serviceInfo.packageName, 0)
        @Suppress("DEPRECATION")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
        Log.i(TAG, "node: package=${serviceInfo.packageName} version=${packageInfo.versionName} ($versionCode) schema=$schema executable=$executableName commands=$commands archive=$archive sha256=${archiveSha256?.take(16)}")
        assumeTrue("plugin predates the NODE_CLI contract (schema=$schema)", schema == "1" && executableName != null)

        val signers = signerDigests(serviceInfo.packageName)
        val ownSigners = signerDigests(context.packageName)
        val trusted = signers.any { it in OFFICIAL_SHA_256 } || signers.any { it in ownSigners }
        Log.i(TAG, "node: signers=$signers official=${signers.any { it in OFFICIAL_SHA_256 }} own=${signers.any { it in ownSigners }}")
        assertTrue("Node.js Runtime signer is neither official nor ours: $signers", trusted)

        val nativeLibraryDir = requireNotNull(serviceInfo.applicationInfo?.nativeLibraryDir)
        val executable = File(nativeLibraryDir, executableName!!)
        Log.i(TAG, "node: executable=${executable.path} exists=${executable.exists()} canExecute=${executable.canExecute()}")
        assertTrue("launcher missing at ${executable.path}", executable.exists())

        val startedAt = System.nanoTime()
        val probe = runProcess(listOf(executable.path, "--version"), timeoutSeconds = 30)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        val selinux = runShell("cat /proc/self/attr/current", timeoutSeconds = 5).output.trim()
        Log.i(TAG, "node: --version exit=${probe.exitCode} elapsed=${elapsedMs}ms output=${probe.output.trim().lines().firstOrNull()} selinux=$selinux")
        if (probe.exitCode == null || probe.exitCode == 126 || probe.output.contains("Permission denied", ignoreCase = true)) {
            Log.w(TAG, "node: ExecDenied on ${Build.MANUFACTURER} ${Build.MODEL} API ${Build.VERSION.SDK_INT}: ${probe.output.trim()}")
        }
        assertEquals("launcher must run from the plugin process: ${probe.output}", 0, probe.exitCode)
        assertTrue("version output expected, got: ${probe.output}", Regex("^v\\d+\\.\\d+\\.\\d+").containsMatchIn(probe.output.trim()))
    }

    private data class ProcessResult(val exitCode: Int?, val output: String)

    private fun runShell(script: String, timeoutSeconds: Long): ProcessResult = runProcess(listOf("/system/bin/sh", "-c", script), timeoutSeconds)

    private fun runProcess(command: List<String>, timeoutSeconds: Long): ProcessResult {
        val process = try {
            ProcessBuilder(command).redirectErrorStream(true).start()
        } catch (e: IOException) {
            return ProcessResult(null, e.message.orEmpty())
        }
        val output = StringBuilder()
        val reader = Thread { runCatching { process.inputStream.bufferedReader().use { output.append(it.readText()) } } }.apply { start() }
        // Process.waitFor(timeout) is API 26+; poll exitValue() so the spike also runs on API 24 / 25.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        var exitCode: Int? = null
        while (exitCode == null && System.nanoTime() < deadline) {
            exitCode = try {
                process.exitValue()
            } catch (_: IllegalThreadStateException) {
                Thread.sleep(50)
                null
            }
        }
        if (exitCode == null) process.destroy()
        reader.join(TimeUnit.SECONDS.toMillis(5))
        return ProcessResult(exitCode, output.toString())
    }

    private fun signerDigests(packageName: String): Set<String> {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo ?: return emptySet()
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val digest = MessageDigest.getInstance("SHA-256")
        return signatures.orEmpty().map { signature -> digest.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }

    private companion object {
        const val TAG = "ThreeShellSpike"

        /** Official AutoJs6 plugin signer (same set as the host's `PluginTrustManager.OFFICIAL_SHA_256`). */
        val OFFICIAL_SHA_256 = setOf("31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213")
    }
}
