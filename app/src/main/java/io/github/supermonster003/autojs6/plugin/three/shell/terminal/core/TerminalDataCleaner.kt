package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import java.io.File
import java.io.IOException

/** Filesystem half of clear-data. The caller must stop sessions and serialize Node installation first. */
object TerminalDataCleaner {

    /** These are the complete deletion targets, not the app's files directory or shared storage. */
    fun directoriesToClear(paths: TerminalPaths): List<File> = listOf(paths.home, paths.prefix)

    @Throws(IOException::class)
    fun clearAndRebuild(paths: TerminalPaths) {
        if (isLink(paths.root)) throw IOException("Terminal root must not be a symbolic link")
        for (directory in directoriesToClear(paths)) {
            if (!deleteTree(directory)) throw IOException("Could not clear terminal directory ${directory.name}")
        }
        paths.ensureLayout()
    }

    /** Do not follow directory links into a project outside the terminal data tree. */
    internal fun deleteTree(file: File): Boolean {
        if (!isLink(file) && file.isDirectory) {
            val children = file.listFiles() ?: return false
            if (!children.map(::deleteTree).all { it }) return false
        }
        // Try deletion even for a dangling symlink, for which exists() returns false.
        return file.delete() || !file.exists()
    }

    private fun isLink(file: File): Boolean {
        val parent = file.absoluteFile.parentFile ?: return false
        return file.canonicalFile != File(parent.canonicalFile, file.name)
    }
}
