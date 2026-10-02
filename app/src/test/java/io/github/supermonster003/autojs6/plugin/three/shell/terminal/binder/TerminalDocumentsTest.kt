package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import com.google.gson.JsonParser
import org.autojs.plugin.terminal.api.TerminalContract
import org.autojs.plugin.terminal.api.TerminalErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Request validation against the D22 ceilings and the documents the host's `TerminalJson` parses (roadmap P2.4). */
class TerminalDocumentsTest {

    @Test
    fun aMinimalRequestTakesTheDefaults() {
        val request = TerminalDocuments.parseOpenRequest("{}")
        assertNull(request.cwd)
        assertNull(request.command)
        assertTrue(request.env.isEmpty())
        assertNull(request.title)
        assertTrue(request.keepOpen)
    }

    @Test
    fun everyFieldIsReadAndTrimmedWhereTheContractAllows() {
        val request = TerminalDocuments.parseOpenRequest(
            """{"cwd":"/sdcard/Scripts","command":"npm run build","env":{"CI":"1","NO_COLOR":"true"},"title":" Build ","keepOpen":false,"unknown":1}""",
        )
        assertEquals("/sdcard/Scripts", request.cwd)
        assertEquals("npm run build", request.command)
        assertEquals(linkedMapOf("CI" to "1", "NO_COLOR" to "true"), request.env)
        assertEquals("Build", request.title)
        assertFalse(request.keepOpen)
        assertNull("a blank command means a plain shell", TerminalDocuments.parseOpenRequest("""{"command":"  \n"}""").command)
        assertNull("a JSON null is an absent field", TerminalDocuments.parseOpenRequest("""{"cwd":null,"env":null}""").cwd)
    }

    @Test
    fun hostileRequestsAreInvalidArguments() {
        val hostile = listOf(
            null,
            "",
            "not json",
            "{unquoted:1}",
            "{'cwd':'/tmp'}",
            "null",
            "{} trailing",
            "{/*comment*/}",
            "[]",
            "\"string\"",
            """{"cwd":""}""",
            """{"cwd":"   "}""",
            """{"cwd":"/${"a".repeat(TerminalContract.MAX_PATH_BYTES)}"}""",
            """{"cwd":12}""",
            """{"command":"echo \u0000"}""",
            """{"command":["ls"]}""",
            """{"env":[]}""",
            """{"env":{"A=B":"1"}}""",
            """{"env":{"":"1"}}""",
            """{"env":{"1A":"1"}}""",
            """{"env":{"A":1}}""",
            """{"env":{"A":null}}""",
            """{"env":{"A":"${"v".repeat(TerminalContract.MAX_ENV_ENTRY_BYTES)}"}}""",
            """{"env":{${(1..TerminalContract.MAX_ENV_ENTRIES + 1).joinToString(",") { "\"V$it\":\"x\"" }}}}""",
            """{"title":""}""",
            """{"title":"   "}""",
            """{"title":"${"t".repeat(TerminalDocuments.MAX_TITLE_BYTES + 1)}"}""",
            """{"keepOpen":"yes"}""",
            """{"command":"${"x".repeat(TerminalContract.MAX_JSON_BYTES)}"}""",
        )
        hostile.forEach { request ->
            try {
                TerminalDocuments.parseOpenRequest(request)
                fail("expected INVALID_ARGUMENT for ${request?.take(40)}")
            } catch (e: TerminalFailure) {
                assertEquals(request?.take(40), TerminalErrorCodes.INVALID_ARGUMENT, e.code)
                assertTrue(e.message.orEmpty().isNotBlank())
            }
        }
    }

    @Test
    fun theEnvironmentCeilingIsInclusive() {
        val full = (1..TerminalContract.MAX_ENV_ENTRIES).joinToString(",") { "\"V$it\":\"x\"" }
        assertEquals(TerminalContract.MAX_ENV_ENTRIES, TerminalDocuments.parseOpenRequest("""{"env":{$full}}""").env.size)
        val value = "v".repeat(TerminalContract.MAX_ENV_ENTRY_BYTES - "A".length - 1)
        assertEquals(value, TerminalDocuments.parseOpenRequest("""{"env":{"A":"$value"}}""").env["A"])
    }

    @Test
    fun subscribeOptionsDefaultToTheTail() {
        assertFalse(TerminalDocuments.parseSubscribeOptions(null))
        assertFalse(TerminalDocuments.parseSubscribeOptions(""))
        assertFalse(TerminalDocuments.parseSubscribeOptions("{}"))
        assertTrue(TerminalDocuments.parseSubscribeOptions("""{"fromStart":true}"""))
        try {
            TerminalDocuments.parseSubscribeOptions("""{"fromStart":"yes"}""")
            fail()
        } catch (e: TerminalFailure) {
            assertEquals(TerminalErrorCodes.INVALID_ARGUMENT, e.code)
        }
    }

