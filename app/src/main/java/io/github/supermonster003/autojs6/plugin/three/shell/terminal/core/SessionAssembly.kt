package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.content.Context
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.NodeCliLocator
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.node.TerminalNodeSetup
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.storage.StorageAccess

/**
 * The two-step session start shared by the terminal screen (roadmap P3.1) and the host-facing
 * Binder (P2.4): [plan] does the blocking work on a worker thread (directory resolution per D18,
 * Node.js resolution per D17 with archive install and `usr/bin` links), [start] creates the
 * session on the main thread from the plan. Both callers therefore show the same banner reasons
 * and export the same environment.
 *
 * zh-CN: 终端界面 (P3.1) 与宿主 Binder (P2.4) 共用的两步会话启动: [plan] 在工作线程完成阻塞工作 (D18 目录解析,
 * D17 Node.js 解析含资产安装与 `usr/bin` 链接), [start] 在主线程按计划创建会话. 两类调用方因此显示同样的横幅原因,
 * 导出同样的环境变量.
 */
object SessionAssembly {

    /**
     * @param directory   where the session starts and why it may have fallen back
     * @param node        Node.js toolchain outcome; `environment` is empty when unavailable
     * @param environment variables laid over the session environment (currently the Node.js set)
     */
    data class Plan(
        val paths: TerminalPaths,
        val directory: StorageAccess.Resolved,
        val node: TerminalNodeSetup.Prepared,
        val environment: Map<String, String>,
    ) {
        val nodeResolution: NodeCliLocator.Resolution get() = node.resolution
    }

    /**
     * Blocking; call it off the main thread.
     *
     * @param requestedDirectory absolute directory from the host or the UI, null for `$HOME`
     * @param refreshNode        re-run the launcher probe even when cached
     * zh-CN: 阻塞调用, 勿在主线程使用.
     */
    @JvmStatic
    @JvmOverloads
    fun plan(
        context: Context,
        requestedDirectory: String?,
        preferences: TerminalPreferences = TerminalPreferences(context),
        refreshNode: Boolean = false,
    ): Plan {
        val app = context.applicationContext
        val paths = TerminalPaths.of(app).ensureLayout()
        val directory = StorageAccess.resolveDirectory(app, requestedDirectory, paths.home)
        val resolution = NodeCliLocator.resolve(app, refreshNode, preferences.nodeIntegrationEnabled)
        val node = TerminalNodeSetup.prepare(app, paths, resolution, preferences.nodeEnvironmentOptions())
        return Plan(paths, directory, node, node.environment)
    }

    /**
     * Main thread: creates the session the plan describes.
     *
     * @param command          command line run inside the directory before the interactive shell (appendix D wrapper), null for a plain shell
     * @param extraEnvironment request-level overrides laid over the plan's environment; null removes a variable
     * @param keepOpen         keep the interactive shell after [command] finishes; false exits with the command's status
     * @param id               a reserved session id (Binder pending sessions), otherwise a fresh one
     * zh-CN: 主线程, 按计划创建会话; [command] 在目录内先于交互式 shell 执行 (附录 D 包装), [extraEnvironment] 为请求级覆盖,
     * null 值删除变量; [keepOpen] 为 false 时命令结束即以其状态退出; [id] 为预留的会话 id.
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        context: Context,
        plan: Plan,
        title: String? = null,
        command: String? = null,
        extraEnvironment: Map<String, String?> = emptyMap(),
        keepOpen: Boolean = true,
        id: String? = null,
    ): TerminalSessionManager.Session {
        val environment = LinkedHashMap<String, String?>(plan.environment)
        environment.putAll(extraEnvironment)
        val directory = plan.directory.directory.path
        val argv = TerminalSessionLauncher.buildCommand(directory, command, keepOpen)
        return TerminalSessionManager.create(context, directory, environment, title, command, id, argv)
    }

}
