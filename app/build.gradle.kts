import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory(
        layout.projectDirectory
            .dir("schemas")
            .asFile.path,
    )
}

val generatedRoomSchemas = layout.buildDirectory.dir("generated/room_schemas")

tasks.register<Copy>("syncRoomSchemas") {
    description = "Copies Room schema JSON files into generated assets so Room can verify data integrity at runtime."
    group = "room"
    from(layout.projectDirectory.dir("schemas"))
    include("**/*.json")
    into(generatedRoomSchemas.map { it.dir("room_schemas") })
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

    sourceSets["main"].assets.directories.add(generatedRoomSchemas.get().asFile.path)

    defaultConfig {
        applicationId = "com.hihusky.mnemora"
        minSdk = 24
        targetSdk = 36
        versionCode = 23
        versionName = "0.0.23"

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
    lint {
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = true
        baseline = rootProject.file("lint-baseline.xml")
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // JSON
    implementation(libs.kotlinx.serialization.json)

    // HTTP
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp)

    // Markdown (Compose)
    implementation(libs.markdown.renderer)
    implementation(libs.markdown.renderer.m3)
    implementation(libs.markdown.renderer.coil3)

    // LaTeX formula rendering for Compose
    implementation(libs.latex.renderer)
    implementation(libs.latex.parser)

    // Testing
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.mockwebserver3)
}

ktlint {
    version.set(libs.versions.ktlintEngine)
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    basePath = rootProject.projectDir.absolutePath
    config.from(rootProject.file("detekt.yml"))
    baseline = rootProject.file("detekt-baseline.xml")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
}
