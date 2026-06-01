package io.github.duminandrew.obsidiangitsync.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules background sync via WorkManager with network constraints.
 *
 * Periodic interval is the WorkManager minimum (15 minutes). [wifiOnly]
 * chooses between UNMETERED and CONNECTED network constraints.
 */
object SyncScheduler {

    private const val PERIODIC_WORK = "periodic_vault_sync"
    private const val ONESHOT_WORK = "oneshot_vault_sync"
    private const val PERIODIC_MINUTES = 15L

    fun enablePeriodic(context: Context, wifiOnly: Boolean) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_MINUTES, TimeUnit.MINUTES,
        ).setConstraints(constraints(wifiOnly)).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun disablePeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
    }

    /** Enqueue an expedited one-off sync (used by the "Sync now" button as a fallback). */
    fun syncNow(context: Context, wifiOnly: Boolean) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints(wifiOnly))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONESHOT_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun constraints(wifiOnly: Boolean): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
}
