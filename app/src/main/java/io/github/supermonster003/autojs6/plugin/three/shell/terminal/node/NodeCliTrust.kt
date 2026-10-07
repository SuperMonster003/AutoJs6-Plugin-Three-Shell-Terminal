package io.github.supermonster003.autojs6.plugin.three.shell.terminal.node

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPreferences
import java.security.MessageDigest

/**
 * Signer trust rule for the Node.js Runtime plugin (roadmap D17): the terminal executes that
 * plugin's launcher inside its own process, so the launcher must come from the official AutoJs6
 * plugin key (the same set as the host's `PluginTrustManager.OFFICIAL_SHA_256`) or from the key
 * this terminal plugin itself is signed with (development builds). Anything else is
 * [Verdict.Untrusted] and the terminal stays a plain shell.
 *
 * Multiple current signers (APK signed by several keys) must all be trusted; a single signer is
 * accepted when it or any key of its rotation lineage is trusted, which is the platform's own
 * notion of "same developer".
 *
 * Since roadmap P8 (D34 Q3) the user may also confirm one exact set of current signers for one
 * package ([UserTrust], [Verdict.UserTrusted]); the confirmation lives in the plugin preferences,
 * covers neither the lineage nor another package, and can be revoked on the settings page.
 *
 * zh-CN: Node.js 运行时插件的签名信任规则 (路线图 D17): 终端在自身进程内执行该插件的启动器, 因此其签名必须属于
 * 官方 AutoJs6 插件密钥集合 (与宿主 `PluginTrustManager.OFFICIAL_SHA_256` 同值) 或与本插件自身签名一致 (开发构建),
 * 否则为 [Verdict.Untrusted], 终端保持纯 shell. 多个当前签名者须全部可信; 单签名者本身或其轮换谱系中任一密钥可信即可.
 */
object NodeCliTrust {

    /** Official AutoJs6 plugin signer SHA-256 digests (lowercase hex). zh-CN: 官方 AutoJs6 插件签名 SHA-256 (小写十六进制). */
    val OFFICIAL_SHA_256: Set<String> = setOf("31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213")

    /**
     * @param current current signers of the APK (several only when the APK is multi-signed)
     * @param lineage rotation lineage ending with the current signer; equals [current] when unknown
     */
    data class Signers(val current: List<String>, val lineage: List<String> = current) {
        val isEmpty: Boolean get() = current.isEmpty()
    }

    sealed class Verdict {
        /** Signed by the official AutoJs6 plugin key. zh-CN: 官方插件密钥签名. */
        object Official : Verdict()

        /** Signed by the same key as this terminal plugin (development builds). zh-CN: 与本插件同一密钥 (开发构建). */
        object Self : Verdict()

        /** The user confirmed exactly these current signers for this package (roadmap P8). zh-CN: 用户为该包确认了这组当前签名者 (P8). */
        data class UserTrusted(val current: List<String>) : Verdict()

        /**
         * Neither; [signers] lists the digests seen (current first, then the lineage), empty when the package has
         * none, and [current] the current signers a user confirmation would cover.
         * zh-CN: 均不是; [signers] 为所见摘要 (当前签名者在前, 其后为谱系), [current] 为用户确认所针对的当前签名者.
         */
        data class Untrusted(val signers: List<String>, val current: List<String> = signers) : Verdict()

        val isTrusted: Boolean get() = this !is Untrusted
    }

    /**
     * Pure rule shared with the JVM table test. Digests are compared case-insensitively.
     * [userTrusted] holds the [fingerprint]s the user confirmed for this package; it never widens the
     * official or own-key rules and only matches the exact current signer set.
     * zh-CN: 与 JVM 判定表共用的纯规则; 摘要比较不区分大小写. [userTrusted] 为用户为该包确认的 [fingerprint],
     * 只匹配完全相同的当前签名者集合.
     */
    @JvmStatic
    @JvmOverloads
    fun evaluate(signers: Signers, ownSigners: Set<String>, userTrusted: Set<String> = emptySet()): Verdict {
        val current = signers.current.map(::normalizeDigest).filter { it.isNotEmpty() }
        if (current.isEmpty()) return Verdict.Untrusted(emptyList())
        val own = ownSigners.map(::normalizeDigest).toSet()
        val official = OFFICIAL_SHA_256
        val confirmed = fingerprint(current) in userTrusted
        if (current.size > 1) {
            return when {
                current.all { it in official } -> Verdict.Official
                current.all { it in official || it in own } -> Verdict.Self
                confirmed -> Verdict.UserTrusted(current)
                else -> Verdict.Untrusted(current)
            }
        }
        val candidates = (current + signers.lineage.map(::normalizeDigest)).filter { it.isNotEmpty() }.distinct()
        return when {
            candidates.any { it in official } -> Verdict.Official
            candidates.any { it in own } -> Verdict.Self
            confirmed -> Verdict.UserTrusted(current)
            else -> Verdict.Untrusted(candidates, current)
        }
    }

