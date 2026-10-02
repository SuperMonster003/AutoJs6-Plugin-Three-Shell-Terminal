package io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.settings

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import java.util.concurrent.Executors

/**
 * The four launcher icon aliases of the Three series (roadmap P5.3, icon specification): the choice
 * is persisted by PackageManager as the single enabled alias, never as a duplicate preference, so it
 * survives the plugin process and is visible to the launcher at once. The manifest default is
 * [AUTO]; the real [io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.LauncherActivity]
 * stays enabled behind every alias so explicit intents and shortcuts keep working.
 *
 * zh-CN: Three 系列的四个启动器图标 alias (P5.3, 图标规范): 选择由 PackageManager 以 "恰好一个启用的 alias"
 * 持久化, 不另存重复偏好, 因而与插件进程无关且启动器即时可见. Manifest 默认 [AUTO]; 真实的 `LauncherActivity`
 * 始终保持启用, 显式 Intent 与快捷方式不受影响.
 */
internal enum class LauncherIconMode(val alias: String) {
    LIGHT("AdaptiveLightIconAlias"),
    DARK("AdaptiveDarkIconAlias"),
    AUTO("AdaptiveAutoIconAlias"),
    TRANSPARENT("TransparentIconAlias");

    fun component(context: Context): ComponentName = ComponentName(context.packageName, className(context.packageName))

    /** Fully qualified alias class name, pure so the JVM tests can check it. */
    fun className(packageName: String): String = "$packageName.launcher.$alias"
}

/** Pure state resolution; explicit choices survive a change of the manifest default. zh-CN: 纯状态解析; 显式选择不受 Manifest 默认值变化影响. */
internal object LauncherIconStatePolicy {

