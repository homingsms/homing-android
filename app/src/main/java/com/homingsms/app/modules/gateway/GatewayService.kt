package com.homingsms.app.modules.gateway

import android.content.Context
import android.os.Build
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.MessageContent
import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.helpers.SubscriptionsHelper
import com.homingsms.app.modules.device.DeviceService
import com.homingsms.app.modules.events.EventBus
import com.homingsms.app.modules.gateway.events.DeviceRegisteredEvent
import com.homingsms.app.modules.gateway.inbox.InboxUploadService
import com.homingsms.app.modules.gateway.inbox.InboxUploadWorker
import com.homingsms.app.modules.gateway.services.SSEForegroundService
import com.homingsms.app.modules.gateway.workers.PullMessagesWorker
import com.homingsms.app.modules.gateway.workers.SendStateWorker
import com.homingsms.app.modules.gateway.workers.SettingsUpdateWorker
import com.homingsms.app.modules.gateway.workers.WebhooksUpdateWorker
import com.homingsms.app.modules.localserver.domain.SimCard
import com.homingsms.app.modules.logs.LogsService
import com.homingsms.app.modules.logs.db.LogEntry
import com.homingsms.app.modules.messages.MessagesService
import com.homingsms.app.modules.messages.MessagesSettings
import com.homingsms.app.modules.messages.data.SendParams
import com.homingsms.app.modules.messages.data.SendRequest
import com.homingsms.app.modules.receiver.data.InboxMessage
import com.homingsms.app.services.PushService
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import java.util.Date

