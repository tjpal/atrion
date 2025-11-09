package dev.tjpal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tjpal.foundation.basics.functional.Button
import dev.tjpal.foundation.basics.functional.WaitingCircle
import dev.tjpal.foundation.basics.text.Text
import dev.tjpal.foundation.structure.graphs.GraphEditor
import dev.tjpal.foundation.templates.WaitingTemplate
import dev.tjpal.foundation.themes.cascade.Cascade
import dev.tjpal.foundation.themes.tokens.ButtonType
import dev.tjpal.foundation.themes.tokens.TextType
import dev.tjpal.foundation.utilities.zoom.InitialScaleMode
import dev.tjpal.model.GraphDefinition
import dev.tjpal.repository.LoadState
import dev.tjpal.viewmodel.GraphEditorUiState
import dev.tjpal.viewmodel.GraphEditorViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.comparisons.then

@Composable
fun LoadingScreen() {
    WaitingTemplate(text = "Loading ...")
}

@Composable
fun ErrorScreen(errorState: LoadState.Error, viewModel: GraphEditorViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Error: ${errorState.message}",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -(24.dp))
        )

        Button(
            onClick = { viewModel.refresh() },
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 24.dp)
        ) {
            Text(text = "Try again")
        }
    }
}

@Composable
fun GraphEditScreen(viewModel: GraphEditorViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        val gridSpacing = 24.dp

        GraphEditor(
            state = uiState.graphState,
            nodes = uiState.nodes,
            edges = uiState.edges,
            gridSpacing = gridSpacing,
            gridExtension = 2000f,
            initialScaleMode = InitialScaleMode.DEFAULT,
            onConnect = viewModel::onConnect,
            onDisconnect = viewModel::onDisconnect
        )
    }
}

@Composable
fun GraphEditorMainScreen(viewModel: GraphEditorViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when(val state = uiState.loadState) {
        is LoadState.Loading -> LoadingScreen()
        is LoadState.Error -> ErrorScreen(errorState = state, viewModel = viewModel)
        is LoadState.Ready -> GraphEditScreen(viewModel = viewModel)
    }
}

@Composable
fun App() {
    Cascade {
        GraphEditorMainScreen()
    }
}
