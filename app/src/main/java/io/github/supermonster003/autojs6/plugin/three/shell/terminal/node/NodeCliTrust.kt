package io.github.supermonster003.autojs6.plugin.three.shell.terminal.node

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
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

        /** Neither; [signers] lists the digests seen, empty when the package has none. zh-CN: 均不是; [signers] 为所见摘要. */
        data class Untrusted(val signers: List<String>) : Verdict()

        val isTrusted: Boolean get() = this !is Untrusted
    }

    /**
     * Pure rule shared with the JVM table test. Digests are compared case-insensitively.
     * zh-CN: 与 JVM 判定表共用的纯规则; 摘要比较不区分大小写.
     */
    @JvmStatic
    fun evaluate(signers: Signers, ownSigners: Set<String>): Verdict {
        val current = signers.current.map(::normalizeDigest).filter { it.isNotEmpty() }
        if (current.isEmpty()) return Verdict.Untrusted(emptyList())
        val own = ownSigners.map(::normalizeDigest).toSet()
        val official = OFFICIAL_SHA_256
        if (current.size > 1) {
            return when {
                current.all { it in official } -> Verdict.Official
                current.all { it in official || it in own } -> Verdict.Self
                else -> Verdict.Untrusted(current)
            }
        }
        val candidates = (current + signers.lineage.map(::normalizeDigest)).filter { it.isNotEmpty() }.distinct()
        return when {
            candidates.any { it in official } -> Verdict.Official
            candidates.any { it in own } -> Verdict.Self
            else -> Verdict.Untrusted(candidates)
        }
    }

    /**
     * Trust verdict for [packageName] against this plugin's own signers.
     * zh-CN: 以本插件自身签名为参照, 对 [packageName] 的信任判定.
     */
    @JvmStatic
    fun verdict(context: Context, packageName: String): Verdict {
        val signers = signersOf(context, packageName) ?: return Verdict.Untrusted(emptyList())
        val own = signersOf(context, context.packageName)?.let { (it.current + it.lineage).toSet() }.orEmpty()
        return evaluate(signers, own)
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
