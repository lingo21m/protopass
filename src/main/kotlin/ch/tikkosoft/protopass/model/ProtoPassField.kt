package ch.tikkosoft.protopass.model

/**
 * Sealed class representing different types of fields that can be retrieved from Proton Pass.
 *
 * This class hierarchy provides a type-safe way to define fields that should be fetched from
 * a Proton Pass item and how they should be represented in the Gradle project.
 *
 * Constants for field names and property names are available in [ProtoPassFields] and [ProjectProperties].
 *
 * @property protonField The name of the field in the Proton Pass item. This is either a built-in field
 *                 (e.g. `password`, `username`, `note`) or the name of a custom field as shown in the
 *                 Proton Pass UI. When retrieving values via the Proton Pass CLI, this is used with the
 *                 `--field <value>` parameter. Fields inside a section can be addressed as `Section.field`.
 *
 * @property projectPropertyName The name of the Gradle project property that will be set with the
 *                       value retrieved from Proton Pass. This defines how you'll access the value
 *                       in your build scripts via `project.property(name)`.
 */
sealed class ProtoPassField(val protonField: String, val projectPropertyName: String) {

    /**
     * Represents a field that contains a string value to be directly set as a project property.
     *
     * Example usage in build.gradle.kts:
     * ```
     * protoPass {
     *     stringField(ProtoPassFields.KEYSTORE_PASSWORD, ProjectProperties.KEYSTORE_PASSWORD)
     * }
     * ```
     */
    class StringField(protonField: String, projectPropertyName: String) : ProtoPassField(protonField, projectPropertyName)

    /**
     * Represents a field that contains a Base64-encoded file that should be decoded and saved to disk.
     *
     * @property fileName The name of the file to be created in the build directory.
     *
     * Example usage in build.gradle.kts:
     * ```
     * protoPass {
     *     fileField(ProtoPassFields.BASE64_KEYSTORE, ProjectProperties.KEYSTORE_PATH, "keystore.jks")
     * }
     * ```
     */
    class FileField(
        protonField: String,
        projectPropertyName: String,
        val fileName: String
    ) : ProtoPassField(protonField, projectPropertyName)
}
