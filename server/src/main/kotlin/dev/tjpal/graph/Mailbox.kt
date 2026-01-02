package dev.tjpal.graph

import dev.tjpal.nodes.payload.NodePayload
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger

data class MailboxMessage(
    val toConnectorId: String,
    val payload: NodePayload,
    val executionId: String
)

/**
 * Coroutine-backed mailbox implementation using kotlinx.coroutines.Channel.
 */
class Mailbox(capacity: Int = Channel.UNLIMITED) {
    private val channel = Channel<MailboxMessage>(capacity)
    private val counter = AtomicInteger(0)
    private val closed = AtomicInteger(0)

    fun enqueue(message: MailboxMessage): Boolean {
        if (closed.get() == 1) return false
        return try {
            val result = channel.trySend(message)
            if (result.isSuccess) counter.incrementAndGet()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    suspend fun receive(): MailboxMessage {
        val msg = channel.receive()
        counter.decrementAndGet()
        return msg
    }

    fun poll(): MailboxMessage? {
        val result = channel.tryReceive()
        return if (result.isSuccess) {
            counter.decrementAndGet()
            result.getOrNull()
        } else null
    }

    fun isEmpty(): Boolean = counter.get() == 0

    fun close() {
        if (closed.compareAndSet(0, 1)) {
            try {
                channel.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
