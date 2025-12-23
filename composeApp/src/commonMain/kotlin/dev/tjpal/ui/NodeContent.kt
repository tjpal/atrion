package dev.tjpal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tjpal.composition.foundation.basics.functional.Button
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.graph.ActiveGraphService
import dev.tjpal.graph.GraphRepository
import dev.tjpal.model.NodeDefinition
import dev.tjpal.model.NodeInstance
import dev.tjpal.model.NodeType
import dev.tjpal.ui.navigation.ConfigureNodeDialogRoute
import dev.tjpal.ui.navigation.LocalNavController
import dev.tjpal.viewmodel.GraphEditorViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NodeContent(
    node: NodeInstance,
    definition: NodeDefinition,
    activeGraphService: ActiveGraphService,
    graphRepository: GraphRepository,
    viewModel: GraphEditorViewModel = koinViewModel()
) {
    val navController = LocalNavController.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(node.definitionName)

        val activeGraph by activeGraphService.activeGraph.collectAsStateWithLifecycle()
        val isInEditMode = activeGraph == null
        val isProcessorNode = definition.type == NodeType.PROCESSOR

        if(isInEditMode) {
            if(isProcessorNode) {
                Button(onClick = { navController.navigate(ConfigureNodeDialogRoute(node.id)) }) {
                    Text("Configure")
                }
            }

            Button(onClick = { graphRepository.removeNode(node.id) }) {
                Text("Delete")
            }
        } else {
            StatusIcon(node.id, viewModel)
        }
    }
}