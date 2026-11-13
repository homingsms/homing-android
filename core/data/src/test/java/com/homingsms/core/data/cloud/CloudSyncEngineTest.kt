package com.homingsms.core.data.cloud

import com.homingsms.core.data.FakeConfig
import com.homingsms.core.data.FakeOutbox
import com.homingsms.core.data.FakeSecrets
import com.homingsms.core.data.RoutingDispatcher
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.CloudSettings
import com.homingsms.core.model.NetworkType
import com.homingsms.core.model.PairingFailure
import com.homingsms.core.model.RemoteDeviceSettings
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CloudSyncEngineTest {
    private val server = MockWebServer()
    private val routes = RoutingDispatcher()
    private val secrets = FakeSecrets()
    private val config = FakeConfig()
    private val outbox = FakeOutbox()
    private val sink = FakeSink()
    private val uploads = mutableMapOf<String, InboxUploadDto>()
    private lateinit var engine: CloudSyncEngine
    private lateinit var serverUrl: String

    @Before
    fun setUp() {
        server.dispatcher = routes
        server.start()
        serverUrl = server.url("/").toString().trimEnd('/')
        val api = DeviceApi(DeviceApi.createClient(), { config.cloud.serverUrl }, { secrets.get(SecretStore.DEVICE_TOKEN) })
        engine =
            CloudSyncEngine(api, outbox, config, secrets, FakeDevice(), sink, { ids ->
                ids.mapNotNull { id -> uploads[id]?.let { id to it } }.toMap()
            }) {
                NOW
            }
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun paired(remote: RemoteDeviceSettings = RemoteDeviceSettings(version = 1)) {
        secrets.put(SecretStore.DEVICE_TOKEN, TOKEN)
        config.value =
            CloudSettings(enabled = true, serverUrl = serverUrl, deviceId = "dev_1", accountId = "acc_1", pairedAt = NOW, remote = remote)
    }

    private fun body(request: mockwebserver3.RecordedRequest): JsonObject = Json.parseToJsonElement(request.body!!.utf8()).jsonObject

    @Test
    fun pairsAndStoresTheToken() = runTest {
        routes.on("POST", "/api/v1/device/pair", 201, PAIR_RESPONSE)
        val state = engine.pair("k7pm-4xq2", serverUrl, allowCleartext = true)
        assertEquals(CloudPairingState.Paired("dev_9", "acc_9", serverUrl, NOW), state)
        assertEquals("hd_secret", secrets.get(SecretStore.DEVICE_TOKEN))
        assertEquals("dev_9", config.cloud.deviceId)
        assertEquals(7, config.cloud.remote?.version)
        assertEquals(listOf("STOP", "QUIT"), config.cloud.remote?.optOutKeywords)
        assertTrue(config.cloud.enabled)

        val request = routes.requests("POST", "/api/v1/device/pair").single()
        assertNull(request.headers["Authorization"])
        val json = body(request)
        assertEquals("K7PM-4XQ2", json["code"]!!.jsonPrimitive.content)
        assertEquals("1.2.3", json["appVersion"]!!.jsonPrimitive.content)
        assertEquals(0, json["sims"]!!.jsonArray.single().jsonObject["slot"]!!.jsonPrimitive.int)
        assertFalse("nulls must be omitted", json.containsKey("name"))
    }

    @Test
    fun pairWithLinkUsesServerFromLink() = runTest {
        routes.on("POST", "/api/v1/device/pair", 201, PAIR_RESPONSE)
        val link = "homing://pair?server=" + java.net.URLEncoder.encode(serverUrl, "UTF-8") + "&code=K7PM-4XQ2"
        assertTrue(engine.pairWithLink(link, allowCleartext = true) is CloudPairingState.Paired)
        assertEquals(serverUrl, config.cloud.serverUrl)
    }

    @Test
    fun invalidPairingCodeFails() = runTest {
        routes.on("POST", "/api/v1/device/pair", 401, """{"statusCode":401,"message":"Unknown pairing code","error":"Unauthorized"}""")
        val state = engine.pair("K7PM-4XQ2", serverUrl, allowCleartext = true) as CloudPairingState.Failed
        assertEquals(PairingFailure.InvalidCode, state.reason)
        assertEquals("Unknown pairing code", state.message)
        assertNull(secrets.get(SecretStore.DEVICE_TOKEN))
        assertEquals(PairingFailure.InvalidLink, (engine.pair("K7PM-4XQ2", "ftp://x") as CloudPairingState.Failed).reason)
        assertEquals(PairingFailure.InvalidLink, (engine.pairWithLink("homing://pair?code=1", true) as CloudPairingState.Failed).reason)
    }

    @Test
    fun heartbeatReportsHealthPullsWhenPendingAndFetchesNewerSettings() = runTest {
        paired()
        routes.on(
            "POST",
            "/api/v1/device/heartbeat",
            200,
            """{"serverTime":"2026-11-12T10:15:30.000Z","pendingMessages":2,"settingsVersion":3}"""
        )
        routes.on("GET", "/api/v1/device/settings", 200, settingsJson(version = 3, paused = false))
        routes.on("POST", "/api/v1/device/messages/pull", 200, """{"messages":[${message("msg_1")},${message("msg_2", simSlot = 1)}]}""")
        routes.on(
            "POST",
            "/api/v1/device/messages/states",
            200,
            """{"results":[{"messageId":"msg_1","status":"accepted","reason":null},""" +
                """{"messageId":"msg_2","status":"accepted","reason":null}]}"""
        )

        assertEquals(SyncOutcome.Success, engine.heartbeat())

        val heartbeat = body(routes.requests("POST", "/api/v1/device/heartbeat").single())
        assertEquals("Bearer $TOKEN", routes.requests("POST", "/api/v1/device/heartbeat").single().headers["Authorization"])
        assertEquals(87, heartbeat["battery"]!!.jsonObject["level"]!!.jsonPrimitive.int)
        assertEquals("wifi", heartbeat["network"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(5, heartbeat["sims"]!!.jsonArray.single().jsonObject["sentToday"]!!.jsonPrimitive.int)
        assertEquals(3, config.cloud.remote?.version)
        assertEquals(listOf("msg_1", "msg_2"), sink.accepted.map { it.id })
        val reports = body(routes.requests("POST", "/api/v1/device/messages/states").single())["reports"]!!.jsonArray
        assertEquals(listOf("queued", "queued"), reports.map { it.jsonObject["state"]!!.jsonPrimitive.content })
        assertEquals(1, reports[1].jsonObject["simSlot"]!!.jsonPrimitive.int)
        assertFalse(reports[0].jsonObject.containsKey("simSlot"))
        assertTrue(outbox.states.isEmpty())
        assertEquals(0, engine.connection.value.consecutiveFailures)
        assertEquals(NOW, engine.connection.value.lastHeartbeatAt)
    }

    @Test
    fun heartbeatWithoutPendingMessagesDoesNotPull() = runTest {
        paired()
        routes.on(
            "POST",
            "/api/v1/device/heartbeat",
            200,
            """{"serverTime":"2026-11-12T10:15:30.000Z","pendingMessages":0,"settingsVersion":1}"""
        )
        assertEquals(SyncOutcome.Success, engine.heartbeat())
        assertTrue(routes.requests("POST", "/api/v1/device/messages/pull").isEmpty())
        assertTrue(routes.requests("GET", "/api/v1/device/settings").isEmpty())
    }

    @Test
    fun pullKeepsLeasingWhileBatchesAreFullAndSkipsWhenPaused() = runTest {
        paired()
        val full = (1..CloudSyncEngine.PULL_LIMIT).joinToString(",") { message("msg_a$it") }
        routes.on("POST", "/api/v1/device/messages/pull", 200, """{"messages":[$full]}""")
        routes.on("POST", "/api/v1/device/messages/pull", 200, """{"messages":[${message("msg_b")}]}""")
        routes.on("POST", "/api/v1/device/messages/states", 200, """{"results":[]}""")
        assertEquals(SyncOutcome.Success, engine.pull())
        assertEquals(2, routes.requests("POST", "/api/v1/device/messages/pull").size)
        assertEquals(CloudSyncEngine.PULL_LIMIT + 1, sink.accepted.size)
        assertEquals(10, body(routes.requests("POST", "/api/v1/device/messages/pull").first())["limit"]!!.jsonPrimitive.int)

        paired(RemoteDeviceSettings(version = 1, paused = true))
        engine.pull()
        assertEquals(2, routes.requests("POST", "/api/v1/device/messages/pull").size)
    }

    @Test
    fun knownExpiredAndRejectedLeasesAreReported() = runTest {
        paired()
        sink.known["msg_known"] = listOf(PendingReport("msg_known", DeviceReportState.SENT, NOW - 5, simSlot = 0, partsCount = 2))
        sink.reject = setOf("msg_bad")
        routes.on(
            "POST",
            "/api/v1/device/messages/pull",
            200,
            """{"messages":[${message(
                "msg_known"
            )},${message("msg_old", validUntil = "2020-01-01T00:00:00.000Z")},${message("msg_bad")}]}"""
        )
        routes.on("POST", "/api/v1/device/messages/states", 200, """{"results":[]}""")
        engine.pull()
        val reports = body(routes.requests("POST", "/api/v1/device/messages/states").single())["reports"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("msg_known", "msg_old", "msg_bad"), reports.map { it["messageId"]!!.jsonPrimitive.content })
        assertEquals("sent", reports[0]["state"]!!.jsonPrimitive.content)
        assertEquals(2, reports[0]["partsCount"]!!.jsonPrimitive.int)
        assertEquals("expired", reports[1]["error"]!!.jsonObject["code"]!!.jsonPrimitive.content)
        assertEquals("failed", reports[2]["state"]!!.jsonPrimitive.content)
        assertEquals(listOf("msg_known", "msg_bad"), sink.accepted.map { it.id })
    }

    @Test
    fun stateReportsAreBatchedAndRetriedDurably() = runTest {
        paired()
        repeat(CloudSyncEngine.STATE_BATCH + 5) { engine.report(PendingReport("msg_$it", DeviceReportState.SENT, NOW)) }
        routes.on("POST", "/api/v1/device/messages/states", 503, """{"statusCode":503,"message":"down"}""")
        routes.on("POST", "/api/v1/device/messages/states", 200, """{"results":[]}""")

        val first = engine.flush()
        assertTrue(first is SyncOutcome.Retry)
        assertEquals(CloudSyncEngine.STATE_BATCH + 5, outbox.states.size)
        assertEquals(1, outbox.states.first().attempts)
        assertEquals(1, engine.connection.value.consecutiveFailures)

        assertEquals(SyncOutcome.Success, engine.flush())
        assertTrue(outbox.states.isEmpty())
        val batches = routes.requests("POST", "/api/v1/device/messages/states").drop(1).map { body(it)["reports"]!!.jsonArray.size }
        assertEquals(listOf(CloudSyncEngine.STATE_BATCH, 5), batches)
    }

    @Test
    fun failedReportsCarryAnErrorCode() = runTest {
        paired()
        engine.report(PendingReport("msg_f", DeviceReportState.FAILED, NOW, errorCode = "no_service", errorMessage = "No service"))
        engine.report(PendingReport("msg_g", DeviceReportState.FAILED, NOW))
        routes.on("POST", "/api/v1/device/messages/states", 200, """{"results":[]}""")
        engine.flush()
        val reports = body(routes.requests("POST", "/api/v1/device/messages/states").single())["reports"]!!.jsonArray.map { it.jsonObject }
        assertEquals("no_service", reports[0]["error"]!!.jsonObject["code"]!!.jsonPrimitive.content)
        assertEquals("generic_failure", reports[1]["error"]!!.jsonObject["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun cancelledRejectionDropsTheLocalMessage() = runTest {
        paired()
        engine.report(PendingReport("msg_c", DeviceReportState.QUEUED, NOW))
        engine.report(PendingReport("msg_d", DeviceReportState.QUEUED, NOW))
        routes.on(
            "POST",
            "/api/v1/device/messages/states",
            200,
            """{"results":[{"messageId":"msg_c","status":"rejected","reason":"cancelled"},""" +
                """{"messageId":"msg_d","status":"duplicate","reason":null}]}"""
        )
        engine.flush()
        assertEquals(listOf("msg_c"), sink.cancelled)
        assertTrue(outbox.states.isEmpty())
    }

    @Test
    fun inboxUploadsAreBatchedAndDeduplicatedByLocalId() = runTest {
        paired()
        uploads["text:1"] = InboxUploadDto("text:1", "sms", "+15550001", receivedAt = "2026-11-12T10:15:30.000Z", text = "hi")
        uploads["data:2"] =
            InboxUploadDto("data:2", "data", "+15550002", receivedAt = "2026-11-12T10:15:30.000Z", data = "AQI=", port = 53739)
        engine.queueInbox("text:1")
        engine.queueInbox("text:1")
        engine.queueInbox("data:2")
        engine.queueInbox("gone:3")
        assertEquals(3, outbox.inbox.size)
        routes.on(
            "POST",
            "/api/v1/device/inbox",
            200,
            """{"results":[{"localId":"text:1","id":"inb_1","duplicate":false},{"localId":"data:2","id":"inb_2","duplicate":true}]}"""
        )
        assertEquals(SyncOutcome.Success, engine.flush())
        val messages = body(routes.requests("POST", "/api/v1/device/inbox").single())["messages"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("text:1", "data:2"), messages.map { it["localId"]!!.jsonPrimitive.content })
        assertEquals(53739, messages[1]["port"]!!.jsonPrimitive.int)
        assertTrue(outbox.inbox.isEmpty())
    }

    @Test
    fun oversizedInboxBatchIsSplit() = runTest {
        paired()
        listOf("a", "b").forEach {
            uploads[it] = InboxUploadDto(it, "sms", "+1555", receivedAt = "2026-11-12T10:15:30.000Z", text = it)
            engine.queueInbox(it)
        }
        routes.on("POST", "/api/v1/device/inbox", 413, """{"statusCode":413,"message":"too large"}""")
        routes.on("POST", "/api/v1/device/inbox", 200, """{"results":[{"localId":"a","id":"inb_a","duplicate":false}]}""")
        routes.on("POST", "/api/v1/device/inbox", 200, """{"results":[{"localId":"b","id":"inb_b","duplicate":false}]}""")
        assertEquals(SyncOutcome.Success, engine.flush())
        val sizes = routes.requests("POST", "/api/v1/device/inbox").map { body(it)["messages"]!!.jsonArray.size }
        assertEquals(listOf(2, 1, 1), sizes)
        assertTrue(outbox.inbox.isEmpty())
    }

    @Test
    fun unauthorizedUnpairs() = runTest {
        paired()
        engine.report(PendingReport("msg_1", DeviceReportState.SENT, NOW))
        routes.on("POST", "/api/v1/device/heartbeat", 401, """{"statusCode":401,"message":"Device removed"}""")
        assertEquals(SyncOutcome.Unpaired, engine.heartbeat())
        assertNull(secrets.get(SecretStore.DEVICE_TOKEN))
        assertNull(config.cloud.deviceId)
        assertTrue(engine.pairing.value is CloudPairingState.Removed)
        assertTrue(outbox.states.isEmpty())
        assertEquals(SyncOutcome.NotPaired, engine.heartbeat())
    }

    @Test
    fun wakeTriggersTheMatchingCall() = runTest {
        paired()
        routes.on("POST", "/api/v1/device/messages/pull", 200, """{"messages":[]}""")
        routes.on("GET", "/api/v1/device/settings", 200, settingsJson(version = 9, paused = true))
        routes.on("POST", "/api/v1/device/heartbeat", 200, """{"serverTime":"x","pendingMessages":0,"settingsVersion":9}""")

        engine.handleWake(mapOf("kind" to "homing.wake", "reason" to "messages_pending", "deviceId" to "dev_1", "sentAt" to "x"))
        assertEquals(1, routes.requests("POST", "/api/v1/device/messages/pull").size)

        engine.handleWake(mapOf("kind" to "homing.wake", "reason" to "settings_changed", "deviceId" to "dev_1"))
        assertEquals(9, config.cloud.remote?.version)
        assertTrue(engine.connection.value.remotePaused)

        engine.handleWake(mapOf("kind" to "homing.wake", "reason" to "device_removed", "deviceId" to "dev_1"))
        assertEquals(1, routes.requests("POST", "/api/v1/device/heartbeat").size)

        engine.handleWake(mapOf("kind" to "homing.wake", "reason" to "messages_pending", "deviceId" to "dev_other"))
        engine.handleWake(mapOf("kind" to "other", "reason" to "messages_pending"))
        assertEquals(1, routes.requests("POST", "/api/v1/device/messages/pull").size)
    }

    @Test
    fun fcmTokenIsSentOnChangeAndCleared() = runTest {
        paired()
        routes.on("PUT", "/api/v1/device/fcm-token", 204)
        engine.onFcmToken("token-1")
        engine.onFcmToken("token-1")
        val requests = routes.requests("PUT", "/api/v1/device/fcm-token")
        assertEquals(1, requests.size)
        assertEquals("""{"fcmToken":"token-1"}""", requests.single().body!!.utf8())
        assertEquals("token-1", config.cloud.reportedFcmToken)

        engine.unpair()
        assertEquals("""{"fcmToken":null}""", routes.requests("PUT", "/api/v1/device/fcm-token").last().body!!.utf8())
        assertEquals(CloudPairingState.Unpaired, engine.pairing.value)
        assertFalse(config.cloud.enabled)
    }

    @Test
    fun networkFailuresAreRetryable() = runTest {
        paired()
        server.close()
        val outcome = engine.heartbeat()
        assertTrue(outcome is SyncOutcome.Retry)
        assertEquals(1, engine.connection.value.consecutiveFailures)
    }

    private class FakeDevice : CloudDeviceInfo {
        override fun identity() = DeviceIdentity(null, "Pixel 8", "Google", "Android 15", "1.2.3")

        override fun sims() = listOf(SimInfoDto(0, "Carrier", "+15550000", null))

        override suspend fun heartbeat() = HeartbeatRequest(
            appVersion = "1.2.3",
            battery = BatteryDto(87, true),
            network = NetworkDto(NetworkType.Wifi),
            sims = listOf(SimHealthDto(0, "Carrier", null, 3, 5)),
            localQueueSize = 0
        )
    }

    private class FakeSink : CloudMessageSink {
        val accepted = mutableListOf<DeviceMessageDto>()
        val cancelled = mutableListOf<String>()
        val known = mutableMapOf<String, List<PendingReport>>()
        var reject = emptySet<String>()

        override suspend fun accept(message: DeviceMessageDto): LeaseAcceptance {
            accepted += message
            known[message.id]?.let { return LeaseAcceptance.Known(it) }
            if (message.id in reject) return LeaseAcceptance.Rejected("generic_failure", "bad")
            return LeaseAcceptance.Enqueued
        }

        override suspend fun cancel(messageId: String) {
            cancelled += messageId
        }
    }

    private companion object {
        const val NOW = 1_794_478_530_000L
        const val TOKEN = "hd_token"
        val PAIR_RESPONSE = """{"deviceId":"dev_9","deviceToken":"hd_secret","accountId":"acc_9","settings":${settingsJson(7, false)}}"""

        fun settingsJson(version: Int, paused: Boolean) =
            """{"version":$version,"paused":$paused,"heartbeatIntervalSeconds":60,"pullIntervalSeconds":60,"leaseSeconds":120,""" +
                """"dailyLimit":null,"sims":[{"slot":0,"enabled":true,"dailyLimit":100}],"optOutKeywords":["stop","QUIT"]}"""

        fun message(id: String, simSlot: Int? = null, validUntil: String? = null) =
            """{"id":"$id","type":"sms","to":"+15550009","text":"Code 1234","data":null,"port":null,"subject":null,"attachments":[],""" +
                """"simSlot":${simSlot ?: "null"},"priority":"otp","deliveryReport":true,"validUntil":${validUntil?.let {
                    "\"$it\""
                } ?: "null"},""" +
                """"attempt":1,"leaseExpiresAt":"2026-11-12T10:17:30.000Z"}"""
    }
}
