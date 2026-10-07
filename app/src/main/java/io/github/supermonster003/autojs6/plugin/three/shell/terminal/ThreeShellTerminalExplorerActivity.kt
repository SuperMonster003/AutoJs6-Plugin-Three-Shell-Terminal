package io.github.supermonster003.autojs6.plugin.three.shell.terminal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.explorer.ExplorerDirectoryRequest
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ui.TerminalActivity

/**
 * The file manager's "Open in terminal" (roadmap P8.1, appendix D Q10): an exported, invisible
 * forwarder for `org.autojs.plugin.EXPLORER_ACTION_EXECUTE` behind the `org.autojs.permission.PLUGIN`
 * signature permission. It refuses a caller it can name that is not signed like this plugin,
 * validates the protocol v23 request ([ExplorerDirectoryRequest]) and opens the terminal in that
 * directory exactly as the host's former menu item did: the most recent session changes into it, or
 * a new session starts there. The terminal owns its task, so Back returns to the file manager.
 *
 * zh-CN: 文件管理器的 "在终端中打开" (P8.1, 附录 D Q10): 导出的不可见转发器, 响应
 * `org.autojs.plugin.EXPLORER_ACTION_EXECUTE`, 受 `org.autojs.permission.PLUGIN` 签名权限保护. 它拒绝签名不一致的
 * 可识别调用方, 校验协议 v23 请求 ([ExplorerDirectoryRequest]), 并与宿主原菜单项一样在该目录打开终端: 最近的会话
 * 切换到该目录, 或在该目录新建会话. 终端拥有自己的任务, 因此返回键回到文件管理器.
 */
class ThreeShellTerminalExplorerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            forward()
        } finally {
            finish()
        }
    }

    private fun forward() {
        val caller = EntryCaller.identify(this)
        if (!EntryCaller.accepts(this, caller)) {
            Log.w(TAG, "refused ${ThreeShellTerminalPlugin.EXPLORER_EXECUTE_ACTION} from ${caller ?: "an unnamed caller"}")
            return
        }
        // The path stays out of the log (AGENTS.md 9); only the refusal is recorded.
        // zh-CN: 路径不写入日志 (AGENTS.md 9); 只记录拒绝本身.
        val request = ExplorerDirectoryRequest.parse(intent)
        if (request == null) {
            Log.w(TAG, "ignored an ${ThreeShellTerminalPlugin.EXPLORER_EXECUTE_ACTION} request that breaks the directory action contract")
            return
        }
        val target = TerminalActivity.intent(this, request.directory).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(target) }.onFailure { error ->
            Log.w(TAG, "cannot start the terminal: ${error.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "ThreeShellTerminalExplorer"
    }

}
