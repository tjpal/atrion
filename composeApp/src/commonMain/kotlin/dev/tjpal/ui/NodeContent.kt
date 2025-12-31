package dev.tjpal.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.composition.foundation.themes.tokens.TextType
import dev.tjpal.composition.foundation.themes.tokens.Theme
import dev.tjpal.model.ExtendedNodeDefinition
import dev.tjpal.model.NodeInstance
import dev.tjpal.model.NodeType
import dev.tjpal.viewmodel.GraphEditorViewModel

private val iconSize = 48.dp
private val toolLabelOffset = 25.dp
private val nodeColumSpacing = 16.dp

@Composable
fun NodeLabelContent(nodeInstance: NodeInstance, nodeDefinition: ExtendedNodeDefinition, viewModel: GraphEditorViewModel) {
    val nodeDesignTokens = Theme.current.graph.node

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = when(nodeDefinition.definition.type) {
            NodeType.PROCESSOR -> maxWidth
            NodeType.INPUT -> maxWidth - nodeDesignTokens.leftCornerBaseRadius * 2
            NodeType.OUTPUT -> maxWidth - nodeDesignTokens.rightCornerBaseRadius * 2
            else -> maxWidth
        }
        val offset = if(nodeDefinition.definition.type == NodeType.INPUT) nodeDesignTokens.leftCornerBaseRadius else 0.dp

        Column(
            modifier = Modifier.width(width).offset(offset, 0.dp),
            verticalArrangement = Arrangement.spacedBy(nodeColumSpacing)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(nodeDefinition.definition.displayedName, type = TextType.PRIMARY)
            }

            val executionMode by viewModel.isGraphActive.collectAsStateWithLifecycle()

            if(executionMode) {
                StatusIcon(nodeInstance.id, viewModel)
            } else {
                val iconBitmap = nodeDefinition.iconImage ?: return@Column
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = nodeDefinition.definition.id,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}

@Composable
fun NodeIconContent(nodeInstance: NodeInstance, nodeDefinition: ExtendedNodeDefinition, viewModel: GraphEditorViewModel) {
    val iconBitmap = nodeDefinition.iconImage

    if(iconBitmap == null) {
        NodeLabelContent(nodeInstance, nodeDefinition, viewModel)
        return
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            bitmap = iconBitmap,
            contentDescription = nodeDefinition.definition.id,
            modifier = Modifier.size(iconSize)
        )
    }

    Box(modifier = Modifier.offset(0.dp, iconSize + toolLabelOffset).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(nodeDefinition.definition.displayedName, type = TextType.PRIMARY)
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
            nodeDefinition = nodeDefinition,
            viewModel = viewModel
        )
    }
}