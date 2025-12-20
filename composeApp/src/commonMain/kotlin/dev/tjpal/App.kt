package dev.tjpal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tjpal.composition.foundation.basics.functional.Button
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.composition.foundation.structure.graphs.GraphEditor
import dev.tjpal.composition.foundation.templates.FloatingBarTemplate
import dev.tjpal.composition.foundation.templates.WaitingTemplate
import dev.tjpal.composition.foundation.themes.cascade.Cascade
import dev.tjpal.composition.foundation.themes.tokens.FloatingBarLocation
import dev.tjpal.composition.foundation.utilities.zoom.InitialScaleMode
import dev.tjpal.graph.LoadState
import dev.tjpal.ui.FunctionBar
import dev.tjpal.ui.navigation.Navigation
import dev.tjpal.viewmodel.GraphEditorViewModel
import org.koin.compose.viewmodel.koinViewModel

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

    FloatingBarTemplate(
        location = FloatingBarLocation.BOTTOM,
        bandThickness = 64.dp,
        barInset = 8.dp,
        floatingBar = { FunctionBar(viewModel) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 24.dp

            key(uiState) {
                GraphEditor(
                    state = uiState.graphState,
                    nodes = uiState.nodes,
                    edges = uiState.edges,
                    gridSpacing = gridSpacing,
                    gridExtension = 2000f,
                    initialScaleMode = InitialScaleMode.DEFAULT,
                    onConnect = viewModel::onConnect,
                    onDisconnect = viewModel::onDisconnect,
                    onNodeDragFinished = viewModel::setNodePosition
                )
            }
        }
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
        Navigation()
    }
}
