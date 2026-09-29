package ch.tikkosoft.protopass.internal

import ch.tikkosoft.protopass.extension.ProtoPassExtension
import ch.tikkosoft.protopass.model.ProtoPassField
import ch.tikkosoft.protopass.model.SecretValues
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.Properties

/**
 * Class responsible for fetching secrets from Proton Pass (via `pass-cli`), saving them to files,
 * and exposing them as properties.
 */
class ProtoPassSecretsFetcher(
    private val extension: ProtoPassExtension,
    private val logger: Logger,
    private val execOperations: ExecOperations,
    private val buildDirectory: File,
    private val propertyResolver: (String) -> String?,
    private val envVarResolver: (String) -> String?
) {

    private data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String) {
        val isSuccess: Boolean get() = exitCode == 0
    }

    /**
     * Validates the Proton Pass configuration and tests connectivity.
     */
    fun validateConfiguration() {
        val effectiveVaultName = determineEffectiveValue("vaultName", extension.vaultName.orNull)

        validateCliAvailable()
        ensureLoggedIn()
        requireVaultExists(effectiveVaultName)

        // Only do the item checks if there are no product flavors
        if (extension.productFlavors.isEmpty()) {
            val effectiveItemTitle = determineEffectiveValue("itemTitle", extension.itemTitle.orNull)
            requireItemExists(effectiveItemTitle, effectiveVaultName)
            reportMissingFields(extension.getFields(), effectiveItemTitle, effectiveVaultName, context = "")
        }

        // Validate each product flavor
        extension.productFlavors.forEach { flavor ->
            val effectiveItemTitle = determineEffectiveValue("itemTitle", flavor.itemTitle.orNull ?: extension.itemTitle.orNull)
            requireItemExists(effectiveItemTitle, effectiveVaultName)
            reportMissingFields(
                extension.getFields() + flavor.getFields(),
                effectiveItemTitle,
                effectiveVaultName,
                context = " for flavor '${flavor.name}'"
            )
        }
    }

    /**
     * Fetches secrets from Proton Pass and writes them to the extension.outputFileName file.
     */
    fun fetchAndSaveSecrets() {
        val effectiveVaultName = determineEffectiveValue("vaultName", extension.vaultName.orNull)
        val effectiveItemTitle = determineEffectiveValue("itemTitle", extension.itemTitle.orNull)
        val effectiveOutputFileName = determineEffectiveValue("outputFileName", extension.outputFileName.get())

        fetchAndSave(extension.getFields(), effectiveVaultName, effectiveItemTitle, effectiveOutputFileName, context = "")
    }

    /**
     * Fetches secrets for a specific flavor and writes them to the extension.outputFileName file.
     */
    fun fetchAndSaveSecretsForFlavor(flavorConfig: ProtoPassExtension.ProductFlavorConfig) {
        val effectiveVaultName = determineEffectiveValue("vaultName", extension.vaultName.orNull)
        val effectiveItemTitle = determineEffectiveValue(
            "itemTitle",
            flavorConfig.itemTitle.orNull ?: extension.itemTitle.orNull
        )
        val effectiveOutputFileName = determineEffectiveValue("outputFileName", extension.outputFileName.get())

        // Combine global fields and flavor-specific fields
        val allFields = extension.getFields() + flavorConfig.getFields()
        fetchAndSave(allFields, effectiveVaultName, effectiveItemTitle, effectiveOutputFileName, context = " for flavor '${flavorConfig.name}'")
    }

    private fun fetchAndSave(
        fields: List<ProtoPassField>,
        vaultName: String,
        itemTitle: String,
        outputFileName: String,
        context: String
    ) {
        validateCliAvailable()
        ensureLoggedIn()

        val fieldValues = mutableMapOf<String, String>()

        logger.lifecycle("Fetching ${fields.size} fields from Proton Pass$context...")

        fields.forEach { field ->
            val value = fetchField(itemTitle, field.protonField, vaultName, logFailure = true)
            if (value != null) {
                fieldValues[field.projectPropertyName] = value
            }
        }

        logger.lifecycle("Successfully fetched ${fieldValues.size} out of ${fields.size} fields$context.")

        val secretsFile = File(ensureBuildDirectoryExists(), outputFileName)
        writeSecretsToProperties(fields, SecretValues(fieldValues), secretsFile)
    }

    /**
     * Writes the fetched secrets to a properties file.
     */
    private fun writeSecretsToProperties(fields: List<ProtoPassField>, secrets: SecretValues, secretsFile: File) {
        val properties = Properties()
        fields.forEach { field ->
            val value = secrets.getOptionalString(field.projectPropertyName)
            if (value == null) {
                logger.warn("No value found for field '${field.projectPropertyName}' (Proton Pass field: ${field.protonField})")
                return@forEach
            }
            when (field) {
                is ProtoPassField.StringField -> {
                    properties.setProperty(field.projectPropertyName, value)
                    logger.lifecycle("Added '${field.projectPropertyName}' to ${secretsFile.name}")
                }

                is ProtoPassField.FileField -> {
                    try {
                        val file = writeBase64File(value, field.fileName)
                        properties.setProperty(field.projectPropertyName, file.absolutePath)
                        logger.lifecycle("Saved file for '${field.projectPropertyName}' at ${file.absolutePath} and added to ${secretsFile.name}")
                    } catch (e: Exception) {
                        logger.error("Failed to save file for '${field.projectPropertyName}': ${e.message}")
                    }
                }
            }
        }
        secretsFile.writer().use { writer ->
            properties.store(writer, "Secrets from Proton Pass")
        }
        logger.lifecycle("Wrote secrets to ${secretsFile.absolutePath}")
    }

    private fun reportMissingFields(fields: List<ProtoPassField>, itemTitle: String, vaultName: String, context: String) {
        val missingFields = fields
            .filter { fetchField(itemTitle, it.protonField, vaultName, logFailure = false) == null }
            .map { it.protonField }

        if (missingFields.isNotEmpty()) {
            logger.warn("The following fields are not found in the Proton Pass item '$itemTitle'$context: ${missingFields.joinToString(", ")}")
        } else {
            logger.lifecycle("All ${fields.size} fields exist in the Proton Pass item '$itemTitle'$context.")
        }
    }

    /**
     * Checks that a vault exists by listing its items.
     */
    private fun requireVaultExists(vaultName: String) {
        val result = runCommand("pass-cli", "item", "list", "--vault-name", vaultName, "--output", "json")
        if (!result.isSuccess) {
            logger.error("Vault '$vaultName' not found in Proton Pass account: ${result.stderr.trim()}")
            throw IllegalArgumentException("Vault '$vaultName' not found.")
        }
    }

    /**
     * Checks that an item exists in the specified vault.
     */
    private fun requireItemExists(itemTitle: String, vaultName: String) {
        val result = runCommand(
            "pass-cli", "item", "view",
            "--vault-name", vaultName,
            "--item-title", itemTitle,
            "--output", "json"
        )
        if (!result.isSuccess) {
            logger.error("Item '$itemTitle' not found in vault '$vaultName': ${result.stderr.trim()}")
            throw IllegalArgumentException("Item '$itemTitle' not found.")
        }
    }

    /**
     * Writes a file from base64-encoded content.
     */
    private fun writeBase64File(base64Content: String, fileName: String): File {
        ensureBuildDirectoryExists()
        return File(buildDirectory, fileName).apply {
            // MIME decoder tolerates line breaks that may be introduced when pasting into Proton Pass
            writeBytes(Base64.getMimeDecoder().decode(base64Content))
        }
    }

    /**
     * Determines the effective value for a configuration field.
     */
    private fun determineEffectiveValue(propertyName: String, extensionValue: String?): String {
        val envVarName = "PROTOPASS_${propertyName.uppercase()}"
        val value = extensionValue
            ?: propertyResolver(propertyName)
            ?: envVarResolver(envVarName)
            ?: throw IllegalArgumentException(
                """
                $propertyName must be provided via one of these methods:
                1) Extension in build.gradle(.kts):
                   protoPass {
                       $propertyName = "your-value"
                   }
                2) Project property: -P$propertyName=your-value
                3) Environment variable: $envVarName=your-value
                """.trimIndent()
            )
        require(value.isNotBlank()) { "$propertyName must not be blank" }
        return value
    }

    /**
     * Validates that the Proton Pass CLI (`pass-cli`) is available on the system.
     */
    private fun validateCliAvailable() {
        val available = try {
            runCommand("pass-cli", "--version").isSuccess
        } catch (e: Exception) {
            false
        }
        require(available) {
            "Error: Proton Pass CLI (`pass-cli`) not found. Install it from https://protonpass.github.io/pass-cli/"
        }
    }

    /**
     * Ensures there is an active Proton Pass session.
     *
     * An existing session (from a previous `pass-cli login`) is reused. Otherwise, if a personal access
     * token is available (environment variable `PROTON_PASS_PERSONAL_ACCESS_TOKEN` or Gradle property
     * `protonPassPersonalAccessToken`), a non-interactive login is performed, which is the recommended
     * way for CI. Interactive logins are not started from Gradle.
     */
    private fun ensureLoggedIn() {
        if (runCommand("pass-cli", "info").isSuccess) return

        val token = envVarResolver(PAT_ENV_VAR) ?: propertyResolver(PAT_PROPERTY)
        if (token.isNullOrBlank()) {
            throw IllegalStateException(
                """
                Not logged in to Proton Pass. Either:
                1) Run `pass-cli login` once in your terminal, or
                2) Provide a personal access token via the $PAT_ENV_VAR environment variable
                   or the -P$PAT_PROPERTY=... Gradle property (recommended for CI).
                """.trimIndent()
            )
        }

        logger.lifecycle("Logging in to Proton Pass with personal access token...")
        // The token is passed via the environment so it never shows up in the process list
        val result = runCommand("pass-cli", "login", extraEnvironment = mapOf(PAT_ENV_VAR to token))
        check(result.isSuccess) { "Failed to log in to Proton Pass (exit code ${result.exitCode}): ${result.stderr.trim()}" }
    }

    /**
     * Fetches an optional field from a Proton Pass item.
     */
    private fun fetchField(itemTitle: String, field: String, vaultName: String, logFailure: Boolean): String? {
        logger.debug("Fetching field: $field")
        val result = runCommand(
            "pass-cli", "item", "view",
            "--vault-name", vaultName,
            "--item-title", itemTitle,
            "--field", field
        )
        if (!result.isSuccess) {
            if (logFailure) logger.error("Failed to fetch field '$field' from '$itemTitle': ${result.stderr.trim()}")
            return null
        }
        // Only strip the trailing newline added by the CLI; whitespace can be part of a secret
        return result.stdout.trimEnd('\r', '\n').ifBlank { null }
    }

    /**
     * Runs a command and captures its output.
     */
    private fun runCommand(vararg args: String, extraEnvironment: Map<String, String> = emptyMap()): CommandResult {
        val output = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()

        val result = execOperations.exec {
            commandLine = args.toList()
            environment(extraEnvironment)
            standardOutput = output
            this.errorOutput = errorOutput
            isIgnoreExitValue = true
        }

        return CommandResult(
            exitCode = result.exitValue,
            stdout = output.toString(Charsets.UTF_8),
            stderr = errorOutput.toString(Charsets.UTF_8)
        )
    }

    /**
     * Ensures the build directory exists.
     */
    private fun ensureBuildDirectoryExists(): File {
        if (!buildDirectory.exists()) {
            logger.lifecycle("Creating build directory: ${buildDirectory.absolutePath}")
            buildDirectory.mkdirs()
        }
        return buildDirectory
    }

    private companion object {
        const val PAT_ENV_VAR = "PROTON_PASS_PERSONAL_ACCESS_TOKEN"
        const val PAT_PROPERTY = "protonPassPersonalAccessToken"
    }
}
