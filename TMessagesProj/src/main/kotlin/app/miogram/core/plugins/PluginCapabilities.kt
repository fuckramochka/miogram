package app.miogram.core.plugins

/**
 * Typed capability model for plugins. A plugin may only request what its
 * signed manifest declares; every host call re-checks against the granted
 * set, so a stale or forged grant cannot outlive the manifest.
 */
enum class PluginCapability(val wireId: Int) {
    /** Read message events delivered to the plugin. */
    READ_MESSAGE_EVENTS(1),

    /** Send messages on behalf of the user. */
    SEND_MESSAGES(2),

    /** Open outbound network connections from inside the sandbox. */
    NETWORK(3),

    /** Persist plugin-private key/value state. */
    PRIVATE_STORAGE(4),

    /** Post local notifications. */
    NOTIFICATIONS(5),

    /** Decorate chat bubble rendering (pure function of render context). */
    UI_DECORATOR(6);

    companion object {
        private val byWire = entries.associateBy(PluginCapability::wireId)

        fun fromWire(wireId: Int): PluginCapability =
            byWire[wireId] ?: throw PluginFormatException("unknown capability $wireId")
    }
}

/** Immutable granted-capability set backed by an int bitmask. */
class CapabilitySet(val mask: Int = 0) {

    operator fun contains(capability: PluginCapability): Boolean =
        mask and capability.bit() != 0

    operator fun plus(capability: PluginCapability): CapabilitySet =
        CapabilitySet(mask or capability.bit())

    fun toList(): List<PluginCapability> =
        PluginCapability.entries.filter { it in this }

    companion object {
        fun of(vararg capabilities: PluginCapability): CapabilitySet =
            CapabilitySet(capabilities.fold(0) { acc, c -> acc or c.bit() })

        private fun PluginCapability.bit(): Int = 1 shl (wireId - 1)
    }
}

/** Sink for security-relevant plugin events; bridge wires the journal. */
fun interface PluginAuditSink {
    fun onEvent(event: PluginAuditEvent)
}

data class PluginAuditEvent(
    val pluginId: String,
    val kind: Kind,
    val detail: String,
) {
    enum class Kind { MANIFEST_VERIFIED, MANIFEST_REJECTED, CAPABILITY_DENIED, SANDBOX_FAULT }
}

/**
 * Central enforcement point. Every host API implementation must route its
 * authorization decision through [require]; direct checks scattered across
 * call sites are how capability systems rot.
 */
class CapabilityGate(private val auditSink: PluginAuditSink?) {

    fun require(pluginId: String, granted: CapabilitySet, requested: PluginCapability): Boolean {
        if (requested in granted) return true
        auditSink?.onEvent(
            PluginAuditEvent(pluginId, PluginAuditEvent.Kind.CAPABILITY_DENIED, requested.name)
        )
        return false
    }
}
