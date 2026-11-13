import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.aboutlibraries.android)
}

if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

val defaultCloudUrl = "https://api.homingsms.com"
val cloudUrl = providers.gradleProperty("homing.cloudUrl")
    .orElse(providers.environmentVariable("HOMING_CLOUD_URL"))
    .getOrElse(defaultCloudUrl)

val releaseKeystore = file(
    providers.environmentVariable("SIGNING_KEY_STORE_PATH").getOrElse("keystore.jks")
)
val releaseSigningEnv = listOf("SIGNING_STORE_PASSWORD", "SIGNING_KEY_ALIAS", "SIGNING_KEY_PASSWORD")
    .associateWith { providers.environmentVariable(it).orNull }
val releaseSigningAvailable = releaseKeystore.isFile && releaseSigningEnv.values.all { !it.isNullOrBlank() }

android {
    namespace = "com.homingsms.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.homingsms.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "CLOUD_URL", "\"$cloudUrl\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseSigningEnv.getValue("SIGNING_STORE_PASSWORD")
                keyAlias = releaseSigningEnv.getValue("SIGNING_KEY_ALIAS")
                keyPassword = releaseSigningEnv.getValue("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config_debug"
        }
        create("debugInsecure") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config_insecure"
        }
        create("insecure") {
            initWith(getByName("debug"))
            isDebuggable = true
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
            manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config_insecure"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
            manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE.md"
            )
        }
    }

    testOptions {
        unitTests {
            // EventBus.emit logs via android.util.Log; local unit tests must not
            // fail on the mockable android.jar "not mocked" stub.
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    lint {
        abortOnError = true
        checkDependencies = false
        baseline = file("lint-baseline.xml")
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
    baseline = rootProject.file("config/detekt/baseline.xml")
    parallel = false
}

ktlint {
    version.set(libs.versions.ktlint.get())
    android.set(true)
    baseline.set(rootProject.file("config/ktlint/baseline.xml"))
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:telephony"))
    implementation(project(":core:server"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:connect"))
    implementation(project(":feature:messages"))
    implementation(project(":feature:inbox"))
    implementation(project(":feature:webhooks"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:about"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)

    implementation(libs.okhttp)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.libphonenumber)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

aboutLibraries {
    offlineMode = true
    collect {
        fetchRemoteLicense = false
        fetchRemoteFunding = false
        filterVariants.addAll("release")
    }
    export {
        excludeFields.addAll("funding", "scm", "description")
    }
    library {
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    }
}

configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
        }
    }
}
