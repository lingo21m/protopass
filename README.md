# ProtoPass Gradle Plugin

A Gradle plugin for Android builds that securely retrieves signing credentials and secrets from a [Proton Pass](https://proton.me/pass) vault using the [Proton Pass CLI](https://protonpass.github.io/pass-cli/).

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/ch.tikkosoft.protopass)](https://plugins.gradle.org/plugin/ch.tikkosoft.protopass)

## Installation

Add the plugin to your project's `build.gradle.kts`:

```kotlin
plugins {
    id("ch.tikkosoft.protopass") version "1.0.0"
}
```

## Prerequisites

- **Proton Pass CLI (`pass-cli`)** installed on your system:
  ```bash
  curl -fsSL https://proton.me/download/pass-cli/install.sh | bash
  ```
- **A Proton account** with Proton Pass
- **A vault with an item containing your credentials as custom fields**

## Authentication

The plugin reuses your existing `pass-cli` session and never starts an interactive login from Gradle.

**Locally**, log in once in your terminal:

```bash
pass-cli login
```

**On CI**, create a [personal access token](https://protonpass.github.io/pass-cli/commands/login/) scoped to your signing vault and expose it as an environment variable. When no session is active, the plugin logs in with it automatically:

```bash
export PROTON_PASS_PERSONAL_ACCESS_TOKEN=pst_xxxx::TOKENKEY
./gradlew fetchProtoPass
```

Alternatively, pass it as the Gradle property `-PprotonPassPersonalAccessToken=...`. Note that personal access token sessions expire after 2 hours.

## Basic Configuration

Configure the plugin in your build file:

```kotlin
protoPass {
    vaultName = "Android"                // Your vault name
    itemTitle = "Android Signing Keys"   // Title of the item holding your secrets

    // Optional: Configure output file names
    outputFileName = "output.properties" // Default
    configFileName = "config.properties" // Default

    // Optional: Configure versioning strategy
    versionStrategy = VersionStrategy.DATE_CODE // Default. Options: DATE_CODE, NUMBER

    // Define string fields to fetch directly
    stringField(ProtoPassFields.KEYSTORE_PASSWORD, ProjectProperties.KEYSTORE_PASSWORD)
    stringField(ProtoPassFields.KEY_ALIAS, ProjectProperties.KEY_ALIAS)
    stringField(ProtoPassFields.KEY_PASSWORD, ProjectProperties.KEY_PASSWORD)

    // Define base64-encoded files that should be decoded and saved
    fileField(ProtoPassFields.BASE64_KEYSTORE, ProjectProperties.KEYSTORE_PATH, "keystore.jks")
    fileField(ProtoPassFields.BASE64_GOOGLE_FIREBASE_SA_JSON, ProjectProperties.GOOGLE_FIREBASE_SA_PATH, "firebase-service-account.json")
    fileField(ProtoPassFields.BASE64_GOOGLE_PLAY_SA_JSON, ProjectProperties.GOOGLE_PLAY_SA_PATH, "play-service-account.json")
}
```

`vaultName` and `itemTitle` can also be provided as project properties (`-PvaultName=...`) or environment variables (`PROTOPASS_VAULTNAME`, `PROTOPASS_ITEMTITLE`).

Field names are passed to `pass-cli item view --field`, so built-in fields like `password` or `note` work too. Fields inside a section can be addressed as `SectionName.FIELD`.

## Product Flavor Support

Configure different secrets for each product flavor:

```kotlin
protoPass {
    vaultName = "Android"

    // Define fields common across all flavors
    stringField(ProtoPassFields.KEYSTORE_PASSWORD, ProjectProperties.KEYSTORE_PASSWORD)

    // Configure flavor-specific secrets
    productFlavors {
        create("production") {
            itemTitle = "Production Keys"
            fileField(ProtoPassFields.BASE64_KEYSTORE, ProjectProperties.KEYSTORE_PATH, "production.keystore")
        }

        create("staging") {
            itemTitle = "Staging Keys"
            fileField(ProtoPassFields.BASE64_KEYSTORE, ProjectProperties.KEYSTORE_PATH, "staging.keystore")
        }
    }
}
```

## Setup Your Proton Pass Vault

Create an item (for example a Login or Custom item) in your vault and add these custom fields (use the *hidden* field type for secrets):

- `KEYSTORE_PASSWORD`: Your keystore password
- `KEY_ALIAS`: Your key alias
- `KEY_PASSWORD`: Your key password
- `BASE64_KEYSTORE`: Your keystore file encoded as base64 (`base64 -i keystore.jks | pbcopy`)
- `BASE64_GOOGLE_FIREBASE_SA_JSON`: Your Firebase service account JSON encoded as base64
- `BASE64_GOOGLE_PLAY_SA_JSON`: Your Google Play service account JSON encoded as base64

You can check a field from your terminal:

```bash
pass-cli item view --vault-name "Android" --item-title "Android Signing Keys" --field KEY_ALIAS
```

## Usage

### Fetch Secrets

To fetch secrets with the default configuration:

```bash
./gradlew fetchProtoPass
```

For flavor-specific secrets:

```bash
./gradlew fetchProtoPassProduction
./gradlew fetchProtoPassStaging
```

### Validate Configuration

To check that the vault, item and all fields exist without writing any secrets:

```bash
./gradlew validateProtoPassConfig
```

### Version Management

The plugin includes a task to manage version codes automatically.

To increment the version code:

```bash
./gradlew incrementVersionCode
```

This will update the `VERSION_CODE` property in your configured `configFileName` (default: `config.properties`).

Supported strategies:
- `VersionStrategy.DATE_CODE` (Default): Generates a version code based on the date (YYYYMMDD) plus a 2-digit counter (e.g., `2025122301`).
- `VersionStrategy.NUMBER`: Simply increments the version code by 1.

## Integration with Android Build

Use the fetched properties in your build.gradle.kts:

```kotlin
android {
    signingConfigs {
        create("release") {
            if (project.hasAllProperties(
                    ProjectProperties.KEYSTORE_PATH,
                    ProjectProperties.KEYSTORE_PASSWORD,
                    ProjectProperties.KEY_ALIAS,
                    ProjectProperties.KEY_PASSWORD
                )
            ) {
                storeFile = file(project.property(ProjectProperties.KEYSTORE_PATH) as String)
                storePassword = project.property(ProjectProperties.KEYSTORE_PASSWORD) as String
                keyAlias = project.property(ProjectProperties.KEY_ALIAS) as String
                keyPassword = project.property(ProjectProperties.KEY_PASSWORD) as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## Setting Properties from Environment Variables

The plugin provides an extension function to set Gradle project properties from environment variables:

```kotlin
android {
    setProjectPropertyFromEnv(ProjectProperties.KEYSTORE_PASSWORD, "KEYSTORE_PASSWORD")
    setProjectPropertyFromEnv(ProjectProperties.KEY_ALIAS, "KEY_ALIAS")
    setProjectPropertyFromEnv(ProjectProperties.KEY_PASSWORD, "KEY_PASSWORD")
}
```

## Cleanup

Secrets are written to the build directory, so they are removed when you run:

```bash
./gradlew clean
```

## Utility Functions

- `project.hasAllProperties(...)`: Check if all specified properties exist
- `project.setProjectPropertyFromEnv(...)`: Set a project property from an environment variable
- `project.loadProjectPropertiesFromFile(...)`: Load a properties file (e.g. the fetched `output.properties`) as project properties
- `project.readVersionCodeFromFile(...)`: Read `VERSION_CODE` from the config file

## Constants

```kotlin
// Field names in Proton Pass
ProtoPassFields.KEYSTORE_PASSWORD
ProtoPassFields.KEY_ALIAS
ProtoPassFields.KEY_PASSWORD
ProtoPassFields.BASE64_KEYSTORE
ProtoPassFields.BASE64_GOOGLE_FIREBASE_SA_JSON
ProtoPassFields.BASE64_GOOGLE_PLAY_SA_JSON

// Project property names
ProjectProperties.KEYSTORE_PASSWORD
ProjectProperties.KEY_ALIAS
ProjectProperties.KEY_PASSWORD
ProjectProperties.KEYSTORE_PATH
ProjectProperties.GOOGLE_FIREBASE_SA_PATH
ProjectProperties.GOOGLE_PLAY_SA_PATH
ProjectProperties.VERSION_CODE
ProjectProperties.VERSION_NAME

// File names
ProtoPassFileNames.KEYSTORE
ProtoPassFileNames.GOOGLE_FIREBASE_SA
ProtoPassFileNames.GOOGLE_PLAY_SA
ProtoPassFileNames.OUTPUT_PROPERTIES
ProtoPassFileNames.CONFIG_PROPERTIES
ProtoPassFileNames.VERSION_FILE
```
