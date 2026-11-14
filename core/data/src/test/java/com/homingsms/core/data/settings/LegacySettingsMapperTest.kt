package com.homingsms.core.data.settings

import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.LegalSettings
import com.homingsms.core.model.LimitPeriod
import com.homingsms.core.model.ProcessingOrder
import com.homingsms.core.model.SimSelectionMode
import com.homingsms.core.model.SpendSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySettingsMapperTest {
    private val legacy = mapOf(
        "autostart" to true,
        "app.language" to "ar",
        "messages.sim_selection_mode" to "RoundRobin",
        "messages.processing_order" to "FIFO",
        "messages.send_interval_min" to "2",
        "messages.send_interval_max" to "5",
        "messages.limit_period" to "PerHour",
        "messages.limit_value" to "100",
        "messages.log_lifetime_days" to "14",
        "messages.work_hours_enabled" to true,
        "messages.work_hours_start" to "08:30",
        "messages.work_hours_end" to "bogus",
        "localserver.ENABLED" to true,
        "localserver.PORT" to "9090",
        "localserver.DEVICE_ID" to "0011aabb",
        "localserver.PASSWORD" to "old-password",
        "localserver.JWT_SECRET" to "jwt",
        "gateway.ENABLED" to true,
        "gateway.REGISTRATION_INFO" to """{"token":"old"}""",
        "webhooks.internet_required" to false,
        "webhooks.signing_key" to "old-key",
        "receiver.content_provider_enabled" to false,
        "receiver.state.last_processed_id" to 42L,
        "incoming.lifetime_days" to "7",
        "logs.lifetime_days" to "3",
        "ping.interval_seconds" to "30",
        "encryption.passphrase" to "correct horse"
    )

    @Test
    fun mapsEveryTypedValue() {
        val result = LegacySettingsMapper.map(legacy, AppSettings())
        val settings = result.settings
        assertTrue(settings.general.autostart)
        assertEquals(SimSelectionMode.RoundRobin, settings.messages.simSelection)
        assertEquals(ProcessingOrder.FIFO, settings.messages.processingOrder)
        assertEquals(2, settings.messages.sendIntervalMinSeconds)
        assertEquals(5, settings.messages.sendIntervalMaxSeconds)
        assertEquals(LimitPeriod.PerHour, settings.messages.limitPeriod)
        assertEquals(100, settings.messages.limitValue)
        assertEquals(14, settings.messages.logLifetimeDays)
        assertTrue(settings.messages.workHours.enabled)
        assertEquals("08:30", settings.messages.workHours.start)
        assertEquals("19:00", settings.messages.workHours.end)
        assertTrue(settings.localServer.enabled)
        assertEquals(9090, settings.localServer.port)
        assertEquals("0011aabb", settings.localServer.deviceId)
        assertFalse(settings.webhooks.internetRequired)
        assertFalse(settings.inbox.contentProviderEnabled)
        assertEquals(7, settings.inbox.lifetimeDays)
        assertEquals(3, settings.logs.lifetimeDays)
        assertFalse("old cloud registration is not carried over", settings.cloud.enabled)
        assertEquals(mapOf(LegacySettingsMapper.PASSPHRASE_SECRET to "correct horse"), result.secrets)
    }

    @Test
    fun consumesLegacyKeysButKeepsLanguageAndReceiverState() {
        val consumed = LegacySettingsMapper.map(legacy, AppSettings()).consumedKeys
        assertTrue("localserver.PASSWORD" in consumed)
        assertTrue("gateway.REGISTRATION_INFO" in consumed)
        assertTrue("encryption.passphrase" in consumed)
        assertFalse("app.language" in consumed)
        assertFalse("receiver.state.last_processed_id" in consumed)
    }

    @Test
    fun invalidValuesFallBackToDefaults() {
        val result = LegacySettingsMapper.map(
            mapOf(
                "localserver.PORT" to "80",
                "messages.limit_period" to "Weekly",
                "messages.limit_value" to "-5",
                "logs.lifetime_days" to "0"
            ),
            AppSettings()
        ).settings
        assertEquals(8080, result.localServer.port)
        assertEquals(LimitPeriod.Disabled, result.messages.limitPeriod)
        assertEquals(0, result.messages.limitValue)
        assertEquals(30, result.logs.lifetimeDays)
        assertNull(result.messages.sendIntervalMaxSeconds)
    }

    @Test
    fun legacyIntervalKeyIsUsedWhenMaxIsMissing() {
        val result = LegacySettingsMapper.map(mapOf("messages.SECONDS_BETWEEN_MESSAGES" to 12), AppSettings()).settings
        assertEquals(12, result.messages.sendIntervalMaxSeconds)
    }

    @Test
    fun legacyMigrationLeavesSpendSettingsAlone() {
        val fresh = LegacySettingsMapper.map(legacy, AppSettings()).settings
        assertEquals(SpendSettings(), fresh.spend)

        val saved = AppSettings(spend = SpendSettings("KES", mapOf(0 to 800_000L)))
        assertEquals(saved.spend, LegacySettingsMapper.map(legacy, saved).settings.spend)
    }

    @Test
    fun legacyMigrationLeavesLegalAcceptanceAlone() {
        val fresh = LegacySettingsMapper.map(legacy, AppSettings()).settings
        assertEquals(LegalSettings(), fresh.legal)

        val saved = AppSettings(legal = LegalSettings().accept(1_700_000_000_000L))
        assertEquals(saved.legal, LegacySettingsMapper.map(legacy, saved).settings.legal)
    }

    @Test
    fun detectsWhetherMigrationIsNeeded() {
        assertTrue(LegacySettingsMapper.hasLegacyValues(legacy))
        assertFalse(LegacySettingsMapper.hasLegacyValues(mapOf("app.language" to "ru", "receiver.state.last_processed_id" to 1L)))
    }
}
