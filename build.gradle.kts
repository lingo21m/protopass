plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.gradle.plugin.publish)
}

group = "ch.tikkosoft"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

/**
 * Gradle Plugin Portal configuration
 *
 * ./gradlew validatePlugins
 * ./gradlew publishPlugins
 */
gradlePlugin {
    website.set("https://bitbucket.org/approppo/protopass")
    vcsUrl.set("https://bitbucket.org/approppo/protopass")
    plugins {
        create("protopass") {
            id = "ch.tikkosoft.protopass"
            implementationClass = "ch.tikkosoft.protopass.ProtoPassPlugin"
            displayName = "ProtoPass Gradle Plugin"
            description = "A Gradle plugin for Android builds that securely retrieves signing credentials and secrets from a Proton Pass vault."
            tags.set(listOf("android", "signing", "proton", "protonpass", "security", "credentials"))
        }
    }
}

/**
 * Local Maven repository for testing
 *
 * ./gradlew publishToMavenLocal
 *
 */
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}
