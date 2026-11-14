package com.homingsms.app.feature.about

import androidx.annotation.RawRes
import androidx.compose.runtime.Immutable

enum class LicenseKind(@RawRes val textRes: Int) {
    Apache2(R.raw.license_apache_2_0),
    Mit(R.raw.license_mit),
    OflAspekta(com.homingsms.app.core.designsystem.R.raw.license_ofl_aspekta),
    OflJetBrainsMono(com.homingsms.app.core.designsystem.R.raw.license_ofl_jetbrains_mono),
    AospNotice(R.raw.notice_aosp),
    HomingNotice(R.raw.notice_homing)
}

sealed interface ComponentLicense {
    data class Bundled(val kind: LicenseKind) : ComponentLicense

    data class External(val name: String, val text: String? = null, val url: String? = null) : ComponentLicense
}

@Immutable
data class OpenSourceComponent(val name: String, val holder: String, val license: ComponentLicense, val version: String? = null) {
    constructor(name: String, holder: String, kind: LicenseKind) : this(name, holder, ComponentLicense.Bundled(kind))
}

@Immutable
data class AboutUiState(
    val versionName: String,
    val versionCode: Long? = null,
    val buildType: String? = null,
    val components: List<OpenSourceComponent> = AboutDefaults.components
)

@Immutable
data class AboutActions(val onBack: (() -> Unit)? = null, val onOpenWebsite: () -> Unit = {})

object AboutDefaults {
    val homing = OpenSourceComponent("Homing", "Homing", LicenseKind.HomingNotice)

    val bundledAssets = listOf(
        OpenSourceComponent("Aspekta", "Ivo Dolenc", LicenseKind.OflAspekta),
        OpenSourceComponent("JetBrains Mono", "The JetBrains Mono Project Authors", LicenseKind.OflJetBrainsMono),
        OpenSourceComponent("MMS PDU encoding", "Esmertec AG, The Android Open Source Project", LicenseKind.AospNotice)
    )

    val components = listOf(
        homing,
        OpenSourceComponent(
            "Android Jetpack, Jetpack Compose, CameraX, Room, WorkManager",
            "The Android Open Source Project",
            LicenseKind.Apache2
        ),
        OpenSourceComponent("Material Components for Android", "The Android Open Source Project", LicenseKind.Apache2),
        OpenSourceComponent("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains s.r.o.", LicenseKind.Apache2),
        OpenSourceComponent("Ktor", "JetBrains s.r.o.", LicenseKind.Apache2),
        OpenSourceComponent("OkHttp", "Square, Inc.", LicenseKind.Apache2),
        OpenSourceComponent("Koin", "Kotzilla and Koin contributors", LicenseKind.Apache2),
        OpenSourceComponent("ZXing", "ZXing authors", LicenseKind.Apache2),
        OpenSourceComponent("libphonenumber", "Google LLC", LicenseKind.Apache2),
        OpenSourceComponent("JNanoID", "Aventrix", LicenseKind.Mit)
    ) + bundledAssets
}
