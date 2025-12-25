package dev.tjpal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tjpal.composition.foundation.basics.functional.Button
import dev.tjpal.composition.foundation.basics.functional.Input
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.composition.foundation.themes.tokens.TextType
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.ParameterDefinition
import dev.tjpal.ui.navigation.LocalNavController
import dev.tjpal.viewmodel.GraphEditorViewModel
import dev.tjpal.viewmodel.NodeCustomData
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConfigureNodeScreen(nodeId: String, viewModel: GraphEditorViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val nodeSpec = uiState.nodes.firstOrNull { it.id == nodeId }
    val associatedData = nodeSpec?.associatedData as? NodeCustomData ?: return
    val parameters = associatedData.definition.parameters

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(width = 600.dp, height = 400.dp)
                .align(Alignment.Center)
                .background(color = Color.White, shape = RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(align = Alignment.TopStart)
                .padding(16.dp)) {

                val paramValues = remember {
                    mutableStateMapOf<String, String>().apply {
                        parameters.forEach { put(it.name, it.default ?: "") }
                    }
                }

                if (parameters.isEmpty()) {
                    Text(text = "No parameters to configure for this node.")
                } else {
                    val scrollState = rememberScrollState()

                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        parameters.forEach { param ->
                            ParameterRow(
                                param = param,
                                value = paramValues[param.name] ?: "",
                                onValueChange = { newValue -> paramValues[param.name] = newValue }
                            )
                        }
                    }
                }

                ControlStrip(nodeId = nodeId, viewModel = viewModel, paramValues = paramValues)
            }
        }
    }
}

@Composable
private fun ControlStrip(
    nodeId: String,
    viewModel: GraphEditorViewModel,
    paramValues: Map<String, String>
) {
    val navController = LocalNavController.current

    val onSave: () -> Unit = {
        val nodeParams = NodeParameters(values = paramValues.toMap())
        viewModel.setNodeParameters(nodeId, nodeParams)

        navController.popBackStack()
    }

    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
        Button(onClick = { navController.popBackStack() }) {
            Text(text = "Close")
        }

        Button(onClick = onSave) {
            Text(text = "Save")
        }
    }
}

@Composable
private fun ParameterRow(param: ParameterDefinition, value: String, onValueChange: (String) -> Unit) {
    val labelWidthWeight = 0.5f

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(labelWidthWeight).padding(end = 8.dp)) {
            Text(text = param.name, type = TextType.PRIMARY)
        }

        Box(modifier = Modifier.weight(1f - labelWidthWeight)) {
            Input(value = value, onValueChange = onValueChange)
        }
    }
}
