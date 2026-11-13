package com.homingsms.app.ui.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import com.homingsms.app.BuildConfig
import com.homingsms.app.data.AppDatabase
import com.homingsms.app.data.entities.Message
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.MessageContent
import com.homingsms.app.modules.cloud.CloudController
import com.homingsms.app.modules.localserver.LocalServerService
import com.homingsms.app.modules.logs.LogsService
import com.homingsms.app.modules.messages.MessagesService
import com.homingsms.app.modules.messages.data.SendParams
import com.homingsms.app.modules.messages.data.SendRequest
import com.homingsms.app.modules.orchestrator.OrchestratorService
import com.homingsms.app.modules.status.AppStateRepository
import com.homingsms.app.modules.webhooks.WebhooksService
import com.homingsms.app.receivers.BootReceiver
import com.homingsms.core.data.db.CloudOutboxDao
import com.homingsms.core.data.repository.InboxRecords
import com.homingsms.core.data.repository.MessageRecords
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.data.webhooks.WebhookRepository
import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.CreatedApiKey
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.Ids
import com.homingsms.core.model.InboxAttachmentContent
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.Page
import com.homingsms.core.model.Scope
import com.homingsms.core.model.WebhookDelivery
import com.homingsms.core.model.WebhookEndpoint
import com.homingsms.core.model.WebhookEventType
import com.homingsms.core.model.api.Iso8601
import java.io.File
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AndroidGatewayControl(
    private val context: Context,
    private val orchestrator: OrchestratorService,
    private val localServer: LocalServerService,
    private val cloud: CloudController,
    private val settings: SettingsRepository,
    private val secrets: SecretStore,
    private val status: AppStateRepository
) : GatewayControl {
    override val defaultCloudUrl: String
        get() = settings.current.cloud.serverUrl ?: BuildConfig.CLOUD_URL

    override val allowCleartext: Boolean
        get() = cloud.allowCleartext

    override fun start() = orchestrator.start(context, false)

    override fun stop() = orchestrator.stop(context)

    override suspend fun setLocalEnabled(enabled: Boolean) {
        localServer.setEnabled(enabled)
        if (!status.serviceRunning.value) return
        if (enabled) localServer.start(context) else localServer.stop(context)
    }

    override suspend fun setLocalPort(port: Int) {
        val previous = settings.current.localServer.port
        localServer.setPort(port)
        if (previous != port && status.serviceRunning.value && settings.current.localServer.enabled) {
            localServer.stop(context)
            localServer.start(context)
        }
    }

    override suspend fun setCloudEnabled(enabled: Boolean) = cloud.setEnabled(enabled)

    override suspend fun createApiKey(name: String, scopes: Set<Scope>): CreatedApiKey = localServer.createKey(name, scopes)

    override suspend fun revokeApiKey(id: String): Boolean = localServer.revokeKey(id)

    override suspend fun pair(code: String, serverUrl: String): CloudPairingState = enableAfter(cloud.pair(code, serverUrl))

    override suspend fun pairWithLink(link: String): CloudPairingState = enableAfter(cloud.pairWithLink(link))

    private suspend fun enableAfter(result: CloudPairingState): CloudPairingState {
        if (result is CloudPairingState.Paired && !settings.current.cloud.enabled) cloud.setEnabled(true)
        return result
    }

    override suspend fun unpair() = cloud.unpair()

    override fun retryCloud() = cloud.retryNow()

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings.update(transform)
    }

    override fun hasPassphrase(): Boolean = !secrets.get(SecretStore.PASSPHRASE).isNullOrEmpty()

    override fun setPassphrase(value: String?) {
        if (value.isNullOrEmpty()) secrets.remove(SecretStore.PASSPHRASE) else secrets.put(SecretStore.PASSPHRASE, value)
    }

    override suspend fun setAutostart(enabled: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, BootReceiver::class.java),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        settings.update { it.copy(general = it.general.copy(autostart = enabled)) }
    }
}

