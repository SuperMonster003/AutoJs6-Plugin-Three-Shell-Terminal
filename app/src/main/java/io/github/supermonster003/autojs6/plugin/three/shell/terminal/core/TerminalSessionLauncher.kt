package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

/**
 * Builds the argv of an interactive shell that starts in a chosen directory.
 *
 * The wrapper `cd`s into `$1` (falling back to `$HOME` when the directory is gone) and then
 * `exec`s the shell named by `$0`, so the pty child is the interactive shell itself and receives
 * signals directly. The profile named by `$ENV` is sourced by the shell on startup.
 *
 * With a command (host `openSession` / `TERMINAL_OPEN` requests, plugin roadmap appendix D) the
 * wrapper runs the command line verbatim after the `cd` and then either `exec`s the interactive
 * shell (`keepOpen`) or exits with the command's status. The command is embedded as written: the
 * caller owns its quoting, the contract bounds its length.
 *
 * zh-CN: 构建在指定目录启动的交互式 shell 的 argv. 包装脚本先 `cd` 到 `$1` (目录不存在时退回 `$HOME`),
 * 再 `exec` 由 `$0` 指定的 shell, 使 pty 子进程就是交互式 shell 本身并直接接收信号. 带命令时 (宿主
 * `openSession` / `TERMINAL_OPEN` 请求, 路线图附录 D) 包装脚本在 `cd` 之后原样执行命令行, 然后 `exec`
 * 交互式 shell (`keepOpen`) 或以命令的状态退出; 命令按原文嵌入, 引号由调用方负责, 长度受契约限制.
 */
object TerminalSessionLauncher {

    const val CD_SCRIPT = "cd -- \"\$1\" 2>/dev/null || cd \"\$HOME\""

    const val WRAPPER_SCRIPT = "$CD_SCRIPT; exec \"\$0\""

    /**
     * @return argv including argv[0]; `argv[0]` doubles as the executable path
     */
    @JvmStatic
    @JvmOverloads
    fun buildCommand(cwd: String, shell: String = TerminalEnvironment.DEFAULT_SHELL): List<String> = listOf(
        shell,
        "-c",
        WRAPPER_SCRIPT,
        shell,
        cwd,
    )

    /**
     * argv of a shell that runs [command] inside [cwd] first; a blank command yields the plain
     * interactive wrapper of [buildCommand].
     *
     * @param keepOpen `exec` the interactive shell after the command instead of exiting with its status
     * zh-CN: 先在 [cwd] 中执行 [command] 的 shell 的 argv; 命令为空白时等同于 [buildCommand] 的纯交互包装.
     */
    @JvmStatic
    @JvmOverloads
    fun buildCommand(cwd: String, command: String?, keepOpen: Boolean, shell: String = TerminalEnvironment.DEFAULT_SHELL): List<String> {
        if (command.isNullOrBlank()) return buildCommand(cwd, shell)
        return listOf(shell, "-c", commandScript(command, keepOpen), shell, cwd)
    }

    /**
     * The `-c` script of the command variant: the `cd`, the command line on its own line (so a
     * trailing `&` or comment cannot swallow the epilogue), then `exec "$0"` or `exit $?`.
     * zh-CN: 命令变体的 `-c` 脚本: `cd`, 独占一行的命令 (避免尾随 `&` 或注释吞掉收尾), 然后 `exec "$0"` 或 `exit $?`.
     */
    @JvmStatic
    fun commandScript(command: String, keepOpen: Boolean): String = buildString {
        append(CD_SCRIPT).append('\n')
        append(command).append('\n')
        append(if (keepOpen) "exec \"\$0\"" else "exit \$?")
    }

}
