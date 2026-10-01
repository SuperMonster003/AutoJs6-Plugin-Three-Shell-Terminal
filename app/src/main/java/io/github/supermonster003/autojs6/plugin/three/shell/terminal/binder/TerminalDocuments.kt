package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalEnvironment
import org.autojs.plugin.terminal.api.TerminalContract
import org.autojs.plugin.terminal.api.TerminalErrorCodes

/**
 * The JSON documents of protocol V1 on the plugin side: request parsing against the D22 ceilings,
 * and the session / error / environment documents the host's `TerminalJson` parses. Everything is
 * built with Gson's tree API, so no reflection (and no ProGuard rule) is involved.
 * zh-CN: 协议 V1 的插件侧 JSON 文档: 按 D22 上限解析请求, 构造宿主 `TerminalJson` 解析的会话 / 错误 / 环境文档; 全部使用 Gson 树 API, 不涉及反射.
 */
internal object TerminalDocuments {

    /** Plugin-side ceiling for a session title; the contract only bounds the whole request. zh-CN: 插件侧的会话标题上限; 契约仅限制整个请求. */
    const val MAX_TITLE_BYTES = 256

    data class OpenRequest(
        val cwd: String?,
        val command: String?,
        val env: Map<String, String>,
        val title: String?,
        val keepOpen: Boolean,
    )

    data class SessionView(
        val id: String,
        val cwd: String,
        val title: String,
        val createdAt: Long,
        val alive: Boolean,
        val exitCode: Int?,
        val pid: Int,
        val state: String,
    )

    data class NodeView(
        val available: Boolean,
        val version: String?,
        val packageName: String?,
        val pluginVersion: String?,
        val reason: String?,
    )

    /**
     * Parses an `openSession` request. Absent fields take their defaults; a present field that is
     * blank, of the wrong type, or beyond its ceiling is `INVALID_ARGUMENT` (D22).
     * zh-CN: 解析 `openSession` 请求: 缺省字段取默认值; 存在但为空白, 类型错误或超限的字段为 `INVALID_ARGUMENT` (D22).
     */
    @JvmStatic
    fun parseOpenRequest(json: String?): OpenRequest {
        val root = parseObject(json, "request")
        val cwd = root.optionalString(TerminalContract.FIELD_CWD)?.also {
            if (it.isBlank()) invalid("cwd must not be blank")
            if (it.indexOf(NUL) >= 0) invalid("cwd must not contain NUL")
            if (utf8Length(it) > TerminalContract.MAX_PATH_BYTES) invalid("cwd exceeds ${TerminalContract.MAX_PATH_BYTES} bytes")
        }
        val command = root.optionalString(TerminalContract.FIELD_COMMAND)?.also {
            if (it.indexOf(NUL) >= 0) invalid("command must not contain NUL")
            if (utf8Length(it) > TerminalContract.MAX_COMMAND_BYTES) invalid("command exceeds ${TerminalContract.MAX_COMMAND_BYTES} bytes")
        }?.takeIf { it.isNotBlank() }
        val env = parseEnv(root.get(TerminalContract.FIELD_ENV))
        val title = root.optionalString(TerminalContract.FIELD_TITLE)?.also {
            if (it.isBlank()) invalid("title must not be blank")
            if (utf8Length(it) > MAX_TITLE_BYTES) invalid("title exceeds $MAX_TITLE_BYTES bytes")
        }?.trim()
        val keepOpen = root.optionalBoolean(TerminalContract.FIELD_KEEP_OPEN) ?: true
        return OpenRequest(cwd, command, env, title, keepOpen)
    }

    /** `fromStart` of the subscribe options; an absent or empty document means the defaults. zh-CN: 订阅选项的 `fromStart`; 缺省或空文档即默认值. */
    @JvmStatic
    fun parseSubscribeOptions(json: String?): Boolean {
        if (json.isNullOrBlank()) return false
        return parseObject(json, "options").optionalBoolean(TerminalContract.FIELD_FROM_START) ?: false
    }

    @JvmStatic
    fun session(view: SessionView): JsonObject = JsonObject().apply {
        addProperty(TerminalContract.FIELD_ID, view.id)
        addProperty(TerminalContract.FIELD_CWD, view.cwd)
        addProperty(TerminalContract.FIELD_TITLE, shortTitle(view.title))
        addProperty(TerminalContract.FIELD_CREATED_AT, view.createdAt)
        addProperty(TerminalContract.FIELD_ALIVE, view.alive)
        if (view.exitCode != null) {
            addProperty(TerminalContract.FIELD_EXIT_CODE, view.exitCode)
        } else {
            add(TerminalContract.FIELD_EXIT_CODE, JsonNull.INSTANCE)
        }
        addProperty(TerminalContract.FIELD_PID, view.pid)
        addProperty(TerminalContract.FIELD_STATE, view.state)
    }

    @JvmStatic
    fun sessions(views: List<SessionView>): String = JsonArray().apply { views.forEach { add(session(it)) } }.toString()

    @JvmStatic
    @JvmOverloads
    fun error(code: String, message: String, sessionId: String? = null): String = JsonObject().apply {
        addProperty(TerminalContract.FIELD_ERROR_CODE, code)
        addProperty(TerminalContract.FIELD_ERROR_MESSAGE, message)
        if (sessionId != null) addProperty(TerminalContract.FIELD_ERROR_SESSION_ID, sessionId)
    }.toString()

