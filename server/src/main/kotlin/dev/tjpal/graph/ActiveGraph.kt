package dev.tjpal.graph

import dev.tjpal.logging.logger
import dev.tjpal.model.GraphDefinition
import dev.tjpal.model.GraphExecutionStatus
import dev.tjpal.model.NodeInstance
import dev.tjpal.model.NodeType
import dev.tjpal.nodes.Node
import dev.tjpal.nodes.NodeActivationContext
import dev.tjpal.nodes.NodeDeactivationContext
import dev.tjpal.nodes.NodeInvocationContext
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
     * Activate the graph: build mailboxes, adjacency map, and create persistent input node instances.
     */
    fun activate() {
        logger.info("Activating graph execution {} (graphId={})", id, graphId)
        createMailboxes()
        buildAdjacencyMap()
        createPersistentInputNodes()
    }

    private fun createMailboxes() {
        for (node in graphDefinition.nodes) {
            mailboxes[node.id] = Mailbox()
        }
        logger.debug("Created {} mailboxes for graph execution {}", mailboxes.size, id)
    }

    private fun buildAdjacencyMap() {
        adjacency.clear()
        for (edge in graphDefinition.edges) {
            val key = Pair(edge.fromNodeId, edge.fromConnectorId)
            val list = adjacency.getOrPut(key) { mutableListOf() }
            list.add(Target(edge.toNodeId, edge.toConnectorId))
        }
        logger.debug("Adjacency map built with {} entries for execution {}", adjacency.size, id)
    }

    private fun createPersistentInputNodes() {
        val allDefinitions = nodeRepository.getAllDefinitions()

        for (nodeDef in graphDefinition.nodes) {
            if (nodeDef.definitionName.isBlank()) continue

            try {
                // Determine node type by resolving the node definition via repository
                val definition = allDefinitions.find { it.name == nodeDef.definitionName }
                val isInput = definition?.type == NodeType.INPUT

                if (isInput) {
                    val nodeInstance = nodeRepository.createNodeInstance(
                        nodeDef.definitionName,
                        nodeDef.parameters
                    )

                    val context = NodeActivationContext(
                        graphInstanceId = id,
                        nodeId = nodeDef.id,
                        parameters = nodeDef.parameters,
                        graph = this
                    )

                    try {
                        nodeInstance.onActivate(context)
                    } catch (e: Exception) {
                        logger.error("Error during onActivate for persistent input node {}", nodeDef.id, e)
                        continue
                    }

                    val job = scope.launch {
                        processMailboxLoop(nodeDef.id, nodeInstance)
                    }

                    runtimeNodes[nodeDef.id] = NodeRuntimeHolder(node = nodeInstance, workerJob = job)

                    logger.debug("Persistent input node created and worker started for node {} in execution {}", nodeDef.id, id)
                }
            } catch (e: Exception) {
                logger.error("Error creating persistent input node {}", nodeDef.id, e)
            }
        }
    }

    fun stop() {
        if (stopped.compareAndSet(false, true).not()) return

        logger.info("Stopping graph execution {} (graphId={})", id, graphId)

        // Cancel worker jobs; their finally blocks will perform onStop for their node instances.
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
        if (stopped.get()) {
            logger.warn("Received input for stopped graph execution {} nodeId={}", id, nodeId)
            return
        }

        val mailbox = mailboxes.computeIfAbsent(nodeId) { Mailbox() }
        val message = MailboxMessage("", payload, executionId) // Input connectorId is ignored by input nodes

        val messageEnqueued = mailbox.enqueue(message)

        if (!messageEnqueued) {
            logger.error("Failed to enqueue message for node {} (mailbox closed) executionId={}", nodeId, executionId)
            return
        }

        logger.debug("Enqueued message to nodeId={} executionId={} mailboxSizeApprox={}", nodeId, executionId, if (mailbox.isEmpty()) 0 else 1)

        scheduleNode(nodeId)
    }

    private fun scheduleNode(nodeId: String) {
        runtimeNodes.computeIfAbsent(nodeId) {
            val nodeDefinition = getNodeDefinition(nodeId)
            val nodeInstance = nodeRepository.createNodeInstance(
                nodeDefinition.definitionName,
                nodeDefinition.parameters
            )

            val activationContext = NodeActivationContext(
                graphInstanceId = id,
                nodeId = nodeDefinition.id,
                parameters = nodeDefinition.parameters,
                graph = this
            )

            try {
                nodeInstance.onActivate(activationContext)
            } catch (e: Exception) {
                logger.error("Error during onActivate for transient node {}", nodeId, e)
            }

            val job = scope.launch {
                processMailboxLoop(nodeId, nodeInstance)
            }

            logger.debug("Scheduled worker for nodeId={} executionId={} (new worker)", nodeId, id)

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
            logger.info("Starting worker coroutine for node {} in execution {}", nodeId, id)

            while (true) {
                val message = try {
                    mailBox.receive()
                } catch (e: Exception) {
                    logger.debug("Mailbox.receive ended for node {} in execution {}", nodeId, id)
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
                    graphInstanceId = id,
                    executionId = message.executionId,
                    nodeId = nodeId,
                    payload = message.payload,
                    graph = this
                )

                try {
                    logger.debug("Delivering message to node {} executionId={} payload={}", nodeId, message.executionId, message.payload)
                    nodeInstance.onEvent(nodeInvocationContext, output)
                } catch (e: Exception) {
                    logger.error("Node {} processing failed for execution {}", nodeId, e.message)
                }
            }
        } finally {
            try {
                // Ensure node lifecycle cleanup is invoked for the instance that was running in this worker.
                val deactivationContext = NodeDeactivationContext(
                    graphInstanceId = id,
                    nodeId = nodeId
                )

                try {
                    nodeInstance.onStop(deactivationContext)
                } catch (e: Exception) {
                    logger.error("Error during onStop for node {} in execution {}", nodeId, id, e)
                }
            } finally {
                runtimeNodes.remove(nodeId)
                logger.info("Worker coroutine stopped for node {} in execution {}", nodeId, id)
            }
        }
    }

    private fun routeOutput(fromNodeId: String, fromConnectorId: String, payload: String, executionId: String) {
        val key = Pair(fromNodeId, fromConnectorId)
        val targets = adjacency[key] ?: emptyList()

        logger.debug("Routing output from {}:{} to {} target(s) for execution {}", fromNodeId, fromConnectorId, targets.size, executionId)

        targets.forEach { target ->
            val mailbox = mailboxes.computeIfAbsent(target.toNodeId) { Mailbox() }
            val message = MailboxMessage(target.toConnectorId, payload, executionId)

            val messageEnqueued = mailbox.enqueue(message)
            if (!messageEnqueued) {
                logger.error("Failed to enqueue message to {} (execution {})", target.toNodeId, executionId)
            } else {
                logger.debug("Enqueued routed message to {} (execution {})", target.toNodeId, executionId)
                scheduleNode(target.toNodeId)
            }
        }
    }

    fun routeFromNode(fromNodeId: String, fromConnectorId: String, payload: String, executionId: String) {
        routeOutput(fromNodeId, fromConnectorId, payload, executionId)
    }

    fun getAttachedToolDefinitionNames(nodeId: String): List<String> {
        val outgoing = graphDefinition.edges.filter { it.fromNodeId == nodeId }

        val targetNodeDefinitionNames = outgoing.mapNotNull { edge ->
            graphDefinition.nodes.find { it.id == edge.toNodeId }?.definitionName
        }.distinct()

        val toolDefinitionNames = nodeRepository.getAllDefinitions().filter { it.type == NodeType.TOOL }.map { it.name }.toSet()

        return targetNodeDefinitionNames.filter { it in toolDefinitionNames }
    }
}
