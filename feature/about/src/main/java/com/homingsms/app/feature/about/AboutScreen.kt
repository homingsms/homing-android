package com.homingsms.app.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.GroupLabel
import com.homingsms.app.core.designsystem.component.HomingBottomSheet
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingNavigationRow
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.component.HomingWordmark
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
fun AboutScreen(state: AboutUiState, actions: AboutActions, modifier: Modifier = Modifier) {
    var openIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    HomingScaffold(
        modifier = modifier,
        topBar = { HomingTopAppBar(title = stringResource(R.string.about_title), onBack = actions.onBack) }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(HomingTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)
            ) {
                HomingWordmark()
                Text(text = stringResource(R.string.about_summary), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = versionLine(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomingTheme.colors.mutedForeground
                )
                GroupLabel(
                    text = stringResource(R.string.about_licenses_title),
                    modifier = Modifier.padding(top = HomingTheme.spacing.md)
                )
                Text(
                    text = stringResource(R.string.about_licenses_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomingTheme.colors.mutedForeground
                )
                HomingPanel(contentPadding = PaddingValues(vertical = HomingTheme.spacing.xs)) {
                    state.components.forEachIndexed { index, component ->
                        if (index > 0) HomingDivider(inset = true)
                        HomingNavigationRow(
                            title = component.name,
                            value = stringResource(R.string.about_component_line, component.holder, component.license.label()),
                            onClick = { openIndex = index }
                        )
                    }
                }
            }
        }
    }
    openIndex?.let { state.components.getOrNull(it) }?.let { component ->
        LicenseSheet(component = component, onDismiss = { openIndex = null })
    }
}

@Composable
private fun versionLine(state: AboutUiState): String {
    val base = state.versionCode?.let { stringResource(R.string.about_version_code, state.versionName, it.toString()) }
        ?: stringResource(R.string.about_version, state.versionName)
    return state.buildType?.let { stringResource(R.string.about_version_build, base, it) } ?: base
}

@Composable
internal fun LicenseKind.label(): String = stringResource(
    when (this) {
        LicenseKind.Apache2, LicenseKind.AospNotice -> R.string.about_license_apache
        LicenseKind.Mit -> R.string.about_license_mit
        LicenseKind.OflAspekta, LicenseKind.OflJetBrainsMono -> R.string.about_license_ofl
    }
)

@Composable
private fun LicenseSheet(component: OpenSourceComponent, onDismiss: () -> Unit) {
    val resources = LocalResources.current
    val text = remember(component.license) {
        resources.openRawResource(component.license.textRes).bufferedReader().use { it.readText() }
    }
    HomingBottomSheet(onDismissRequest = onDismiss, title = component.name) {
        Text(
            text = stringResource(R.string.about_component_line, component.holder, component.license.label()),
            style = MaterialTheme.typography.bodyMedium,
            color = HomingTheme.colors.mutedForeground
        )
        Text(text = text, style = HomingTheme.text.monoSmall)
    }
}
