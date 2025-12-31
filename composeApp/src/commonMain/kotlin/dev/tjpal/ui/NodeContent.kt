package dev.tjpal.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.composition.foundation.themes.tokens.TextType
import dev.tjpal.model.ExtendedNodeDefinition
import dev.tjpal.model.NodeInstance
import dev.tjpal.model.NodeType
import dev.tjpal.viewmodel.GraphEditorViewModel

@Composable
fun NodeLabelContent(nodeInstance: NodeInstance, viewModel: GraphEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(nodeInstance.definitionName, type = TextType.PRIMARY)
        }

        StatusIcon(nodeInstance.id, viewModel)
    }
}

@Composable
fun NodeIconContent(nodeInstance: NodeInstance, nodeDefinition: ExtendedNodeDefinition, viewModel: GraphEditorViewModel) {
    val iconBitmap = nodeDefinition.iconImage
    val iconSize = 64.dp

    if(iconBitmap == null) {
        NodeLabelContent(nodeInstance, viewModel)
        return
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            bitmap = iconBitmap,
            contentDescription = nodeDefinition.definition.name,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun NodeContent(node: NodeInstance, nodeDefinition: ExtendedNodeDefinition, viewModel: GraphEditorViewModel) {
    when(nodeDefinition.definition.type) {
        NodeType.TOOL -> NodeIconContent(
            nodeInstance = node,
            nodeDefinition = nodeDefinition,
            viewModel = viewModel
        )
        else -> NodeLabelContent(
            nodeInstance = node,
            viewModel = viewModel
        )
    }
}