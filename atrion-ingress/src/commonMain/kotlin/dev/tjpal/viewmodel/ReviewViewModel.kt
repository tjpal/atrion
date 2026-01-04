package dev.tjpal.viewmodel

import androidx.lifecycle.ViewModel
import dev.tjpal.model.ReviewRecord
import dev.tjpal.repo.ReviewRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI visible state of the Reviews screen.
 */
sealed interface ReviewUiState {
    object Loading : ReviewUiState
    data class Ready(val reviews: List<ReviewRecord>) : ReviewUiState
    data class Error(val throwable: Throwable) : ReviewUiState
}

/**
 * Exposes a StateFlow of ReviewUiState (Loading, Ready, Error) and methods to refresh the data.
 */
class ReviewViewModel(
    private val repository: ReviewRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default // Tests can inject a TestDispatcher
): ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state: MutableStateFlow<ReviewUiState> = MutableStateFlow(ReviewUiState.Loading)
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    /**
     * Refreshes reviews by calling the repository. This is launched on the view model scope.
     * Emits Loading, then Ready on success, or Error on failure.
     */
    fun refresh() {
        scope.launch {
            _state.value = ReviewUiState.Loading
            try {
                repository.refresh()
                val list = repository.listReviews()
                _state.value = ReviewUiState.Ready(list)
            } catch (t: Throwable) {
                _state.value = ReviewUiState.Error(t)
            }
        }
    }
}
