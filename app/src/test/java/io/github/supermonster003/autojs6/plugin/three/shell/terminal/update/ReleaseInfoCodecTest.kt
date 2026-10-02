package io.github.supermonster003.autojs6.plugin.three.shell.terminal.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Strict decoding of the GitHub release document and the bounded fetch around it (AGENTS.md 12). */
class ReleaseInfoCodecTest {

    private val tag = "v1.2.0"
    private val url = "https://github.com/${ReleaseInfoCodec.REPOSITORY}/releases/tag/$tag"
    private val document = """{"draft":false,"prerelease":false,"tag_name":"$tag","html_url":"$url","body":"# Notes\n\n* `Fix` one"}"""

    @Test
    fun aPublicStableReleaseDecodesAndRoundTrips() {
        val release = ReleaseInfoCodec.decode(document)
        assertEquals(ReleaseInfo(tag, url, "# Notes\n\n* `Fix` one"), release)
        assertEquals(release, ReleaseInfoCodec.decode(ReleaseInfoCodec.encode(release)))
        assertEquals(ReleaseInfo(tag, url, ""), ReleaseInfoCodec.decode(document.replace(""""body":"# Notes\n\n* `Fix` one"""", """"body":null""")))
    }

    @Test
    fun draftsPreReleasesAndForeignPagesAreRejected() {
        val rejected = listOf(
            document.replace("\"draft\":false", "\"draft\":true"),
            document.replace("\"prerelease\":false", "\"prerelease\":true"),
            document.replace("\"draft\":false,", ""),
            document.replace(tag, "v1.2.0-beta.1"),
            document.replace(tag, "latest"),
            document.replace("https://github.com", "http://github.com"),
            document.replace("github.com", "github.com.evil.example"),
            document.replace("/releases/tag/$tag\"", "/releases/tag/$tag?x=1\""),
            document.replace("/releases/tag/$tag\"", "/releases/tag/v1.2.1\""),
            document.replace("\"body\":\"# Notes\\n\\n* `Fix` one\"", "\"body\":42"),
            "[]",
            "not json",
            "{" + "\"a\":1,".repeat(ReleaseInfoCodec.MAX_DOCUMENT_CHARS / 6) + "\"draft\":false}",
        )
        rejected.forEach { text ->
            try {
                ReleaseInfoCodec.decode(text)
                fail("decoded: ${text.take(80)}")
            } catch (_: IllegalArgumentException) {
                // expected
            } catch (_: IllegalStateException) {
                // expected (Gson parse failures)
            } catch (_: com.google.gson.JsonParseException) {
                // expected
            }
        }
        assertTrue(ReleaseInfoCodec.validUrl(url, tag))
        assertFalse(ReleaseInfoCodec.validUrl("https://user@github.com/${ReleaseInfoCodec.REPOSITORY}/releases/tag/$tag", tag))
        assertFalse(ReleaseInfoCodec.validUrl("https://github.com:8443/${ReleaseInfoCodec.REPOSITORY}/releases/tag/$tag", tag))
        assertFalse(ReleaseInfoCodec.validUrl("$url#notes", tag))
    }

    @Test
    fun releaseNotesAreTruncatedOnACharacterBoundary() {
        val notes = "x".repeat(ReleaseInfoCodec.MAX_NOTES_CHARS - 1) + "😀" + "tail"
        val release = ReleaseInfoCodec.decode(ReleaseInfoCodec.encode(ReleaseInfo(tag, url, notes)))
        assertEquals(ReleaseInfoCodec.MAX_NOTES_CHARS - 1, release.notes.length)
        assertFalse(Character.isHighSurrogate(release.notes.last()))
    }

    @Test
    fun theRepositoryMapsHttpOutcomesOntoResults() {
        assertEquals(UpdateResult.Success(ReleaseInfoCodec.decode(document)), fetch(200, document))
        assertEquals(UpdateResult.Success(null), fetch(404, "{}"))
        assertEquals(UpdateResult.Failure(UpdateFailure.HTTP), fetch(500, "{}"))
        assertEquals(UpdateResult.Failure(UpdateFailure.HTTP), fetch(302, "{}"))
        assertEquals(UpdateResult.Failure(UpdateFailure.MALFORMED), fetch(200, "{\"draft\":true}"))
        assertEquals(UpdateResult.Failure(UpdateFailure.MALFORMED), fetch(200, byteArrayOf(0xFF.toByte(), 0xFE.toByte(), '{'.code.toByte())))
        assertEquals(UpdateResult.Failure(UpdateFailure.TOO_LARGE), fetch(200, ByteArray(AppUpdateRepository.MAX_RESPONSE_BYTES + 1) { ' '.code.toByte() }))
        assertEquals(UpdateResult.Failure(UpdateFailure.NETWORK), AppUpdateRepository { throw IOException("offline") }.fetchLatest(UpdateCancellation()))
        val cancelled = UpdateCancellation().also { it.cancel() }
        assertEquals(UpdateResult.Failure(UpdateFailure.NETWORK), AppUpdateRepository { throw AssertionError("must not connect") }.fetchLatest(cancelled))
    }

    @Test
    fun cancellationDisconnectsTheLiveConnection() {
        var disconnected = 0
        val connection = object : FakeConnection(200, ByteArray(0)) {
            override fun disconnect() {
                disconnected++
            }
        }
        val cancellation = UpdateCancellation()
        cancellation.attach(connection)
        cancellation.cancel()
        assertTrue(cancellation.cancelled)
        assertEquals(1, disconnected)
        // Attaching after cancellation disconnects at once.
        UpdateCancellation().also { it.cancel() }.attach(connection)
        assertEquals(2, disconnected)
    }

    private fun fetch(code: Int, body: String): UpdateResult = fetch(code, body.toByteArray(Charsets.UTF_8))

    private fun fetch(code: Int, body: ByteArray): UpdateResult =
        AppUpdateRepository { FakeConnection(code, body) }.fetchLatest(UpdateCancellation())

    private open class FakeConnection(private val code: Int, private val body: ByteArray) : HttpURLConnection(URL(AppUpdateRepository.ENDPOINT)) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
        override fun getResponseCode(): Int = code
        override fun getInputStream(): InputStream = ByteArrayInputStream(body)
    }

}
