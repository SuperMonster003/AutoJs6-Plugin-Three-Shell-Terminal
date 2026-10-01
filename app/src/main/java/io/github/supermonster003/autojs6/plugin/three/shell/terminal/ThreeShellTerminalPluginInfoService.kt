package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginInfo

/** Answers `org.autojs.plugin.INFO` (category `terminal`) for the AutoJs6 plugin center. */
class ThreeShellTerminalPluginInfoService : Service() {

    private val binder = object : IPluginInfoProvider.Stub() {
        override fun getInfo(): PluginInfo = threeShellTerminalPluginRuntimeInfo().toPluginInfo()
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
