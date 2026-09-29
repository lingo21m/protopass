package ch.tikkosoft.protopass.internal

import ch.tikkosoft.protopass.model.ProjectProperties
import ch.tikkosoft.protopass.model.VersionStrategy
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties

/**
 * Handles version code calculation based on date patterns
 */
class VersionHandler {
    /**
     * Calculates and updates the version code based on the strategy.
     *
     * @param configFile The properties file in which to update the `VERSION_CODE`.
     * @param strategy The strategy to use for incrementing the version code.
     * @return The new version code as a 32-bit integer.
     */
    fun calculateVersionCode(configFile: File, strategy: VersionStrategy = VersionStrategy.DATE_CODE): Int {
        // Load properties from configFile
        val props = Properties()
        if (configFile.exists()) {
            configFile.reader().use { props.load(it) }
        }

        // Read the last version code from VERSION_CODE property, default to 2024010100 if missing
        val lastVersionCode = props.getProperty(ProjectProperties.VERSION_CODE)?.toLongOrNull() ?: 2024010100L

        val finalVersionCode = when (strategy) {
            VersionStrategy.DATE_CODE -> calculateDateBasedVersionCode(lastVersionCode)
            VersionStrategy.NUMBER -> calculateNumberBasedVersionCode(lastVersionCode)
        }

        // Update only VERSION_CODE in config.properties, preserving other properties
        props.setProperty(ProjectProperties.VERSION_CODE, finalVersionCode.toString())
        configFile.writer().use { props.store(it, "Updated ${ProjectProperties.VERSION_CODE}") }

        return finalVersionCode.toInt() // Return as 32-bit integer for Android
    }

    private fun calculateDateBasedVersionCode(lastVersionCode: Long): Long {
        // Get current date as YYYYMMDD (e.g., "20250331")
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toLong()

        // Calculate the new version code
        val newVersionCode = if ((lastVersionCode / 100) == currentDate) {
            // Same day: increment XX (e.g., 2025033100 -> 2025033101)
            lastVersionCode + 1
        } else {
            // New day: start at 00 (e.g., 2025033199 -> 2025040100)
            currentDate * 100
        }

        // Ensure the new version code is greater than the last one
        return if (newVersionCode <= lastVersionCode) {
            lastVersionCode + 1 // Fallback increment
        } else {
            newVersionCode
        }
    }

    private fun calculateNumberBasedVersionCode(lastVersionCode: Long): Long {
        return lastVersionCode + 1
    }

}
