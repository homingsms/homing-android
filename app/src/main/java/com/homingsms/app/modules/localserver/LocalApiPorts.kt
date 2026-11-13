package com.homingsms.app.modules.localserver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.homingsms.app.BuildConfig
import com.homingsms.app.data.entities.Message
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.MessageContent
import com.homingsms.app.helpers.SubscriptionsHelper
import com.homingsms.app.modules.cloud.CloudController
import com.homingsms.app.modules.messages.MessagesService
import com.homingsms.app.modules.messages.data.SendParams
import com.homingsms.app.modules.messages.data.SendRequest
import com.homingsms.app.modules.messages.exceptions.ConflictException
import com.homingsms.app.modules.webhooks.WebhooksService
import com.homingsms.core.data.db.MessageApiMeta
import com.homingsms.core.data.db.StatsDao
import com.homingsms.core.data.repository.ApiKeyRepository
import com.homingsms.core.data.repository.InboxRecords
import com.homingsms.core.data.repository.MessageRecords
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.data.webhooks.WebhookRepository
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.Ids
import com.homingsms.core.model.InboxAttachmentContent
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessagePriority
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.Page
import com.homingsms.core.model.WebhookDelivery
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEndpoint
import com.homingsms.core.model.api.HealthDto
import com.homingsms.core.model.api.Iso8601
import com.homingsms.core.model.api.ValidatedSend
import com.homingsms.core.server.ApiKeyAuthenticator
import com.homingsms.core.server.CancelResult
import com.homingsms.core.server.CreatedWebhook
import com.homingsms.core.server.HEALTH_DEGRADED
import com.homingsms.core.server.HEALTH_OK
import com.homingsms.core.server.HealthPort
import com.homingsms.core.server.InboxPort
import com.homingsms.core.server.LocalApiConfig
import com.homingsms.core.server.MessagesPort
import com.homingsms.core.server.NewWebhook
import com.homingsms.core.server.SendResult
import com.homingsms.core.server.WebhookPatch
import com.homingsms.core.server.WebhooksPort
import java.util.Date
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalMessagesPort(private val messages: MessagesService, private val records: MessageRecords) : MessagesPort {
    private val sendLock = Mutex()

    override suspend fun send(request: ValidatedSend, requestHash: String): SendResult = sendLock.withLock {
        request.idempotencyKey?.let { key ->
            records.findByIdempotencyKey(key)?.let { existing ->
                if (existing.requestHash != requestHash) {
                    return@withLock SendResult.Conflict("idempotencyKey was already used with a different request body")
                }
                val record = records.get(existing.messageId) ?: return@withLock SendResult.Conflict("idempotencyKey is in use")
                return@withLock SendResult.Replayed(record)
            }
        }
        val id = Ids.message()
        val sendRequest = SendRequest(
            EntitySource.Local,
            com.homingsms.app.modules.messages.data.Message(
                id,
                content(request),
                listOf(request.to),
                isEncrypted = false,
                createdAt = Date()
            ),
            SendParams(
                withDeliveryReport = request.deliveryReport,
                skipPhoneValidation = true,
                simNumber = request.simSlot?.plus(1),
                validUntil = request.validUntil?.let(::Date),
                scheduleAt = request.scheduleAt?.let(::Date),
                priority = if (request.priority == MessagePriority.Otp) Message.PRIORITY_EXPEDITED else Message.PRIORITY_DEFAULT
            )
        )
        try {
            messages.enqueueMessage(sendRequest)
        } catch (_: ConflictException) {
            return@withLock SendResult.Conflict("Message already exists")
        }
        records.insertMeta(MessageApiMeta(id, request.idempotencyKey, requestHash, MessageRecords.encodeMetadata(request.metadata)))
        val record = records.get(id) ?: return@withLock SendResult.Rejected("Message could not be stored")
        SendResult.Created(record)
    }

    override suspend fun list(filter: MessageFilter, cursor: Cursor?, limit: Int): Page<MessageRecord> =
        records.list(filter, cursor, limit, EntitySource.Local)

    override suspend fun get(id: String): MessageRecord? = records.get(id, EntitySource.Local)

    override suspend fun cancel(id: String): CancelResult {
        val record = records.get(id, EntitySource.Local) ?: return CancelResult.NotFound
        if (record.state !in cancellable) return CancelResult.Conflict("Message in state ${record.state.value} cannot be cancelled")
        val failure = try {
            messages.cancelMessage(id)
            null
        } catch (_: IllegalStateException) {
            CancelResult.Conflict("Message can no longer be cancelled")
        } catch (_: IllegalArgumentException) {
            CancelResult.NotFound
        }
        return failure ?: records.get(id, EntitySource.Local)?.let { CancelResult.Cancelled(it) } ?: CancelResult.NotFound
    }

    private fun content(request: ValidatedSend): MessageContent = when (request.type) {
        MessageType.Sms -> MessageContent.Text(request.text.orEmpty())

        MessageType.Data -> MessageContent.Data(request.data.orEmpty(), (request.port ?: 0).toUShort())

        MessageType.Mms -> MessageContent.Mms(
            subject = request.subject,
            text = request.text,
            attachments = request.attachments.map { MessageContent.Mms.Attachment(it.contentType, it.name, it.base64) }
        )
    }

    private companion object {
        val cancellable = setOf(MessageState.Pending, MessageState.Scheduled)
    }
}

