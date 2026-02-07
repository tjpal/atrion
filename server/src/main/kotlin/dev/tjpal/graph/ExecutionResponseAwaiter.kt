package dev.tjpal.graph

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * Global registry for pending synchronous REST response based on execution id.
 *
 * - registerAwaiter(executionId) -> returns a CompletableDeferred<OutputRecord> the caller awaits
 * - notifyOutput(executionId, record) -> completes and removes the waiter if present
 * - cancelAwaiter(executionId) -> cancels and removes the waiter (cleanup)
 */
object ExecutionResponseAwaiter {
    private val map: ConcurrentHashMap<String, CompletableDeferred<OutputRecord>> = ConcurrentHashMap()

    fun registerAwaiter(executionId: String): CompletableDeferred<OutputRecord> {
        val deferred = CompletableDeferred<OutputRecord>()
        val previous = map.putIfAbsent(executionId, deferred)
        if (previous != null) {
            throw IllegalStateException("Awaiter already registered for executionId=$executionId")
        }
        return deferred
    }

    fun notifyOutput(executionId: String, record: OutputRecord): Boolean {
        val deferred = map.remove(executionId)
        if (deferred != null) {
            if (!deferred.isCompleted) {
                deferred.complete(record)
            }
            return true
        }
        return false
    }

    fun cancelAwaiter(executionId: String) {
        val deferred = map.remove(executionId)
        deferred?.cancel()
    }
}