    @Test
    fun sessionDocumentsCarryEveryContractField() {
        val running = TerminalDocuments.session(TerminalDocuments.SessionView("7", "/data/home", "sh", 1700000000000L, true, null, 4242, TerminalContract.STATE_RUNNING))
        assertEquals("7", running["id"].asString)
        assertEquals("/data/home", running["cwd"].asString)
        assertEquals("sh", running["title"].asString)
        assertEquals(1700000000000L, running["createdAt"].asLong)
        assertTrue(running["alive"].asBoolean)
        assertTrue("the host reads a null exit code as 'still running'", running["exitCode"].isJsonNull)
        assertEquals(4242, running["pid"].asInt)
        assertEquals("running", running["state"].asString)
        val exited = TerminalDocuments.session(TerminalDocuments.SessionView("7", "/data/home", "sh", 1L, false, 130, 4242, TerminalContract.STATE_EXITED))
        assertEquals(130, exited["exitCode"].asInt)
        assertEquals("exited", exited["state"].asString)
        val list = JsonParser.parseString(TerminalDocuments.sessions(emptyList()))
        assertTrue(list.isJsonArray && list.asJsonArray.isEmpty)
    }

    @Test
    fun errorAndEnvironmentDocumentsMatchTheProtocol() {
        val error = JsonParser.parseString(TerminalDocuments.error(TerminalErrorCodes.SESSION_NOT_FOUND, "no session 9", "9")).asJsonObject
        assertEquals("SESSION_NOT_FOUND", error["code"].asString)
        assertEquals("no session 9", error["message"].asString)
        assertEquals("9", error["sessionId"].asString)
        assertFalse(JsonParser.parseString(TerminalDocuments.error("INTERNAL", "x")).asJsonObject.has("sessionId"))

        val environment = JsonParser.parseString(
            TerminalDocuments.environment(
                home = "/data/home",
                prefix = "/data/home/usr",
                shell = "/system/bin/sh",
                node = TerminalDocuments.NodeView(false, null, "pkg", null, "plugin-missing"),
                npmRegistry = "https://registry.npmjs.org/",
                storageAccess = TerminalContract.STORAGE_ACCESS_DENIED,
                pluginVersion = "1.0.0",
                contractVersion = 1,
            ),
        ).asJsonObject
        assertEquals("/data/home", environment["home"].asString)
        assertEquals("/data/home/usr", environment["prefix"].asString)
        assertEquals("/system/bin/sh", environment["shell"].asString)
        val node = environment["node"].asJsonObject
        assertFalse(node["available"].asBoolean)
        assertTrue(node["version"].isJsonNull)
        assertEquals("pkg", node["packageName"].asString)
        assertEquals("plugin-missing", node["reason"].asString)
        assertEquals("https://registry.npmjs.org/", environment["npmRegistry"].asString)
        assertEquals("denied", environment["storageAccess"].asString)
        assertEquals("1.0.0", environment["pluginVersion"].asString)
        assertEquals(1, environment["contractVersion"].asInt)
    }

    @Test
    fun transcriptPaddingIsDroppedAndReplaysEndClosedRows() {
        assertEquals("line1\nline2", TerminalDocuments.trimTranscriptPadding("line1\nline2\n\n\n", 0))
        assertEquals("", TerminalDocuments.trimTranscriptPadding("\n\n   \n", 0))
        assertEquals("", TerminalDocuments.trimTranscriptPadding("", -1))
        assertEquals("$ ", TerminalDocuments.trimTranscriptPadding("$ \n\n", 2))
        assertEquals("$", TerminalDocuments.trimTranscriptPadding("$ \n\n", 0))
        assertEquals("line1\n", TerminalDocuments.replayText("line1\n\n\n", 0))
        assertEquals("line1\n", TerminalDocuments.replayText("line1", -1))
        assertEquals("$ ", TerminalDocuments.replayText("$ \n\n\n", 2))
        assertEquals("", TerminalDocuments.replayText("\n\n", 0))
    }

    @Test
    fun transcriptsAreTrimmedToTheNewestBytesOnACharacterBoundary() {
        assertEquals("abc" to false, TerminalDocuments.trimTranscript("abc", 3))
        assertEquals("bc" to true, TerminalDocuments.trimTranscript("abc", 2))
        assertEquals("" to true, TerminalDocuments.trimTranscript("abc", 0))
        // Each CJK character is three UTF-8 bytes: a cut inside a character skips it entirely.
        assertEquals("终端" to true, TerminalDocuments.trimTranscript("a终端", 6))
        assertEquals("端" to true, TerminalDocuments.trimTranscript("a终端", 5))
    }

    @Test
    fun transcriptRepliesAlsoRespectTheUtf16ParcelBudget() {
        val text = "x".repeat(600_000) + "\uD83D\uDE00end"
        val trimmed = TerminalDocuments.trimTranscript(text, TerminalContract.MAX_TRANSCRIPT_BYTES, 491_520)
        assertTrue(trimmed.second)
        assertEquals(491_520, trimmed.first.length)
        assertTrue(trimmed.first.endsWith("\uD83D\uDE00end"))
        assertEquals("end" to true, TerminalDocuments.trimTranscript("x\uD83D\uDE00end", 100, 4))
    }

}