class LocalInboxPort(private val records: InboxRecords) : InboxPort {
    override suspend fun list(filter: InboxFilter, cursor: Cursor?, limit: Int): Page<InboxRecord> = records.list(filter, cursor, limit)

    override suspend fun get(id: String): InboxRecord? = records.get(id)

    override suspend fun attachment(id: String, attachmentId: String): InboxAttachmentContent? = records.attachment(id, attachmentId)
}

class LocalWebhooksPort(private val repository: WebhookRepository, private val webhooks: WebhooksService) : WebhooksPort {
    override suspend fun list(): List<WebhookEndpoint> = repository.list()

    override suspend fun create(webhook: NewWebhook): CreatedWebhook {
        val (endpoint, secret) = repository.create(webhook.url, webhook.events, webhook.description, webhook.enabled)
        return CreatedWebhook(endpoint, secret)
    }

    override suspend fun get(id: String): WebhookEndpoint? = repository.get(id)

    override suspend fun update(id: String, patch: WebhookPatch): WebhookEndpoint? = repository.update(
        id = id,
        url = patch.url,
        events = patch.events,
        description = patch.description,
        descriptionSet = patch.descriptionSet,
        enabled = patch.enabled
    )

    override suspend fun delete(id: String): Boolean = repository.delete(id)

    override suspend fun rotateSecret(id: String): String? = repository.rotateSecret(id)

    override suspend fun test(id: String): WebhookDelivery? = repository.test(id)?.also { webhooks.wake() }

    override suspend fun deliveries(id: String, status: WebhookDeliveryStatus?, cursor: Cursor?, limit: Int): Page<WebhookDelivery>? =
        repository.deliveries(id, status, cursor, limit)

    override suspend fun redeliver(id: String, deliveryId: String): WebhookDelivery? =
        repository.redeliver(id, deliveryId)?.also { webhooks.wake() }
}

class LocalHealthPort(
    private val context: Context,
    private val stats: StatsDao,
    private val settings: SettingsRepository,
    private val cloud: CloudController
) : HealthPort {
    override suspend fun health(): HealthDto {
        val database = runCatching { stats.oldestSendingAt() }.isSuccess
        val telephony = SubscriptionsHelper.getActiveSimCards(context).isNotEmpty() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val checks = buildMap {
            put("database", if (database) UP else DOWN)
            put("telephony", if (telephony) UP else DOWN)
            if (settings.current.cloud.enabled) put("cloud", if (cloud.connection.value.reachable) UP else DOWN)
        }
        return HealthDto(
            status = if (checks.values.all { it == UP }) HEALTH_OK else HEALTH_DEGRADED,
            version = BuildConfig.VERSION_NAME,
            time = Iso8601.format(System.currentTimeMillis()),
            checks = checks
        )
    }

    private companion object {
        const val UP = "up"
        const val DOWN = "down"
    }
}

fun localApiConfig(
    messages: MessagesPort,
    inbox: InboxPort,
    webhooks: WebhooksPort,
    keys: ApiKeyRepository,
    health: HealthPort,
    settings: SettingsRepository
): LocalApiConfig = LocalApiConfig(
    messages = messages,
    inbox = inbox,
    webhooks = webhooks,
    authenticator = ApiKeyAuthenticator { keys.authenticate(it) },
    health = health,
    deviceIds = { listOfNotNull(settings.current.localServer.deviceId, settings.current.cloud.deviceId) },
    allowCleartextWebhooks = BuildConfig.DEBUG,
    prettyJson = BuildConfig.DEBUG
)
