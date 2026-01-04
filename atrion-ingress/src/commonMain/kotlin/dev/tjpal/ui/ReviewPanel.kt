package dev.tjpal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tjpal.composition.foundation.basics.functional.Button
import dev.tjpal.composition.foundation.basics.functional.MultiLineInput
import dev.tjpal.composition.foundation.basics.spacing.HorizontalDivider
import dev.tjpal.composition.foundation.basics.text.Link
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.model.ReviewDecision
import dev.tjpal.model.ReviewRecord
import dev.tjpal.model.ReviewStatus
import dev.tjpal.viewmodel.ReviewUiState
import dev.tjpal.viewmodel.ReviewViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReviewPanel(viewModel: ReviewViewModel = koinViewModel()) {
    val uiState by viewModel.state.collectAsState(initial = ReviewUiState.Loading)

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    var selectedReviewId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState) {
        if (uiState is ReviewUiState.Ready && selectedReviewId == null) {
            val list = (uiState as ReviewUiState.Ready).reviews.filter { it.status == ReviewStatus.PENDING }
            selectedReviewId = list.firstOrNull()?.reviewId
        }
    }

    Row(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (uiState) {
                is ReviewUiState.Loading -> {
                    Text("Loading reviews...")
                }
                is ReviewUiState.Error -> {
                    val err = (uiState as ReviewUiState.Error).throwable
                    Text("Failed to load reviews: ${err.message}")
                }
                is ReviewUiState.Ready -> {
                    val reviews = (uiState as ReviewUiState.Ready).reviews
                    ReviewsSidebar(
                        reviews = reviews,
                        selectedId = selectedReviewId,
                        onSelect = { id -> selectedReviewId = id },
                        onRefresh = { viewModel.refresh() }
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth(0.7f).padding(8.dp)) {
            when (uiState) {
                is ReviewUiState.Loading -> {
                    Text("Loading review details...")
                }
                is ReviewUiState.Error -> {
                    val err = (uiState as ReviewUiState.Error).throwable
                    Text("Error: ${err.message}")
                }
                is ReviewUiState.Ready -> {
                    val reviews = (uiState as ReviewUiState.Ready).reviews
                    val selected = selectedReviewId?.let { id -> reviews.firstOrNull { it.reviewId == id } }
                    if (selected != null) {
                        ReviewDetails(
                            review = selected,
                            onSubmitDecision = { decision, reviewComment ->
                                viewModel.submitDecision(selected.reviewId, decision, reviewComment)
                            }
                        )
                    } else {
                        Text("No review selected")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewsSidebar(reviews: List<ReviewRecord>, selectedId: String?, onSelect: (String) -> Unit, onRefresh: () -> Unit) {
    Button(onClick = onRefresh) {
        Text("Refresh")
    }

    if(reviews.isEmpty()) {
        Text("No reviews available")
        return
    }

    reviews.forEach {
        Link(text = it.reviewId, onClick = { onSelect(it.reviewId) })
    }
}

@Composable
private fun ReviewDetails(review: ReviewRecord, onSubmitDecision: (decision: ReviewDecision, reviewComment: String) -> Unit) {
    Text("Instructions:")
    Text(review.reviewInstructions)
    HorizontalDivider()

    Text("Existing decision description:")
    Text(review.reviewDecisionDescription)
    HorizontalDivider()

    var reviewComment by remember { mutableStateOf("") }

    MultiLineInput(
        modifier = Modifier.fillMaxWidth(),
        value = reviewComment,
        onValueChange = { reviewComment = it },
        numVisibleLines = 12
    )

    HorizontalDivider()

    DecisionButtons(
        onSubmit = { decision -> onSubmitDecision(decision, reviewComment) }
    )
}

@Composable
private fun DecisionButtons(onSubmit: (ReviewDecision) -> Unit) {
    Row {
        Button(onClick = { onSubmit(ReviewDecision.ACCEPTED) }) {
            Text("Approve")
        }
        Button(onClick = { onSubmit(ReviewDecision.DECLINED) }) {
            Text("Declined")
        }
        Button(onClick = { onSubmit(ReviewDecision.COMMENTED) }) {
            Text("Request Changes")
        }
    }
}
