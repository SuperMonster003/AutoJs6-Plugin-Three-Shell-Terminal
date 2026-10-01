package io.github.supermonster003.autojs6.plugin.three.shell.terminal.storage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import org.autojs.plugin.terminal.api.TerminalContract
import org.autojs.plugin.terminal.api.TerminalErrorCodes
import java.io.File

/**
 * Storage permission state and start-directory resolution of the plugin (roadmap D6 / D18).
 *
 * The shell runs under the plugin's own uid, so the host's storage grants do not apply: a directory
 * on shared storage (`/sdcard`, `/storage/...`) needs the plugin's all-files access on API 30+ or
 * the legacy runtime permissions below, except the plugin's own `Android/{data,obb,media}/<pkg>`
 * folders. [resolveDirectory] classifies by the permission state, never by parsing shell errors
 * (P0 spike evidence), and falls back to `$HOME` with a reason the UI banner and the Binder error
 * document reuse: [TerminalErrorCodes.STORAGE_PERMISSION_REQUIRED] or
 * [TerminalErrorCodes.DIRECTORY_INACCESSIBLE]. The host passes absolute paths without any
 * pre-check; relative or blank paths are therefore inaccessible, not resolved against `$HOME`.
 *
 * zh-CN: 插件的存储权限状态与起始目录解析 (路线图 D6 / D18). shell 以插件自身 uid 运行, 宿主的存储授权不适用:
 * 共享存储目录 (`/sdcard`, `/storage/...`) 在 API 30+ 需要插件的全部文件访问权限, 更低版本需要旧式运行时权限,
 * 插件自己的 `Android/{data,obb,media}/<pkg>` 目录除外. [resolveDirectory] 按权限状态而非 shell 报错判定,
 * 回退到 `$HOME` 时附带横幅与 Binder 错误文档共用的原因码. 宿主只传绝对路径且不做预检; 相对路径或空路径视为不可访问.
 */
object StorageAccess {

    /** Permission state of shared storage for this uid. zh-CN: 本 uid 对共享存储的权限状态. */
    enum class State(val contractValue: String) {
        GRANTED(TerminalContract.STORAGE_ACCESS_GRANTED),

        /** API < 30: `READ_EXTERNAL_STORAGE` (and `WRITE_EXTERNAL_STORAGE`) not granted. zh-CN: API < 30 旧式运行时权限未授予. */
        DENIED_LEGACY(TerminalContract.STORAGE_ACCESS_DENIED),

        /** API 30+: "All files access" not granted. zh-CN: API 30+ 未授予全部文件访问. */
        DENIED_ALL_FILES(TerminalContract.STORAGE_ACCESS_DENIED),

        /** Shared storage is not mounted, so no directory can live on it. zh-CN: 共享存储未挂载. */
        NOT_APPLICABLE(TerminalContract.STORAGE_ACCESS_NOT_APPLICABLE);

        val isGranted: Boolean get() = this == GRANTED
    }

    /** Where a requested path lives. zh-CN: 请求路径所在的区域. */
    enum class PathKind {
        /** Blank or relative. zh-CN: 空或相对路径. */
        INVALID,

        /** Shared storage that needs the plugin's own grant. zh-CN: 需要插件自身授权的共享存储. */
        SHARED,

        /** App-private or system paths, accessible without a storage grant. zh-CN: 无需存储授权的私有或系统路径. */
        PRIVATE,
    }

    /**
     * @param directory      directory the session starts in; `home` when a fallback applied
     * @param requested      the path as requested, for messages
     * @param fallbackReason null when [directory] is the requested path, otherwise an error code
     * zh-CN: 解析结果; 回退时 [directory] 为 home 且 [fallbackReason] 为原因码.
     */
    data class Resolved(val directory: File, val requested: String?, val fallbackReason: String?) {
        val fellBack: Boolean get() = fallbackReason != null
    }

    /** Runtime permissions of the legacy storage model (API < 30). zh-CN: 旧式存储模型的运行时权限 (API < 30). */
    val LEGACY_PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    /**
     * Roots whose subtree counts as shared storage besides the device's external storage directory.
     * The `/sdcard` alias is deliberate: callers pass paths in that spelling, and the real external
     * root is consulted as well through [Environment.getExternalStorageDirectory].
     * zh-CN: 除设备外部存储目录外, 其子树视为共享存储的根; `/sdcard` 别名是有意保留的, 真实外部根另行参与判定.
     */
    @Suppress("SdCardPath")
    val SHARED_ROOTS: List<String> = listOf("/sdcard", "/mnt/sdcard", "/storage", "/mnt/user", "/mnt/media_rw")

    private val OWN_EXTERNAL_DIRS = listOf("data", "obb", "media")

