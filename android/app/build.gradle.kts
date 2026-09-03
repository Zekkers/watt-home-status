import java.io.File
import java.util.Properties
import org.gradle.api.GradleException

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun envOrProp(envName: String, propName: String): String? {
    System.getenv(envName)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    return keystoreProperties.getProperty(propName)?.trim()?.takeIf { it.isNotEmpty() }
}

fun resolveKeystoreFile(path: String): File {
    val file = File(path)
    return if (file.isAbsolute) file else rootProject.file(path)
}

val storeFileValue = envOrProp("KEYSTORE_FILE", "storeFile")
val storePasswordValue = envOrProp("KEYSTORE_PASSWORD", "storePassword")
val keyAliasValue = envOrProp("KEY_ALIAS", "keyAlias")
val keyPasswordValue = envOrProp("KEY_PASSWORD", "keyPassword")
val familyStoreFile = storeFileValue?.let { resolveKeystoreFile(it) }

val familySigningError: String? = when {
    listOf(storeFileValue, storePasswordValue, keyAliasValue, keyPasswordValue).any { it.isNullOrEmpty() } ->
        "Family release signing is not configured. For a local sideload build, copy android/keystore.properties.example to android/keystore.properties and point storeFile at the family .jks. For GitHub Actions, set repo secrets KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD. Refusing to ship an unsigned or debug-signed APK as a family build."
    familyStoreFile?.isFile != true ->
        "Family keystore file was not found${familyStoreFile?.let { " at ${it.absolutePath}" } ?: ""}. Refusing to ship an unsigned or debug-signed APK as a family build."
    else -> null
}
val familySigningReady = familySigningError == null

android {
    namespace = "com.zekkers.watthome"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zekkers.watthome"
        minSdk = 26
        targetSdk = 35
        versionCode = 23
        versionName = "1.2.11"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (familySigningReady) {
            create("family") {
                storeFile = checkNotNull(familyStoreFile)
                storePassword = checkNotNull(storePasswordValue)
                keyAlias = checkNotNull(keyAliasValue)
                keyPassword = checkNotNull(keyPasswordValue)
            }
        }
    }

    buildTypes {
        debug {
            if (familySigningReady) {
                signingConfig = signingConfigs.getByName("family")
            }
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (familySigningReady) {
                signingConfig = signingConfigs.getByName("family")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

tasks.register<Copy>("copySideloadApk") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    into(layout.projectDirectory.dir("release"))
    rename { "watt-home-status.apk" }
}

val requireFamilyReleaseSigning = tasks.register("requireFamilyReleaseSigning") {
    doLast {
        val message = familySigningError
        if (message != null) {
            throw GradleException(message)
        }
    }
}

tasks.configureEach {
    if (name == "packageRelease" || name == "assembleRelease") {
        dependsOn(requireFamilyReleaseSigning)
    }
}

afterEvaluate {
    tasks.named("assembleRelease").configure {
        finalizedBy("copySideloadApk")
    }
}
