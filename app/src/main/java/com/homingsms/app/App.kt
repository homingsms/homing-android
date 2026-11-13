package com.homingsms.app

import android.app.Application
import com.homingsms.app.data.dbModule
import com.homingsms.app.modules.cloud.cloudModule
import com.homingsms.app.modules.encryption.encryptionModule
import com.homingsms.app.modules.events.eventBusModule
import com.homingsms.app.modules.incoming.incomingModule
import com.homingsms.app.modules.localserver.localserverModule
import com.homingsms.app.modules.logs.logsModule
import com.homingsms.app.modules.messages.messagesModule
import com.homingsms.app.modules.mms.mmsModule
import com.homingsms.app.modules.notifications.notificationsModule
import com.homingsms.app.modules.orchestrator.OrchestratorService
import com.homingsms.app.modules.orchestrator.orchestratorModule
import com.homingsms.app.modules.receiver.receiverModule
import com.homingsms.app.modules.settings.settingsModule
import com.homingsms.app.modules.status.statusModule
import com.homingsms.app.modules.webhooks.webhooksModule
import com.homingsms.app.receivers.EventsReceiver
import com.homingsms.app.ui.uiDataModule
import com.homingsms.app.ui.uiModule
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application() {
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
                webhooksModule,
                cloudModule,
                localserverModule,
                statusModule,
                orchestratorModule,
                uiDataModule,
                uiModule
            )
        }

        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(requireNotNull(Thread.getDefaultUncaughtExceptionHandler()), get())
        )

        EventsReceiver.register(this)

        get<OrchestratorService>().start(this, true)
    }
}
