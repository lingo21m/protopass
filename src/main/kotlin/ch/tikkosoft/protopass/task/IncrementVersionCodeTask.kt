package ch.tikkosoft.protopass.task


import ch.tikkosoft.protopass.internal.VersionHandler
import ch.tikkosoft.protopass.model.VersionStrategy
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class IncrementVersionCodeTask @Inject constructor() : DefaultTask() {

    @get:InputFile
    abstract val configFile: RegularFileProperty

    @get:Input
    abstract val versionStrategy: Property<VersionStrategy>

    @TaskAction
    fun run() {
        val versionHandler = VersionHandler()
        val file = configFile.get().asFile
        val strategy = versionStrategy.getOrElse(VersionStrategy.DATE_CODE)
        val newVersionCode = versionHandler.calculateVersionCode(file, strategy)
        logger.lifecycle("Version code incremented to $newVersionCode using strategy $strategy")
    }
}
