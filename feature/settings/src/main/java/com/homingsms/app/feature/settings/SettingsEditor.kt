package com.homingsms.app.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.GroupLabel
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingNavigationRow
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingSwitchRow
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.theme.CAP_WARNING_PERCENT
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.formatClockTime
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.formatPercent
import com.homingsms.app.core.designsystem.util.simLabel

internal sealed interface SettingsEditor {
    data object RateLimit : SettingsEditor
    data object SendDelay : SettingsEditor
    data class DailyCap(val slot: Int, val current: Int?) : SettingsEditor
    data object WorkStart : SettingsEditor
    data object WorkEnd : SettingsEditor
    data object Routing : SettingsEditor
    data object DataPort : SettingsEditor
    data object OptOutKeywords : SettingsEditor
    data object LocalPort : SettingsEditor
    data object CloudUrl : SettingsEditor
    data object PullInterval : SettingsEditor
    data object Passphrase : SettingsEditor
}
