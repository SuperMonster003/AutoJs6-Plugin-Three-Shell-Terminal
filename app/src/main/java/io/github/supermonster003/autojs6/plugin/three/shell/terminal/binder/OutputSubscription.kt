package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

import android.os.ParcelFileDescriptor
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.shell.terminal.core.TerminalPtySession
import org.autojs.plugin.terminal.api.TerminalContract
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * One output subscription (D21): a pipe whose read end goes to the host, a writer thread that
 * drains an [OutputBuffer] of at most [TerminalContract.OUTPUT_BUFFER_BYTES] into the write end,
 * and the pty tap that feeds it on the main thread. When the host stops reading, the pipe fills,
 * the buffer overflows, the oldest bytes are dropped and the drop is reported (coalesced to one
 * report per second). The write end drains on session exit and closes immediately on cancellation
 * or host death.
 * zh-CN: 一个输出订阅 (D21): 读端交给宿主的管道, 把至多 [TerminalContract.OUTPUT_BUFFER_BYTES] 的 [OutputBuffer]
 * 排入写端的写线程, 以及在主线程喂入数据的 pty tap. 宿主停止读取时管道填满, 缓冲溢出, 丢弃最旧字节并报告
 * (每秒合并为一次). 会话结束时先刷完缓冲再关闭写端, 取消订阅或宿主消失时立即关闭.
 */
internal class OutputSubscription(
    val id: String,
    val sessionId: String,
    /** Process that asked for the subscription (the host, or this process for local calls). zh-CN: 发起订阅的进程. */
    val ownerPid: Int,
    capacity: Int = TerminalContract.OUTPUT_BUFFER_BYTES,
    private val onOverflow: (droppedBytes: Long) -> Unit,
    private val onClosed: (OutputSubscription) -> Unit,
) : TerminalPtySession.OutputTap {

    private val pipe: Array<ParcelFileDescriptor> = ParcelFileDescriptor.createPipe()

    /** The read end handed to the subscriber; the receiver owns it once it crossed the Binder. zh-CN: 交给订阅方的读端. */
    val readEnd: ParcelFileDescriptor get() = pipe[0]

    private val sink = pipe[1]

    private val buffer = OutputBuffer(capacity)
    private val lock = ReentrantLock()
    private val available = lock.newCondition()
    private var finishing = false
    @Volatile
    private var aborted = false
    private var unreported = 0L
    private var lastReportAt = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val reportTask = Runnable { reportOverflow() }

    @Volatile
    private var attached: TerminalPtySession? = null

    /** True once the write end is closed. zh-CN: 写端已关闭时为 true. */
    @Volatile
    var isClosed: Boolean = false
        private set

    /** Still accepting output (neither finishing nor closed). zh-CN: 仍在接收输出 (既未结束也未关闭). */
    val isActive: Boolean get() = lock.withLock { !finishing && !isClosed }

    private val finishListener: () -> Unit = { finish() }
    private val writer = thread(name = "TerminalOutput-$id", start = false, isDaemon = true) { pump() }

    fun start() = writer.start()

    override fun onOutput(data: ByteArray, offset: Int, count: Int) = offer(data, offset, count)

    fun offer(data: ByteArray, offset: Int = 0, count: Int = data.size) {
        var report = 0L
        lock.withLock {
            if (finishing || isClosed) return
            val dropped = buffer.append(data, offset, count)
            if (dropped > 0) {
                unreported += dropped
                val now = SystemClock.elapsedRealtime()
                if (now - lastReportAt >= OVERFLOW_REPORT_INTERVAL_MS) {
                    report = unreported
                    unreported = 0
                    lastReportAt = now
                }
                handler.removeCallbacks(reportTask)
                if (unreported > 0) handler.postDelayed(reportTask, OVERFLOW_REPORT_INTERVAL_MS)
            }
            available.signalAll()
        }
        if (report > 0) {
            runCatching { onOverflow(report) }.onFailure { Log.w(TAG, "Overflow report for $id failed: ${it.message}") }
        }
    }

    /**
     * Main thread: replays the transcript when asked, then taps the session until it finishes.
     * zh-CN: 主线程: 按需回放 transcript, 然后 tap 会话直至其结束.
     */
    fun attach(pty: TerminalPtySession, fromStart: Boolean) {
        lock.withLock {
            if (finishing || isClosed) return
            attached = pty
            if (fromStart) {
                runCatching { pty.transcriptText }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { offer(it.toByteArray(Charsets.UTF_8)) }
            }
            pty.addOutputTap(this)
            pty.addFinishListener(finishListener)
        }
    }

    /** Stops accepting output; the writer drains what is buffered, then closes the write end. zh-CN: 停止接收输出; 写线程刷完缓冲后关闭写端. */
    fun finish() {
        lock.withLock {
            if (finishing) return
            finishing = true
            detach()
            available.signalAll()
        }
        reportOverflow()
        // A vanished or non-reading subscriber must not retain a full pipe and its writer forever.
        handler.postDelayed({ if (!isClosed) abort() }, DRAIN_TIMEOUT_MS)
    }

    /** Drops what is buffered and closes the write end now: the reader is gone. zh-CN: 丢弃缓冲并立即关闭写端: 读端已消失. */
    fun abort() {
        lock.withLock {
            aborted = true
            finishing = true
            detach()
            buffer.clear()
            available.signalAll()
        }
        runCatching { sink.close() }
        writer.interrupt()
        reportOverflow()
    }

    private fun reportOverflow() {
        val report = lock.withLock {
            handler.removeCallbacks(reportTask)
            unreported.also { unreported = 0; lastReportAt = SystemClock.elapsedRealtime() }
        }
        if (report > 0) runCatching { onOverflow(report) }
    }

    private fun detach() {
        val pty = attached ?: return
        attached = null
        pty.removeOutputTap(this)
        pty.removeFinishListener(finishListener)
    }

    private fun pump() {
        try {
            while (true) {
                val chunk = lock.withLock {
                    while (buffer.isEmpty && !finishing) available.await()
                    buffer.poll(CHUNK_BYTES)
                } ?: break
                var offset = 0
                while (offset < chunk.size && !aborted) {
                    // POLLOUT guarantees space for PIPE_BUF bytes. This is the only writer, so a
                    // write of at most Android/Linux PIPE_BUF (4096) after poll cannot fill and block.
                    // Unlike closing a blocked write, the bounded poll also works on API 24.
                    val poll = StructPollfd().apply {
                        fd = sink.fileDescriptor
                        events = OsConstants.POLLOUT.toShort()
                    }
                    if (Os.poll(arrayOf(poll), WRITE_POLL_MS) > 0 && !aborted) {
                        offset += Os.write(sink.fileDescriptor, chunk, offset, minOf(PIPE_WRITE_BYTES, chunk.size - offset))
                    }
                }
            }
        } catch (_: ErrnoException) {
            // Broken pipe, closed descriptor, or a cancelled poll.
        } catch (_: IOException) {
            // The reader closed its end (or its process died): nothing more can be delivered.
        } catch (_: InterruptedException) {
            // Shutting down.
        } finally {
            runCatching { sink.close() }
            isClosed = true
            lock.withLock { detach() }
            reportOverflow()
            runCatching { onClosed(this) }
        }
    }

    companion object {
        private const val TAG = "OutputSubscription"
        private const val CHUNK_BYTES = 16 * 1024
        private const val OVERFLOW_REPORT_INTERVAL_MS = 1_000L
        private const val DRAIN_TIMEOUT_MS = 5_000L
        private const val WRITE_POLL_MS = 100
        private const val PIPE_WRITE_BYTES = 4096
    }

}
