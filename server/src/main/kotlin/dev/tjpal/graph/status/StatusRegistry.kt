package dev.tjpal.graph.status

import dev.tjpal.config.Config
import dev.tjpal.logging.logger
import dev.tjpal.model.StatusEntry
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe in-memory registry for node status entries. Keeps a bounded history whose capacity is configured.
 * New entries are broadcast to subscribers via a SharedFlow so consumers can await new events without polling.
 */
@Singleton
class StatusRegistry @Inject constructor(private val config: Config) {
    private val logger = logger<StatusRegistry>()

    private val capacity: Int = config.statusRetentionEntries.takeIf { it > 0 } ?: 10000
    private val history: ArrayDeque<StatusEntry> = ArrayDeque() // Thread safe

    // Should never overflow as long as the client picks up the entries in a timely manner.
    private val updates: MutableSharedFlow<StatusEntry> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun registerStatusEvent(entry: StatusEntry) {
        try {
            synchronized(history) {
                history.addLast(entry)
                while (history.size > capacity) history.removeFirst()
            }

            updates.tryEmit(entry)
        } catch (e: Exception) {
            logger.error("StatusRegistry: failed to register status", e)
        }
    }

    fun getStatuses(): List<StatusEntry> = synchronized(history) {
        history.toList()
    }

    fun getStatusesSince(timestamp: Long): List<StatusEntry> = synchronized(history) {
        history.filter { it.timestamp > timestamp }
    }

    /**
     * Suspends until at least one new status with timestamp > sinceTimestamp is available.
     * Returns all entries that were created after sinceTimestamp. If entries already exist they are returned immediately.
     */
    suspend fun waitForNewStatus(sinceTimestamp: Long): List<StatusEntry> {
        val existing = getStatusesSince(sinceTimestamp)
        if (existing.isNotEmpty()) {
            return existing
        }

        // Suspend until the first update appears that is newer than sinceTimestamp
        try {
            updates.filter { it.timestamp > sinceTimestamp }.first()
        } catch (e: Exception) {
            // Might throw an cancellation. Propagate it up.
            throw e
        }

        // After receiving at least one new entry, return whatever is now present since the timestamp
        return getStatusesSince(sinceTimestamp)
    }
}
