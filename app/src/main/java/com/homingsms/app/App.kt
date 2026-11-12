package com.homingsms.app

import android.app.Application
import android.content.Context
import com.homingsms.app.data.dbModule
import com.homingsms.app.helpers.LocaleHelper
import com.homingsms.app.modules.connection.connectionModule
import com.homingsms.app.modules.device.deviceModule
import com.homingsms.app.modules.encryption.encryptionModule
import com.homingsms.app.modules.events.eventBusModule
import com.homingsms.app.modules.gateway.GatewayService
import com.homingsms.app.modules.incoming.incomingModule
import com.homingsms.app.modules.localserver.localserverModule
import com.homingsms.app.modules.logs.logsModule
import com.homingsms.app.modules.messages.messagesModule
import com.homingsms.app.modules.mms.mmsModule
import com.homingsms.app.modules.notifications.notificationsModule
import com.homingsms.app.modules.orchestrator.OrchestratorService
import com.homingsms.app.modules.orchestrator.orchestratorModule
import com.homingsms.app.modules.ping.pingModule
import com.homingsms.app.modules.receiver.receiverModule
import com.homingsms.app.modules.settings.settingsModule
import com.homingsms.app.modules.webhooks.webhooksModule
import com.homingsms.app.receivers.EventsReceiver
import healthModule
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App: Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let { LocaleHelper.onAttach(it) })
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                eventBusModule,
                settingsModule,
                dbModule,
                logsModule,
                notificationsModule,
                mmsModule,
                messagesModule,
                incomingModule,
                receiverModule,
                encryptionModule,
                com.homingsms.app.modules.gateway.gatewayModule,
                healthModule,
                webhooksModule,
                localserverModule,
                pingModule,
                connectionModule,
                orchestratorModule,
                deviceModule,
            )
        }

        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(
                Thread.getDefaultUncaughtExceptionHandler()!!,
                get()
            )
        )

        instance = this

        EventsReceiver.register(this)

        get<OrchestratorService>().start(this, true)
    }

    val gatewayService: GatewayService by inject()

    companion object {
        lateinit var instance: App
            private set
    }
}
