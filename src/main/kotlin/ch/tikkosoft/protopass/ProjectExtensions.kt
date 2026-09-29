package ch.tikkosoft.protopass

import ch.tikkosoft.protopass.model.ProtoPassFileNames
import ch.tikkosoft.protopass.model.ProjectProperties
import org.gradle.api.Project
import java.io.File
import java.util.Properties

/**
 * Sets a project property from an environment variable.
 *
 * If the environment variable is set, the project property is set to the value of the environment variable.
 * If the environment variable is not set, a message is logged indicating that the project property could not be set.
 *
 * @param projectPropertyName The name of the project property to set.
 * @param envVariableName The name of the environment variable to read.
 */
fun Project.setProjectPropertyFromEnv(projectPropertyName: String, envVariableName: String) {
    val envValue = System.getenv(envVariableName)
    if (envValue != null) {
        extensions.extraProperties.set(projectPropertyName, envValue)
        logger.lifecycle("Set project property '$projectPropertyName' from environment variable '$envVariableName'.")
    } else {
        logger.lifecycle("'$envVariableName' is null, cannot set project property '$projectPropertyName'.")
    }
}

/**
 * Checks if the project has all the specified properties.
 *
 * @param projectPropertyNames The names of the properties to check.
 * @return true if all specified properties exist, false otherwise.
 */
fun Project.hasAllProperties(vararg projectPropertyNames: String): Boolean {
    return projectPropertyNames.all { hasProperty(it) }
}

/**
 * Loads properties from a file and sets them as project properties.
 *
 * @param outputFilePath The path to the output properties file.
 */
fun Project.loadProjectPropertiesFromFile(outputFilePath: String) {
    val propertiesFile = File(outputFilePath)
    if (propertiesFile.exists()) {
        val props = Properties()
        propertiesFile.reader().use { reader ->
            props.load(reader)
        }
        props.forEach { key, value ->
            extensions.extraProperties.set(key.toString(), value.toString())
            logger.lifecycle("Set project property '$key' from file '$outputFilePath'.")
        }
    } else {
        logger.warn("Properties file '$outputFilePath' not found.")
    }
}

/**
 * Reads the version code from the config properties file in the project directory.
 *
 * @param configFileName The name of the config file (defaults to ProtoPassFileNames.CONFIG_PROPERTIES)
 * @param defaultValue The default value to return if the file doesn't exist or the property isn't found
 * @return The version code as an integer
 */
fun Project.readVersionCodeFromFile(configFileName: String = ProtoPassFileNames.CONFIG_PROPERTIES, defaultValue: Int = 1): Int {
    val configFile = File(projectDir, configFileName)
    return if (configFile.exists()) {
        try {
            val props = Properties()
            configFile.reader().use { props.load(it) }
            props.getProperty(ProjectProperties.VERSION_CODE)?.toIntOrNull() ?: defaultValue
        } catch (e: Exception) {
            logger.warn("Failed to read version code from config file '$configFileName': ${e.message}")
            defaultValue
        }
    } else {
        logger.warn("Config file '$configFileName' not found. Using default value $defaultValue.")
        defaultValue
    }
}
