package app.miogram.core.plugins

/**
 * Maps a semantic host operation to the capability it requires.
 *
 * Default-deny discipline: an operation absent from this table is rejected
 * before any guest code runs — adding new ops MUST mean editing this file,
 * which keeps the security surface reviewable in one place.
 */
object OpPolicy {

    /** @return null when the operation is not part of the known surface. */
    fun requiredCapability(op: String): PluginCapability? = when (op) {
        "on_message_receive" -> PluginCapability.READ_MESSAGE_EVENTS
        "on_chat_history_batch" -> PluginCapability.READ_MESSAGE_EVENTS

        "send_message" -> PluginCapability.SEND_MESSAGES

        "http_fetch" -> PluginCapability.NETWORK

        "storage_get", "storage_put", "storage_delete" -> PluginCapability.PRIVATE_STORAGE

        "notify_post" -> PluginCapability.NOTIFICATIONS

        "render_decorator" -> PluginCapability.UI_DECORATOR

        else -> null
    }

    /**
     * Operations callable while the plugin handles a background trigger;
     * everything else is treated as user-initiated and equally permitted —
     * the split exists so future stages can throttle background work.
     */
    fun isBackgroundTrigger(op: String): Boolean =
        op.startsWith("on_")
}

/** Storage contract for installed distributions. */
interface PluginRepository {
    fun save(plugin: InstalledPlugin)
    fun find(pluginId: String): InstalledPlugin?
    fun list(): List<InstalledPlugin>
    fun delete(pluginId: String)
}

class InMemoryPluginRepository : PluginRepository {
    private val plugins = LinkedHashMap<String, InstalledPlugin>()

    override fun save(plugin: InstalledPlugin) {
        plugins[plugin.pluginId] = plugin
    }

    override fun find(pluginId: String): InstalledPlugin? = plugins[pluginId]?.let { it.withCode(it.code.copyOf()) }

    override fun list(): List<InstalledPlugin> = plugins.values.map { it.withCode(it.code.copyOf()) }

    override fun delete(pluginId: String) {
        plugins.remove(pluginId)
    }
}
