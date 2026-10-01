package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Process
import org.autojs.plugin.terminal.api.ITerminalCallback
import org.autojs.plugin.terminal.api.ITerminalPlugin
import org.autojs.plugin.terminal.api.TerminalContract

/**
 * Debug-only stand-in for a host that dies: lives in the `:binder_death_client` process, and on
 * [CODE_ATTACH] registers a callback on the terminal Binder it is handed and subscribes to the
 * given session (keeping the read end open, never reading it); [CODE_DIE] kills the process so the
 * test can observe the death recipient removing the callback and closing the subscription.
 * zh-CN: 仅调试构建的 "会死亡的宿主": 在 `:binder_death_client` 进程中, 收到 [CODE_ATTACH] 后向传入的终端 Binder 注册回调并订阅会话
 * (持有读端, 从不读取); [CODE_DIE] 杀死进程, 供测试观察死亡回调移除回调与关闭订阅.
 */
class TerminalDeathProbeService : Service() {

    private var plugin: ITerminalPlugin? = null
    private var held: ParcelFileDescriptor? = null

    private val callback = object : ITerminalCallback.Stub() {
        override fun onSessionsChanged(count: Int, sessionsJson: String?) = Unit
        override fun onSessionExited(sessionId: String?, exitCode: Int) = Unit
        override fun onOutputOverflow(sessionId: String?, subscriptionId: String?, droppedBytes: Long) = Unit
    }

    private val control = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (Binder.getCallingUid() != Process.myUid()) return false
            return when (code) {
                CODE_ATTACH -> {
                    val target = ITerminalPlugin.Stub.asInterface(data.readStrongBinder())
                    val sessionId = data.readString()
                    plugin = target
                    target.registerCallback(callback)
                    val result = target.subscribeOutput(sessionId, "{}")
                    held = descriptorOf(result)
                    reply?.writeString(result.getString(TerminalContract.KEY_SUBSCRIPTION_ID))
                    reply?.writeString(result.getString(TerminalContract.KEY_ERROR_JSON))
                    true
                }
                CODE_DIE -> {
                    Process.killProcess(Process.myPid())
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = control

    @Suppress("DEPRECATION")
    private fun descriptorOf(bundle: Bundle): ParcelFileDescriptor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        bundle.getParcelable(TerminalContract.KEY_FD, ParcelFileDescriptor::class.java)
    } else {
        bundle.getParcelable(TerminalContract.KEY_FD)
    }

    companion object {
        const val CODE_ATTACH = 1
        const val CODE_DIE = 2
    }

}
