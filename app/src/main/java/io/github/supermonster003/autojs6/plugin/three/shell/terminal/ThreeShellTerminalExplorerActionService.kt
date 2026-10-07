package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.explorer.TerminalExplorerAction
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenKeys
import org.autojs.plugin.explorer.api.ExplorerArchiveOpenValues
import org.autojs.plugin.explorer.api.IExplorerActionPlugin
import org.autojs.plugin.explorer.api.IExplorerArchiveSession

/**
 * Answers `org.autojs.plugin.EXPLORER_ACTION` for the AutoJs6 file manager (roadmap P8.1, appendix D
 * Q10): the Explorer Action identity and a catalog with the single "Open in terminal" directory
 * action. The archive and host-file-information transactions belong to other action shapes and are
 * never used by this catalog; they answer with an error and close the received descriptor.
 * zh-CN: 为 AutoJs6 文件管理器响应 `org.autojs.plugin.EXPLORER_ACTION` (P8.1, 附录 D Q10): Explorer Action 身份与只含
 * "在终端中打开" 目录动作的目录. 归档与宿主文件信息事务属于其它动作形态, 本目录从不使用; 它们返回错误并关闭收到的描述符.
 */
class ThreeShellTerminalExplorerActionService : Service() {

    private val binder = object : IExplorerActionPlugin.Stub() {

        override fun getInfo(): PluginInfo = TerminalExplorerAction.pluginInfo(this@ThreeShellTerminalExplorerActionService)

        override fun getActionCatalog(): Bundle = TerminalExplorerAction.catalog()

        override fun openArchive(source: ParcelFileDescriptor?, request: Bundle?): IExplorerArchiveSession? {
            closeQuietly(source)
            return null
        }

        override fun openArchiveV11(source: ParcelFileDescriptor?, request: Bundle?): Bundle {
            closeQuietly(source)
            return Bundle().apply {
                putInt(ExplorerArchiveOpenKeys.ERROR_CODE, ExplorerArchiveOpenValues.ERROR_UNRECOVERABLE)
                putString(ExplorerArchiveOpenKeys.ERROR_MESSAGE, "3-Shell Terminal does not open archives")
            }
        }

        override fun getHostFileInfo(source: ParcelFileDescriptor?, request: Bundle?): Bundle? {
            closeQuietly(source)
            return null
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun closeQuietly(descriptor: ParcelFileDescriptor?) {
        runCatching { descriptor?.close() }
    }

}
