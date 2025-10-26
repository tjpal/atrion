package dev.tjpal.nodes

class LLMProcessingNode(private val parametersJson: String) : Node {
    override fun onActivate(context: NodeActivationContext) {
        // TODO
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        // For now just forward the input payload to the "text_out" connector
        output.send("text_out", context.payload)
    }

    override fun onStop(context: NodeDeactivationContext) {
        // TODO
    }
}
