package ch.tikkosoft.protopass.model


/*
    const val: Values are inlined at compile time (causing the obfuscation issue)
    val: Values are accessed at runtime through the object reference
 */

// Proton Pass custom field names
object ProtoPassFields {
    val KEYSTORE_PASSWORD = "KEYSTORE_PASSWORD"
    val KEY_ALIAS = "KEY_ALIAS"
    val KEY_PASSWORD = "KEY_PASSWORD"
    val BASE64_KEYSTORE = "BASE64_KEYSTORE"
    val BASE64_GOOGLE_FIREBASE_SA_JSON = "BASE64_GOOGLE_FIREBASE_SA_JSON"
    val BASE64_GOOGLE_PLAY_SA_JSON = "BASE64_GOOGLE_PLAY_SA_JSON"
}

// Project property names
object ProjectProperties {
    val KEYSTORE_PASSWORD = "KEYSTORE_PASSWORD"
    val KEY_ALIAS = "KEY_ALIAS"
    val KEY_PASSWORD = "KEY_PASSWORD"
    val KEYSTORE_PATH = "KEYSTORE_PATH"
    val VERSION_CODE = "VERSION_CODE"
    val VERSION_NAME = "VERSION_NAME"
    val GOOGLE_FIREBASE_SA_PATH = "GOOGLE_FIREBASE_SA_PATH"
    val GOOGLE_PLAY_SA_PATH = "GOOGLE_PLAY_SA_PATH"
}

// File names
object ProtoPassFileNames {
    val KEYSTORE = "temp-keystore.jks"
    val GOOGLE_FIREBASE_SA = "temp-google-firebase-sa.json"
    val GOOGLE_PLAY_SA = "temp-google-play-sa.json"
    val OUTPUT_PROPERTIES = "output.properties"
    val CONFIG_PROPERTIES = "config.properties"
    val VERSION_FILE = "version.txt"
}
