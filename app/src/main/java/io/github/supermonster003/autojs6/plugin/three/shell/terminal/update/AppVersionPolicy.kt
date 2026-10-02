package io.github.supermonster003.autojs6.plugin.three.shell.terminal.update

/**
 * Semantic version comparison for release tags (`v1.2.3`, `1.2.3-beta.1`): numeric parts compare
 * without integer overflow, pre-release identifiers follow SemVer 2.0 and build metadata never
 * changes the order. Anything that does not parse is simply "not newer" and "not ignored".
 * zh-CN: 发行 tag 的语义化版本比较: 数字段比较不溢出, 预发布标识按 SemVer 2.0, 构建元数据不影响顺序;
 * 无法解析的值一律视为 "不更新" 且 "未忽略".
 */
internal object AppVersionPolicy {

    private val PATTERN = Regex(
        "[vV]?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:\\.(0|[1-9][0-9]*))?" +
            "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?",
    )

    internal data class Version(val parts: List<String>, val pre: List<String>) : Comparable<Version> {

        override fun compareTo(other: Version): Int {
            parts.zip(other.parts).forEach { (a, b) -> numeric(a, b).takeIf { it != 0 }?.let { return it } }
            if (pre.isEmpty() || other.pre.isEmpty()) return pre.isEmpty().compareTo(other.pre.isEmpty())
            pre.zip(other.pre).forEach { (a, b) ->
                val aNumeric = a.all(Char::isDigit)
                val bNumeric = b.all(Char::isDigit)
                val order = when {
                    aNumeric && bNumeric -> numeric(a, b)
                    aNumeric != bNumeric -> if (aNumeric) -1 else 1
                    else -> a.compareTo(b)
                }
                if (order != 0) return order
            }
            return pre.size.compareTo(other.pre.size)
        }

    }

    /** Digit strings without leading zeros: the longer one is larger, equal lengths compare lexically. */
    private fun numeric(a: String, b: String): Int = a.length.compareTo(b.length).takeIf { it != 0 } ?: a.compareTo(b)

    fun parse(text: String?): Version? {
        if (text == null || text.length > 128) return null
        val match = PATTERN.matchEntire(text.trim()) ?: return null
        val pre = match.groupValues[4].takeIf { it.isNotEmpty() }?.split('.').orEmpty()
        if (pre.any { it.length > 1 && it.all(Char::isDigit) && it.startsWith('0') }) return null
        return Version(listOf(match.groupValues[1], match.groupValues[2], match.groupValues[3].ifEmpty { "0" }), pre)
    }

    fun isNewer(remote: String?, installed: String?): Boolean =
        parse(remote)?.let { a -> parse(installed)?.let { a > it } } == true

    fun isIgnored(remote: String?, ignored: String?): Boolean =
        parse(remote)?.let { a -> parse(ignored)?.let { a.compareTo(it) == 0 } } == true

}
