import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.homingsms.app.feature.messages"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        targetSdk = 35
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        targetSdk = 35
        abortOnError = true
        checkDependencies = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml", "config/detekt/compose.yml"))
    parallel = false
}

ktlint {
    version.set(libs.versions.ktlint.get())
    android.set(true)
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

val roborazziTaskRequested = gradle.startParameter.taskNames.any { it.contains("roborazzi", ignoreCase = true) }

tasks.withType<Test>().configureEach {
    maxHeapSize = "3g"
    if (!roborazziTaskRequested) {
        systemProperty("roborazzi.test.verify", "true")
    }
}

dependencies {
    implementation(project(":core:designsystem"))

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(project(":core:testing"))
    testImplementation(libs.androidx.compose.ui.test.manifest)
}

configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
        }
    }
}
