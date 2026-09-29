package ch.tikkosoft.protopass.model

/**
 * Class to hold secret values retrieved from Proton Pass.
 */
class SecretValues(private val values: Map<String, String>) {
    fun getString(key: String): String =
        values[key] ?: throw IllegalStateException("Missing value for $key")

    fun getOptionalString(key: String): String? = values[key]

    fun containsKey(key: String): Boolean = values.containsKey(key)

    fun getAllKeys(): Set<String> = values.keys
}
