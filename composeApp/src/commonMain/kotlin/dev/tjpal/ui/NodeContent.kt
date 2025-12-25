package dev.tjpal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.model.NodeInstance
import dev.tjpal.viewmodel.GraphEditorViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NodeContent(node: NodeInstance, viewModel: GraphEditorViewModel = koinViewModel()) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(node.definitionName)

        StatusIcon(node.id, viewModel)
    }
}