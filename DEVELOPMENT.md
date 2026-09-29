# ProtoPass Plugin Development Guide

This guide is intended for developers working on the ProtoPass Gradle plugin itself. It covers building, testing, and publishing the plugin.

## Project Structure

- `src/main/kotlin`: Plugin source code
- `src/test/kotlin`: Unit and integration tests
- `build.gradle.kts`: Build configuration and dependencies

## Local Development & Testing

### Publishing to Local Maven

To test the plugin in a local project without publishing it remotely, you can publish it to your local Maven repository (`~/.m2/repository`).

Run the following command:

```bash
./gradlew publishToMavenLocal
```

### Consuming the Local Plugin

In the consumer project (e.g., an Android app), update `settings.gradle.kts` to include `mavenLocal()` in the plugin repositories:

```kotlin
pluginManagement {
    repositories {
        mavenLocal() // Add this line
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
```

Then, in the consumer's `build.gradle.kts`, apply the plugin with the version you just published (check `build.gradle.kts` in this project for the current version):

```kotlin
plugins {
    id("ch.tikkosoft.protopass") version "1.0.0" // Use the version from build.gradle.kts
}
```

## Publishing to Gradle Plugin Portal

### Prerequisites

1.  **Gradle Plugin Portal Account**: You need an account on [plugins.gradle.org](https://plugins.gradle.org/).
2.  **API Keys**: Get your API key and secret from your profile page.
3.  **`gradle.properties`**: Add your keys to `~/.gradle/gradle.properties` (global) or project-level `gradle.properties` (not recommended for secrets).

```properties
gradle.publish.key=YOUR_KEY
gradle.publish.secret=YOUR_SECRET
```

### Publishing Steps

1.  **Update Version**: Update the `version` in `build.gradle.kts`.
2.  **Run Validation**: Ensure the plugin metadata is correct.

    ```bash
    ./gradlew validatePlugins
    ```

3.  **Publish**:

    ```bash
    ./gradlew publishPlugins
    ```

This will upload the plugin to the Gradle Plugin Portal. It may take a few minutes to become available.

## Best Practices

### Versioning

- Follow [Semantic Versioning](https://semver.org/) (MAJOR.MINOR.PATCH).
- Use `-SNAPSHOT` for development versions (e.g., `1.0.1-SNAPSHOT`) to avoid caching issues during local testing.

### Testing

- **Unit Tests**: Write tests for your logic in `src/test/kotlin`.
- **Integration Tests**: Use `GradleRunner` in your tests to simulate applying the plugin to a real project.

### Code Style

- Keep the `ProtoPassExtension` clean and documented, as it's the public API for the plugin users.