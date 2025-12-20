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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tjpal.composition.foundation.basics.functional.Button
import dev.tjpal.composition.foundation.basics.functional.Input
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.composition.foundation.themes.tokens.TextType
import dev.tjpal.model.ParameterDefinition
import dev.tjpal.ui.navigation.LocalNavController
import dev.tjpal.viewmodel.GraphEditorViewModel
import dev.tjpal.viewmodel.NodeCustomData
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConfigureNodeScreen(nodeId: String, viewModel: GraphEditorViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val nodeSpec = uiState.nodes.firstOrNull { it.id == nodeId }
    val associatedData = nodeSpec?.associatedData as? NodeCustomData ?: return
    val parameters = associatedData.definition.parameters

    Box(modifier = Modifier.fillMaxSize().background(Color(0x80000000))) {
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

                Text(text = "Configure node: ${associatedData.definition.name}")

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
                            ParameterRow(param = param)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { navController.popBackStack() }) {
                        Text(text = "Close")
                    }

                    Button(onClick = { navController.popBackStack() }) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterRow(param: ParameterDefinition, labelWeight: Float = 0.5f) {
    val labelWidthWeight = 0.5f
    var value by remember { mutableStateOf(param.default ?: "") }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(labelWidthWeight).padding(end = 8.dp)) {
            Text(text = param.name, type = TextType.PRIMARY)
        }

        Box(modifier = Modifier.weight(1f - labelWidthWeight)) {
            Input(value = value, onValueChange = { value = it })
        }
    }
}