package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** [NativeLibraryInventory] reports exactly the ABIs whose complete pty library set is packaged (roadmap D14). */
class NativeLibraryInventoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `the known ABIs and library names mirror the build script`() {
        assertEquals(listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"), NativeLibraryInventory.knownAbis)
        assertEquals(listOf("libjackpal-androidterm5.so", "libjackpal-termexec2.so"), NativeLibraryInventory.libraryNames)
    }

    @Test
    fun `a universal APK reports all four ABIs in build order`() {
        val entries = NativeLibraryInventory.knownAbis.flatMap { abi -> NativeLibraryInventory.libraryNames.map { "lib/$abi/$it" } }
        assertArrayEquals(arrayOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"), NativeLibraryInventory.supportedAbis(entries.shuffled()))
    }

    @Test
    fun `an ABI with a missing library or an unknown ABI is not reported`() {
        val entries = listOf(
            "lib/arm64-v8a/libjackpal-androidterm5.so",
            "lib/arm64-v8a/libjackpal-termexec2.so",
            "lib/armeabi-v7a/libjackpal-termexec2.so",
            "lib/mips/libjackpal-androidterm5.so",
            "lib/mips/libjackpal-termexec2.so",
            "classes.dex",
        )
        assertArrayEquals(arrayOf("arm64-v8a"), NativeLibraryInventory.supportedAbis(entries))
        assertArrayEquals(emptyArray<String>(), NativeLibraryInventory.supportedAbis(emptyList()))
    }

    @Test
    fun `split APKs are merged and unreadable paths are ignored`() {
        val base = zip("base.apk", "classes.dex", "AndroidManifest.xml")
        val split = zip("split_config.x86_64.apk", "lib/x86_64/libjackpal-androidterm5.so", "lib/x86_64/libjackpal-termexec2.so")
        val missing = File(temporaryFolder.root, "missing.apk")
        val abis = NativeLibraryInventory.supportedAbisFromApks(
            apkPaths = listOf(base, split, missing),
            extractedLibraries = emptyList(),
            is64Bit = true,
            supported32BitAbis = listOf("x86"),
            supported64BitAbis = listOf("x86_64"),
        )
        assertArrayEquals(arrayOf("x86_64"), abis)
    }

    @Test
    fun `extracted libraries fall back to the ABI of the running process`() {
        val stripped = zip("base.apk", "classes.dex")
        val directory = temporaryFolder.newFolder("lib")
        val extracted = NativeLibraryInventory.libraryNames.map { File(directory, it).apply { writeBytes(byteArrayOf(0x7f, 'E'.code.toByte())) } }
        val abis = NativeLibraryInventory.supportedAbisFromApks(
            apkPaths = listOf(stripped),
            extractedLibraries = extracted,
            is64Bit = false,
            supported32BitAbis = listOf("armeabi-v7a", "armeabi"),
            supported64BitAbis = listOf("arm64-v8a"),
        )
        assertArrayEquals(arrayOf("armeabi-v7a"), abis)

        val partial = NativeLibraryInventory.supportedAbisFromApks(
            apkPaths = listOf(stripped),
            extractedLibraries = extracted.take(1),
            is64Bit = false,
            supported32BitAbis = listOf("armeabi-v7a"),
            supported64BitAbis = listOf("arm64-v8a"),
        )
        assertArrayEquals(arrayOf("armeabi-v7a"), partial)

        extracted.last().delete()
        val incomplete = NativeLibraryInventory.supportedAbisFromApks(
            apkPaths = listOf(stripped),
            extractedLibraries = extracted,
            is64Bit = true,
            supported32BitAbis = listOf("armeabi-v7a"),
            supported64BitAbis = listOf("arm64-v8a"),
        )
        assertArrayEquals(emptyArray<String>(), incomplete)
    }

    private fun zip(name: String, vararg entries: String): File {
        val file = File(temporaryFolder.root, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { entry ->
                zip.putNextEntry(ZipEntry(entry))
                zip.write(byteArrayOf(0))
                zip.closeEntry()
            }
        }
        return file
    }
}
