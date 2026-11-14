package com.homingsms.app.feature.settings

import com.homingsms.app.core.designsystem.preview.PREVIEW_NOW_MILLIS

internal object SettingsSamples {
    val typical = SettingsUiState(
        sending = SendingSettings(
            rateLimitPerMinute = 20,
            delayBetweenSeconds = 3,
            dailyCaps = listOf(SimCapUi(0, "Vodafone UK", 500), SimCapUi(1, "EE", null)),
            workingHours = WorkingHours(enabled = true, startMinutes = 8 * 60, endMinutes = 20 * 60),
            routing = SimRoutingMode.RoundRobin,
            failoverToOtherSim = true,
            deliveryReports = true
        ),
        spend = SpendSettingsUi(currencyCode = "GBP", rates = listOf(SimRateUi(0, "Vodafone UK", 7_500), SimRateUi(1, "EE", null))),
        receiving = ReceivingSettings(forwardDataSms = true, dataSmsPort = 8901),
        server = ServerSettings(
            localEnabled = true,
            localPort = 8080,
            cloudPaired = true,
            cloudServerUrl = "https://api.homingsms.com/mobile/v1",
            pullIntervalSeconds = 60
        ),
        security = SecuritySettings(apiKeyCount = 2, webhookCount = 1, encryptionEnabled = true, encryptionPassphraseSet = true),
        advanced = AdvancedSettings(keepAliveEnabled = true, batteryOptimizationIgnored = false),
        legal = LegalSettingsUi(termsAcceptedAtMillis = PREVIEW_NOW_MILLIS - 86_400_000L),
        appVersion = "1.0.0 (1)"
    )
}