    @JvmStatic
    fun environment(
        home: String,
        prefix: String,
        shell: String,
        node: NodeView,
        npmRegistry: String,
        storageAccess: String,
        pluginVersion: String,
        contractVersion: Int,
    ): String = JsonObject().apply {
        addProperty(TerminalContract.FIELD_HOME, home)
        addProperty(TerminalContract.FIELD_PREFIX, prefix)
        addProperty(TerminalContract.FIELD_SHELL, shell)
        add(
            TerminalContract.FIELD_NODE,
            JsonObject().apply {
                addProperty(TerminalContract.FIELD_NODE_AVAILABLE, node.available)
                addNullable(TerminalContract.FIELD_NODE_VERSION, node.version)
                addNullable(TerminalContract.FIELD_NODE_PACKAGE_NAME, node.packageName)
                addNullable(TerminalContract.FIELD_NODE_PLUGIN_VERSION, node.pluginVersion)
                addNullable(TerminalContract.FIELD_NODE_REASON, node.reason)
            },
        )
        addProperty(TerminalContract.FIELD_NPM_REGISTRY, npmRegistry)
        addProperty(TerminalContract.FIELD_STORAGE_ACCESS, storageAccess)
        addProperty(TerminalContract.FIELD_PLUGIN_VERSION, pluginVersion)
        addProperty(TerminalContract.FIELD_CONTRACT_VERSION, contractVersion)
    }.toString()

    /**
     * Keeps the newest [maxBytes] of UTF-8 text, cut on a character boundary; the flag says whether
     * anything was dropped.
     * zh-CN: 保留最新的 [maxBytes] 字节 UTF-8 文本并在字符边界截断; 标志表示是否有丢弃.
     */
    @JvmStatic
    fun trimTranscript(text: String, maxBytes: Int): Pair<String, Boolean> {
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size <= maxBytes) return text to false
        if (maxBytes <= 0) return "" to true
        var start = bytes.size - maxBytes
        while (start < bytes.size && (bytes[start].toInt() and 0xC0) == 0x80) start++
        return String(bytes, start, bytes.size - start, Charsets.UTF_8) to true
    }

    private fun shortTitle(title: String): String {
        val bytes = title.toByteArray(Charsets.UTF_8)
        if (bytes.size <= MAX_TITLE_BYTES) return title
        var end = MAX_TITLE_BYTES
        while ((bytes[end].toInt() and 0xC0) == 0x80) end--
        return String(bytes, 0, end, Charsets.UTF_8)
    }

    @JvmStatic
    fun utf8Length(text: String): Int = text.toByteArray(Charsets.UTF_8).size

    private const val NUL = '\u0000'
    private val strictJson = GsonBuilder().setStrictness(Strictness.STRICT).create()

    private fun parseObject(json: String?, what: String): JsonObject {
        if (json.isNullOrBlank()) invalid("$what is empty")
        if (utf8Length(json) > TerminalContract.MAX_JSON_BYTES) invalid("$what exceeds ${TerminalContract.MAX_JSON_BYTES} bytes")
        val element = try {
            strictJson.fromJson(json, JsonElement::class.java)
        } catch (_: RuntimeException) {
            invalid("$what is not valid JSON")
        }
        return element.takeIf { it.isJsonObject }?.asJsonObject ?: invalid("$what is not a JSON object")
    }

    private fun parseEnv(element: JsonElement?): Map<String, String> {
        if (element == null || element.isJsonNull) return emptyMap()
        val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: invalid("env must be an object")
        if (obj.size() > TerminalContract.MAX_ENV_ENTRIES) invalid("env has more than ${TerminalContract.MAX_ENV_ENTRIES} entries")
        val env = LinkedHashMap<String, String>()
        for ((name, value) in obj.entrySet()) {
            if (!TerminalEnvironment.isValidName(name)) invalid("env contains an invalid variable name")
            val text = value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                ?: invalid("env value of $name must be a string")
            if (!TerminalEnvironment.isValidValue(text)) invalid("env value of $name must not contain NUL")
            if (utf8Length(name) + 1 + utf8Length(text) > TerminalContract.MAX_ENV_ENTRY_BYTES) {
                invalid("env entry $name exceeds ${TerminalContract.MAX_ENV_ENTRY_BYTES} bytes")
            }
            env[name] = text
        }
        return env
    }

    private fun JsonObject.optionalString(name: String): String? {
        val element = get(name) ?: return null
        if (element.isJsonNull) return null
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isString) invalid("$name must be a string")
        return element.asString
    }

    private fun JsonObject.optionalBoolean(name: String): Boolean? {
        val element = get(name) ?: return null
        if (element.isJsonNull) return null
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isBoolean) invalid("$name must be a boolean")
        return element.asBoolean
    }

    private fun JsonObject.addNullable(name: String, value: String?) {
        if (value != null) addProperty(name, value) else add(name, JsonNull.INSTANCE)
    }

    private fun invalid(message: String): Nothing = throw TerminalFailure(TerminalErrorCodes.INVALID_ARGUMENT, message)

}
