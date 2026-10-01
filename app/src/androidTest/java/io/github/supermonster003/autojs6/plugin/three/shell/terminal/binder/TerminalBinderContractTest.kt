package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import android.content.ComponentName
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPlugin
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.ThreeShellTerminalPluginService
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.SessionAssembly
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPaths
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalSessionManager
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.service.SessionNotifications
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.terminal.api.ITerminalCallback
import org.autojs.plugin.terminal.api.ITerminalPlugin
import org.autojs.plugin.terminal.api.TerminalCapabilityKeys
import org.autojs.plugin.terminal.api.TerminalContract
import org.autojs.plugin.terminal.api.TerminalErrorCodes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * Roadmap P2.4 `TerminalBinderContractTest`: the `ITerminalPlugin` router against the frozen contract.
 * Most cases drive an in-process router with a permissive guard (the instrumentation runs under the
 * plugin uid, which the production `HostCallerGuard` must refuse); real transactions cross into the
 * debug `:binder_test` endpoint, and host death is played by the debug `:binder_death_client` probe.
 * zh-CN: P2.4 契约测试: 多数用例驱动进程内的宽松守卫路由 (测试以插件 uid 运行, 生产守卫必须拒绝);
 * 真实事务进入调试 `:binder_test` 端点, 宿主死亡由调试 `:binder_death_client` 探针扮演.
 */
