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
     * @param extraEnvironment request-level overrides laid over the plan's environment; null removes a variable
     * zh-CN: 主线程, 按计划创建会话; [extraEnvironment] 为请求级覆盖, null 值删除变量.
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        context: Context,
        plan: Plan,
        title: String? = null,
        command: String? = null,
        extraEnvironment: Map<String, String?> = emptyMap(),
    ): TerminalSessionManager.Session {
        val environment = LinkedHashMap<String, String?>(plan.environment)
        environment.putAll(extraEnvironment)
        return TerminalSessionManager.create(context, plan.directory.directory.path, environment, title, command)
    }

}
