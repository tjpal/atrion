package dev.tjpal.tools

import dev.tjpal.logging.logger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

data class ToolInfo(
    val definitionName: String,
    val toolClass: KClass<out Tool>
)

@Singleton
class ToolRegistry @Inject constructor() {
    private val logger = logger<ToolRegistry>()
    private val map: ConcurrentHashMap<String, ToolInfo> = ConcurrentHashMap()

    fun register(definitionName: String, info: ToolInfo) {
        map[definitionName] = info
        logger.debug("Registered tool definition={} class={}", definitionName, info.toolClass.qualifiedName)
    }

    fun resolve(definitionName: String): KClass<out Tool>? {
        return map[definitionName]?.toolClass
    }

    fun getInfo(definitionName: String): ToolInfo? = map[definitionName]

    fun listAll(): List<ToolInfo> = map.values.toList()
}
