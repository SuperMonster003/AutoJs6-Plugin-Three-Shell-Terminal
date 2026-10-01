package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import org.autojs.plugin.terminal.api.ITerminalCallback
import org.autojs.plugin.terminal.api.TerminalContract
import org.autojs.plugin.terminal.api.TerminalErrorCodes

/**
 * The host callbacks of one Binder (D21): at most [TerminalContract.MAX_CALLBACKS], keyed by their
 * Binder so a re-registration is idempotent, removed automatically when their process dies (the
 * owner is told so it can release what else that process held) or when a dispatch finds them dead.
 * zh-CN: 一个 Binder 的宿主回调 (D21): 至多 [TerminalContract.MAX_CALLBACKS], 以 Binder 为键使重复注册幂等, 进程死亡
 * (并通知持有者释放该进程的其他资源) 或分发时发现已死即自动移除.
 */
internal class CallbackRegistry(
    private val limit: Int = TerminalContract.MAX_CALLBACKS,
    private val onDeath: (ownerPid: Int) -> Unit = {},
) {

    private class Entry(val callback: ITerminalCallback, val ownerPid: Int) {
        lateinit var death: IBinder.DeathRecipient
    }

    private val lock = Any()
    private val entries = LinkedHashMap<IBinder, Entry>()

    val size: Int get() = synchronized(lock) { entries.size }

    /**
     * Registers [callback] on behalf of [ownerPid]; a known Binder is ignored, one beyond the limit
     * is refused with `INVALID_ARGUMENT`.
     * zh-CN: 代表 [ownerPid] 注册 [callback]; 已知的 Binder 忽略, 超出上限以 `INVALID_ARGUMENT` 拒绝.
     */
    fun register(callback: ITerminalCallback, ownerPid: Int) {
        val binder = callback.asBinder()
        val entry = Entry(callback, ownerPid)
        entry.death = IBinder.DeathRecipient { if (remove(binder)) onDeath(ownerPid) }
        synchronized(lock) {
            if (binder in entries) return
            if (entries.size >= limit) {
                throw IllegalArgumentException("${TerminalErrorCodes.INVALID_ARGUMENT}: more than $limit callbacks")
            }
            entries[binder] = entry
        }
        try {
            binder.linkToDeath(entry.death, 0)
        } catch (_: RemoteException) {
            if (remove(binder)) onDeath(ownerPid)
        }
    }

    fun unregister(callback: ITerminalCallback): Boolean = remove(callback.asBinder())

    fun clear() {
        val snapshot = synchronized(lock) { entries.keys.toList() }
        snapshot.forEach { remove(it) }
    }

    /** Invokes [action] on every callback; a dead one is dropped, a failing one is logged. zh-CN: 对每个回调执行 [action]; 已死的丢弃, 失败的记录日志. */
    fun dispatch(action: (ITerminalCallback) -> Unit) {
        val snapshot = synchronized(lock) { entries.values.toList() }
        snapshot.forEach { entry ->
            try {
                action(entry.callback)
            } catch (_: RemoteException) {
                if (remove(entry.callback.asBinder())) onDeath(entry.ownerPid)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Callback dispatch failed: ${e.message}")
            }
        }
    }

    private fun remove(binder: IBinder): Boolean {
        val entry = synchronized(lock) { entries.remove(binder) } ?: return false
        runCatching { binder.unlinkToDeath(entry.death, 0) }
        return true
    }

    private companion object {
        const val TAG = "CallbackRegistry"
    }

}
