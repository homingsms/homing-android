package com.homingsms.app.modules.messages

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import android.util.Base64
import androidx.core.app.ActivityCompat
import com.homingsms.app.data.dao.MessagesDao
import com.homingsms.app.data.entities.Message
import com.homingsms.app.data.entities.MessageWithRecipients
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.MessageContent
import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.helpers.PhoneHelper
import com.homingsms.app.helpers.SubscriptionsHelper
import com.homingsms.app.modules.encryption.EncryptionService
import com.homingsms.app.modules.logs.LogsService
import com.homingsms.app.modules.logs.db.LogEntry
import com.homingsms.app.modules.messages.data.MessageSort
import com.homingsms.app.modules.messages.data.SendParams
import com.homingsms.app.modules.messages.data.SendRequest
import com.homingsms.app.modules.messages.data.StoredSendRequest
import com.homingsms.app.modules.messages.exceptions.ConflictException
import com.homingsms.app.modules.messages.workers.LogTruncateWorker
import com.homingsms.app.modules.messages.workers.SendMessagesWorker
import com.homingsms.app.modules.mms.MmsSender
import com.homingsms.app.receivers.EventsReceiver
import com.homingsms.core.model.ErrorCodes
import com.homingsms.core.model.SimSelectionMode
import com.homingsms.core.telephony.SmsResults
import java.util.Date
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class MessagesService(
    private val context: Context,
    private val settings: MessagesSettings,
    private val dao: MessagesDao, // todo: use MessagesRepository
    private val messages: MessagesRepository,
    private val encryptionService: EncryptionService,
    private val machine: MessageStateService,
    private val logsService: LogsService,
    private val mmsSender: MmsSender,
    private val smsLimitMonitor: SmsLimitMonitor
) {
    /**
     * Serializes the worker (KEEP/REPLACE) decisions made from the DB snapshot
     * with the enqueues and worker reschedules, so that a concurrent enqueue
     * cannot install a delayed work between the snapshot read and the start call.
     */
    private val sendWorkerLock = Any()
    val processingOrder
        get() = settings.processingOrder

    private val countryCode: String? =
        (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).networkCountryIso

    // #region Lifecycle
    fun start(context: Context) {
        SendMessagesWorker.start(context, true, SendMessagesWorker.IMMEDIATE)
        LogTruncateWorker.start(context)
    }

    fun stop(context: Context) {
        LogTruncateWorker.stop(context)
        SendMessagesWorker.stop(context)
    }
    // #endregion

    // #region Cancel
    suspend fun cancelMessage(id: String): MessageWithRecipients = machine.transitionRecipients(id, ProcessingState.Cancelled)
    // #endregion

    // #region Send
    fun enqueueMessage(request: SendRequest): MessageWithRecipients {
        val priority = request.params.priority ?: Message.PRIORITY_DEFAULT
        val scheduleAt = request.params.scheduleAt?.time

        synchronized(sendWorkerLock) {
            val nextScheduled = dao.nextScheduledTime()?.takeIf { it > 0 }

            val message = try {
                messages.enqueue(request)
            } catch (_: android.database.sqlite.SQLiteConstraintException) {
                throw ConflictException()
            }

            val workHoursStart = if (priority < Message.PRIORITY_EXPEDITED) {
                settings.nextWorkHoursStart()
            } else {
                null
            }
            val startTime = when {
                workHoursStart != null && workHoursStart < (nextScheduled ?: 0) -> workHoursStart

                scheduleAt != null &&
                    scheduleAt > System.currentTimeMillis() &&
                    scheduleAt < (nextScheduled ?: 0) -> scheduleAt

                else -> SendMessagesWorker.IMMEDIATE
            }

            if (startTime == SendMessagesWorker.IMMEDIATE) {
                SendMessagesWorker.start(
                    context,
                    nextScheduled != null || priority >= Message.PRIORITY_EXPEDITED,
                    SendMessagesWorker.IMMEDIATE
                )
            } else {
                SendMessagesWorker.start(context, true, startTime)
            }

            return message
        }
    }
    // #endregion

    // #region Read
    fun getMessage(id: String): MessageWithRecipients? = dao.get(id)

    /**
     * Count messages based on state and date range
     */
    fun countMessages(source: EntitySource, state: ProcessingState?, start: Long, end: Long) = dao.count(source, state, start, end)

    /**
     * Get messages with pagination and filtering
     */
    fun selectMessages(
        source: EntitySource,
        state: ProcessingState?,
        start: Long,
        end: Long,
        limit: Int,
        offset: Int,
        sort: MessageSort = MessageSort.CreatedAtDesc
    ) = when (sort) {
        MessageSort.CreatedAtDesc -> dao.selectDescending(source, state, start, end, limit, offset)
        MessageSort.CreatedAtAsc -> dao.selectAscending(source, state, start, end, limit, offset)
    }
    // #endregion

    suspend fun processStateIntent(intent: Intent, resultCode: Int) {
        logsService.insert(
            LogEntry.Priority.DEBUG,
            MODULE_NAME,
            "Status intent received with action ${intent.action} and result code $resultCode",
            mapOf(
                "data" to intent.dataString,
                "uri" to intent.extras?.getString("uri"),
                "pdu" to intent.extras?.getByteArray("pdu")?.joinToString("") { "%02x".format(it) }
            )
        )

        if (intent.action == EventsReceiver.ACTION_MMS_SENT) {
            val id = intent.getStringExtra(EventsReceiver.EXTRA_MESSAGE_ID) ?: return
            val (mmsState, mmsError) = if (resultCode == Activity.RESULT_OK) {
                ProcessingState.Sent to null
            } else {
                ProcessingState.Failed to ErrorCodes.encode(ErrorCodes.MMS_FAILED, "MMS send result: ${mmsErrorToMessage(resultCode)}")
            }
            try {
                machine.transitionRecipients(id, mmsState, mmsError)
            } finally {
                MmsSender.cleanup(context, id)
            }
            return
        }

        val (state, error) = when (intent.action) {
            EventsReceiver.ACTION_SENT -> when {
                resultCode != Activity.RESULT_OK -> {
                    if (SmsResults.isLimitHit(resultCode)) smsLimitMonitor.recordLimitHit()
                    ProcessingState.Failed to SmsResults.error(resultCode)
                }

                intent.data != null -> ProcessingState.Sent to null

                else -> return
            }

            EventsReceiver.ACTION_DELIVERED -> when (resultCode) {
                Activity.RESULT_OK -> {
                    val pdu = intent.extras?.getByteArray("pdu")
                    val format = intent.getStringExtra("format")
                    val message = if (format != null) {
                        SmsMessage.createFromPdu(pdu, format)
                    } else {
                        SmsMessage.createFromPdu(pdu)
                    }

                    val rawStatus = message.status
                    val scAddress = message.serviceCenterAddress
                    val status = if (format == "3gpp2") {
                        rawStatus
                    } else {
                        rawStatus and 0x7F
                    }
                    logsService.insert(
                        LogEntry.Priority.DEBUG,
                        MODULE_NAME,
                        "Delivery report parsed",
                        mapOf(
                            "format" to (format ?: "default(3gpp)"),
                            "rawStatus" to rawStatus,
                            "status" to status,
                            "scAddress" to scAddress
                        )
                    )

                    when {
                        format == "3gpp2" -> when (rawStatus ushr 24 and 0x3) {
                            0 -> ProcessingState.Delivered to (rawStatus ushr 16 and 0xFF).takeIf { it > 0 }
                                ?.let { "Delivery result from SC $scAddress: $it" }

                            1 -> return

                            // SC will make more attempts
                            else -> ProcessingState.Failed to ErrorCodes.encode(
                                ErrorCodes.DELIVERY_FAILED,
                                "Delivery result from SC $scAddress: ${rawStatus ushr 16 and 0xFF}"
                            )
                        }

                        status < 0b0100000 -> ProcessingState.Delivered to status.takeIf { it > 0 }
                            ?.let { "Delivery result from SC $scAddress: $it" }

                        status < 0b1000000 -> return

                        // SC will make more attempts
                        else -> ProcessingState.Failed to ErrorCodes.encode(
                            ErrorCodes.DELIVERY_FAILED,
                            "Delivery result from SC $scAddress: $status"
                        )
                    }
                }

                else -> ProcessingState.Failed to ErrorCodes.encode(ErrorCodes.DELIVERY_FAILED, "Delivery result: $resultCode")
            }

            else -> return
        }

        val (id, phone) = intent.dataString?.split("|", limit = 2) ?: return

        machine.transitionRecipient(id, phone, state, error)
    }

    suspend fun truncateLog() {
        val lifetime = settings.logLifetimeDays ?: return

        dao.truncateLog(System.currentTimeMillis() - lifetime * 86400000L)
    }

    internal suspend fun sendPendingMessages() {
        try {
            var previousPriority = Message.PRIORITY_MIN

            while (true) {
                val message = messages.getPending(settings.processingOrder) ?: return
                delay(1L)

                val priority = message.params.priority ?: Message.PRIORITY_DEFAULT

                // apply rate limit if:
                // - message is not expedited
                // - message is expedited and previous message had higher or equal priority
                if (priority < Message.PRIORITY_EXPEDITED ||
                    previousPriority >= priority
                ) {
                    applyLimit()
                }

                // skip work hours for expedited messages
                if (priority < Message.PRIORITY_EXPEDITED && applyWorkHoursLimit()) {
                    continue
                }

                if (!withContext(NonCancellable) { sendMessage(message) }) {
                    // if message was not sent - don't need any delay before next message
                    continue
                }

                // don't apply delay for expedited messages
                if (priority >= Message.PRIORITY_EXPEDITED && previousPriority < priority) {
                    previousPriority = priority
                    continue
                }

                previousPriority = priority

                settings.sendIntervalRange?.let {
                    delay(it.random() * 1000L)
                }
            }
        } finally {
            if (coroutineContext.isActive) {
                synchronized(sendWorkerLock) {
                    // After processing all pending messages, check if there are any scheduled messages for the future
                    val nextScheduledTime = dao.nextScheduledTime() ?: 0
                    if (nextScheduledTime > System.currentTimeMillis()) {
                        SendMessagesWorker.start(context, true, nextScheduledTime)
                    }
                }
            }
        }
    }

    private suspend fun applyLimit() {
        if (!settings.limitEnabled) {
            return
        }

        val processedStats =
            dao.countProcessedFrom(System.currentTimeMillis() - settings.limitPeriod.durationMs)
        if (processedStats.count < settings.limitValue) {
            return
        }

        delay(settings.limitPeriod.durationMs - (System.currentTimeMillis() - processedStats.lastTimestamp) + 1000L)
    }

    /**
     * Delays message sending until work hours start if outside work hours.
     *
     * @return `true` if the current message should be skipped because due
     * expedited messages must be sent first
     */
    private suspend fun applyWorkHoursLimit(): Boolean {
        val nextStart = settings.nextWorkHoursStart() ?: return false

        val delayMs = nextStart - System.currentTimeMillis()
        if (delayMs > 0) {
            val shouldDelay = synchronized(sendWorkerLock) {
                if (dao.countExpeditedDue(Message.PRIORITY_EXPEDITED, Date()) > 0) {
                    logsService.insert(
                        LogEntry.Priority.DEBUG,
                        MODULE_NAME,
                        "Deferring message outside work hours: expedited messages are due"
                    )
                    false
                } else {
                    SendMessagesWorker.start(context, true, nextStart)
                    true
                }
            }
            if (shouldDelay) {
                delay(delayMs)
            }

            return !shouldDelay
        }

        return false
    }

    /**
     * @return `true` if message was sent
     */
    private suspend fun sendMessage(request: StoredSendRequest): Boolean {
        if (request.params.validUntil?.before(Date()) == true) {
            machine.transitionRecipients(
                request.message.id,
                ProcessingState.Failed,
                ErrorCodes.encode(ErrorCodes.EXPIRED, "Message expired before it was sent")
            )
            return false
        }

        // Re-check state: message might have been cancelled while in queue
        val currentState = dao.get(request.message.id)?.message?.state
        if (currentState == ProcessingState.Cancelled) {
            logsService.insert(
                LogEntry.Priority.INFO,
                MODULE_NAME,
                "Skipping cancelled message ${request.message.id}"
            )
            return false
        }

        try {
            when (request.message.content) {
                is MessageContent.Mms -> sendMMS(request)
                else -> sendSMS(request)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            machine.transitionRecipients(
                request.message.id,
                ProcessingState.Failed,
                ErrorCodes.encode(ErrorCodes.SEND_ERROR, "Can't send message: " + e.message)
            )
        }

        return false
    }

    private suspend fun sendMMS(request: StoredSendRequest) {
        val message = request.message
        val id = message.id
        val content = message.content as MessageContent.Mms

        val simNumber = selectSimNumber(request.id, request.params)
        if (request.params.simNumber == null && simNumber != null) {
            dao.updateSimNumber(id, simNumber + 1)
        }

        // Resolve a subscription ID + MSISDN from the chosen SIM slot (if any).
        // Verizon and some other US carriers silently drop MMS whose From
        // header is the insert-address-token, so we pass an explicit MSISDN
        // whenever the platform exposes one.
        val subscriptionId = simNumber?.let { SubscriptionsHelper.getSubscriptionId(context, it) }
        val fromMsisdn = simNumber?.let { SubscriptionsHelper.getPhoneNumber(context, it) }
            ?: SubscriptionsHelper.getActiveSimCards(context)?.firstOrNull()?.phoneNumber

        val decryptedContent = if (message.isEncrypted) {
            MessageContent.Mms(
                subject = content.subject?.let { encryptionService.decrypt(it) },
                text = content.text?.let { encryptionService.decrypt(it) },
                attachments = content.attachments.map {
                    it.copy(
                        name = it.name?.let { v -> encryptionService.decrypt(v) },
                        data = encryptionService.decrypt(it.data)
                    )
                }
            )
        } else {
            content
        }

        val recipients = message.phoneNumbers.map { source ->
            decryptAndNormalizePhone(
                source,
                message.isEncrypted,
                request.params.skipPhoneValidation
            )
        }

        dao.updatePartsCount(id, 1 + decryptedContent.attachments.size)

        // Don't set fromMsisdn — insert-address-token lets the MMSC fill
        // the From field, which matches how Google Messages composes its
        // PDUs and is what Verizon's MMSC expects.
        mmsSender.send(id, recipients, decryptedContent, subscriptionId, fromMsisdn)

        // Move all recipients out of Pending immediately so the worker loop
        // does not re-pull this message while we wait for ACTION_MMS_SENT.
        // State transitions to Sent/Failed are driven by processStateIntent.
        // Only transition recipients that are still Pending: if the
        // ACTION_MMS_SENT callback already recorded Sent/Failed, the terminal
        // state must not be overwritten.
        try {
            machine.transitionRecipients(id, ProcessingState.Processed)
        } catch (_: IllegalStateException) {
            // The ACTION_MMS_SENT callback already advanced recipients to
            // Sent/Failed before this call executed. This is a benign race
            // condition; the callback owns the final state.
        }
    }

    private fun selectSimNumber(id: Long, params: SendParams): Int? {
        if (params.simNumber != null) {
            return params.simNumber - 1
        }

        val simSlots = SubscriptionsHelper.selectAvailableSimSlots(context)?.sorted() ?: return null
        if (simSlots.isEmpty()) {
            throw RuntimeException("No SIMs found")
        }

        return when (settings.simSelectionMode) {
            SimSelectionMode.OSDefault -> SubscriptionsHelper.getDefaultSmsSimSlot(context)?.takeIf { it in simSlots }
            SimSelectionMode.RoundRobin -> simSlots[(id % simSlots.size).toInt()]
            SimSelectionMode.Random -> simSlots.random()
        }
    }

    private suspend fun sendSMS(request: StoredSendRequest) {
        val message = request.message
        val id = message.id

        val simNumber = selectSimNumber(request.id, request.params)
        val smsManager: SmsManager = getSmsManager(simNumber)

        if (request.params.simNumber == null && simNumber != null) {
            dao.updateSimNumber(id, simNumber + 1)
        }

        val sendFn: (String, PendingIntent, PendingIntent?) -> Unit =
            when (val content = message.content) {
                is MessageContent.Text -> {
                    // Handle text messages
                    val text = when (message.isEncrypted) {
                        true -> encryptionService.decrypt(content.text)
                        false -> content.text
                    }

                    val parts = smsManager.divideMessage(text)
                    dao.updatePartsCount(id, parts.size)

                    if (parts.size > 1) {
                        { phoneNumber: String, sentIntent: PendingIntent, deliveredIntent: PendingIntent? ->
                            smsManager.sendMultipartTextMessage(
                                phoneNumber,
                                null,
                                parts,
                                ArrayList(List(parts.size) { sentIntent }),
                                deliveredIntent?.let { intent -> ArrayList(List(parts.size) { intent }) }
                            )
                        }
                    } else {
                        { phoneNumber: String, sentIntent: PendingIntent, deliveredIntent: PendingIntent? ->
                            smsManager.sendTextMessage(
                                phoneNumber,
                                null,
                                text,
                                sentIntent,
                                deliveredIntent
                            )
                        }
                    }
                }

                is MessageContent.Mms ->
                    throw IllegalStateException("MMS content must be handled via sendMMS")

                is MessageContent.Data -> {
                    val data = when (message.isEncrypted) {
                        true -> encryptionService.decrypt(content.data)
                        false -> content.data
                    }
                    val decodedData = try {
                        Base64.decode(data, Base64.DEFAULT)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalArgumentException(
                            "Invalid Base64 data for message ${message.id}",
                            e
                        )
                    }
                    dao.updatePartsCount(id, 1);

                    { phoneNumber: String, sentIntent: PendingIntent, deliveredIntent: PendingIntent? ->
                        smsManager.sendDataMessage(
                            phoneNumber,
                            null, // scAddress
                            content.port.toShort(),
                            decodedData,
                            sentIntent,
                            deliveredIntent
                        )
                    }
                }
            }

        request.message.phoneNumbers
            .forEach { sourcePhoneNumber ->
                val requestCode = id.hashCode()
                val sentIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    Intent(
                        EventsReceiver.ACTION_SENT,
                        Uri.parse("$id|$sourcePhoneNumber"),
                        context,
                        EventsReceiver::class.java
                    ),
                    PendingIntent.FLAG_MUTABLE
                )
                val deliveredIntent = when (request.params.withDeliveryReport) {
                    false -> null

                    true -> PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        Intent(
                            EventsReceiver.ACTION_DELIVERED,
                            Uri.parse("$id|$sourcePhoneNumber"),
                            context,
                            EventsReceiver::class.java
                        ),
                        PendingIntent.FLAG_MUTABLE
                    )
                }

                try {
                    val normalizedPhoneNumber = decryptAndNormalizePhone(
                        sourcePhoneNumber,
                        message.isEncrypted,
                        request.params.skipPhoneValidation
                    )

                    sendFn(normalizedPhoneNumber, sentIntent, deliveredIntent)

                    machine.transitionRecipient(id, sourcePhoneNumber, ProcessingState.Processed)
                } catch (th: Throwable) {
                    logsService.insert(
                        LogEntry.Priority.ERROR,
                        MODULE_NAME,
                        "Can't send message: " + th.message,
                        mapOf(
                            "stacktrace" to th.stackTraceToString()
                        )
                    )

                    machine.transitionRecipient(
                        id,
                        sourcePhoneNumber,
                        ProcessingState.Failed,
                        ErrorCodes.encode(ErrorCodes.SEND_ERROR, "sendSMS: " + th.message)
                    )
                }
            }
    }

    @SuppressLint("NewApi")
    private fun getSmsManager(simNumber: Int?): SmsManager = if (simNumber != null) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw UnsupportedOperationException("SIM selection requires READ_PHONE_STATE permission")
        }

        val subscriptionManager = SubscriptionsHelper.getSubscriptionsManager(context)
            ?: throw UnsupportedOperationException("SIM selection available from API 22")

        subscriptionManager.activeSubscriptionInfoList.orEmpty().find {
            it.simSlotIndex == simNumber
        }?.let {
            if (Build.VERSION.SDK_INT < 31) {
                @Suppress("DEPRECATION")
                SmsManager.getSmsManagerForSubscriptionId(it.subscriptionId)
            } else {
                context.getSystemService(SmsManager::class.java)
                    .createForSubscriptionId(it.subscriptionId)
            }
        } ?: throw UnsupportedOperationException("SIM ${simNumber + 1} not found")
    } else {
        if (Build.VERSION.SDK_INT < 31) {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        } else {
            context.getSystemService(SmsManager::class.java)
        }
    }

    private fun mmsErrorToMessage(resultCode: Int): String = when (resultCode) {
        Activity.RESULT_OK -> "OK"
        SmsManager.MMS_ERROR_UNSPECIFIED -> "MMS_ERROR_UNSPECIFIED (An unspecified error occurred)"
        SmsManager.MMS_ERROR_INVALID_APN -> "MMS_ERROR_INVALID_APN (The APN is invalid)"
        SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS -> "MMS_ERROR_UNABLE_CONNECT_MMS (Unable to connect to the MMS service)"
        SmsManager.MMS_ERROR_HTTP_FAILURE -> "MMS_ERROR_HTTP_FAILURE (HTTP failure during MMS transfer)"
        SmsManager.MMS_ERROR_IO_ERROR -> "MMS_ERROR_IO_ERROR (An I/O error occurred)"
        SmsManager.MMS_ERROR_RETRY -> "MMS_ERROR_RETRY (The carrier indicated a retry is needed)"
        SmsManager.MMS_ERROR_CONFIGURATION_ERROR -> "MMS_ERROR_CONFIGURATION_ERROR (MMS configuration error)"
        SmsManager.MMS_ERROR_NO_DATA_NETWORK -> "MMS_ERROR_NO_DATA_NETWORK (No data network available)"
        else -> "Unknown MMS error: $resultCode"
    }

    private suspend fun decryptAndNormalizePhone(source: String, isEncrypted: Boolean, skipValidation: Boolean): String {
        val phone = if (isEncrypted) encryptionService.decrypt(source) else source
        return if (skipValidation) {
            phone.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        } else {
            PhoneHelper.filterPhoneNumber(phone, countryCode ?: "RU")
        }
    }
}
