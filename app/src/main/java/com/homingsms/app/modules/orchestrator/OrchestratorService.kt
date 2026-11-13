package com.homingsms.app.modules.orchestrator

import android.content.Context
import com.homingsms.app.modules.cloud.CloudController
import com.homingsms.app.modules.incoming.IncomingMessagesService
import com.homingsms.app.modules.localserver.LocalServerService
import com.homingsms.app.modules.logs.LogsService
import com.homingsms.app.modules.logs.db.LogEntry
import com.homingsms.app.modules.messages.MessagesService
import com.homingsms.app.modules.receiver.ReceiverService
import com.homingsms.app.modules.status.AppStateRepository
import com.homingsms.app.modules.webhooks.WebhooksService
import com.homingsms.core.data.settings.SettingsRepository

class OrchestratorService(
    private val messagesSvc: MessagesService,
    private val incomingSvc: IncomingMessagesService,
    private val cloud: CloudController,
    private val localServerSvc: LocalServerService,
    private val webhooksSvc: WebhooksService,
    private val receiverService: ReceiverService,
    private val logsSvc: LogsService,
    private val settings: SettingsRepository,
    private val state: AppStateRepository
) {
    fun start(context: Context, autostart: Boolean) {
        if (autostart && !settings.current.general.autostart) return

        localServerSvc.ensureDeviceId()
        logsSvc.start(context)
        messagesSvc.start(context)
        incomingSvc.start(context)
        webhooksSvc.start()

        try {
            localServerSvc.start(context)
            cloud.start()
        } catch (e: IllegalStateException) {
            logsSvc.insert(
                LogEntry.Priority.WARN,
                MODULE_NAME,
                "Can't start foreground services while the app is running in the background",
                mapOf("error" to e.message)
            )
        }

        try {
            receiverService.start(context)
        } catch (e: SecurityException) {
            logsSvc.insert(LogEntry.Priority.WARN, MODULE_NAME, "Can't register receiver", mapOf("error" to e.message))
        }
        state.setRunning(true)
    }

    fun stop(context: Context) {
        receiverService.stop(context)
        localServerSvc.stop(context)
        cloud.stop()
        webhooksSvc.stop()
        messagesSvc.stop(context)
        incomingSvc.stop(context)
        logsSvc.stop(context)
        state.setRunning(false)
    }
}