class AndroidMessageStore(private val records: MessageRecords, private val messages: MessagesService, database: AppDatabase) :
    MessageStore {
    override val changes: Flow<Unit> = database.invalidationTracker
        .createFlow("message", "messagerecipient", "messagestate", "recipientstate", emitInitialState = false)
        .map { }

    override suspend fun list(filter: MessageFilter, cursor: Cursor?, limit: Int): Page<MessageRecord> =
        records.list(filter, cursor, limit, source = null)

    override suspend fun get(id: String): StoredMessage? {
        val local = records.get(id, EntitySource.Local)
        if (local != null) return StoredMessage(local, local = true)
        return records.get(id)?.let { StoredMessage(it, local = false) }
    }

    override suspend fun retry(id: String): String? = withContext(Dispatchers.IO) {
        val stored = messages.getMessage(id)?.takeIf { it.message.source == EntitySource.Local } ?: return@withContext null
        val original = stored.message
        val newId = Ids.message()
        messages.enqueueMessage(
            SendRequest(
                EntitySource.Local,
                com.homingsms.app.modules.messages.data.Message(
                    id = newId,
                    content = Message.decodeContent(original.type, original.content),
                    phoneNumbers = stored.recipients.map { it.phoneNumber },
                    isEncrypted = original.isEncrypted,
                    createdAt = Date()
                ),
                SendParams(
                    withDeliveryReport = original.withDeliveryReport,
                    skipPhoneValidation = original.skipPhoneValidation,
                    simNumber = null,
                    validUntil = null,
                    scheduleAt = null,
                    priority = original.priority
                )
            )
        )
        newId
    }

    override suspend fun cancel(id: String): Boolean = try {
        messages.cancelMessage(id)
        true
    } catch (_: IllegalStateException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    override suspend fun sendTest(to: String, text: String): String = withContext(Dispatchers.IO) {
        val id = Ids.message()
        messages.enqueueMessage(
            SendRequest(
                EntitySource.Local,
                com.homingsms.app.modules.messages.data.Message(id, MessageContent.Text(text), listOf(to), false, Date()),
                SendParams(
                    withDeliveryReport = true,
                    skipPhoneValidation = true,
                    simNumber = null,
                    validUntil = null,
                    scheduleAt = null,
                    priority = Message.PRIORITY_EXPEDITED
                )
            )
        )
        id
    }
}

class AndroidInboxStore(private val records: InboxRecords, private val outbox: CloudOutboxDao, database: AppDatabase) : InboxStore {
    override val changes: Flow<Unit> = database.invalidationTracker
        .createFlow("incoming_messages", "cloud_inbox_outbox", emitInitialState = false)
        .map { }

    override suspend fun list(filter: InboxFilter, cursor: Cursor?, limit: Int): Page<InboxRecord> = records.list(filter, cursor, limit)

    override suspend fun get(id: String): InboxRecord? = records.get(id)

    override suspend fun attachment(id: String, attachmentId: String): InboxAttachmentContent? =
        withContext(Dispatchers.IO) { records.attachment(id, attachmentId) }

    override suspend fun pendingCloudUpload(localId: String): CloudUpload? =
        outbox.inboxEntry(localId)?.let { CloudUpload(it.attempts, it.lastError) }
}

class AndroidWebhookStore(private val repository: WebhookRepository, private val service: WebhooksService) : WebhookStore {
    override val endpoints: Flow<List<WebhookEndpoint>> = repository.endpointsFlow

    override fun recentDeliveries(endpointId: String, limit: Int): Flow<List<WebhookDelivery>> =
        repository.recentDeliveries(endpointId, limit)

    override suspend fun get(id: String): WebhookEndpoint? = repository.get(id)

    override suspend fun create(url: String, events: Set<WebhookEventType>, enabled: Boolean): WebhookEndpoint =
        repository.create(url, events, null, enabled).first

    override suspend fun update(id: String, url: String?, events: Set<WebhookEventType>?, enabled: Boolean?): WebhookEndpoint? =
        repository.update(id, url = url, events = events, enabled = enabled)

    override suspend fun delete(id: String): Boolean = repository.delete(id)

    override suspend fun secret(id: String): String = withContext(Dispatchers.IO) { repository.secretFor(id) }

    override suspend fun rotateSecret(id: String): String? = repository.rotateSecret(id)

    override suspend fun deliveries(endpointId: String, cursor: Cursor?, limit: Int): Page<WebhookDelivery>? =
        repository.deliveries(endpointId, null, cursor, limit)

    override suspend fun sendTest(id: String): WebhookDelivery? = repository.test(id)?.also { service.wake() }

    override suspend fun redeliver(endpointId: String, deliveryId: String): WebhookDelivery? =
        repository.redeliver(endpointId, deliveryId)?.also { service.wake() }
}

class AndroidFileSharing(private val context: Context, private val logs: LogsService) : FileSharing {
    override suspend fun exportLogs(): SharedFile? = withContext(Dispatchers.IO) {
        val entries = logs.select()
        if (entries.isEmpty()) return@withContext null
        val text = entries.joinToString("\n") { entry ->
            listOfNotNull(
                Iso8601.format(entry.createdAt),
                entry.priority.name,
                entry.module,
                entry.message,
                entry.context?.toString()
            ).joinToString(" | ")
        }
        write("homing-logs.txt", text.toByteArray(Charsets.UTF_8), "text/plain")
    }

    override suspend fun attachmentFile(name: String?, contentType: String, bytes: ByteArray): SharedFile = withContext(Dispatchers.IO) {
        val safeName = name?.substringAfterLast('/')?.filter { it.isLetterOrDigit() || it in SAFE_CHARS }?.takeIf { it.isNotBlank() }
            ?: "attachment"
        write(safeName, bytes, contentType)
    }

    private fun write(name: String, bytes: ByteArray, mimeType: String): SharedFile {
        val directory = File(context.cacheDir, SHARED_DIR).apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > MAX_AGE_MS }?.forEach { it.delete() }
        val file = File(directory, name)
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        return SharedFile(uri.toString(), mimeType)
    }

    private companion object {
        const val SHARED_DIR = "shared"
        const val SAFE_CHARS = "._-"
        const val MAX_AGE_MS = 86_400_000L
    }
}
