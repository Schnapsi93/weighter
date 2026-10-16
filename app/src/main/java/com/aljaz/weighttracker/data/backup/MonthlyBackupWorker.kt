package com.aljaz.weighttracker.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aljaz.weighttracker.WeightTrackerApp

/**
 * Background job started daily by WorkManager (see [BackupScheduler]).
 * It uploads a backup only if none has succeeded this month, so the
 * daily run costs almost nothing when there is nothing to do.
 *
 * WorkManager creates this class itself, so dependencies are taken
 * from the app's [com.aljaz.weighttracker.di.AppContainer].
 */
class MonthlyBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val backupManager = (applicationContext as WeightTrackerApp).container.backupManager

        val result = backupManager.backupIfDue()
            ?: return Result.success()  // already backed up this month

        return when (result) {
            NextcloudResult.Success -> Result.success()
            is NextcloudResult.Failure -> when (result.error) {
                // Temporary problems: let WorkManager try again later (with growing delays).
                BackupError.NETWORK, BackupError.SERVER ->
                    if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()

                // Retrying won't help (wrong password, no setup…).
                // The error is shown in Settings; tomorrow's run checks again.
                else -> Result.failure()
            }
        }
    }

    private companion object {
        /** Retries per daily run before giving up until the next day. */
        const val MAX_RETRIES = 3
    }
}