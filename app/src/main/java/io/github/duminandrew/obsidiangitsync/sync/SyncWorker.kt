package io.github.duminandrew.obsidiangitsync.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import io.github.duminandrew.obsidiangitsync.R

/**
 * Background worker that runs one sync pass. Used both for the periodic
 * auto-sync schedule and (optionally) for an expedited one-off run.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = SyncRepository.getInstance(applicationContext)
        return try {
            val result = repo.runSync()
            if (result.success) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val context = applicationContext
        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        context.getString(R.string.sync_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply { description = context.getString(R.string.sync_channel_desc) }
                )
            }
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.sync_notification_title))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "vault_sync"
        const val NOTIFICATION_ID = 4201
    }
}
