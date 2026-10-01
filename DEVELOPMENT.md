# ProtoPass Plugin Development Guide

This guide is intended for developers working on the ProtoPass Gradle plugin itself. It covers building, testing, and publishing the plugin.

## Project Structure

- `src/main/kotlin`: Plugin source code
- `src/test/kotlin`: Unit and integration tests
- `build.gradle.kts`: Build configuration and dependencies

## Local Development & Testing

The quickest way to try out changes in a real project is to build the plugin yourself, publish it to your local Maven repository (`~/.m2/repository`) and point the consumer project at `mavenLocal()`. Nothing is uploaded anywhere.

### Prerequisites

- **JDK 17**: The build uses a Java 17 toolchain. Gradle will pick up an installed JDK 17 automatically.
- No global Gradle installation is needed; use the included wrapper (`./gradlew`).

### Building the Plugin

Compile the plugin and run the test suite:

```bash
./gradlew build
```

The plugin JAR is written to `build/libs/`.

### Publishing to Local Maven

Publish the plugin to your local Maven repository:

```bash
./gradlew publishToMavenLocal
```

This installs two artifacts, both using the `version` from `build.gradle.kts`:

| Artifact | Location in `~/.m2/repository` |
| --- | --- |
| Plugin implementation (`ch.tikkosoft:protopass`) | `ch/tikkosoft/protopass/<version>/` |
| Plugin marker (`ch.tikkosoft.protopass.gradle.plugin`) | `ch/tikkosoft/protopass/ch.tikkosoft.protopass.gradle.plugin/<version>/` |

Gradle uses the plugin marker to resolve `id("ch.tikkosoft.protopass")` to the implementation artifact, so both must be present.

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

Keep `mavenLocal()` first so the locally built plugin takes precedence over a published version with the same number. Remove it again once you are done testing.

### Iterating on Changes

After changing the plugin, run `./gradlew publishToMavenLocal` again and rebuild the consumer project. Gradle caches resolved plugins, so if the consumer does not pick up your changes:

- Use a `-SNAPSHOT` version (e.g., `1.0.1-SNAPSHOT`) in both projects while developing, or
- Run the consumer build with `--refresh-dependencies`.

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