package com.homingsms.app.modules.cloud

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.homingsms.core.data.cloud.WakeReason

object PushTokens {
    fun available(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun refresh(context: Context) {
        if (!available(context)) return
        val appContext = context.applicationContext
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) CloudSyncWorker.fcmToken(appContext, task.result)
        }
    }
}

class HomingMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        CloudSyncWorker.fcmToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["kind"] != WakeReason.KIND) return
        CloudSyncWorker.wake(applicationContext, message.data)
    }
}
