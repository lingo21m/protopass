package ch.tikkosoft.protopass.model

/**
 * Defines the strategy for incrementing the version code.
 */
enum class VersionStrategy {
    /**
     * Increments the version code based on the current date and a daily counter.
     * Format: YYYYMMDDXX (e.g., 2025033100)
     */
    DATE_CODE,

    /**
     * Simply increments the version code by 1.
     */
    NUMBER
}