@RunWith(AndroidJUnit4::class)
class TerminalBinderContractTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private val home: String get() = context.filesDir.path

    private lateinit var binder: TerminalPluginBinder
    private val events = RecordingCallback()

    @Before
    fun setUp() {
        instrumentation.runOnMainSync { TerminalSessionManager.closeAll() }
        waitUntil("no sessions before the test") { TerminalSessionManager.allSessions.isEmpty() }
        // A cold Node resolution (or a not yet extracted npm archive) makes every first open `pending`; the
        // pending path has its own test, so the toolchain is prepared once up front like a first session would.
        SessionAssembly.plan(context, null)
        binder = TerminalPluginBinder(context, PermissiveGuard)
        binder.registerCallback(events)
    }

    @After
    fun tearDown() {
        binder.closeAllSessions()
        binder.close()
        waitUntil("no sessions after the test") { TerminalSessionManager.allSessions.isEmpty() }
    }

    @Test
    fun capabilitiesAndEnvironmentDeclareTheFrozenContract() {
        val capabilities = binder.capabilities
        assertEquals(
            setOf(
                PluginCapabilityKeys.REQUIRES_HOST_VERSION,
                TerminalCapabilityKeys.CONTRACT_VERSION,
                TerminalCapabilityKeys.FEATURES_KEY,
                TerminalCapabilityKeys.MAX_SESSIONS,
                TerminalCapabilityKeys.MAX_SUBSCRIPTIONS,
                TerminalCapabilityKeys.NODE_CLI,
            ),
            capabilities.keySet(),
        )
        assertEquals(ThreeShellTerminalPlugin.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
        assertEquals(TerminalContract.CONTRACT_VERSION, capabilities.getInt(TerminalCapabilityKeys.CONTRACT_VERSION))
        assertEquals(
            listOf(TerminalCapabilityKeys.FEATURE_NODE_CLI, TerminalCapabilityKeys.FEATURE_OUTPUT_SUBSCRIPTION, TerminalCapabilityKeys.FEATURE_TRANSCRIPT),
            capabilities.getStringArray(TerminalCapabilityKeys.FEATURES_KEY)?.toList(),
        )
        assertEquals(TerminalContract.MAX_SESSIONS, capabilities.getInt(TerminalCapabilityKeys.MAX_SESSIONS))
        assertEquals(TerminalContract.MAX_SUBSCRIPTIONS_PER_SESSION, capabilities.getInt(TerminalCapabilityKeys.MAX_SUBSCRIPTIONS))
        val nodeState = capabilities.getString(TerminalCapabilityKeys.NODE_CLI)
        assertTrue("unknown node state $nodeState", TerminalContract.isNodeCliState(nodeState))
        assertEquals(capabilities.keySet(), binder.info.capabilities?.keySet())

        val environment = JsonParser.parseString(binder.environment).asJsonObject
        assertEquals(TerminalContract.CONTRACT_VERSION, environment["contractVersion"].asInt)
        assertEquals(TerminalPaths.of(context).home.path, environment["home"].asString)
        assertEquals(TerminalPaths.of(context).prefix.path, environment["prefix"].asString)
        assertEquals("/system/bin/sh", environment["shell"].asString)
        assertTrue(environment["npmRegistry"].asString.startsWith("https://"))
        assertTrue(
            environment["storageAccess"].asString in setOf(
                TerminalContract.STORAGE_ACCESS_GRANTED,
                TerminalContract.STORAGE_ACCESS_DENIED,
                TerminalContract.STORAGE_ACCESS_NOT_APPLICABLE,
            ),
        )
        assertEquals(context.packageManager.getPackageInfo(context.packageName, 0).versionName, environment["pluginVersion"].asString)
        val node = environment["node"].asJsonObject
        assertEquals(nodeState == TerminalContract.NODE_CLI_AVAILABLE, node["available"].asBoolean)
        assertTrue("an unavailable toolchain names its reason", node["available"].asBoolean || node["reason"].asString == nodeState)
    }

    @Test
    fun happyPathOpenWriteReadClose() {
        val opened = json(binder.openSession("""{"cwd":"$home","title":"contract"}"""))
        assertTrue(opened["alive"].asBoolean)
        assertTrue(opened["exitCode"].isJsonNull)
        assertEquals("contract", opened["title"].asString)
        val id = opened["id"].asString
        awaitRunning(binder, opened)
        assertEquals(listOf(id), sessionIds(binder.listSessions()))
        waitUntil("the sessions change reached the callback") { events.lastSessions?.let { sessionIds(it) } == listOf(id) }

        val subscription = binder.subscribeOutput(id, """{"fromStart":true}""")
        assertNull(subscription.getString(TerminalContract.KEY_ERROR_JSON))
        assertTrue(subscription.getString(TerminalContract.KEY_SUBSCRIPTION_ID)!!.isNotBlank())
        val reader = PipeReader(descriptorOf(subscription))
        binder.writeInput(id, "printf '__%s__\\n' ok\n".toByteArray())
        reader.awaitText("the pipe carries the shell output") { it.contains("__ok__") }

        val transcript = binder.readTranscript(id, TerminalContract.MAX_TRANSCRIPT_BYTES)
        assertNull(transcript.getString(TerminalContract.KEY_ERROR_JSON))
        assertTrue(transcript.getString(TerminalContract.KEY_TEXT)!!.contains("__ok__"))
        assertFalse(transcript.getBoolean(TerminalContract.KEY_TRUNCATED))
        val tail = binder.readTranscript(id, 4)
        assertTrue(tail.getBoolean(TerminalContract.KEY_TRUNCATED))
        assertTrue(tail.getString(TerminalContract.KEY_TEXT)!!.toByteArray().size <= 4)

        assertTrue(binder.closeSession(id))
        events.awaitExit(id)
        reader.awaitEof("the write end closes once the session is gone")
        waitUntil("the session list is empty again") { sessionIds(binder.listSessions()).isEmpty() }
        assertFalse(binder.closeSession(id))
        assertEquals(TerminalErrorCodes.SESSION_NOT_FOUND, errorOf(binder.subscribeOutput(id, "{}"))["code"].asString)
    }

    @Test
    fun hostileRequestsAreRefusedWithoutCrashing() {
        fun code(request: String) = json(binder.openSession(request))["code"]?.asString
        assertEquals(TerminalErrorCodes.INVALID_ARGUMENT, code("not json"))
        assertEquals(TerminalErrorCodes.INVALID_ARGUMENT, code("[]"))
        assertEquals(TerminalErrorCodes.INVALID_ARGUMENT, code("""{"cwd":"   "}"""))
        assertEquals(TerminalErrorCodes.INVALID_ARGUMENT, code("""{"command":"${"x".repeat(TerminalContract.MAX_COMMAND_BYTES + 1)}"}"""))
        assertEquals(TerminalErrorCodes.INVALID_ARGUMENT, code("""{"env":{"A=B":"1"}}"""))
        assertEquals(TerminalErrorCodes.INVALID_ARGUMENT, code("""{"title":""}"""))
        assertEquals(TerminalErrorCodes.INVALID_ARGUMENT, code("""{"keepOpen":"yes"}"""))
        assertEquals(TerminalErrorCodes.DIRECTORY_INACCESSIBLE, code("""{"cwd":"$home/definitely-missing"}"""))
        assertEquals(TerminalErrorCodes.DIRECTORY_INACCESSIBLE, code("""{"cwd":"relative/path"}"""))

        assertEquals(TerminalErrorCodes.SESSION_NOT_FOUND, errorOf(binder.subscribeOutput("missing", "{}"))["code"].asString)
        assertEquals(TerminalErrorCodes.INVALID_ARGUMENT, errorOf(binder.subscribeOutput("", "{}"))["code"].asString)
        assertEquals(TerminalErrorCodes.SESSION_NOT_FOUND, errorOf(binder.readTranscript("missing", 1024))["code"].asString)
        assertEquals(TerminalErrorCodes.INVALID_ARGUMENT, errorOf(binder.readTranscript("missing", 0))["code"].asString)
        binder.writeInput("missing", "ignored\n".toByteArray())
        binder.writeInput(null, null)
        binder.unsubscribeOutput("missing")
        binder.unsubscribeOutput(null)
        assertFalse(binder.closeSession("missing"))
        assertFalse(binder.closeSession(null))
        val oversized = assertThrows(IllegalArgumentException::class.java) { binder.writeInput("missing", ByteArray(TerminalContract.MAX_INPUT_BYTES + 1)) }
        assertTrue(oversized.message!!.startsWith(TerminalErrorCodes.INVALID_ARGUMENT + ":"))
        assertThrows(IllegalArgumentException::class.java) { binder.registerCallback(null) }
        assertTrue(sessionIds(binder.listSessions()).isEmpty())
        assertEquals(0, binder.closeAllSessions())
    }

    @Test
    fun theSeventeenthSessionHitsTheSessionLimit() {
        val ids = (1..TerminalContract.MAX_SESSIONS).map { openPlainSession() }
        assertEquals(TerminalContract.MAX_SESSIONS, ids.toSet().size)
        assertEquals(TerminalErrorCodes.SESSION_LIMIT, json(binder.openSession("{}"))["code"].asString)
        assertEquals(TerminalContract.MAX_SESSIONS, sessionIds(binder.listSessions()).size)
        assertTrue(binder.closeSession(ids.first()))
        waitUntil("the closed session leaves the list") { sessionIds(binder.listSessions()).size == TerminalContract.MAX_SESSIONS - 1 }
        openPlainSession()
        assertEquals(TerminalContract.MAX_SESSIONS, binder.closeAllSessions())
        waitUntil("every session is gone") { sessionIds(binder.listSessions()).isEmpty() }
    }

    @Test
    fun theFifthSubscriptionHitsTheSubscriptionLimit() {
        val id = openPlainSession()
        val subscriptions = (1..TerminalContract.MAX_SUBSCRIPTIONS_PER_SESSION).map {
            binder.subscribeOutput(id, "{}").also { assertNull(it.getString(TerminalContract.KEY_ERROR_JSON)) }
        }
        val refused = errorOf(binder.subscribeOutput(id, "{}"))
        assertEquals(TerminalErrorCodes.SUBSCRIPTION_LIMIT, refused["code"].asString)
        assertEquals(id, refused["sessionId"].asString)
        assertEquals(TerminalContract.MAX_SUBSCRIPTIONS_PER_SESSION, binder.openSubscriptionIds(id).size)
        binder.unsubscribeOutput(subscriptions.first().getString(TerminalContract.KEY_SUBSCRIPTION_ID))
        val replacement = binder.subscribeOutput(id, "{}")
        assertNull("unsubscribing frees a slot at once", replacement.getString(TerminalContract.KEY_ERROR_JSON))
        (subscriptions + replacement).forEach { descriptorOf(it).close() }
    }

    @Test
    fun callbacksAreCappedAtEightAndKeyedByBinder() {
        val extra = (2..TerminalContract.MAX_CALLBACKS).map { RecordingCallback().also(binder::registerCallback) }
        binder.registerCallback(extra.first())
        assertEquals(TerminalContract.MAX_CALLBACKS, binder.callbackCount)
        val refused = assertThrows(IllegalArgumentException::class.java) { binder.registerCallback(RecordingCallback()) }
        assertTrue(refused.message!!.startsWith(TerminalErrorCodes.INVALID_ARGUMENT + ":"))
        binder.unregisterCallback(extra.first())
        assertEquals(TerminalContract.MAX_CALLBACKS - 1, binder.callbackCount)
        binder.registerCallback(RecordingCallback())
        assertEquals(TerminalContract.MAX_CALLBACKS, binder.callbackCount)
        val id = openPlainSession()
        waitUntil("every callback hears about the new session") { extra.drop(1).all { it.lastSessions?.let(::sessionIds) == listOf(id) } }
        assertTrue(binder.closeSession(id))
        extra.drop(1).forEach { it.awaitExit(id) }
    }

    @Test
    fun aSlowToolchainPlanYieldsAPendingSessionThatCompletesAndFlushesQueuedWork() {
        val release = CountDownLatch(1)
        val slow = TerminalPluginBinder(context, PermissiveGuard) { ctx, cwd, preferences, generation ->
            check(release.await(10, TimeUnit.SECONDS))
            SessionAssembly.plan(ctx, cwd, preferences, dataGeneration = generation)
        }
        val slowEvents = RecordingCallback()
        try {
            slow.registerCallback(slowEvents)
            val opened = json(slow.openSession("""{"cwd":"$home","title":"pending"}"""))
            assertEquals(opened.toString(), TerminalContract.STATE_PENDING, opened["state"].asString)
            assertTrue(opened["alive"].asBoolean)
            assertEquals(-1, opened["pid"].asInt)
            val id = opened["id"].asString
            val subscription = slow.subscribeOutput(id, "{}")
            assertNull("a pending session accepts subscriptions", subscription.getString(TerminalContract.KEY_ERROR_JSON))
            val reader = PipeReader(descriptorOf(subscription))
            slow.writeInput(id, "printf '__%s__\\n' queued\n".toByteArray())
            val waiting = slow.readTranscript(id, 1024)
            assertEquals("", waiting.getString(TerminalContract.KEY_TEXT))
            release.countDown()
            waitUntil("the pending session starts running") {
                sessions(slow.listSessions()).any { it["id"].asString == id && it["state"].asString == TerminalContract.STATE_RUNNING }
            }
            reader.awaitText("queued input runs once the shell is up") { it.contains("__queued__") }
            assertTrue(slow.closeSession(id))
            slowEvents.awaitExit(id)
            reader.awaitEof("the pipe closes with the session")

            val cancelled = json(slow.openSession("""{"cwd":"$home"}"""))["id"].asString
            assertTrue(slow.closeSession(cancelled))
            slowEvents.awaitExit(cancelled)
            waitUntil("a cancelled pending session never materialises") {
                sessionIds(slow.listSessions()).isEmpty() && TerminalSessionManager.get(cancelled) == null
            }
            SystemClock.sleep(500)
            assertNull("the cancelled plan must not start a session later", TerminalSessionManager.get(cancelled))
        } finally {
            release.countDown()
            slow.closeAllSessions()
            slow.close()
        }
    }

    @Test
    fun theProductionServiceRefusesThePluginUidAsHost() {
        val bound = try {
            bind(ComponentName(context, ThreeShellTerminalPluginService::class.java))
        } catch (_: SecurityException) {
            // Without an installed host the PLUGIN permission is undefined and the platform already refuses the bind.
            return
        }
        bound.use { connection ->
            val plugin = ITerminalPlugin.Stub.asInterface(connection.binder)
            assertTrue(plugin is TerminalPluginBinder)
            assertThrows(SecurityException::class.java) { plugin.listSessions() }
            assertThrows(SecurityException::class.java) { plugin.capabilities }
            assertThrows(SecurityException::class.java) { plugin.openSession("{}") }
            assertThrows(SecurityException::class.java) { plugin.registerCallback(events) }
        }
    }

    @Test
    fun anUnprivilegedTestPackageCannotBindTheProductionService() {
        val testPackage = instrumentation.context.packageName
        bind(ComponentName(testPackage, UnprivilegedClientService::class.java.name)).use { client ->
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                assertTrue(client.binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0))
                assertTrue("the client runs under the test APK uid", reply.readInt() != Process.myUid())
                assertEquals("the test package has no PLUGIN permission", 1, reply.readInt())
                assertEquals("the production bind is refused by Android", 1, reply.readInt())
            } finally {
                data.recycle()
                reply.recycle()
            }
        }
    }

    @Test
    fun pendingInputIsBoundedAndCancellationDoesNotStartTheShell() {
        val release = CountDownLatch(1)
        val slow = TerminalPluginBinder(context, PermissiveGuard) { ctx, cwd, preferences, generation ->
            check(release.await(10, TimeUnit.SECONDS))
            SessionAssembly.plan(ctx, cwd, preferences, dataGeneration = generation)
        }
        try {
            val started = SystemClock.elapsedRealtime()
            val id = json(slow.openSession("{}"))["id"].asString
            assertTrue("open must not wait for the blocked worker", SystemClock.elapsedRealtime() - started < 1_000)
            slow.writeInput(id, ByteArray(TerminalContract.MAX_INPUT_BYTES))
            val overflow = assertThrows(IllegalArgumentException::class.java) { slow.writeInput(id, byteArrayOf(1)) }
            assertTrue(overflow.message!!.startsWith(TerminalErrorCodes.INVALID_ARGUMENT + ":"))
            val reader = PipeReader(descriptorOf(slow.subscribeOutput(id, "{}")))
            assertTrue(slow.closeSession(id))
            reader.awaitEof("cancelling a pending session closes its pipe")
            release.countDown()
            SystemClock.sleep(500)
            assertNull(TerminalSessionManager.get(id))
        } finally {
            release.countDown()
            slow.closeAllSessions()
            slow.close()
        }
    }

    @Test
    fun aCommandDeliversItsTailAndExitCodeBeforeThePipeCloses() {
        val opened = json(binder.openSession("""{"cwd":"$home","command":"sleep 1; printf '__%s__' final; exit 7","keepOpen":false}"""))
        val id = opened["id"].asString
        val reader = PipeReader(descriptorOf(binder.subscribeOutput(id, "{\"fromStart\":true}")))
        events.awaitExit(id)
        reader.awaitEof("the command's output is drained on exit")
        assertTrue(reader.text(), reader.text().contains("__final__"))
        assertEquals(7, events.exits.first { it.first == id }.second)
    }

    @Test
    fun realTransactionsCrossTheProcessBoundary(): Unit = bind(ComponentName(context, TerminalBinderTestService::class.java)).use { endpoint ->
        val plugin = ITerminalPlugin.Stub.asInterface(endpoint.binder)
        assertFalse("the endpoint lives in :binder_test", plugin is TerminalPluginBinder)
        assertEquals(TerminalContract.CONTRACT_VERSION, plugin.capabilities.getInt(TerminalCapabilityKeys.CONTRACT_VERSION))
        val remoteEvents = RecordingCallback()
        plugin.registerCallback(remoteEvents)
        val opened = json(plugin.openSession("""{"cwd":"$home","title":"remote"}"""))
        val id = opened["id"].asString
        // The endpoint process starts cold, so its first open may legitimately be `pending`.
        awaitRunning(plugin, opened)
        try {
            val subscription = plugin.subscribeOutput(id, "{}")
            assertNull(subscription.getString(TerminalContract.KEY_ERROR_JSON))
            val reader = PipeReader(descriptorOf(subscription))
            plugin.writeInput(id, "printf '__%s__\\n' remote\n".toByteArray())
            reader.awaitText("output crosses the process boundary") { it.contains("__remote__") }
            val oversized = assertThrows(IllegalArgumentException::class.java) { plugin.writeInput(id, ByteArray(TerminalContract.MAX_INPUT_BYTES + 1)) }
            assertTrue(oversized.message!!.startsWith(TerminalErrorCodes.INVALID_ARGUMENT + ":"))
            assertEquals(TerminalErrorCodes.SESSION_NOT_FOUND, errorOf(plugin.readTranscript("missing", 10))["code"].asString)
            assertTrue(plugin.readTranscript(id, 4096).getString(TerminalContract.KEY_TEXT)!!.contains("__remote__"))
            assertTrue(plugin.closeSession(id))
            remoteEvents.awaitExit(id)
            reader.awaitEof("the remote write end closes with the session")
        } finally {
            plugin.closeAllSessions()
            plugin.unregisterCallback(remoteEvents)
        }
    }

    @Test
    fun hostDeathClosesItsSubscriptionsAndDropsItsCallback() {
        val id = openPlainSession()
        bind(ComponentName(context, TerminalDeathProbeService::class.java)).use { probe ->
            val subscriptionId = attachProbe(probe.binder, id)
            assertEquals("the probe's callback joined the test's own", 2, binder.callbackCount)
            assertEquals(listOf(subscriptionId), binder.openSubscriptionIds(id))
            val data = Parcel.obtain()
            try {
                runCatching { probe.binder.transact(TerminalDeathProbeService.CODE_DIE, data, null, IBinder.FLAG_ONEWAY) }
            } finally {
                data.recycle()
            }
            waitUntil("the dead host's callback is removed") { binder.callbackCount == 1 }
            waitUntil("the dead host's subscription write end is closed") { binder.openSubscriptionIds(id).isEmpty() }
        }
        assertEquals(listOf(id), sessionIds(binder.listSessions()))
    }

    @Test
    fun overflowDropsTheOldestBytesAndReportsThem() {
        val reported = AtomicLong()
        val closed = CountDownLatch(1)
        val subscription = OutputSubscription("t", "s", Process.myPid(), capacity = 16, onOverflow = { reported.addAndGet(it) }, onClosed = { closed.countDown() })
        subscription.offer("0123456789".toByteArray())
        subscription.offer("abcdefghijklmnop".toByteArray())
        assertEquals(10L, reported.get())
        subscription.start()
        subscription.finish()
        val delivered = ParcelFileDescriptor.AutoCloseInputStream(subscription.readEnd).use { it.readBytes() }
        assertEquals("abcdefghijklmnop", String(delivered))
        assertTrue(closed.await(5, TimeUnit.SECONDS))
        assertTrue(subscription.isClosed)
        assertFalse(subscription.isActive)
    }

    @Test
    fun cancellingAFullPipeReleasesItsWriter() {
        val closed = CountDownLatch(1)
        val subscription = OutputSubscription("blocked", "s", Process.myPid(), onOverflow = {}, onClosed = { closed.countDown() })
        try {
            subscription.start()
            subscription.offer(ByteArray(TerminalContract.OUTPUT_BUFFER_BYTES))
            SystemClock.sleep(100)
            subscription.abort()
            assertTrue("a non-reading client cannot retain the writer", closed.await(5, TimeUnit.SECONDS))
        } finally {
            subscription.readEnd.close()
            subscription.abort()
        }
    }

    @Test
    fun theForegroundNotificationClosesAllSessionsWithoutAnActivity() {
        assumeTrue("background activity restricted", Build.VERSION.SDK_INT < 28 || !context.getSystemService(ActivityManager::class.java).isBackgroundRestricted)
        assumeTrue("notifications not granted", SessionNotifications.isPermissionGranted(context))
        openPlainSession()
        openPlainSession()
        val manager = context.getSystemService(NotificationManager::class.java)
        waitUntil("the foreground notification appears without an Activity") {
            manager.activeNotifications.any { it.id == SessionNotifications.NOTIFICATION_ID }
        }
        val notification = manager.activeNotifications.first { it.id == SessionNotifications.NOTIFICATION_ID }.notification
        assertTrue(notification.actions.isNotEmpty())
        notification.actions.last().actionIntent.send()
        waitUntil("the notification action closes all sessions") { sessionIds(binder.listSessions()).isEmpty() }
        waitUntil("the notification is removed") { manager.activeNotifications.none { it.id == SessionNotifications.NOTIFICATION_ID } }
    }

    @Test
    fun aLargeTranscriptFitsInARemoteBinderReply(): Unit = bind(ComponentName(context, TerminalBinderTestService::class.java)).use { endpoint ->
        val plugin = ITerminalPlugin.Stub.asInterface(endpoint.binder)
        val opened = json(plugin.openSession("""{"cwd":"$home"}"""))
        awaitRunning(plugin, opened)
        val id = opened["id"].asString
        try {
            val reader = PipeReader(descriptorOf(plugin.subscribeOutput(id, "{}")))
            plugin.writeInput(id, "printf '%0600000d' 0; printf '\\n__%s__\\n' BIG_DONE\n".toByteArray())
            reader.awaitText("the large output has arrived") { it.contains("__BIG_DONE__") }
            val reply = plugin.readTranscript(id, TerminalContract.MAX_TRANSCRIPT_BYTES)
            assertNull(reply.getString(TerminalContract.KEY_ERROR_JSON))
            assertTrue(reply.getBoolean(TerminalContract.KEY_TRUNCATED))
            assertTrue(reply.getString(TerminalContract.KEY_TEXT)!!.contains("__BIG_DONE__"))
        } finally {
            plugin.closeAllSessions()
        }
    }

    // ---- helpers ----------------------------------------------------------------------------

    private fun openPlainSession(): String {
        val opened = json(binder.openSession("""{"cwd":"$home"}"""))
        awaitRunning(binder, opened)
        return opened["id"].asString
    }

    private fun awaitRunning(plugin: ITerminalPlugin, opened: JsonObject) {
        val id = requireNotNull(opened["id"]) { "not a session document: $opened" }.asString
        val state = opened["state"].asString
        assertTrue("unexpected state in $opened", state == TerminalContract.STATE_RUNNING || state == TerminalContract.STATE_PENDING)
        if (state == TerminalContract.STATE_PENDING) {
            waitUntil("session $id leaves the pending state", 60_000) {
                sessions(plugin.listSessions()).any { it["id"].asString == id && it["state"].asString == TerminalContract.STATE_RUNNING }
            }
        }
    }

    private fun attachProbe(probe: IBinder, sessionId: String): String {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeStrongBinder(binder)
            data.writeString(sessionId)
            assertTrue(probe.transact(TerminalDeathProbeService.CODE_ATTACH, data, reply, 0))
            val subscriptionId = reply.readString()
            val error = reply.readString()
            assertNull(error, error)
            return requireNotNull(subscriptionId)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun bind(component: ComponentName): BoundService {
        val ready = CountDownLatch(1)
        var binder: IBinder? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                binder = service
                ready.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        assertTrue("bindService returned false for $component", context.bindService(Intent().setComponent(component), connection, Context.BIND_AUTO_CREATE))
        if (!ready.await(10, TimeUnit.SECONDS)) {
            context.unbindService(connection)
            error("$component did not bind")
        }
        return BoundService(requireNotNull(binder), connection)
    }

    private inner class BoundService(val binder: IBinder, private val connection: ServiceConnection) : Closeable {
        override fun close() = context.unbindService(connection)
    }

    private class RecordingCallback : ITerminalCallback.Stub() {
        val exits = CopyOnWriteArrayList<Pair<String, Int>>()

        @Volatile
        var lastSessions: String? = null

        override fun onSessionsChanged(count: Int, sessionsJson: String?) {
            lastSessions = sessionsJson
        }

        override fun onSessionExited(sessionId: String?, exitCode: Int) {
            exits += sessionId.orEmpty() to exitCode
        }

        override fun onOutputOverflow(sessionId: String?, subscriptionId: String?, droppedBytes: Long) = Unit

        fun awaitExit(id: String): Unit = waitUntil("exit of session $id") { exits.any { it.first == id } }
    }

    private class PipeReader(descriptor: ParcelFileDescriptor) {
        private val received = ByteArrayOutputStream()

        @Volatile
        var eof = false
            private set

        init {
            thread(name = "PipeReader", isDaemon = true) {
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                    val chunk = ByteArray(4096)
                    while (true) {
                        val read = input.read(chunk)
                        if (read < 0) break
                        synchronized(received) { received.write(chunk, 0, read) }
                    }
                }
                eof = true
            }
        }

        fun text(): String = synchronized(received) { received.toString("UTF-8") }

        fun awaitText(what: String, predicate: (String) -> Boolean): Unit = waitUntil(what) { predicate(text()) }

        fun awaitEof(what: String): Unit = waitUntil(what) { eof }
    }

    private object PermissiveGuard : CallerGuard {
        override fun enforceHost(): Int = android.os.Binder.getCallingUid()
    }

    private companion object {
        fun json(document: String): JsonObject = JsonParser.parseString(document).asJsonObject

        fun sessions(document: String): List<JsonObject> = (JsonParser.parseString(document) as JsonArray).map { it.asJsonObject }

        fun sessionIds(document: String): List<String> = sessions(document).map { it["id"].asString }

        fun errorOf(bundle: Bundle): JsonObject = json(requireNotNull(bundle.getString(TerminalContract.KEY_ERROR_JSON)) { "expected an error document" })

        @Suppress("DEPRECATION")
        fun descriptorOf(bundle: Bundle): ParcelFileDescriptor {
            val descriptor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(TerminalContract.KEY_FD, ParcelFileDescriptor::class.java)
            } else {
                bundle.getParcelable(TerminalContract.KEY_FD)
            }
            assertNotNull("the subscription bundle carries the read end", descriptor)
            return requireNotNull(descriptor)
        }

        fun waitUntil(what: String, timeoutMillis: Long = 20_000, condition: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + timeoutMillis
            while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
            assertTrue("Timed out waiting for: $what", condition())
        }
    }

}