    /** The manifest default is the only alias that counts as enabled while in the DEFAULT state. */
    fun enabled(mode: LauncherIconMode, state: Int): Boolean =
        state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && mode == LauncherIconMode.AUTO)

    /**
     * Interrupted or externally edited states have no trustworthy "last selection": prefer an
     * explicit Auto, then the stable visible option order, never the map order; nothing explicit
     * means the manifest default.
     * zh-CN: 被打断或被外部修改的状态没有可信的 "最近选择": 先取显式 Auto, 再按稳定的选项顺序, 都没有则取 Manifest 默认.
     */
    fun resolve(states: Map<LauncherIconMode, Int>): LauncherIconMode {
        val explicit = LauncherIconMode.entries.filter { states[it] == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
        return explicit.firstOrNull { it == LauncherIconMode.AUTO } ?: explicit.firstOrNull() ?: LauncherIconMode.AUTO
    }

    /** True when exactly one alias is effectively enabled, i.e. the launcher shows exactly one entry. */
    fun isNormalized(states: Map<LauncherIconMode, Int>): Boolean =
        LauncherIconMode.entries.count { enabled(it, states[it] ?: PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) } == 1

    /** The component states that make [mode] the only enabled alias. */
    fun statesFor(mode: LauncherIconMode): Map<LauncherIconMode, Int> = LauncherIconMode.entries.associateWith {
        if (it == mode) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

}

/** Reads and switches the aliases; every operation is idempotent and rolls back on failure. zh-CN: 读取与切换 alias; 操作幂等, 失败回滚. */
internal object LauncherIcons {

    private val worker by lazy { Executors.newSingleThreadExecutor { Thread(it, "LauncherIcons").apply { isDaemon = true } } }

    fun snapshot(context: Context): Map<LauncherIconMode, Int> = LauncherIconMode.entries.associateWith {
        context.packageManager.getComponentEnabledSetting(it.component(context))
    }

    fun current(context: Context): LauncherIconMode = LauncherIconStatePolicy.resolve(snapshot(context))

    /** Idempotent repair of default / mixed states after an upgrade; no duplicate preference is stored. */
    @Synchronized
    fun normalize(context: Context) {
        select(context, current(context))
    }

    fun normalizeAsync(context: Context, onComplete: (() -> Unit)? = null) {
        val application = context.applicationContext
        worker.execute {
            try {
                normalize(application)
            } catch (_: Exception) {
                // Keep the prior state; a later launch retries. Nothing about shortcuts is logged.
            } finally {
                onComplete?.invoke()
            }
        }
    }

    /**
     * Makes [mode] the single launcher entry. Shortcut ownership moves before the previous alias
     * disappears; a failure restores the previous states and rethrows.
     * zh-CN: 使 [mode] 成为唯一的启动器入口; 快捷方式归属在旧 alias 消失前迁移; 失败则恢复旧状态并重新抛出.
     */
    @Synchronized
    fun select(context: Context, mode: LauncherIconMode) {
        val pm = context.packageManager
        val before = snapshot(context)
        if (LauncherIconStatePolicy.isNormalized(before) && LauncherIconStatePolicy.enabled(mode, before.getValue(mode))) {
            // A manifest-default upgrade may already show one Auto entry while old mutable shortcuts
            // still belong to the now-disabled alias; repair that ownership too.
            refreshShortcuts(context, mode)
            return
        }
        val previous = LauncherIconStatePolicy.resolve(before)
        try {
            pm.setComponentEnabledSetting(mode.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            refreshShortcuts(context, mode)
            apply(context, LauncherIconStatePolicy.statesFor(mode))
        } catch (failure: Exception) {
            runCatching { apply(context, before) }
            runCatching { refreshShortcuts(context, previous) }
            throw failure
        }
    }

    private fun apply(context: Context, states: Map<LauncherIconMode, Int>) {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.setComponentEnabledSettings(
                states.map { (mode, state) -> PackageManager.ComponentEnabledSetting(mode.component(context), state, PackageManager.DONT_KILL_APP) },
            )
        } else {
            // No atomic batch before API 33: enable the wanted entry first so a launchable component always exists.
            states.entries.sortedBy { if (LauncherIconStatePolicy.enabled(it.key, it.value)) 0 else 1 }.forEach { (mode, state) ->
                pm.setComponentEnabledSetting(mode.component(context), state, PackageManager.DONT_KILL_APP)
            }
        }
    }

    /**
     * Mutable shortcuts (dynamic and pinned, not manifest-declared) are re-parented to the chosen
     * alias; entries Android disabled because the app changed are re-enabled, explicit
     * `disableShortcuts` decisions are left alone.
     * zh-CN: 可变快捷方式 (动态与固定, 非 Manifest 声明) 改挂到所选 alias; 因应用变更被系统禁用的条目恢复启用,
     * 显式 `disableShortcuts` 的决定不改动.
     */
    private fun refreshShortcuts(context: Context, mode: LauncherIconMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val mutable = (manager.dynamicShortcuts + manager.pinnedShortcuts).distinctBy { it.id }
            .filterNot { it.isDeclaredInManifest || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && it.isImmutable) }
        val target = mode.component(context)
        val launchers = LauncherIconMode.entries.map { it.component(context) }.toMutableSet()
        context.packageManager.getActivityInfo(target, 0).targetActivity?.let { launchers += ComponentName(context.packageName, it) }
        val recoverable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mutable.filter {
            !it.isEnabled && it.disabledReason == ShortcutInfo.DISABLED_REASON_APP_CHANGED && it.activity in launchers
        }.map { it.id } else emptyList()
        val updates = mutable.filter { it.activity != target }.map { shortcut ->
            ShortcutInfo.Builder(context, shortcut.id).setActivity(target).apply {
                shortcut.shortLabel?.let(::setShortLabel)
                shortcut.longLabel?.let(::setLongLabel)
                shortcut.intents?.let(::setIntents)
            }.build()
        }
        if (updates.isNotEmpty()) check(manager.updateShortcuts(updates)) { "The launcher could not update shortcut ownership" }
        if (recoverable.isNotEmpty()) manager.enableShortcuts(recoverable)
    }

}

/** Update-only repair of the alias states: no UI, no network, no service. zh-CN: 仅在包更新后修复 alias 状态: 无界面, 无网络, 无服务. */
class LauncherIconUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        LauncherIcons.normalizeAsync(context) { pending.finish() }
    }

}
