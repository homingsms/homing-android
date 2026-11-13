package com.homingsms.app.modules.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.homingsms.app.R

class NotificationsService(context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
            NotificationManager

    private val icons = mapOf(
        NOTIFICATION_ID_LOCAL_SERVICE to R.drawable.notif_server,
        NOTIFICATION_ID_SEND_WORKER to R.drawable.notif_send,
        NOTIFICATION_ID_CLOUD_SERVICE to R.drawable.notif_realtime_events
    )

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.sms_gateway)
            val descriptionText = context.getString(R.string.local_sms_gateway_notifications)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    fun notify(context: Context, id: Int, contentText: String) {
        notificationManager.notify(id, makeNotification(context, id, contentText))
    }

    fun makeNotification(context: Context, id: Int, contentText: String, contentIntent: PendingIntent? = null): Notification =
        NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getText(R.string.sms_gateway))
            .setContentText(contentText)
            .setSmallIcon(icons[id] ?: R.drawable.ic_sms)
            .setContentIntent(
                contentIntent
                    ?: PendingIntent.getActivity(
                        context,
                        0,
                        context.packageManager.getLaunchIntentForPackage(context.packageName),
                        PendingIntent.FLAG_IMMUTABLE
                    )
            )
            .build()

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "homing"

        const val NOTIFICATION_ID_LOCAL_SERVICE = 1
        const val NOTIFICATION_ID_SEND_WORKER = 2
        const val NOTIFICATION_ID_CLOUD_SERVICE = 7
    }
}
