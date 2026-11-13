package com.homingsms.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingTextField
import com.homingsms.app.core.designsystem.component.HomingTimeline
import com.homingsms.app.core.designsystem.component.TimelineEntry
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.util.formatClockTime

@Composable
internal fun TestMessageStep(test: TestMessageUi, actions: OnboardingActions) {
    StepHeading(title = stringResource(R.string.onboarding_test_title), body = stringResource(R.string.onboarding_test_body))
    val busy = test.status == TestMessageStatus.Sending
    HomingTextField(
        value = test.phoneNumber,
        onValueChange = actions.onTestNumberChange,
        label = stringResource(R.string.onboarding_test_number),
        supportingText = stringResource(R.string.onboarding_test_number_help),
        mono = true,
        enabled = !busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
    )
    HomingButton(
        text = stringResource(
            if (test.status is TestMessageStatus.Failed) R.string.onboarding_test_retry else R.string.onboarding_test_send
        ),
        onClick = actions.onSendTest,
        icon = HomingIcons.Send,
        enabled = test.phoneNumber.isNotBlank(),
        loading = busy,
        modifier = Modifier.fillMaxWidth()
    )
    if (test.status != TestMessageStatus.Idle) {
        HomingPanel(
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)
        ) {
            HomingTimeline(entries = timeline(test.status))
            if (test.status is TestMessageStatus.Received) {
                Text(
                    text = stringResource(R.string.onboarding_test_arrived),
                    style = MaterialTheme.typography.titleMedium,
                    color = HomingTheme.colors.success
                )
            }
        }
    }
}

@Composable
private fun timeline(status: TestMessageStatus): List<TimelineEntry> {
    val sending = TimelineEntry(stringResource(R.string.onboarding_test_sending), null, StatusTone.Info)
    val sentTitle = stringResource(R.string.onboarding_test_sent)
    val deliveredTitle = stringResource(R.string.onboarding_test_delivered)
    val receivedTitle = stringResource(R.string.onboarding_test_received)
    val waiting = stringResource(R.string.onboarding_test_waiting)
    return when (status) {
        TestMessageStatus.Idle -> emptyList()

        TestMessageStatus.Sending -> listOf(
            sending,
            TimelineEntry(sentTitle, null, StatusTone.Neutral, pending = true)
        )

        is TestMessageStatus.Sent -> listOf(
            TimelineEntry(sentTitle, formatClockTime(status.sentAtMillis), StatusTone.Brand),
            TimelineEntry(receivedTitle, null, StatusTone.Neutral, detail = waiting, pending = true)
        )

        is TestMessageStatus.Delivered -> listOf(
            TimelineEntry(sentTitle, formatClockTime(status.sentAtMillis), StatusTone.Brand),
            TimelineEntry(deliveredTitle, formatClockTime(status.deliveredAtMillis), StatusTone.Success),
            TimelineEntry(receivedTitle, null, StatusTone.Neutral, detail = waiting, pending = true)
        )

        is TestMessageStatus.Received -> listOfNotNull(
            TimelineEntry(sentTitle, formatClockTime(status.sentAtMillis), StatusTone.Brand),
            status.deliveredAtMillis?.let { TimelineEntry(deliveredTitle, formatClockTime(it), StatusTone.Success) },
            TimelineEntry(receivedTitle, formatClockTime(status.receivedAtMillis), StatusTone.Success, detail = status.text)
        )

        is TestMessageStatus.Failed -> listOf(
            TimelineEntry(
                stringResource(R.string.onboarding_test_failed),
                null,
                StatusTone.Danger,
                detail = status.reason ?: stringResource(R.string.onboarding_test_failed_body)
            )
        )
    }
}
