package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Host-facing terminal service answering `org.autojs.plugin.TERMINAL` (category `terminal`).
 *
 * P0 returns a placeholder Binder that only carries the future `ITerminalPlugin` descriptor:
 * the host can discover and bind the service, but every transaction is rejected until roadmap
 * P2.4 replaces this object with the `ITerminalPlugin.Stub` of the host `terminal-api` module.
 */
class ThreeShellTerminalPluginService : Service() {

    private val binder: IBinder = Binder().apply {
        attachInterface(null, ThreeShellTerminalPlugin.SERVICE_DESCRIPTOR)
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
