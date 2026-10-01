package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import java.io.File
import java.net.URLEncoder

/**
 * Non-UI operations ported from the host's TerminalNpmActions. The caller shows dialogs and
 * types the returned command into the selected session; constructing an action never executes it.
 * zh-CN: 自宿主 TerminalNpmActions 提取的非 UI 操作. 调用方展示对话框并把返回命令送入所选会话.
 */
object TerminalNpmActions {

    data class PackageManager(val name: String, val versionCommand: String)

    /** The UI pairs these with its localized explanation of corepack and Android limitations. */
    val otherPackageManagers = listOf(PackageManager("Yarn", "yarn --version"), PackageManager("pnpm", "pnpm --version"))

    fun npmInit(): String = "npm init"

    fun npmInstall(): String = "npm install"

    /** Whitespace-separated package specs, matching the original menu. Blank input has no action. */
    fun npmInstallPackage(input: String): String? {
        require('\u0000' !in input) { "Package specs must not contain NUL" }
        val packages = input.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        return packages.takeIf { it.isNotEmpty() }?.let { "npm install " + ShellQuoting.join(it) }
    }

    fun scripts(directory: File?): List<NpmProjectScripts.Script>? = directory?.let(NpmProjectScripts::read)

    fun npmRunScript(name: String): String {
        require(name.isNotBlank() && '\u0000' !in name) { "Script name must be non-blank and contain no NUL" }
        return "npm run " + ShellQuoting.quote(name)
    }

    fun searchUrl(query: String): String = query.trim().let {
        if (it.isEmpty()) "https://www.npmjs.com/"
        else "https://www.npmjs.com/search?q=" + URLEncoder.encode(it, "UTF-8").replace("+", "%20")
    }
}
