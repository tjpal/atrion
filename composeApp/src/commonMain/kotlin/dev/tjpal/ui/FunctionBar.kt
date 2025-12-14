package dev.tjpal.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import atrion.composeapp.generated.resources.Res
import atrion.composeapp.generated.resources.build_mode
import atrion.composeapp.generated.resources.debug_mode
import atrion.composeapp.generated.resources.play_mode
import atrion.composeapp.generated.resources.store
import dev.tjpal.composition.foundation.basics.functional.FloatingBar
import dev.tjpal.composition.foundation.basics.functional.GroupBuilder
import dev.tjpal.composition.foundation.basics.functional.IconButton
import dev.tjpal.composition.foundation.basics.functional.WaitingCircle
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.composition.foundation.themes.tokens.ButtonType
import dev.tjpal.composition.foundation.themes.tokens.FloatingBarOrientation
import dev.tjpal.model.ExtendedNodeDefinition
import dev.tjpal.repository.LoadState
import dev.tjpal.viewmodel.GraphEditorViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val buttonSize = 64.dp

fun GroupBuilder.operationButtons(viewModel: GraphEditorViewModel) {
    item {
        IconButton(type = ButtonType.SHY, onClick = { viewModel.save() }) {
            Image(
                painter = painterResource(Res.drawable.store),
                contentDescription = "Store",
                modifier = Modifier.size(buttonSize)
            )
        }
    }
}

fun GroupBuilder.modeButtons(viewModel: GraphEditorViewModel) {
    data class FunctionalButton(
        val icon: DrawableResource,
        val content: String = "",
        val action: () -> Unit
    )

    val functionalButtons = listOf(
        FunctionalButton(Res.drawable.build_mode) { viewModel.save() },
        FunctionalButton(Res.drawable.play_mode) { viewModel.switchToEditMode() },
        FunctionalButton(Res.drawable.debug_mode) { viewModel.switchToExecutionMode() }
    )

    functionalButtons.forEach {
        item {
            IconButton(type = ButtonType.SHY, onClick = it.action) {
                Image(
                    painter = painterResource(it.icon),
                    contentDescription = it.content,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

fun GroupBuilder.nodeButtons(nodeDefinitions: LoadState<List<ExtendedNodeDefinition>>, viewModel: GraphEditorViewModel) {
    val nodeItems = mutableListOf<@Composable ()-> Unit>()

    when(nodeDefinitions) {
        is LoadState.Loading -> nodeItems.add { WaitingCircle(modifier = Modifier.size(buttonSize)) }
        is LoadState.Error -> nodeItems.add { Text("Error") }
        is LoadState.Ready<List<ExtendedNodeDefinition>> -> {
            val definitions = (nodeDefinitions as LoadState.Ready<List<ExtendedNodeDefinition>>).data

            definitions.forEach { definition ->
                item {
                    IconButton(
                        type = ButtonType.SHY,
                        onClick = { viewModel.insertNode(definition) },
                        modifier = Modifier.size(buttonSize)
                    ) {
                        definition.iconImage?.let {
                            Image(
                                bitmap = it,
                                contentDescription = definition.definition.name,
                                modifier = Modifier.size(buttonSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FunctionBar(viewModel: GraphEditorViewModel) {
    val nodeDefinitions by viewModel.nodeDefinitions.collectAsStateWithLifecycle()

    FloatingBar(buttonExtent = buttonSize, orientation = FloatingBarOrientation.HORIZONTAL) {
        group {
            operationButtons(viewModel)
        }
        group {
            modeButtons(viewModel)
        }
        group {
            nodeButtons(nodeDefinitions, viewModel)
        }
    }
}
