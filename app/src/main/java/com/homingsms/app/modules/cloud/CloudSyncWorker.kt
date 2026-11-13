package com.homingsms.app.modules.cloud

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.homingsms.core.data.cloud.CloudSyncEngine
import com.homingsms.core.data.cloud.SyncOutcome
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CloudSyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params),
    KoinComponent {
    private val engine: CloudSyncEngine by inject()

    override suspend fun doWork(): Result {
        val outcome = when (inputData.getString(KEY_ACTION)) {
            ACTION_FLUSH -> engine.flush()

            ACTION_WAKE -> engine.handleWake(
                inputData.keyValueMap.filterKeys { it.startsWith(WAKE_PREFIX) }
                    .map { (key, value) -> key.removePrefix(WAKE_PREFIX) to value.toString() }
                    .toMap()
            )

            ACTION_FCM_TOKEN -> engine.onFcmToken(inputData.getString(KEY_TOKEN))

            else -> engine.heartbeat()
        }
        return when (outcome) {
            is SyncOutcome.Retry -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        private const val KEY_ACTION = "action"
        private const val KEY_TOKEN = "token"
        private const val WAKE_PREFIX = "wake."
        private const val ACTION_HEARTBEAT = "heartbeat"
        private const val ACTION_FLUSH = "flush"
        private const val ACTION_WAKE = "wake"
        private const val ACTION_FCM_TOKEN = "fcm"
        private const val PERIODIC = "cloud-periodic"
        private const val FLUSH = "cloud-flush"
        private const val WAKE = "cloud-wake"
        private const val FCM = "cloud-fcm"
        private const val PERIODIC_MINUTES = 15L
        private const val BACKOFF_SECONDS = 15L

        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(PERIODIC_MINUTES, TimeUnit.MINUTES)
                .setConstraints(network)
                .setInputData(Data.Builder().putString(KEY_ACTION, ACTION_HEARTBEAT).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun flush(context: Context) {
            val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(network)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .setInputData(Data.Builder().putString(KEY_ACTION, ACTION_FLUSH).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(FLUSH, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        fun wake(context: Context, data: Map<String, String>) {
            val input = Data.Builder().putString(KEY_ACTION, ACTION_WAKE)
            data.forEach { (key, value) -> input.putString(WAKE_PREFIX + key, value) }
            val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(network)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(input.build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WAKE, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        fun fcmToken(context: Context, token: String?) {
            val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(network)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .setInputData(Data.Builder().putString(KEY_ACTION, ACTION_FCM_TOKEN).putString(KEY_TOKEN, token).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(FCM, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancelAll(context: Context) {
            val manager = WorkManager.getInstance(context)
            listOf(PERIODIC, FLUSH, WAKE).forEach(manager::cancelUniqueWork)
        }
    }
}
