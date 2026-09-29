package ch.tikkosoft.protopass.extension

import ch.tikkosoft.protopass.model.ProtoPassField
import ch.tikkosoft.protopass.model.ProtoPassFileNames
import ch.tikkosoft.protopass.model.VersionStrategy
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension class for configuring the ProtoPass plugin with dynamic fields.
 */
open class ProtoPassExtension @Inject constructor(objectFactory: ObjectFactory) {
    val vaultName: Property<String> = objectFactory.property(String::class.java)
    val itemTitle: Property<String> = objectFactory.property(String::class.java)
    val outputFileName: Property<String> = objectFactory.property(String::class.java).convention(ProtoPassFileNames.OUTPUT_PROPERTIES)
    val configFileName: Property<String> = objectFactory.property(String::class.java).convention(ProtoPassFileNames.CONFIG_PROPERTIES)
    val versionStrategy: Property<VersionStrategy> = objectFactory.property(VersionStrategy::class.java).convention(VersionStrategy.DATE_CODE)

    // Container for product flavors configurations
    val productFlavors: NamedDomainObjectContainer<ProductFlavorConfig> =
        objectFactory.domainObjectContainer(ProductFlavorConfig::class.java) { name ->
            objectFactory.newInstance(ProductFlavorConfig::class.java, name)
        }

    private val fields = mutableListOf<ProtoPassField>()

    fun productFlavors(action: Action<NamedDomainObjectContainer<ProductFlavorConfig>>) {
        action.execute(productFlavors)
    }

    fun stringField(protonField: String, projectPropertyName: String) {
        fields.add(ProtoPassField.StringField(protonField, projectPropertyName))
    }

    fun fileField(protonField: String, projectPropertyName: String, fileName: String) {
        fields.add(ProtoPassField.FileField(protonField, projectPropertyName, fileName))
    }

    fun getFields(): List<ProtoPassField> = fields.toList()

    /**
     * Rejects values that are configured but blank. Missing values are resolved later from
     * Gradle properties or environment variables (see ProtoPassSecretsFetcher).
     */
    fun validate() {
        require(vaultName.orNull?.isBlank() != true) { "vaultName must not be blank" }
        require(itemTitle.orNull?.isBlank() != true) { "itemTitle must not be blank" }

        productFlavors.forEach { flavor ->
            require(flavor.itemTitle.orNull?.isBlank() != true) { "itemTitle must not be blank for product flavor '${flavor.name}'" }
        }
    }

    // Flavor-specific configuration
    open class ProductFlavorConfig @Inject constructor(val name: String, objectFactory: ObjectFactory) {
        val itemTitle: Property<String> = objectFactory.property(String::class.java)
        private val fields = mutableListOf<ProtoPassField>()

        // Add methods to define flavor-specific fields
        fun stringField(protonField: String, projectPropertyName: String) {
            fields.add(ProtoPassField.StringField(protonField, projectPropertyName))
        }

        fun fileField(protonField: String, projectPropertyName: String, fileName: String) {
            fields.add(ProtoPassField.FileField(protonField, projectPropertyName, fileName))
        }

        fun getFields(): List<ProtoPassField> = fields.toList()
    }
}
