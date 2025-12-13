package dev.tjpal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tjpal.composition.foundation.basics.functional.Button
import dev.tjpal.composition.foundation.basics.functional.FloatingBar
import dev.tjpal.composition.foundation.basics.functional.WaitingCircle
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.composition.foundation.structure.graphs.GraphEditor
import dev.tjpal.composition.foundation.templates.FloatingBarTemplate
import dev.tjpal.composition.foundation.templates.WaitingTemplate
import dev.tjpal.composition.foundation.themes.cascade.Cascade
import dev.tjpal.composition.foundation.themes.tokens.ButtonType
import dev.tjpal.composition.foundation.themes.tokens.FloatingBarLocation
import dev.tjpal.composition.foundation.themes.tokens.FloatingBarOrientation
import dev.tjpal.composition.foundation.utilities.zoom.InitialScaleMode
import dev.tjpal.model.NodeDefinition
import dev.tjpal.repository.LoadState
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
fun FunctionToolbar(viewModel: GraphEditorViewModel) {
    data class FunctionalButton(
        val label: String,
        val action: () -> Unit
    )

    val functionalButtons = listOf(
        FunctionalButton("S") { viewModel.save() },
        FunctionalButton("E") { viewModel.switchToEditMode() },
        FunctionalButton("R") { viewModel.switchToExecutionMode() }
    )

    val buttonSize = 48.dp

    val nodeDefinitions by viewModel.nodeDefinitions.collectAsStateWithLifecycle()
    val nodeItems = mutableListOf<@Composable ()-> Unit>()

    when(nodeDefinitions) {
        is LoadState.Loading -> nodeItems.add { WaitingCircle(modifier = Modifier.size(buttonSize)) }
        is LoadState.Error -> nodeItems.add { Text("Error") }
        is LoadState.Ready<List<NodeDefinition>> -> {
            val definitions = (nodeDefinitions as LoadState.Ready<List<NodeDefinition>>).data

            definitions.forEach { definition ->
                nodeItems.add {
                    Button(
                        type = ButtonType.SHY,
                        onClick = { viewModel.insertNode(definition) },
                        modifier = Modifier.size(buttonSize)
                    ) {
                        Text(definition.name)
                    }
                }
            }
        }
    }

    FloatingBar(buttonExtent = buttonSize, orientation = FloatingBarOrientation.HORIZONTAL) {
        group {
            functionalButtons.forEach {
                item {
                    Button(type = ButtonType.SHY, onClick = it.action) {
                        Text(it.label)
                    }
                }
            }
        }
        group {
            nodeItems.forEach {
                item { it() }
            }
        }
    }
}

@Composable
fun GraphEditScreen(viewModel: GraphEditorViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FloatingBarTemplate(
        location = FloatingBarLocation.BOTTOM,
        bandThickness = 48.dp,
        barInset = 8.dp,
        floatingBar = { FunctionToolbar(viewModel) }
    ) {
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
