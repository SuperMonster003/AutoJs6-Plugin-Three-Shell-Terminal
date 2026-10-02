package io.github.supermonster003.autojs6.plugin.three.shell.terminal.update

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** The one public stable release the GitHub Releases API reports as latest. */
internal data class ReleaseInfo(val tag: String, val url: String, val notes: String)

/**
 * Strict decoding of the `releases/latest` document (AGENTS.md 12): drafts and pre-releases are
 * rejected, the tag must be a stable semantic version and the page URL must point at this very
 * repository's release page, so a hijacked answer cannot send the user elsewhere. The same codec
 * round-trips the cached copy.
 * zh-CN: `releases/latest` 文档的严格解码 (AGENTS.md 12): 拒绝草稿与预发布, tag 必须是稳定的语义化版本,
 * 页面 URL 必须指向本仓库的发行页, 被劫持的响应无法把用户带到别处; 缓存副本用同一编解码器往返.
 */
internal object ReleaseInfoCodec {

    const val REPOSITORY = "SuperMonster003/AutoJs6-Plugin-Three-Shell-Terminal"
    const val SOURCE = "https://github.com/$REPOSITORY"
    const val MAX_DOCUMENT_CHARS = 256 * 1024
    const val MAX_NOTES_CHARS = 16 * 1024

    fun validUrl(url: String, tag: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.host == "github.com" && uri.port == -1 && uri.userInfo == null &&
            uri.query == null && uri.fragment == null && uri.path == "/$REPOSITORY/releases/tag/$tag"
    }.getOrDefault(false)

    fun decode(text: String): ReleaseInfo {
        require(text.length <= MAX_DOCUMENT_CHARS) { "release document too large" }
        val element = JsonParser.parseString(text)
        require(element.isJsonObject) { "release document is not an object" }
        val value = element.asJsonObject
        require(value.boolean("draft") == false && value.boolean("prerelease") == false) { "not a public stable release" }
        val tag = requireNotNull(value.string("tag_name")) { "missing tag_name" }
        require(AppVersionPolicy.parse(tag)?.pre?.isEmpty() == true) { "tag is not a stable version" }
        val url = requireNotNull(value.string("html_url")) { "missing html_url" }
        require(validUrl(url, tag)) { "html_url is not this repository's release page" }
        val body = value.get("body")
        val notes = if (body == null || body.isJsonNull) "" else requireNotNull(value.string("body")) { "body is not a string" }
        return ReleaseInfo(tag, url, truncate(notes, MAX_NOTES_CHARS))
    }

    fun encode(release: ReleaseInfo): String = JsonObject().apply {
        addProperty("draft", false)
        addProperty("prerelease", false)
        addProperty("tag_name", release.tag)
        addProperty("html_url", release.url)
        addProperty("body", release.notes)
    }.toString().also { decode(it) }

    private fun truncate(text: String, max: Int): String {
        if (text.length <= max) return text
        val end = if (Character.isHighSurrogate(text[max - 1])) max - 1 else max
        return text.substring(0, end)
    }

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun JsonObject.boolean(key: String): Boolean? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean

}

internal enum class UpdateFailure { NETWORK, HTTP, TOO_LARGE, MALFORMED }

internal sealed class UpdateResult {
    /** [release] is null when the repository has no public stable release (HTTP 404). */
    data class Success(val release: ReleaseInfo?) : UpdateResult()
    data class Failure(val reason: UpdateFailure) : UpdateResult()
}

/** Cancellation token shared by the UI and the worker; disconnects the live connection. */
internal class UpdateCancellation {

    private val stopped = AtomicBoolean()
    private val connection = AtomicReference<HttpURLConnection?>()

    val cancelled: Boolean get() = stopped.get()

    fun attach(value: HttpURLConnection) {
        connection.set(value)
        if (cancelled) value.disconnect()
    }

    fun cancel() {
        stopped.set(true)
        connection.getAndSet(null)?.disconnect()
    }

}

internal fun interface UpdateSource {
    fun fetchLatest(cancellation: UpdateCancellation): UpdateResult
}

/**
 * HTTPS only, bounded response, cancellable IO, no redirects and no APK transfer: the plugin only
 * learns the tag, the release page and the notes, and never sends anything but the request itself.
 * zh-CN: 仅 HTTPS, 响应有上限, 可取消, 不跟随重定向, 不传输 APK: 插件只获取 tag, 发行页与说明, 除请求本身不发送任何内容.
 */
internal class AppUpdateRepository(
    private val connect: () -> HttpURLConnection = { URL(ENDPOINT).openConnection() as HttpURLConnection },
) : UpdateSource {

    override fun fetchLatest(cancellation: UpdateCancellation): UpdateResult {
        if (cancellation.cancelled) return UpdateResult.Failure(UpdateFailure.NETWORK)
        val connection = try {
            connect()
        } catch (_: Exception) {
            return UpdateResult.Failure(UpdateFailure.NETWORK)
        }
        cancellation.attach(connection)
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.setRequestProperty("User-Agent", "AutoJs6-Plugin-Three-Shell-Terminal")
        return try {
            when (connection.responseCode) {
                404 -> UpdateResult.Success(null)
                200 -> connection.inputStream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (!cancellation.cancelled) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (out.size() + count > MAX_RESPONSE_BYTES) return UpdateResult.Failure(UpdateFailure.TOO_LARGE)
                        out.write(buffer, 0, count)
                    }
                    if (cancellation.cancelled) return UpdateResult.Failure(UpdateFailure.NETWORK)
                    runCatching {
                        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(out.toByteArray())).toString()
                        UpdateResult.Success(ReleaseInfoCodec.decode(text))
                    }.getOrElse { UpdateResult.Failure(UpdateFailure.MALFORMED) }
                }
                else -> UpdateResult.Failure(UpdateFailure.HTTP)
            }
        } catch (_: Exception) {
            UpdateResult.Failure(UpdateFailure.NETWORK)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val ENDPOINT = "https://api.github.com/repos/${ReleaseInfoCodec.REPOSITORY}/releases/latest"
        const val TIMEOUT_MS = 10_000
        const val MAX_RESPONSE_BYTES = 256 * 1024
    }

}
