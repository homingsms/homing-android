package com.homingsms.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.UsageBar
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.feature.connect.CloudPairingPanel

@Composable
fun OnboardingScreen(state: OnboardingUiState, actions: OnboardingActions, modifier: Modifier = Modifier) {
    HomingScaffold(
        modifier = modifier,
        bottomBar = { if (state.step != OnboardingStep.Welcome) OnboardingBottomBar(state, actions) }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(HomingTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)
            ) {
                if (state.step != OnboardingStep.Welcome) StepProgress(state)
                StepContent(state, actions)
            }
        }
    }
}

@Composable
private fun StepContent(state: OnboardingUiState, actions: OnboardingActions) {
    when (state.step) {
        OnboardingStep.Welcome -> WelcomeStep(state, actions)

        OnboardingStep.Permissions -> PermissionsStep(state, actions)

        OnboardingStep.Battery -> BatteryStep(state.battery, actions)

        OnboardingStep.RestrictedSettings -> RestrictedSettingsStep(actions)

        OnboardingStep.Mode -> ModeStep(state.mode, actions.onModeSelected)

        OnboardingStep.CloudPairing -> {
            StepHeading(
                title = stringResource(R.string.onboarding_pair_title),
                body = stringResource(R.string.onboarding_pair_body)
            )
            CloudPairingPanel(cloud = state.cloud, pairingCode = state.pairingCode, actions = actions.pairing, showHeader = false)
        }

        OnboardingStep.TestMessage -> TestMessageStep(state.test, actions)
    }
}

@Composable
private fun StepProgress(state: OnboardingUiState) {
    val total = state.steps.size - 1
    val current = state.stepIndex
    Column(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.onboarding_step_of, current, total),
            style = MaterialTheme.typography.labelMedium,
            color = HomingTheme.colors.mutedForeground
        )
        UsageBar(fraction = current.toFloat() / total, tone = StatusTone.Brand)
    }
}

@Composable
private fun OnboardingBottomBar(state: OnboardingUiState, actions: OnboardingActions) {
    val last = OnboardingFlow.next(state) == null
    Surface(color = HomingTheme.colors.background) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))) {
            HorizontalDivider(thickness = 1.dp, color = HomingTheme.colors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomingButton(
                    text = stringResource(R.string.onboarding_back),
                    onClick = actions.onBack,
                    style = HomingButtonStyle.Text
                )
                Spacer(Modifier.weight(1f))
                if (state.step == OnboardingStep.TestMessage && !state.test.arrived) {
                    HomingButton(
                        text = stringResource(R.string.onboarding_skip_test),
                        onClick = actions.onFinish,
                        style = HomingButtonStyle.Text
                    )
                    Spacer(Modifier.width(HomingTheme.spacing.sm))
                }
                HomingButton(
                    text = stringResource(if (last) R.string.onboarding_finish else R.string.onboarding_continue),
                    onClick = if (last) actions.onFinish else actions.onNext,
                    enabled = state.canContinue && (!last || state.test.arrived)
                )
            }
        }
    }
}

@Composable
internal fun StepHeading(title: String, body: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semanticsHeading()
        )
        if (body != null) {
            Text(text = body, style = MaterialTheme.typography.bodyLarge, color = HomingTheme.colors.mutedForeground)
        }
    }
}

internal fun Modifier.semanticsHeading(): Modifier = semantics { heading() }
