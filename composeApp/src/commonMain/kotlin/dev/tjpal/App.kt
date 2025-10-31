package dev.tjpal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tjpal.foundation.structure.graphs.GraphEditor
import dev.tjpal.foundation.themes.cascade.Cascade
import dev.tjpal.foundation.utilities.zoom.InitialScaleMode
import dev.tjpal.viewmodel.GraphEditorViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GraphEditorDemoScreen(viewModel: GraphEditorViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
fun App() {
    Cascade {
        GraphEditorDemoScreen()
    }
}
