package ch.tikkosoft.protopass

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertTrue

class ProtoPassPluginFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var buildFile: File
    private lateinit var settingsFile: File

    @Before
    fun setup() {
        buildFile = tempFolder.newFile("build.gradle.kts")
        settingsFile = tempFolder.newFile("settings.gradle.kts")
        settingsFile.writeText("rootProject.name = \"test-project\"")
    }

    @Test
    fun `can apply plugin and register tasks`() {
        buildFile.writeText("""
            plugins {
                id("ch.tikkosoft.protopass")
            }
            
            protoPass {
                vaultName.set("test-vault")
                itemTitle.set("test-item")
                
                productFlavors {
                    create("production") {
                        itemTitle.set("prod-item")
                    }
                }
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(tempFolder.root)
            .withArguments("tasks")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("fetchProtoPass - Fetches secrets from Proton Pass using default configuration."))
        assertTrue(result.output.contains("fetchProtoPassProduction - Fetches secrets from Proton Pass for 'production' flavor."))
        assertTrue(result.output.contains("validateProtoPassConfig - Validates Proton Pass configuration and field existence."))
        assertTrue(result.output.contains("incrementVersionCode - Increments version code based on date pattern"))
    }

    @Test
    fun `incrementVersionCode execution updates config file`() {
        val configFile = tempFolder.newFile("config.properties")
        configFile.writeText("VERSION_CODE=1\n")

        buildFile.writeText("""
            plugins {
                id("ch.tikkosoft.protopass")
            }
        """.trimIndent())

        GradleRunner.create()
            .withProjectDir(tempFolder.root)
            .withArguments("incrementVersionCode")
            .withPluginClasspath()
            .build()

        val content = configFile.readText()
        assertTrue(content.contains("VERSION_CODE="), "File should contain VERSION_CODE")
        val versionCode = content.lines().first { it.startsWith("VERSION_CODE=") }.split("=")[1]
        assertTrue(versionCode.toLong() > 1, "Version code should have been incremented to a date-based format")
    }

    @Test
    fun `configuration cache is supported`() {
        buildFile.writeText("""
            plugins {
                id("ch.tikkosoft.protopass")
            }
            protoPass {
                vaultName.set("test-vault")
                itemTitle.set("test-item")
            }
        """.trimIndent())

        // First run to store cache
        GradleRunner.create()
            .withProjectDir(tempFolder.root)
            .withArguments("help", "--configuration-cache")
            .withPluginClasspath()
            .build()

        // Second run to hit cache
        val result = GradleRunner.create()
            .withProjectDir(tempFolder.root)
            .withArguments("help", "--configuration-cache")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("Reusing configuration cache"))
    }

    @Test
    fun `validateProtoPassConfig fails when vaultName is missing`() {
        buildFile.writeText("""
            plugins {
                id("ch.tikkosoft.protopass")
            }
            protoPass {
                // missing vaultName
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(tempFolder.root)
            .withArguments("validateProtoPassConfig")
            .withPluginClasspath()
            .buildAndFail()

        assertTrue(result.output.contains("vaultName must be provided"))
        assertTrue(result.output.contains("PROTOPASS_VAULTNAME"))
    }

    @Test
    fun `validateProtoPassConfig fails when vaultName is blank`() {
        buildFile.writeText("""
            plugins {
                id("ch.tikkosoft.protopass")
            }
            protoPass {
                vaultName.set("  ")
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(tempFolder.root)
            .withArguments("validateProtoPassConfig")
            .withPluginClasspath()
            .buildAndFail()

        assertTrue(result.output.contains("vaultName must not be blank"))
    }
}
