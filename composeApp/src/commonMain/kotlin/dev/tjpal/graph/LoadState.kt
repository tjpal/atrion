package dev.tjpal.graph

/**
 * LoadState wrapper representing loading / ready / error states for a single value.
 */
sealed class LoadState<out T> {
    object Loading : LoadState<Nothing>()
    data class Ready<T>(val data: T) : LoadState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : LoadState<Nothing>()

    companion object {
        fun <T> ready(data: T): LoadState<T> = Ready(data)
        fun error(message: String, throwable: Throwable? = null): LoadState<Nothing> = Error(message, throwable)
    }
}
