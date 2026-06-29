import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("androidx.room") version "2.8.4"
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

tasks.register<Copy>("syncRoomSchemas") {
    description = "Copies Room schema JSON files into generated assets so Room can verify data integrity at runtime."
    group = "room"
    from("$projectDir/schemas")
    include("**/*.json")
    into("$buildDir/generated/room_schemas/room_schemas")
}

tasks.configureEach {
    if (
        name != "syncRoomSchemas" &&
        (
            name.contains("Lint", ignoreCase = true) ||
                (
                    name.startsWith("merge") &&
                        name.endsWith("Assets")
                )
        )
    ) {
        dependsOn("syncRoomSchemas")
    }
}

val localProps =
    Properties()
        .apply {
            rootProject
                .file("local.properties")
                .takeIf { it.exists() }
                ?.inputStream()
                ?.use { load(it) }
        }

android {
    namespace = "com.hihusky.mnemora"
    compileSdk = 37

    sourceSets["main"].assets.srcDir("$buildDir/generated/room_schemas")

    defaultConfig {
        applicationId = "com.hihusky.mnemora"
        minSdk = 24
        targetSdk = 35
        versionCode = 16
        versionName = "0.0.16"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = localProps.getProperty("signing.storeFile")?.let { rootProject.file(it) }
            storePassword = localProps.getProperty("signing.storePassword")
            keyAlias = localProps.getProperty("signing.keyAlias")
            keyPassword = localProps.getProperty("signing.keyPassword")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Mnemora (test)")
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            resValue("string", "app_name", "Mnemora")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildFeatures {
        compose = true
        resValues = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2025.12.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // HTTP
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    // Markdown (Compose)
    implementation("com.mikepenz:multiplatform-markdown-renderer:0.40.2")
    implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.40.2")
    implementation("com.mikepenz:multiplatform-markdown-renderer-coil2:0.40.2")

    // LaTeX formula rendering for Compose
    implementation("io.github.huarangmeng:latex-renderer:1.3.9")
    implementation("io.github.huarangmeng:latex-parser:1.3.9")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("androidx.room:room-testing:2.8.4")
}

detekt {
    config.from(rootProject.file("detekt.yml"))
}
