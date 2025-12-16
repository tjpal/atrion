package dev.tjpal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tjpal.composition.foundation.basics.text.Text
import dev.tjpal.viewmodel.GraphEditorViewModel

/**
 * Simple status display composable for nodes. For now it just shows the textual node status as provided by the StatusEntry flow.
 */
@Composable
fun StatusIcon(nodeId: String, viewModel: GraphEditorViewModel, modifier: Modifier = Modifier) {
    val statusEntry by viewModel.observeNodeStatus(nodeId).collectAsStateWithLifecycle()

    val statusText = statusEntry?.nodeStatus?.name ?: "No status"

    Text(text = statusText, modifier = modifier)
}
