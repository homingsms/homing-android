package com.homingsms.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.homingsms.app.core.designsystem.preview.HomingPreviewSurface
import com.homingsms.app.core.designsystem.preview.HomingPreviews
import com.homingsms.app.core.designsystem.theme.HomingTheme

@HomingPreviews
@Composable
private fun HomeOnlinePreview() {
    HomingPreviewSurface { HomeScreen(HomeSamples.online, HomeActions()) }
}

@HomingPreviews
@Composable
private fun HomeSmsLimitPreview() {
    HomingPreviewSurface { HomeScreen(HomeSamples.smsLimit, HomeActions()) }
}

@HomingPreviews
@Composable
private fun HomeOfflinePreview() {
    HomingPreviewSurface { HomeScreen(HomeSamples.offline, HomeActions()) }
}

@HomingPreviews
@Composable
private fun HomeWithSpendPreview() {
    HomingPreviewSurface { HomeScreen(HomeSamples.withSpend, HomeActions()) }
}

@HomingPreviews
@Composable
private fun SpendPanelPreview() {
    HomingPreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)) {
            SpendPanel(HomeSamples.spendMultiSim, onSetSmsRate = {})
            SpendPanel(HomeSamples.spendSingleSim, onSetSmsRate = {})
            SpendPanel(HomeSamples.spendZero, onSetSmsRate = {})
            SpendPanel(HomeSamples.spendPartlyPriced, onSetSmsRate = {})
            SpendPanel(HomeSamples.spendNoRate, onSetSmsRate = {})
        }
    }
}
