package io.github.supermonster003.autojs6.plugin.three.shell.terminal.node

import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths

/**
 * Environment variables that expose the Node.js Runtime plugin's node / npm inside a terminal session.
 *
 * The launcher finds its scripts through `AUTOJS6_NODE_CLI_ROOT`; npm and corepack are pointed at
 * caches under `$PREFIX` so nothing lands in the shared home by surprise, global installs go to
 * `$PREFIX/lib/node_modules`, and `bin-links` stays off because files the plugin writes cannot be
 * executed on Android (W^X). Registry variables are only exported when the user picked a
 * non-default registry so a user-level `.npmrc` keeps working otherwise. Corepack is told not to
 * chase the newest package manager releases: pnpm 12 and later ship as native binaries without an
 * Android build, so its bundled defaults (Yarn 1.x, pnpm 11.x) are the ones that can actually run.
 *
 * zh-CN: 在终端会话中暴露 Node.js 运行时插件 node / npm 的环境变量. 启动器通过 `AUTOJS6_NODE_CLI_ROOT` 找到脚本;
 * npm / corepack 的缓存放在 `$PREFIX` 下, 全局安装进 `$PREFIX/lib/node_modules`, `bin-links` 关闭
 * (插件写入的文件在 Android 上不可执行). 仅当用户选择了非默认 registry 时才导出 registry 变量,
 * 以便用户级 `.npmrc` 在默认情况下继续生效. corepack 不追最新版: pnpm 12 起为原生二进制且没有
 * Android 构建, 因此使用其内置默认版本 (Yarn 1.x, pnpm 11.x) 才能真正运行.
 */
object TerminalNodeEnvironment {

    const val CLI_ROOT_VARIABLE = "AUTOJS6_NODE_CLI_ROOT"
    const val DEFAULT_REGISTRY = "https://registry.npmjs.org/"
    const val NPMMIRROR_REGISTRY = "https://registry.npmmirror.com/"

    /**
     * @param registry     registry URL chosen in settings, or null / the default to leave npm's own default
     * @param ignoreScripts export `npm_config_ignore_scripts=true`
     */
    data class Options(
        val registry: String? = null,
        val ignoreScripts: Boolean = false,
    )

    /**
     * Returns [url] when it is an acceptable custom registry (`https://` only), otherwise null.
     * zh-CN: [url] 为可接受的自定义 registry (仅 `https://`) 时原样返回, 否则返回 null.
     */
    @JvmStatic
    fun sanitizeRegistry(url: String?): String? {
        val trimmed = url?.trim().orEmpty()
        if (!trimmed.startsWith("https://", ignoreCase = true) || trimmed.length <= "https://".length) return null
        if (trimmed.any { it.isWhitespace() || it == ' ' }) return null
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    @JvmStatic
    @JvmOverloads
    fun build(paths: TerminalPaths, options: Options = Options()): LinkedHashMap<String, String> {
        val env = LinkedHashMap<String, String>()
        env[CLI_ROOT_VARIABLE] = paths.nodeCliRoot.path
        env["npm_config_cache"] = paths.npmCache.path
        env["npm_config_prefix"] = paths.prefix.path
        env["npm_config_bin_links"] = "false"
        env["npm_config_update_notifier"] = "false"
        if (options.ignoreScripts) {
            env["npm_config_ignore_scripts"] = "true"
        }
        val registry = options.registry?.takeIf { it.isNotBlank() && !it.equals(DEFAULT_REGISTRY, ignoreCase = true) }
        if (registry != null) {
            env["npm_config_registry"] = registry
            env["COREPACK_NPM_REGISTRY"] = registry
        }
        env["COREPACK_HOME"] = paths.corepackHome.path
        env["COREPACK_DEFAULT_TO_LATEST"] = "0"
        return env
    }

}
