package io.github.supermonster003.autojs6.plugin.three.shell.terminal.core

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Owns the original pty descriptor for one reader and one writer. Polling makes cancellation
 * bounded on API 24, where closing a descriptor does not reliably interrupt a blocked read.
 * The fd stays open until active I/O has returned, preventing reuse by an unrelated new session.
 * No dup is created: the existing JNI fork path closes this same master fd in the child.
 */
internal class PtyIo(private val owned: ParcelFileDescriptor) {
    private val lock = Any()
    private var users = 0
    private var released = false
    @Volatile private var closed = false

    val input: InputStream = object : InputStream() {
        override fun read(): Int {
            val byte = ByteArray(1)
            return if (read(byte, 0, 1) < 0) -1 else byte[0].toInt() and 255
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            checkBounds(buffer, offset, length)
            if (length == 0) return 0
            if (!acquire()) return -1
            try {
                val poll = polling(OsConstants.POLLIN)
                while (!closed) {
                    try {
                        if (Os.poll(arrayOf(poll), 100) > 0 && !closed) {
                            // The only reader consumes the bytes reported ready by poll.
                            val count = Os.read(owned.fileDescriptor, buffer, offset, length)
                            return if (count == 0) -1 else count
                        }
                    } catch (error: ErrnoException) {
                        if (closed || error.errno == OsConstants.EIO) return -1
                        if (error.errno != OsConstants.EINTR) throw IOException(error)
                    }
                }
                return -1
            } finally { releaseUser() }
        }

        override fun close() = this@PtyIo.close()
    }

    val output: OutputStream = object : OutputStream() {
        override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)

        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            checkBounds(buffer, offset, length)
            if (length == 0) return
            if (!acquire()) throw IOException("Terminal closed")
            try {
                val poll = polling(OsConstants.POLLOUT)
                var position = offset
                while (position < offset + length) {
                    if (closed) throw IOException("Terminal closed")
                    try {
                        if (Os.poll(arrayOf(poll), 100) > 0 && !closed) {
                            val count = Os.write(owned.fileDescriptor, buffer, position, minOf(4096, offset + length - position))
                            if (count <= 0) throw IOException("Terminal write made no progress")
                            position += count
                        }
                    } catch (error: ErrnoException) {
                        if (error.errno != OsConstants.EINTR) throw IOException(error)
                    }
                }
            } finally { releaseUser() }
        }

        override fun close() = this@PtyIo.close()
    }

    private fun polling(event: Int) = StructPollfd().apply {
        fd = owned.fileDescriptor
        events = event.toShort()
    }

    private fun acquire(): Boolean = synchronized(lock) {
        if (closed) false else { users++; true }
    }

    private fun releaseUser() = synchronized(lock) {
        users--
        releaseIfClosed()
    }

    private fun close() = synchronized(lock) {
        closed = true
        releaseIfClosed()
    }

    private fun releaseIfClosed() {
        if (closed && users == 0 && !released) {
            released = true
            owned.close()
        }
    }

    private fun checkBounds(buffer: ByteArray, offset: Int, length: Int) {
        if (offset < 0 || length < 0 || offset > buffer.size - length) throw IndexOutOfBoundsException()
    }
}
