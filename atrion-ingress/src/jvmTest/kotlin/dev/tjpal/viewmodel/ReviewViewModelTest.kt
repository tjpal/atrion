package dev.tjpal.viewmodel

import dev.tjpal.model.ReviewRecord
import dev.tjpal.model.ReviewStatus
import dev.tjpal.repo.ReviewRepository
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewViewModelTest {

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun refresh_success_updatesStateToReady_andCallsRepository() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val sample = ReviewRecord(
            reviewId = "r1",
            graphInstanceId = "g1",
            executionId = "e1",
            nodeId = "n1",
            reviewInstructions = "Do X",
            reviewDecisionDescription = "",
            timestamp = 0L,
            status = ReviewStatus.PENDING
        )

        val repo = mockk<ReviewRepository>()
        coEvery { repo.refresh() } returns Unit
        coEvery { repo.listReviews() } returns listOf(sample)

        val viewModel = ReviewViewModel(repo, dispatcher)
        assertTrue(viewModel.state.value is ReviewUiState.Loading)

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is ReviewUiState.Ready)

        state as ReviewUiState.Ready
        assertEquals(1, state.reviews.size)
        assertEquals("r1", state.reviews[0].reviewId)

        // verify repository calls happened in sequence
        coVerifySequence {
            repo.refresh()
            repo.listReviews()
        }
    }

    @Test
    fun refresh_failure_updatesStateToError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = mockk<ReviewRepository>()
        val exception = RuntimeException("fetch failed")
        coEvery { repo.refresh() } throws exception

        val viewModel = ReviewViewModel(repo, dispatcher)

        assertTrue(viewModel.state.value is ReviewUiState.Loading)

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is ReviewUiState.Error)
        state as ReviewUiState.Error
        assertEquals(exception, state.throwable)
    }
}