    @JvmStatic
    fun state(context: Context): State {
        val mounted = runCatching { Environment.getExternalStorageState() }.getOrNull()
        if (mounted != Environment.MEDIA_MOUNTED && mounted != Environment.MEDIA_MOUNTED_READ_ONLY) return State.NOT_APPLICABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) State.GRANTED else State.DENIED_ALL_FILES
        } else {
            val granted = LEGACY_PERMISSIONS.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
            if (granted) State.GRANTED else State.DENIED_LEGACY
        }
    }

    /**
     * Lexically normalizes an absolute path: collapses `//` and `.`, resolves `..` against the
     * path itself (clamped at `/`), drops the trailing slash. Symlinks are not resolved, so the
     * shell still `cd`s into the path the user named.
     *
     * @return null for blank or relative input
     * zh-CN: 词法规范化绝对路径 (合并 `//` 与 `.`, 就地解析 `..`, 去尾部斜杠); 不解析符号链接; 空或相对路径返回 null.
     */
    @JvmStatic
    fun normalize(path: String?): String? {
        val trimmed = path?.trim() ?: return null
        if (!trimmed.startsWith("/")) return null
        val segments = ArrayList<String>()
        trimmed.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.size - 1)
                else -> segments.add(segment)
            }
        }
        return "/" + segments.joinToString("/")
    }

    /**
     * Pure classification used by [resolveDirectory] and the JVM table test.
     *
     * @param externalRoot `Environment.getExternalStorageDirectory().path`, e.g. `/storage/emulated/0`
     * @param ownPackage   this plugin's package, whose `Android/{data,obb,media}` folders are private
     * zh-CN: [resolveDirectory] 与 JVM 判定表共用的纯函数分类.
     */
    @JvmStatic
    fun classify(path: String?, externalRoot: String, ownPackage: String): PathKind {
        val normalized = normalize(path) ?: return PathKind.INVALID
        val roots = (SHARED_ROOTS + normalize(externalRoot)).filterNotNull()
        val sharedRoot = roots.firstOrNull { isUnder(normalized, it) } ?: return PathKind.PRIVATE
        // /storage/emulated/0/Android/data/<ownPackage>/... stays accessible without a storage grant.
        val segments = normalized.removePrefix(sharedRoot).trim('/').split('/').filter { it.isNotEmpty() }
        val android = segments.indexOf("Android")
        if (android >= 0 && segments.getOrNull(android + 1) in OWN_EXTERNAL_DIRS && segments.getOrNull(android + 2) == ownPackage) {
            return PathKind.PRIVATE
        }
        return PathKind.SHARED
    }

    @JvmStatic
    fun isSharedStorage(context: Context, path: String?): Boolean =
        classify(path, Environment.getExternalStorageDirectory().path, context.packageName) == PathKind.SHARED

    /**
     * Resolves the start directory of a session (D18): a readable, searchable directory is used as
     * named; a shared-storage directory without the plugin's grant falls back to [home] with
     * [TerminalErrorCodes.STORAGE_PERMISSION_REQUIRED]; a missing, non-directory, unreadable,
     * relative or blank path falls back to [home] with [TerminalErrorCodes.DIRECTORY_INACCESSIBLE].
     * A null or empty [path] means "no preference" and yields [home] without a reason.
     *
     * zh-CN: 解析会话起始目录 (D18): 可读可搜索的目录按原样使用; 共享存储目录且未授权回退 [home] 并给出
     * `STORAGE_PERMISSION_REQUIRED`; 不存在 / 非目录 / 不可读 / 相对 / 空白路径回退 [home] 并给出
     * `DIRECTORY_INACCESSIBLE`; [path] 为 null 或空串表示无偏好, 返回 [home] 且无原因码.
     */
    @JvmStatic
    fun resolveDirectory(context: Context, path: String?, home: File): Resolved {
        if (path.isNullOrEmpty()) return Resolved(home, path, null)
        val normalized = normalize(path) ?: return Resolved(home, path, TerminalErrorCodes.DIRECTORY_INACCESSIBLE)
        if (classify(normalized, Environment.getExternalStorageDirectory().path, context.packageName) == PathKind.SHARED && !state(context).isGranted) {
            return Resolved(home, path, TerminalErrorCodes.STORAGE_PERMISSION_REQUIRED)
        }
        val directory = File(normalized)
        if (!directory.isDirectory || !directory.canRead() || !directory.canExecute()) {
            return Resolved(home, path, TerminalErrorCodes.DIRECTORY_INACCESSIBLE)
        }
        return Resolved(directory, path, null)
    }

    /**
     * Intents that open the "All files access" switch on API 30+, most specific first: the
     * per-app page with a `package:` URI, then the generic list some OEMs only resolve. Empty below
     * API 30, where [LEGACY_PERMISSIONS] are requested at run time instead.
     * zh-CN: API 30+ 打开 "全部文件访问" 开关的 Intent, 先带包名 URI 的单应用页, 再退回部分 OEM 才能解析的通用列表; API 30 以下为空, 改为运行时请求 [LEGACY_PERMISSIONS].
     */
    @JvmStatic
    fun allFilesAccessIntents(packageName: String): List<Intent> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        return listOf(
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")),
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        )
    }

    /**
     * The [allFilesAccessIntents] this device can actually resolve, in preference order.
     * zh-CN: 本设备实际能解析的 [allFilesAccessIntents], 按优先级排列.
     */
    @JvmStatic
    fun resolvableAllFilesAccessIntents(context: Context): List<Intent> =
        allFilesAccessIntents(context.packageName).filter { intent ->
            runCatching { intent.resolveActivity(context.packageManager) != null }.getOrDefault(false)
        }

    /** Runtime permissions an Activity should request on this API level, empty on API 30+. zh-CN: 本 API 级别应由 Activity 请求的运行时权限, API 30+ 为空. */
    @JvmStatic
    fun legacyPermissionsToRequest(): Array<String> =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) LEGACY_PERMISSIONS.copyOf() else emptyArray()

    private fun isUnder(path: String, root: String): Boolean {
        val normalizedRoot = root.trimEnd('/').ifEmpty { "/" }
        return path == normalizedRoot || (normalizedRoot != "/" && path.startsWith("$normalizedRoot/"))
    }

}
