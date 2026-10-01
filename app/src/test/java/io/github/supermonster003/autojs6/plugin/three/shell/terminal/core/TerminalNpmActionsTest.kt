package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import org.junit.Assert.*
import org.junit.Test
import java.net.URLDecoder
import java.nio.file.Files

class TerminalNpmActionsTest {
    @Test
    fun packageSpecsAndScriptNamesRemainShellArguments() {
        assertNull(TerminalNpmActions.npmInstallPackage(" \t\n"))
        assertEquals("npm install @scope/pkg@1.2.3 'name;echo'", TerminalNpmActions.npmInstallPackage(" @scope/pkg@1.2.3\nname;echo "))
        assertEquals("npm run 'build && echo hacked'", TerminalNpmActions.npmRunScript("build && echo hacked"))
        assertThrows(IllegalArgumentException::class.java) { TerminalNpmActions.npmInstallPackage("pkg\u0000other") }
        assertThrows(IllegalArgumentException::class.java) { TerminalNpmActions.npmRunScript("  ") }
    }

    @Test
    fun searchTermsDoNotBecomeOtherUrlParameters() {
        assertEquals("https://www.npmjs.com/", TerminalNpmActions.searchUrl("  "))
        val query = "@scope/中文 + &sort=evil#fragment"
        val url = TerminalNpmActions.searchUrl(" $query ")
        assertTrue(url.startsWith("https://www.npmjs.com/search?q="))
        assertFalse(url.contains("&sort="))
        assertFalse(url.contains("#"))
        assertEquals(query, URLDecoder.decode(url.substringAfter("?q="), "UTF-8"))
    }

    @Test
    fun scriptChoicesKeepProjectOrderAndDistinguishMissingFromEmpty() {
        val dir = Files.createTempDirectory("npm-actions").toFile()
        try {
            assertNull(TerminalNpmActions.scripts(dir))
            val file = NpmProjectScripts.packageJsonOf(dir)
            file.writeText("{}")
            assertTrue(TerminalNpmActions.scripts(dir)!!.isEmpty())
            file.writeText("""{"scripts":{"z-last":"node z.js","build":"node build.js","invalid":3}}""")
            assertEquals(listOf("z-last", "build"), TerminalNpmActions.scripts(dir)!!.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }
}
