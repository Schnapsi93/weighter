package com.aljaz.weighttracker.data.backup

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Turns the automatic monthly backup on and off by scheduling or
 * cancelling [MonthlyBackupWorker] with WorkManager.
 *
 * WorkManager stores the schedule itself, so it survives app restarts,
 * phone reboots and app updates – no need to re-schedule on start.
 *
 * @param workManager Android's WorkManager instance.
 */
class BackupScheduler(
    private val workManager: WorkManager,
) {

    /**
     * Schedules the daily backup check. Safe to call repeatedly: an
     * existing schedule is updated instead of creating a duplicate.
     * The first check runs soon after scheduling.
     */
    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // needs internet
            .setRequiresBatteryNotLow(true)                  // don't drain a low battery
            .build()

        val request = PeriodicWorkRequestBuilder<MonthlyBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Stops automatic backups. */
    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        /** Unique name, so there is never more than one schedule. */
        const val WORK_NAME = "monthly_nextcloud_backup"
    }
}