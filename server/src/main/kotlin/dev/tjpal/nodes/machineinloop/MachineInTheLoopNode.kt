package dev.tjpal.nodes.machineinloop

import dev.tjpal.ai.LLM
import dev.tjpal.ai.Request
import dev.tjpal.graph.status.StatusRegistry
import dev.tjpal.logging.logger
import dev.tjpal.model.NodeParameters
import dev.tjpal.model.NodeStatus
import dev.tjpal.model.StatusEntry
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeActivationContext
import dev.tjpal.nodes.NodeDeactivationContext
import dev.tjpal.nodes.NodeInvocationContext
import dev.tjpal.nodes.NodeOutput
import dev.tjpal.nodes.payload.RawPayload
import dev.tjpal.prompt.PromptsRepository
import kotlinx.serialization.json.Json

class MachineInTheLoopNode(
    private val parameters: NodeParameters,
    private val llm: LLM,
    private val statusRegistry: StatusRegistry,
    private val promptsRepository: PromptsRepository,
    private val json: Json
) : Node {
    private val logger = logger<MachineInTheLoopNode>()

    override fun onActivate(context: NodeActivationContext) {
        logger.debug("MachineInTheLoopNode.onActivate graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }

    override suspend fun onEvent(context: NodeInvocationContext, output: NodeOutput) {
        val inputText = context.payload.asString()

        try {
            val promptParam = parameters.values["Prompt"] ?: ""
            val referenceParam = parameters.values["Reference"] ?: ""

            registerStatus(context, NodeStatus.RUNNING, inputPayload = inputText, outputPayload = null, message = "Starting automatic review")

            val chain = llm.createResponseRequestChain()
            val request = Request(
                input = promptParam,
                instructions = referenceParam,
                tools = emptyList(),
                toolStaticParameters = null,
                responseType = null
            )

            val response = chain.createResponse(request)
            val summary = response.message

            chain.delete()
            output.send("out", RawPayload(summary))

            registerStatus(context, NodeStatus.FINISHED, inputPayload = null, outputPayload = summary, message = null)

            logger.info("MachineInTheLoopNode finished processing for executionId={} nodeId={}", context.executionId, context.nodeId)
        } catch (e: Exception) {
            logger.error("MachineInTheLoopNode: error during processing for executionId={} nodeId={}: {}", context.executionId, context.nodeId, e.message)
            registerStatus(context, NodeStatus.ERROR, inputPayload = inputText, outputPayload = null, message = e.message)
        }
    }

    override fun onStop(context: NodeDeactivationContext) {
        logger.debug("MachineInTheLoopNode.onStop graphInstanceId={} nodeId={}", context.graphInstanceId, context.nodeId)
    }


    private fun registerStatus(
        context: NodeInvocationContext,
        status: NodeStatus,
        inputPayload: String?,
        outputPayload: String?,
        message: String?
    ) {
        val entry = StatusEntry(
            graphInstanceId = context.graphInstanceId,
            executionId = context.executionId,
            nodeId = context.nodeId,
            timestamp = System.currentTimeMillis(),
            nodeStatus = status,
            inputPayload = inputPayload,
            outputPayload = outputPayload,
            message = message
        )

        statusRegistry.registerStatusEvent(entry)
    }
}
