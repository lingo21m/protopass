package ch.tikkosoft.protopass

import ch.tikkosoft.protopass.extension.ProtoPassExtension
import ch.tikkosoft.protopass.task.FetchProtoPassTask
import ch.tikkosoft.protopass.task.IncrementVersionCodeTask
import ch.tikkosoft.protopass.task.ValidateProtoPassConfigTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class ProtoPassPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("protoPass", ProtoPassExtension::class.java)

        // Default task for non-flavor configuration
        project.tasks.register("fetchProtoPass", FetchProtoPassTask::class.java) {
            group = "protopass"
            description = "Fetches secrets from Proton Pass using default configuration."
            this.extension.set(extension)
        }

        // Create tasks for product flavors lazily
        extension.productFlavors.all {
            val flavor = this
            val taskName = "fetchProtoPass${flavor.name.replaceFirstChar { it.uppercase() }}"
            project.tasks.register(taskName, FetchProtoPassTask::class.java) {
                group = "protopass"
                description = "Fetches secrets from Proton Pass for '${flavor.name}' flavor."
                this.extension.set(extension)
                this.flavor.set(flavor)
            }
        }

        // Validation task
        project.tasks.register("validateProtoPassConfig", ValidateProtoPassConfigTask::class.java) {
            group = "protopass"
            description = "Validates Proton Pass configuration and field existence."
            this.extension.set(extension)
        }

        // Register increment version task
        project.tasks.register("incrementVersionCode", IncrementVersionCodeTask::class.java) {
            group = "protopass"
            description = "Increments version code based on date pattern (YYYYMMDD + counter)"

            configFile.set(project.layout.projectDirectory.file(extension.configFileName))
            versionStrategy.set(extension.versionStrategy)
        }
    }
}