    /**
     * Trust verdict for [packageName] against this plugin's own signers and the user's confirmations.
     * zh-CN: 以本插件自身签名与用户确认为参照, 对 [packageName] 的信任判定.
     */
    @JvmStatic
    fun verdict(context: Context, packageName: String): Verdict {
        val signers = signersOf(context, packageName) ?: return Verdict.Untrusted(emptyList())
        val own = signersOf(context, context.packageName)?.let { (it.current + it.lineage).toSet() }.orEmpty()
        val confirmed = TerminalPreferences(context).trustedNodeSigners.filter { it.packageName == packageName }.map { it.fingerprint }.toSet()
        return evaluate(signers, own, confirmed)
    }

    /** Order-independent key of a current signer set. zh-CN: 当前签名者集合的顺序无关键. */
    @JvmStatic
    fun fingerprint(current: List<String>): String =
        current.map(::normalizeDigest).filter { it.isNotEmpty() }.distinct().sorted().joinToString(",")

    /**
     * One user confirmation: [packageName] signed by exactly [digests] (normalized, sorted). Stored as
     * `package|sha256[,sha256]`; malformed values are dropped when read.
     * zh-CN: 一条用户确认: [packageName] 恰由 [digests] 签名. 存储为 `package|sha256[,sha256]`, 读取时丢弃格式错误的值.
     */
    class UserTrust private constructor(val packageName: String, val digests: List<String>) {

        val fingerprint: String get() = digests.joinToString(",")

        fun encode(): String = "$packageName|$fingerprint"

        override fun equals(other: Any?): Boolean = other is UserTrust && other.packageName == packageName && other.digests == digests

        override fun hashCode(): Int = 31 * packageName.hashCode() + digests.hashCode()

        override fun toString(): String = "UserTrust(${encode()})"

        companion object {

            const val MAX_ENTRIES = 8
            const val MAX_DIGESTS = 4
            private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
            private val DIGEST = Regex("[0-9a-f]{64}")

            /** A valid confirmation, or null for a malformed package name or digest list. zh-CN: 有效的确认, 包名或摘要非法时为 null. */
            @JvmStatic
            fun of(packageName: String, digests: List<String>): UserTrust? {
                val normalized = digests.map(::normalizeDigest).filter { it.isNotEmpty() }.distinct().sorted()
                if (packageName.length > 255 || !PACKAGE.matches(packageName)) return null
                if (normalized.isEmpty() || normalized.size > MAX_DIGESTS || !normalized.all(DIGEST::matches)) return null
                return UserTrust(packageName, normalized)
            }

            @JvmStatic
            fun decode(value: String): UserTrust? {
                val separator = value.indexOf('|')
                if (separator <= 0) return null
                return of(value.substring(0, separator), value.substring(separator + 1).split(','))
            }

        }

    }

    /**
     * Current signers and rotation lineage of an installed package, or null when it is not installed.
     * zh-CN: 已安装包的当前签名者与轮换谱系; 未安装时为 null.
     */
    @JvmStatic
    fun signersOf(context: Context, packageName: String): Signers? {
        val pm = context.packageManager
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = packageInfo(pm, packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo ?: return Signers(emptyList())
                if (info.hasMultipleSigners()) {
                    val current = info.apkContentsSigners.orEmpty().map(::sha256)
                    Signers(current, current)
                } else {
                    val lineage = info.signingCertificateHistory.orEmpty().map(::sha256)
                    Signers(lineage.takeLast(1), lineage)
                }
            } else {
                @Suppress("DEPRECATION")
                val current = packageInfo(pm, packageName, PackageManager.GET_SIGNATURES).signatures.orEmpty().map(::sha256)
                Signers(current, current)
            }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun packageInfo(pm: PackageManager, packageName: String, flags: Int) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            pm.getPackageInfo(packageName, flags)
        }

    @JvmStatic
    fun sha256(signature: Signature): String = sha256(signature.toByteArray())

    @JvmStatic
    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun normalizeDigest(digest: String): String = digest.trim().lowercase().replace(":", "")

}
