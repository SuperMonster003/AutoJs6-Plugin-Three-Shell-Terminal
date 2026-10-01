package io.github.supermonster003.autojs6.plugin.three.shell.terminal.binder

/**
 * Bounded FIFO of output bytes behind an [OutputSubscription] (D21): chunks are appended as they
 * arrive; once the total exceeds [capacity] the oldest bytes are dropped and counted. Not thread
 * safe on its own; the owner serialises access.
 * zh-CN: [OutputSubscription] 背后的有界字节 FIFO (D21): 分块追加, 总量超过 [capacity] 时丢弃并计数最旧的字节; 自身不保证线程安全, 由持有者串行访问.
 */
internal class OutputBuffer(val capacity: Int) {

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    private val bytes = ByteArray(capacity)
    private var head = 0

    /** Bytes currently buffered. zh-CN: 当前缓冲的字节数. */
    var size: Int = 0
        private set

    /** Bytes dropped since creation. zh-CN: 创建以来丢弃的字节数. */
    var dropped: Long = 0
        private set

    val isEmpty: Boolean get() = size == 0

    /**
     * Appends a copy of the bytes and returns how many of the oldest bytes had to be dropped to
     * stay within [capacity].
     * zh-CN: 追加字节副本, 返回为保持在 [capacity] 内而丢弃的最旧字节数.
     */
    fun append(data: ByteArray, offset: Int, count: Int): Int {
        require(offset >= 0 && count >= 0 && offset <= data.size - count)
        if (count == 0) return 0
        val removed = (size.toLong() + count - capacity).coerceAtLeast(0).toInt()
        val retained = minOf(count, capacity)
        val evicted = minOf(removed, size)
        head = (head + evicted) % capacity
        size -= evicted
        val source = offset + count - retained
        val tail = (head + size) % capacity
        val first = minOf(retained, capacity - tail)
        data.copyInto(bytes, tail, source, source + first)
        data.copyInto(bytes, 0, source + first, source + retained)
        size += retained
        dropped += removed
        return removed
    }

    /** Removes at most [limit] oldest bytes, or null when empty. */
    fun poll(limit: Int = Int.MAX_VALUE): ByteArray? {
        require(limit > 0)
        if (isEmpty) return null
        val take = minOf(size, limit)
        val first = minOf(take, capacity - head)
        val out = ByteArray(take)
        bytes.copyInto(out, 0, head, head + first)
        bytes.copyInto(out, first, 0, take - first)
        head = (head + take) % capacity
        size -= take
        return out
    }

    fun clear() {
        head = 0
        size = 0
    }

}
