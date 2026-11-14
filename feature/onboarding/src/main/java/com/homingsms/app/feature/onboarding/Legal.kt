package com.homingsms.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
internal fun LegalSummary(title: String, modifier: Modifier = Modifier) {
    HomingPanel(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semanticsHeading()
        )
        LegalPoint(stringResource(R.string.onboarding_legal_point_sim))
        LegalPoint(stringResource(R.string.onboarding_legal_point_consent))
        LegalPoint(stringResource(R.string.onboarding_legal_point_responsible))
    }
}

@Composable
private fun LegalPoint(text: String) {
    Row {
        Box(
            modifier = Modifier
                .padding(top = 9.dp)
                .size(5.dp)
                .background(HomingTheme.colors.mutedForeground, CircleShape)
        )
        Spacer(Modifier.width(HomingTheme.spacing.md))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = HomingTheme.colors.mutedForeground,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun LegalAgreementRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenLegal: (LegalLink) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HomingTheme.colors
    val terms = stringResource(R.string.onboarding_legal_agree_terms)
    val acceptableUse = stringResource(R.string.onboarding_legal_agree_acceptable_use)
    val privacy = stringResource(R.string.onboarding_legal_agree_privacy)
    val sentence = stringResource(R.string.onboarding_legal_agree, terms, acceptableUse, privacy)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = colors.brand, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)
    )
    val text = buildAnnotatedString {
        append(sentence)
        listOf(LegalLink.Terms to terms, LegalLink.AcceptableUse to acceptableUse, LegalLink.Privacy to privacy).forEach { (link, label) ->
            val start = sentence.indexOf(label)
            if (start >= 0) {
                addLink(LinkAnnotation.Clickable(link.name, linkStyles) { onOpenLegal(link) }, start, start + label.length)
            }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HomingTheme.spacing.minTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = HomingTheme.spacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (checked) HomingIcons.CheckboxChecked else HomingIcons.CheckboxEmpty,
            contentDescription = null,
            tint = if (checked) colors.brand else colors.mutedForeground
        )
        Spacer(Modifier.width(HomingTheme.spacing.md))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun UpdatedTermsScreen(state: UpdatedTermsUiState, actions: UpdatedTermsActions, modifier: Modifier = Modifier) {
    HomingScaffold(modifier = modifier) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(HomingTheme.spacing.gutter)
                    .padding(top = HomingTheme.spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.xxl)
            ) {
                StepHeading(
                    title = stringResource(R.string.onboarding_legal_updated_title),
                    body = stringResource(R.string.onboarding_legal_updated_body)
                )
                LegalSummary(stringResource(R.string.onboarding_legal_summary_updated_title))
                Column(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)) {
                    LegalAgreementRow(
                        checked = state.accepted,
                        onCheckedChange = actions.onAcceptedChange,
                        onOpenLegal = actions.onOpenLegal
                    )
                    HomingButton(
                        text = stringResource(R.string.onboarding_legal_accept),
                        onClick = actions.onAccept,
                        enabled = state.accepted,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
