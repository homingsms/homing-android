package com.homingsms.app.modules.webhooks

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.data.webhooks.WebhookDispatcher
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WebhookWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params),
    KoinComponent {
    private val dispatcher: WebhookDispatcher by inject()

    override suspend fun doWork(): Result = try {
        val next = dispatcher.processDue()
        if (next != null) schedule(applicationContext, inject<SettingsRepository>().value, next - System.currentTimeMillis())
        Result.success()
    } catch (_: IOException) {
        Result.retry()
    }

    companion object {
        private const val NOW = "webhooks-now"
        private const val LATER = "webhooks-later"

        fun runNow(context: Context, settings: SettingsRepository) {
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, request(settings, 0))
        }

        fun schedule(context: Context, settings: SettingsRepository, delayMs: Long) {
            WorkManager.getInstance(context).enqueueUniqueWork(LATER, ExistingWorkPolicy.REPLACE, request(settings, delayMs))
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NOW)
            WorkManager.getInstance(context).cancelUniqueWork(LATER)
        }

        private fun request(settings: SettingsRepository, delayMs: Long) = OneTimeWorkRequestBuilder<WebhookWorker>()
            .setInitialDelay(delayMs.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        if (settings.current.webhooks.internetRequired) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED
                    )
                    .build()
            )
            .build()
    }
}
