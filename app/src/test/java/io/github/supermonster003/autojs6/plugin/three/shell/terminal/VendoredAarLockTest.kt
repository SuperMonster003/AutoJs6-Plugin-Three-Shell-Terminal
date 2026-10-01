package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * The two lock files, the staged AAR files, the native provenance record and the third-party notices
 * must describe the same bytes (roadmap D13); Gradle enforces the lock at configuration time, this test
 * keeps the human-readable records honest as well.
 */
class VendoredAarLockTest {

    private val root: Path = findProjectRoot()

    @Test
    fun `vendored lock lists exactly the three jackpal artifacts and their digests match the files`() {
        val lock = readLock(root.resolve("locks/vendored-aars.lock"))
        assertEquals(setOf("term-1_0_70", "emulatorview-1_0_42", "libtermexec-1_0"), lock.keys)
        lock.forEach { (id, entry) ->
            val file = root.resolve("libs/jackpal").resolve(entry.file)
            assertTrue("$id: missing ${entry.file}", Files.isRegularFile(file))
            assertEquals("$id: digest drifted from the lock", entry.sha256, sha256(file))
        }
    }

    @Test
    fun `host api lock lists the three host contract artifacts and their digests match the files`() {
        val lock = readLock(root.resolve("locks/host-api-aars.lock"))
        assertEquals(setOf("common-plugin-api", "nodejs-api", "terminal-api"), lock.keys)
        lock.forEach { (id, entry) ->
            val file = root.resolve("libs").resolve(entry.file)
            assertTrue("$id: missing ${entry.file}", Files.isRegularFile(file))
            assertEquals("$id: digest drifted from the lock", entry.sha256, sha256(file))
        }
    }

    @Test
    fun `the native provenance record matches the locked libtermexec artifact`() {
        val provenance = Files.readString(root.resolve("native/jackpal-termexec/provenance.json"))
        val lock = readLock(root.resolve("locks/vendored-aars.lock"))
        val aarSha256 = Regex("\"aarSha256\"\\s*:\\s*\"([0-9a-f]{64})\"").find(provenance)?.groupValues?.get(1)
        assertEquals(lock.getValue("libtermexec-1_0").sha256, aarSha256)
        assertTrue(provenance.contains("\"ndkVersion\": \"28.2.13676358\""))
        NativeLibraryInventory.knownAbis.forEach { abi ->
            NativeLibraryInventory.libraryNames.forEach { library ->
                assertTrue("provenance lacks jni/$abi/$library", provenance.contains("\"path\": \"jni/$abi/$library\""))
            }
        }
        assertTrue("every native library must be 16 KB load-aligned", Regex("\"minLoadAlign\"\\s*:\\s*(\\d+)").findAll(provenance).all { it.groupValues[1] == "16384" })
    }

    @Test
    fun `the libtermexec AAR packages the pty libraries for every supported ABI`() {
        val entries = ZipFile(root.resolve("libs/jackpal/libtermexec-1_0.aar").toFile()).use { zip ->
            zip.entries().asSequence().map { it.name }.filterNot { it.endsWith("/") }.toList()
        }
        val expected = NativeLibraryInventory.knownAbis.flatMap { abi -> NativeLibraryInventory.libraryNames.map { "jni/$abi/$it" } }
        assertEquals(expected.toSet(), entries.filter { it.startsWith("jni/") }.toSet())
    }

    @Test
    fun `the third party notices repeat every locked digest`() {
        val notices = Files.readString(root.resolve("THIRD_PARTY_NOTICES.md"))
        (readLock(root.resolve("locks/vendored-aars.lock")) + readLock(root.resolve("locks/host-api-aars.lock"))).forEach { (id, entry) ->
            assertTrue("THIRD_PARTY_NOTICES.md must list ${entry.file}", notices.contains("`${entry.file}`"))
            assertTrue("THIRD_PARTY_NOTICES.md must repeat the digest of $id", notices.contains(entry.sha256))
        }
    }

    private data class LockEntry(val file: String, val sha256: String)

    private fun readLock(path: Path): Map<String, LockEntry> {
        val lines = Files.readAllLines(path).map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        assertEquals("format=1", lines.first())
        val values = lines.drop(1).associate { line -> line.substringBefore('=') to line.substringAfter('=') }
        val ids = values.keys.map { it.substringBeforeLast('.') }.toSet()
        return ids.associateWith { id ->
            val sha256 = values.getValue("$id.sha256")
            assertTrue("$id: lock digest must be lowercase hex", Regex("[0-9a-f]{64}").matches(sha256))
            LockEntry(values.getValue("$id.file"), sha256)
        }
    }

    private fun sha256(path: Path): String =
        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it) }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
