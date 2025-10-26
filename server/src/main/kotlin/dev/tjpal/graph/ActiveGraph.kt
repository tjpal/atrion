package dev.tjpal.graph

import dev.tjpal.graph.model.GraphDefinition
import dev.tjpal.graph.model.GraphExecutionStatus
import dev.tjpal.graph.model.NodeInstance
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeActivationContext
import dev.tjpal.nodes.NodeOutput
import dev.tjpal.nodes.NodeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class ActiveGraph(
    val id: String,
    val graphId: String,
    private val graphDefinition: GraphDefinition,
    private val nodeRepository: NodeRepository
) {
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
                    executionId = id,
                    nodeId = nodeDef.id,
                    parametersJson = nodeDef.parametersJson
                )
                nodeInstance.onActivate(context)
            } catch (e: Exception) {
                println("Error activating node ${nodeDef.id}: ${e.message}")
            }
        }
    }

    fun stop() {
        if (stopped.compareAndSet(false, true).not()) return

        cancelAndStopRuntimeWorkers()

        mailboxes.values.forEach { it.close() }
        mailboxes.clear()

        scope.cancel()
    }

    private fun cancelAndStopRuntimeWorkers() {
        for ((_, holder) in runtimeNodes.entries) {
            try {
                holder.workerJob.cancel()
            } catch (e: Exception) {
                // Ignored. Cannot be acted upon.
            }

            try {
                holder.node.onStop()
            } catch (e: Exception) {
                // Ignored. Cannot be acted upon.
            }
        }
        runtimeNodes.clear()
    }

    /**
     * Is invoked by external registries for input events. Input nodes do not wait for their input. Instead they
     * rely on the graph to create an instance once an input event is received.
     */
    fun onInputEvent(nodeId: String, payload: String) {
        if (stopped.get()) return

        val mailbox = mailboxes.computeIfAbsent(nodeId) { Mailbox() }
        val message = MailboxMessage("", payload) // Input connectorId is ignored by input nodes

        val messageEnqueued = mailbox.enqueue(message)

        if (!messageEnqueued) {
            println("Failed to enqueue message for node $nodeId (mailbox closed)")
            return
        }

        scheduleNode(nodeId)
    }

    private fun scheduleNode(nodeId: String) {
        runtimeNodes.computeIfAbsent(nodeId) { key ->
            val job = scope.launch {
                processMailboxLoop(key)
            }

            val nodeDefinition = getNodeDefinition(nodeId)
            val nodeInstance = nodeRepository.createNodeInstance(
                nodeDefinition.definitionName,
                nodeDefinition.parametersJson
            )

            NodeRuntimeHolder(node = nodeInstance, workerJob = job)
        }
    }

    private fun getNodeDefinition(nodeId: String): NodeInstance {
        return graphDefinition.nodes.find { it.id == nodeId }
            ?: throw IllegalArgumentException("No node definition found for nodeId=$nodeId")
    }

    /**
     * Process messages for a given nodeId. This method runs in a coroutine.
     */
    private suspend fun processMailboxLoop(nodeId: String) {
        val mailBox = mailboxes[nodeId] ?: throw IllegalStateException("Mailbox not found for nodeId=$nodeId")
        val nodeInstance = runtimeNodes[nodeId]?.node ?: throw IllegalStateException("Node instance not found for nodeId=$nodeId")

        try {
            while (true) {
                val message = try {
                    mailBox.receive()
                } catch (e: Exception) {
                    break
                }

                // NodeOutput implementation will call back into this ActiveGraph to route outputs
                val output = object : NodeOutput {
                    override fun send(outputConnectorId: String, payload: String) {
                        routeOutput(nodeId, outputConnectorId, payload)
                    }
                }

                // Deliver the payload to the node instance
                nodeInstance.onEvent(message.payload, output)
            }
        } finally {
            runtimeNodes.remove(nodeId)
        }
    }

    private fun routeOutput(fromNodeId: String, fromConnectorId: String, payload: String) {
        val key = Pair(fromNodeId, fromConnectorId)
        val targets = adjacency[key] ?: emptyList()

        targets.forEach { target ->
            val mailbox = mailboxes.computeIfAbsent(target.toNodeId) { Mailbox() }
            val message = MailboxMessage(target.toConnectorId, payload)

            val messageEnqueued = mailbox.enqueue(message)
            if (!messageEnqueued) {
                println("Failed to enqueue message to ${target.toNodeId}")
            } else {
                scheduleNode(target.toNodeId)
            }
        }
    }
}