class GatewayService(
    private val inboxUploadService: InboxUploadService,
    private val messagesService: MessagesService,
    private val settings: GatewaySettings,
    private val events: EventBus,
    private val logsService: LogsService,
    private val deviceService: DeviceService,
) {
    private val eventsReceiver by lazy { EventsReceiver() }

    private var _api: GatewayApi? = null

    private val api
        get() = _api ?: GatewayApi(
            settings.serverUrl,
            settings.privateToken
        ).also { _api = it }

    //region Start, stop, etc...
    fun start(context: Context) {
        if (!settings.enabled) return

        PushService.register(context)
        PullMessagesWorker.start(context)
        WebhooksUpdateWorker.start(context)
        SettingsUpdateWorker.start(context)
        InboxUploadWorker.start(context)

        eventsReceiver.start()
    }

    fun stop(context: Context) {
        eventsReceiver.stop()

        InboxUploadWorker.stop(context)
        SSEForegroundService.stop(context)
        SettingsUpdateWorker.stop(context)
        WebhooksUpdateWorker.stop(context)
        PullMessagesWorker.stop(context)

        this._api = null
    }

    fun isActiveLiveData(context: Context) = PullMessagesWorker.getStateLiveData(context)
    //endregion

    //region Account
    suspend fun getLoginCode(): GatewayApi.GetUserCodeResponse {
        val password = settings.password
            ?: throw IllegalStateException("Password is not set")

        return getLoginCodeWithPassword(password)
    }

    suspend fun getLoginCodeWithPassword(password: String): GatewayApi.GetUserCodeResponse {
        val username = settings.username
            ?: throw IllegalStateException("Username is not set")

        return api.getUserCode(username to password)
    }

    suspend fun changePassword(current: String, new: String) {
        val info = settings.registrationInfo
            ?: throw IllegalStateException("The device is not registered on the server")

        this.api.changeUserPassword(
            info.token,
            GatewayApi.PasswordChangeRequest(current, new)
        )

        settings.registrationInfo = info.copy(password = new)

        events.emit(
            DeviceRegisteredEvent.Success(
                api.hostname,
                info.login,
                new,
            )
        )
    }
    //endregion

    //region Device
    internal suspend fun registerDevice(
        context: Context,
        pushToken: String?,
        registerMode: RegistrationMode
    ) {
        if (!settings.enabled) return

        val settings = settings.registrationInfo
        val accessToken = settings?.token

        if (accessToken != null) {
            // if there's an access token, try to update push token
            try {
                updateDevice(context, pushToken)
                return
            } catch (e: ClientRequestException) {
                // if token is invalid, try to register new one
                if (e.response.status != HttpStatusCode.Unauthorized) {
                    throw e
                }
            }
        }

        try {
            val deviceName = "${Build.MANUFACTURER}/${Build.PRODUCT}"
            val simCards = SubscriptionsHelper.getActiveSimCards(context)
            val keyInfo = deviceService.ensureKey()
            val request = GatewayApi.DeviceRegisterRequest(
                deviceName,
                pushToken,
                simCards.toDTO(),
                publicKey = keyInfo?.publicKeyBase64,
                keyVersion = keyInfo?.keyVersion,
            )
            val response = when (registerMode) {
                RegistrationMode.Anonymous -> api.deviceRegister(request, null)
                is RegistrationMode.WithCode -> api.deviceRegister(request, registerMode.code)
                is RegistrationMode.WithCredentials -> api.deviceRegister(
                    request,
                    registerMode.login to registerMode.password
                )
            }

            this.settings.fcmToken = pushToken
            this.settings.registrationInfo = response

            events.emit(
                DeviceRegisteredEvent.Success(
                    api.hostname,
                    response.login,
                    response.password,
                )
            )
        } catch (th: Throwable) {
            events.emit(
                DeviceRegisteredEvent.Failure(
                    api.hostname,
                    th.localizedMessage ?: th.message ?: th.toString()
                )
            )

            throw th
        }
    }

    internal suspend fun updateDevice(context: Context, pushToken: String?) {
        if (!settings.enabled) return

        val settings = settings.registrationInfo ?: return
        val accessToken = settings.token
        val simCards = SubscriptionsHelper.getActiveSimCards(context)
        val encryptionKey = deviceService.ensureKey()

        api.devicePatch(
            accessToken,
            GatewayApi.DevicePatchRequest(
                settings.id,
                pushToken,
                encryptionKey?.publicKeyBase64,
                encryptionKey?.keyVersion,
                simCards.toDTO(),
            )
        )

        this.settings.fcmToken = pushToken

        events.emit(
            DeviceRegisteredEvent.Success(
                api.hostname,
                settings.login,
                settings.password,
            )
        )
    }

    private fun List<SimCard>.toDTO(): List<GatewayApi.SimCard> = map {
        GatewayApi.SimCard(
            it.slotIndex,
            it.simNumber,
            it.phoneNumber.redact(),
            it.carrierName.redact(),
            it.iccid.redact(),
        )
    }

    sealed class RegistrationMode {
        object Anonymous : RegistrationMode()
        class WithCredentials(val login: String, val password: String) : RegistrationMode()
        class WithCode(val code: String) : RegistrationMode()
    }
    //endregion

    //region Messages
    internal suspend fun getNewMessages(context: Context) {
        if (!settings.enabled) return
        val settings = settings.registrationInfo ?: return
        val processingOrder = when (messagesService.processingOrder) {
            MessagesSettings.ProcessingOrder.LIFO -> GatewayApi.ProcessingOrder.LIFO
            MessagesSettings.ProcessingOrder.FIFO -> GatewayApi.ProcessingOrder.FIFO
        }
        val messages = api.getMessages(settings.token, processingOrder)
        for (message in messages) {
            try {
                processMessage(context, message)
            } catch (th: Throwable) {
                logsService.insert(
                    LogEntry.Priority.ERROR,
                    MODULE_NAME,
                    "Failed to process message",
                    mapOf(
                        "message" to message,
                        "exception" to th.stackTraceToString(),
                    )
                )
                th.printStackTrace()
            }
        }
    }

    private suspend fun processMessage(context: Context, message: GatewayApi.Message) {
        when (message.state) {
            GatewayApi.MessageState.Pending, null -> {}
            GatewayApi.MessageState.Cancelling -> {
                handleCloudCancel(message, messagesService::cancelMessage, ::sendState)
                return
            }

            GatewayApi.MessageState.Cancelled,
            GatewayApi.MessageState.Processed,
            GatewayApi.MessageState.Sent,
            GatewayApi.MessageState.Delivered,
            GatewayApi.MessageState.Failed -> return
        }

        val messageState = messagesService.getMessage(message.id)
        if (messageState != null) {
            SendStateWorker.start(context, message.id)
            return
        }

        val request = SendRequest(
            EntitySource.Cloud,
            com.homingsms.app.modules.messages.data.Message(
                message.id,
                when (val content = message.content) {
                    is GatewayApi.MessageContent.Text -> MessageContent.Text(content.text)
                    is GatewayApi.MessageContent.Data -> MessageContent.Data(
                        content.data,
                        content.port
                    )
                    is GatewayApi.MessageContent.Mms -> MessageContent.Mms(
                        subject = content.subject,
                        text = content.text,
                        attachments = content.attachments.map {
                            MessageContent.Mms.Attachment(
                                contentType = it.contentType,
                                name = it.name,
                                data = it.data,
                            )
                        }
                    )
                },
                message.phoneNumbers,
                message.isEncrypted ?: false,
                message.createdAt ?: Date(),
            ),
            SendParams(
                message.withDeliveryReport ?: true,
                skipPhoneValidation = true,
                simNumber = message.simNumber,
                validUntil = message.validUntil,
                scheduleAt = message.scheduleAt,
                priority = message.priority,
            )
        )
        messagesService.enqueueMessage(request)
    }

    internal suspend fun sendState(
        request: GatewayApi.MessagePatchRequest
    ) {
        val settings = settings.registrationInfo ?: return

        api.patchMessages(
            settings.token,
            listOf(request)
        )
    }
    //endregion

    //region Inbox
    fun enqueueInboxMessage(
        context: Context,
        message: InboxMessage,
        sender: String,
        recipient: String?,
        simNumber: Int?,
        date: Date,
    ) {
        if (!settings.enabled) {
            return
        }

        inboxUploadService.enqueue(
            context, message, sender, recipient, simNumber, date
        )
    }

    internal suspend fun uploadInbox(messages: List<GatewayApi.InboxMessage>) {
        val token = settings.registrationInfo?.token
            ?: return

        api.postInbox(token, messages)
    }
    //endregion

    //region Webhooks
    internal suspend fun getWebHooks(): List<GatewayApi.WebHook> {
        val settings = settings.registrationInfo
        return if (settings != null) {
            api.getWebHooks(settings.token)
        } else {
            emptyList()
        }
    }
    //endregion

    //region Settings
    internal suspend fun getSettings(): Map<String, *>? {
        val settings = settings.registrationInfo ?: return null

        return api.getSettings(settings.token)
    }
    //endregion

    //region Utility
    suspend fun getPublicIP(): String {
        return GatewayApi(
            settings.serverUrl,
            settings.privateToken
        )
            .getDevice(settings.registrationInfo?.token)
            .externalIp
    }
    //endregion

    private fun String?.redact(): String? = when {
        this == null -> null
        length > 4 -> replaceRange(
            0,
            length - 4,
            "*".repeat(length - 4)
        )

        else -> "****"
    }
}

