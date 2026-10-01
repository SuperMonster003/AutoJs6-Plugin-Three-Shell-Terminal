package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.content.Context
import android.os.Build
import java.io.File
import java.util.zip.ZipFile

/**
 * Derives `supportedAbis` from the pty libraries actually packaged in the installed APK set, so a
 * per-ABI split reports one ABI and the universal APK reports all four (roadmap D14). The ZIP-free
 * fallback covers installers that strip `lib/` after extraction: the ABI of the running process
 * is reported when the extracted library exists.
 */
internal object NativeLibraryInventory {

    /** Order matches the ABI list of `app/build.gradle.kts` and the 16 KB verifier. */
    val knownAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

    val libraryNames = listOf("libjackpal-androidterm5.so", "libjackpal-termexec2.so")

    fun supportedAbis(context: Context): Array<String> {
        val applicationInfo = context.applicationInfo
        return supportedAbisFromApks(
            apkPaths = (listOfNotNull(applicationInfo.sourceDir) + applicationInfo.splitSourceDirs.orEmpty()).map(::File),
            extractedLibraries = applicationInfo.nativeLibraryDir?.let { directory -> libraryNames.map { File(directory, it) } }.orEmpty(),
            is64Bit = android.os.Process.is64Bit(),
            supported32BitAbis = Build.SUPPORTED_32_BIT_ABIS.toList(),
            supported64BitAbis = Build.SUPPORTED_64_BIT_ABIS.toList(),
        )
    }

    fun supportedAbisFromApks(
        apkPaths: Iterable<File>,
        extractedLibraries: List<File>,
        is64Bit: Boolean,
        supported32BitAbis: List<String>,
        supported64BitAbis: List<String>,
    ): Array<String> {
        val entries = buildSet {
            apkPaths.forEach { path ->
                runCatching {
                    ZipFile(path).use { zip ->
                        val enumeration = zip.entries()
                        while (enumeration.hasMoreElements()) {
                            add(enumeration.nextElement().name)
                        }
                    }
                }
            }
        }
        val packagedAbis = supportedAbis(entries)
        if (packagedAbis.isNotEmpty()) return packagedAbis

        val processAbis = if (is64Bit) supported64BitAbis else supported32BitAbis
        val loadedAbi = processAbis.firstOrNull { it in knownAbis }
            .takeIf { extractedLibraries.isNotEmpty() && extractedLibraries.all { it.isFile } }
        return listOfNotNull(loadedAbi).toTypedArray()
    }

    /** ABIs whose complete library set is present among the given ZIP entry names. */
    fun supportedAbis(entryNames: Iterable<String>): Array<String> {
        val entries = entryNames.toHashSet()
        return knownAbis.filter { abi -> libraryNames.all { "lib/$abi/$it" in entries } }.toTypedArray()
    }
}
