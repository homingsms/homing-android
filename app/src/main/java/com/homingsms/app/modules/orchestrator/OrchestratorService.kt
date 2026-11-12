package com.homingsms.app.modules.orchestrator

import android.content.Context
import com.homingsms.app.helpers.SettingsHelper
import com.homingsms.app.helpers.SubscriptionsHelper
import com.homingsms.app.modules.device.DeviceService
import com.homingsms.app.modules.gateway.GatewayService
import com.homingsms.app.modules.incoming.IncomingMessagesService
import com.homingsms.app.modules.localserver.LocalServerService
import com.homingsms.app.modules.logs.LogsService
import com.homingsms.app.modules.logs.db.LogEntry
import com.homingsms.app.modules.messages.MessagesService
import com.homingsms.app.modules.ping.PingService
import com.homingsms.app.modules.receiver.ReceiverService
import com.homingsms.app.modules.webhooks.WebHooksService
import com.homingsms.app.modules.webhooks.domain.WebHookEvent
import com.homingsms.app.modules.webhooks.payload.AppStartedPayload

class OrchestratorService(
    private val messagesSvc: MessagesService,
    private val incomingSvc: IncomingMessagesService,
    private val gatewaySvc: GatewayService,
    private val localServerSvc: LocalServerService,
    private val webHooksSvc: WebHooksService,
    private val receiverService: ReceiverService,
    private val pingSvc: PingService,
    private val logsSvc: LogsService,
    private val deviceService: DeviceService,
    private val settings: SettingsHelper,
) {
    fun start(context: Context, autostart: Boolean) {
        if (autostart && !settings.autostart) {
            return
        }

        logsSvc.start(context)
        messagesSvc.start(context)
        incomingSvc.start(context)
        webHooksSvc.start(context)
        deviceService.start(context)
        gatewaySvc.start(context)

        try {
            localServerSvc.start(context)
            pingSvc.start(context)
        } catch (e: Throwable) {
            logsSvc.insert(
                LogEntry.Priority.WARN,
                MODULE_NAME,
                "Can't start foreground services while the app is running in the background"
            )
        }

        try {
            receiverService.start(context)
        } catch (e: Throwable) {
            logsSvc.insert(
                LogEntry.Priority.WARN,
                MODULE_NAME,
                "Can't register receiver"
            )
        }

        val simCards = SubscriptionsHelper.getActiveSimCards(context)
        webHooksSvc.emit(
            context,
            WebHookEvent.AppStarted,
            AppStartedPayload(simCards)
        )
    }

    fun stop(context: Context) {
        receiverService.stop(context)
        pingSvc.stop(context)
        localServerSvc.stop(context)

        gatewaySvc.stop(context)
        webHooksSvc.stop(context)
        messagesSvc.stop(context)
        incomingSvc.stop(context)
        logsSvc.stop(context)
        deviceService.stop(context)
    }
}
