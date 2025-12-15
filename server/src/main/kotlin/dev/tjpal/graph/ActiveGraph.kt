package dev.tjpal.graph

import dev.tjpal.model.GraphDefinition
import dev.tjpal.model.GraphExecutionStatus
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeActivationContext
import dev.tjpal.nodes.NodeOutput
import dev.tjpal.nodes.NodeRepository
import dev.tjpal.model.NodeInstance
import dev.tjpal.nodes.NodeDeactivationContext
import dev.tjpal.nodes.NodeInvocationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import dev.tjpal.logging.logger

class ActiveGraph(
    val id: String,
    val graphId: String,
    private val graphDefinition: GraphDefinition,
    private val nodeRepository: NodeRepository
) {
    private val logger = logger<ActiveGraph>()

    fun getExecutionStatus(): GraphExecutionStatus = GraphExecutionStatus(
        id = id,
        graphId = graphId,
        nodeExecutionStates = emptyList()
    )

    private val scopeJob: Job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + scopeJob)
    private val stopped = AtomicBoolean(false)

    // Each node has a mailbox which is used to hold input data until it can be processed.
    private val mailboxes: ConcurrentHashMap<String, Mailbox> = ConcurrentHashMap()

    // Adjacency map for routing outputs to inputs: (fromNodeId, fromConnectorId) -> List of (toNodeId, toConnectorId)
    private val adjacency: MutableMap<Pair<String, String>, MutableList<Target>> = mutableMapOf()
    data class Target(val toNodeId: String, val toConnectorId: String)

    // Runtime node instances and workers
    private val runtimeNodes: ConcurrentHashMap<String, NodeRuntimeHolder> = ConcurrentHashMap()
    data class NodeRuntimeHolder(
        val node: Node,
        val workerJob: Job
    )

    /**
     * Activate the graph: build mailboxes, adjacency map, and call onActivate on all nodes to allow them to register external hooks.
     */
    fun activate() {
        createMailboxes()
        buildAdjacencyMap()
        callOnActivateOnNodes()
    }

    private fun createMailboxes() {
        for (node in graphDefinition.nodes) {
            mailboxes[node.id] = Mailbox()
        }
    }

    private fun buildAdjacencyMap() {
        adjacency.clear()
        for (edge in graphDefinition.edges) {
            val key = Pair(edge.fromNodeId, edge.fromConnectorId)
            val list = adjacency.getOrPut(key) { mutableListOf() }
            list.add(Target(edge.toNodeId, edge.toConnectorId))
        }
    }

    private fun callOnActivateOnNodes() {
        for (nodeDef in graphDefinition.nodes) {
            try {
                val nodeInstance = nodeRepository.createNodeInstance(
                    nodeDef.definitionName,
                    nodeDef.parametersJson
                )

                val context = NodeActivationContext(
                    graphInstanceId = id,
                    nodeId = nodeDef.id,
                    parametersJson = nodeDef.parametersJson,
                    graph = this
                )

                nodeInstance.onActivate(context)
            } catch (e: Exception) {
                logger.error("Error activating node {}", nodeDef.id, e)
            }
        }
    }

    fun stop() {
        if (stopped.compareAndSet(false, true).not()) return

        cancelAndStopRuntimeWorkers()

        mailboxes.values.forEach { it.close() }
        mailboxes.clear()

        scope.cancel()

        callStopOnNodes()
    }

    private fun callStopOnNodes() {
        for (nodeDef in graphDefinition.nodes) {
            try {
                val nodeInstance = nodeRepository.createNodeInstance(
                    nodeDef.definitionName,
                    nodeDef.parametersJson
                )

                val context = NodeDeactivationContext(
                    graphInstanceId = id,
                    nodeId = nodeDef.id
                )

                nodeInstance.onStop(context)
            } catch (e: Exception) {
                logger.error("Error stopping node {}", nodeDef.id, e)
            }
        }
    }

    private fun cancelAndStopRuntimeWorkers() {
        for ((_, holder) in runtimeNodes.entries) {
            try {
                holder.workerJob.cancel()
            } catch (e: Exception) {
                // Ignored. Cannot be acted upon.
                logger.debug("Error while canceling worker job", e)
            }
        }
        runtimeNodes.clear()
    }

    /**
     * Is invoked by external registries for input events. Input nodes do not wait for their input. Instead they
     * rely on the graph to create an instance once an input event is received.
     */
    fun onInputEvent(nodeId: String, payload: String, executionId: String) {
        if (stopped.get()) return

        val mailbox = mailboxes.computeIfAbsent(nodeId) { Mailbox() }
        val message = MailboxMessage("", payload, executionId) // Input connectorId is ignored by input nodes

        val messageEnqueued = mailbox.enqueue(message)

        if (!messageEnqueued) {
            logger.error("Failed to enqueue message for node {} (mailbox closed)", nodeId)
            return
        }

        scheduleNode(nodeId)
    }

    private fun scheduleNode(nodeId: String) {
        runtimeNodes.computeIfAbsent(nodeId) {
            val nodeDefinition = getNodeDefinition(nodeId)
            val nodeInstance = nodeRepository.createNodeInstance(
                nodeDefinition.definitionName,
                nodeDefinition.parametersJson
            )

            val job = scope.launch {
                processMailboxLoop(nodeId, nodeInstance)
            }

            NodeRuntimeHolder(node = nodeInstance, workerJob = job)
        }
    }

    private fun getNodeDefinition(nodeId: String): NodeInstance {
        return graphDefinition.nodes.find { it.id == nodeId }
            ?: throw IllegalStateException("Node definition not found for nodeId=$nodeId")
    }

    /**
     * Process messages for a given nodeId. This method runs in a coroutine.
     */
    private suspend fun processMailboxLoop(nodeId: String, nodeInstance: Node) {
        val mailBox = mailboxes[nodeId] ?: throw IllegalStateException("Mailbox not found for nodeId=$nodeId")

        try {
            while (true) {
                val message = try {
                    mailBox.receive()
                } catch (e: Exception) {
                    break
                }

                // NodeOutput implementation will call back into this ActiveGraph to route outputs. It must
                // capture the per-input executionId so that downstream mailboxes receive messages associated to it.
                val output = object : NodeOutput {
                    override fun send(outputConnectorId: String, payload: String) {
                        routeOutput(nodeId, outputConnectorId, payload, message.executionId)
                    }
                }

                // Deliver the payload to the node instance
                val nodeInvocationContext = NodeInvocationContext(
                    executionId = message.executionId,
                    nodeId = nodeId,
                    payload = message.payload
                )

                nodeInstance.onEvent(nodeInvocationContext, output)
            }
        } finally {
            runtimeNodes.remove(nodeId)
        }
    }

    private fun routeOutput(fromNodeId: String, fromConnectorId: String, payload: String, executionId: String) {
        val key = Pair(fromNodeId, fromConnectorId)
        val targets = adjacency[key] ?: emptyList()

        targets.forEach { target ->
            val mailbox = mailboxes.computeIfAbsent(target.toNodeId) { Mailbox() }
            val message = MailboxMessage(target.toConnectorId, payload, executionId)

            val messageEnqueued = mailbox.enqueue(message)
            if (!messageEnqueued) {
                logger.error("Failed to enqueue message to {}", target.toNodeId)
            } else {
                scheduleNode(target.toNodeId)
            }
        }
    }
}
