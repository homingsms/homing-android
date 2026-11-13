package com.homingsms.app.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {

    @Test
    fun localModeSkipsPairing() {
        assertEquals(
            listOf(
                OnboardingStep.Welcome,
                OnboardingStep.Permissions,
                OnboardingStep.Battery,
                OnboardingStep.Mode,
                OnboardingStep.TestMessage
            ),
            OnboardingFlow.steps(GatewayMode.Local, needsRestrictedSettings = false)
        )
    }

    @Test
    fun cloudModeAndSideloadAddSteps() {
        val steps = OnboardingFlow.steps(GatewayMode.Both, needsRestrictedSettings = true)
        assertTrue(OnboardingStep.CloudPairing in steps)
        assertTrue(steps.indexOf(OnboardingStep.RestrictedSettings) < steps.indexOf(OnboardingStep.Permissions))
    }

    @Test
    fun nextAndPreviousFollowVisibleSteps() {
        val state = OnboardingUiState(step = OnboardingStep.Mode, mode = GatewayMode.Cloud)
        assertEquals(OnboardingStep.CloudPairing, OnboardingFlow.next(state))
        assertEquals(OnboardingStep.Battery, OnboardingFlow.previous(state))
        assertNull(OnboardingFlow.next(state.copy(step = OnboardingStep.TestMessage)))
    }

    @Test
    fun permissionsStepNeedsRequiredPermissionsOnly() {
        val state = OnboardingUiState(
            step = OnboardingStep.Permissions,
            permissions = OnboardingPermission.entries.map {
                PermissionItem(it, if (it.required) PermissionStatus.Granted else PermissionStatus.Denied)
            }
        )
        assertTrue(state.canContinue)
        assertFalse(state.copy(permissions = state.permissions.map { it.copy(status = PermissionStatus.Denied) }).canContinue)
    }
}
