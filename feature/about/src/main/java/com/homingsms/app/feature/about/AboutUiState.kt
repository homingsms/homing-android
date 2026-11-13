package com.homingsms.app.feature.about

import androidx.annotation.RawRes
import androidx.compose.runtime.Immutable

enum class LicenseKind(@RawRes val textRes: Int) {
    Apache2(R.raw.license_apache_2_0),
    Mit(R.raw.license_mit),
    OflAspekta(com.homingsms.app.core.designsystem.R.raw.license_ofl_aspekta),
    OflJetBrainsMono(com.homingsms.app.core.designsystem.R.raw.license_ofl_jetbrains_mono),
    AospNotice(R.raw.notice_aosp)
}

@Immutable
data class OpenSourceComponent(val name: String, val holder: String, val license: LicenseKind)

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
    val components = listOf(
        OpenSourceComponent("Homing", "Homing", LicenseKind.Apache2),
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
        OpenSourceComponent("JNanoID", "Aventrix", LicenseKind.Mit),
        OpenSourceComponent("Aspekta", "Ivo Dolenc", LicenseKind.OflAspekta),
        OpenSourceComponent("JetBrains Mono", "The JetBrains Mono Project Authors", LicenseKind.OflJetBrainsMono),
        OpenSourceComponent("MMS PDU encoding", "Esmertec AG, The Android Open Source Project", LicenseKind.AospNotice)
    )
}