/**
 * Resolves a wire-level "Cancelling" instruction from the gateway (used by
 * [GatewayService.processMessage]). Cancelling is a server-side async-cancel
 * marker that is handled here and never persisted locally:
 * - [cancelMessage] succeeds (message found, still Pending): nothing else to do;
 * - [cancelMessage] throws [IllegalArgumentException] (message missing locally):
 *   report a Cancelled patch back to the server;
 * - [cancelMessage] throws [IllegalStateException] (already sent/cancelled):
 *   the actual outcome was or will be reported, stay silent.
 */
internal suspend fun handleCloudCancel(
    message: GatewayApi.Message,
    cancelMessage: suspend (String) -> Unit,
    sendCancelled: suspend (GatewayApi.MessagePatchRequest) -> Unit
) {
    try {
        cancelMessage(message.id)
    } catch (_: IllegalArgumentException) {
        // message not found locally — nothing to cancel
        sendCancelled(
            GatewayApi.MessagePatchRequest(
                message.id,
                ProcessingState.Cancelled,
                message.phoneNumbers.map {
                    GatewayApi.RecipientState(
                        it,
                        ProcessingState.Cancelled,
                        null
                    )
                },
                mapOf(ProcessingState.Cancelled to Date())
            )
        )
    } catch (_: IllegalStateException) {
        // message not in Pending state — already sent/cancelled
    }
}
