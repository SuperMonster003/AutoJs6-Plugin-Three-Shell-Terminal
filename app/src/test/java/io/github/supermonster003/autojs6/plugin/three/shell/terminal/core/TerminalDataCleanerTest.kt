package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class TerminalDataCleanerTest {
    @Test
    fun onlyHomeAndUsrAreClearedAndTheLayoutIsRebuilt() {
        val files = Files.createTempDirectory("terminal-clear").toFile()
        try {
            val paths = TerminalPaths(File(files, "terminal")).ensureLayout()
            assertEquals(listOf(paths.home, paths.prefix), TerminalDataCleaner.directoriesToClear(paths))
            File(paths.home, "project.txt").writeText("remove")
            File(paths.bin, "installed-command").writeText("remove")
            paths.npmCache.mkdirs()
            File(paths.npmCache, "cached").writeText("remove")
            val sibling = File(files, "user-project.txt").apply { writeText("keep") }
            val rootMarker = File(paths.root, "outside-managed-dirs").apply { writeText("keep") }
            TerminalDataCleaner.clearAndRebuild(paths)
            assertTrue(paths.home.isDirectory)
            assertTrue(paths.home.listFiles()!!.isEmpty())
            assertTrue(paths.bin.listFiles()!!.isEmpty())
            assertFalse(paths.npmCache.exists())
            assertEquals(TerminalPaths.DEFAULT_PROFILE, paths.profile.readText())
            assertEquals("keep", sibling.readText())
            assertEquals("keep", rootMarker.readText())
        } finally {
            files.deleteRecursively()
        }
    }

    @Test
    fun firstRunAndRepeatedClearAreValid() {
        val files = Files.createTempDirectory("terminal-clear-new").toFile()
        try {
            val paths = TerminalPaths(File(files, "terminal"))
            repeat(2) {
                TerminalDataCleaner.clearAndRebuild(paths)
                assertTrue(paths.home.isDirectory)
                assertTrue(paths.profile.isFile)
            }
        } finally {
            files.deleteRecursively()
        }
    }

    @Test
    fun anUnusableRootIsReportedAsFailure() {
        val files = Files.createTempDirectory("terminal-clear-failed").toFile()
        try {
            val root = File(files, "terminal").apply { writeText("not a directory") }
            assertThrows(IOException::class.java) { TerminalDataCleaner.clearAndRebuild(TerminalPaths(root)) }
            assertEquals("not a directory", root.readText())
        } finally {
            files.deleteRecursively()
        }
    }
}
