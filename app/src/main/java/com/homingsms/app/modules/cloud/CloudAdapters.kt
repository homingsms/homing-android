package com.homingsms.app.modules.cloud

import android.os.Build
import com.homingsms.app.BuildConfig
import com.homingsms.app.data.entities.MessageWithRecipients
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.MessageContent
import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.modules.messages.MessagesService
import com.homingsms.app.modules.messages.data.Message
import com.homingsms.app.modules.messages.data.SendParams
import com.homingsms.app.modules.messages.data.SendRequest
import com.homingsms.app.modules.messages.exceptions.ConflictException
import com.homingsms.core.data.cloud.BatteryDto
import com.homingsms.core.data.cloud.CloudConfigStore
import com.homingsms.core.data.cloud.CloudDeviceInfo
import com.homingsms.core.data.cloud.CloudMessageSink
import com.homingsms.core.data.cloud.DeviceIdentity
import com.homingsms.core.data.cloud.DeviceMessageDto
import com.homingsms.core.data.cloud.DeviceReportState
import com.homingsms.core.data.cloud.HeartbeatRequest
import com.homingsms.core.data.cloud.LeaseAcceptance
import com.homingsms.core.data.cloud.NetworkDto
import com.homingsms.core.data.cloud.PendingReport
import com.homingsms.core.data.cloud.SimHealthDto
import com.homingsms.core.data.cloud.SimInfoDto
import com.homingsms.core.data.repository.StatsRepository
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.model.CloudSettings
import com.homingsms.core.model.ErrorCodes
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.api.Iso8601
import com.homingsms.core.telephony.DeviceHealthReader
import java.util.Date

class SettingsCloudConfigStore(private val settings: SettingsRepository) : CloudConfigStore {
    override val cloud: CloudSettings
        get() = settings.current.cloud

    override suspend fun update(transform: (CloudSettings) -> CloudSettings) {
        settings.update { it.copy(cloud = transform(it.cloud)) }
    }
}

class AndroidCloudDeviceInfo(
    private val settings: SettingsRepository,
    private val health: DeviceHealthReader,
    private val stats: StatsRepository
) : CloudDeviceInfo {
    override fun identity(): DeviceIdentity = DeviceIdentity(
        name = settings.current.cloud.deviceName,
        model = Build.MODEL.orEmpty(),
        manufacturer = Build.MANUFACTURER.orEmpty(),
        osVersion = "Android ${Build.VERSION.RELEASE}",
        appVersion = BuildConfig.VERSION_NAME
    )

    override fun sims(): List<SimInfoDto> = health.simCards().map {
        SimInfoDto(slot = it.slotIndex, carrier = it.carrierName, phoneNumber = it.phoneNumber, iccid = it.iccid)
    }

    override suspend fun heartbeat(): HeartbeatRequest {
        val snapshot = health.read(stats.sentToday(), stats.queued())
        return HeartbeatRequest(
            appVersion = snapshot.appVersion,
            battery = BatteryDto(snapshot.battery.level, snapshot.battery.charging),
            network = NetworkDto(snapshot.network),
            sims = snapshot.sims.map { SimHealthDto(it.slot, it.carrier, it.phoneNumber, it.signalLevel, it.sentToday) },
            localQueueSize = snapshot.localQueueSize
        )
    }
}

class MessagesCloudSink(private val messages: MessagesService) : CloudMessageSink {
    override suspend fun accept(message: DeviceMessageDto): LeaseAcceptance {
        messages.getMessage(message.id)?.let { return LeaseAcceptance.Known(reportsFor(it)) }
        val content = contentOf(message) ?: return LeaseAcceptance.Rejected(ErrorCodes.GENERIC_FAILURE, "Unsupported message content")
        val request = SendRequest(
            EntitySource.Cloud,
            Message(message.id, content, listOf(message.to), isEncrypted = false, createdAt = Date()),
            SendParams(
                withDeliveryReport = message.deliveryReport,
                skipPhoneValidation = true,
                simNumber = message.simSlot?.plus(1),
                validUntil = message.validUntil?.let(Iso8601::parse)?.let(::Date),
                scheduleAt = null,
                priority = if (message.priority == "otp") com.homingsms.app.data.entities.Message.PRIORITY_EXPEDITED else null
            )
        )
        return try {
            messages.enqueueMessage(request)
            LeaseAcceptance.Enqueued
        } catch (_: ConflictException) {
            messages.getMessage(message.id)?.let { LeaseAcceptance.Known(reportsFor(it)) } ?: LeaseAcceptance.Enqueued
        }
    }

    override suspend fun cancel(messageId: String) {
        try {
            messages.cancelMessage(messageId)
        } catch (_: IllegalArgumentException) {
            return
        } catch (_: IllegalStateException) {
            return
        }
    }

    private fun contentOf(message: DeviceMessageDto): MessageContent? = when (MessageType.fromValue(message.type)) {
        MessageType.Sms -> message.text?.let { MessageContent.Text(it) }

        MessageType.Data -> {
            val data = message.data
            val port = message.port
            if (data != null && port != null) MessageContent.Data(data, port.toUShort()) else null
        }

        MessageType.Mms -> MessageContent.Mms(
            subject = message.subject,
            text = message.text,
            attachments = message.attachments.map { MessageContent.Mms.Attachment(it.contentType, it.name, it.data) }
        )

        null -> null
    }

    companion object {
        fun reportsFor(row: MessageWithRecipients, now: Long = System.currentTimeMillis()): List<PendingReport> {
            val message = row.message
            val error = row.recipients.firstNotNullOfOrNull { ErrorCodes.decode(it.error) }
            val slot = message.simNumber?.minus(1)
            val state = when (row.message.state) {
                ProcessingState.Pending -> DeviceReportState.QUEUED
                ProcessingState.Processed -> DeviceReportState.SENDING
                ProcessingState.Sent -> DeviceReportState.SENT
                ProcessingState.Delivered -> DeviceReportState.DELIVERED
                ProcessingState.Failed -> DeviceReportState.FAILED
                ProcessingState.Cancelled -> return emptyList()
            }
            return listOf(
                PendingReport(
                    messageId = message.id,
                    state = state,
                    at = now,
                    simSlot = slot,
                    partsCount = message.partsCount,
                    errorCode = error?.code?.takeIf { state == DeviceReportState.FAILED },
                    errorMessage = error?.message?.takeIf { state == DeviceReportState.FAILED }
                )
            )
        }
    }
}
