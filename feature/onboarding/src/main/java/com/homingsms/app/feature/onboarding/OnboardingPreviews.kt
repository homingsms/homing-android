package com.homingsms.app.feature.onboarding

import androidx.compose.runtime.Composable
import com.homingsms.app.core.designsystem.preview.HomingPreviewSurface
import com.homingsms.app.core.designsystem.preview.HomingPreviews

@HomingPreviews
@Composable
private fun OnboardingWelcomePreview() {
    HomingPreviewSurface { OnboardingScreen(OnboardingSamples.welcome, OnboardingActions()) }
}

@HomingPreviews
@Composable
private fun UpdatedTermsPreview() {
    HomingPreviewSurface { UpdatedTermsScreen(OnboardingSamples.updatedTerms, UpdatedTermsActions()) }
}

@HomingPreviews
@Composable
private fun OnboardingPermissionsPreview() {
    HomingPreviewSurface { OnboardingScreen(OnboardingSamples.permissions, OnboardingActions()) }
}

@HomingPreviews
@Composable
private fun OnboardingBatteryPreview() {
    HomingPreviewSurface { OnboardingScreen(OnboardingSamples.battery, OnboardingActions()) }
}

@HomingPreviews
@Composable
private fun OnboardingRestrictedPreview() {
    HomingPreviewSurface { OnboardingScreen(OnboardingSamples.restricted, OnboardingActions()) }
}

@HomingPreviews
@Composable
private fun OnboardingModePreview() {
    HomingPreviewSurface { OnboardingScreen(OnboardingSamples.mode, OnboardingActions()) }
}

@HomingPreviews
@Composable
private fun OnboardingPairingPreview() {
    HomingPreviewSurface { OnboardingScreen(OnboardingSamples.pairing, OnboardingActions()) }
}

@HomingPreviews
@Composable
private fun OnboardingTestPreview() {
    HomingPreviewSurface { OnboardingScreen(OnboardingSamples.testArrived, OnboardingActions()) }
}
