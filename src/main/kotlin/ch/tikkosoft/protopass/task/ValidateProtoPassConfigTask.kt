package ch.tikkosoft.protopass.task

import ch.tikkosoft.protopass.extension.ProtoPassExtension
import ch.tikkosoft.protopass.internal.ProtoPassSecretsFetcher
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class ValidateProtoPassConfigTask @Inject constructor(
    private val layout: ProjectLayout,
    private val execOperations: ExecOperations,
    private val providers: ProviderFactory
) : DefaultTask() {

    @get:Internal
    abstract val extension: Property<ProtoPassExtension>

    @TaskAction
    fun run() {
        val ext = extension.orNull ?: throw IllegalStateException("ProtoPassExtension not set")
        ext.validate()
        val secretsFetcher = ProtoPassSecretsFetcher(
            extension = ext,
            logger = logger,
            execOperations = execOperations,
            buildDirectory = layout.buildDirectory.get().asFile,
            propertyResolver = { name -> providers.gradleProperty(name).orNull },
            envVarResolver = { name -> providers.environmentVariable(name).orNull }
        )
        secretsFetcher.validateConfiguration()
    }
}
