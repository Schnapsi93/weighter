package com.aljaz.weighttracker.data.backup

import com.aljaz.weighttracker.data.csv.WeightCsv
import com.aljaz.weighttracker.data.repository.WeightRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import com.aljaz.weighttracker.BuildConfig

/**
 * Creates backups: exports all entries as CSV, uploads them to Nextcloud
 * and records the outcome. Used by the Settings screen ("Back up now")
 * and, from Milestone 8, by the monthly background job.
 *
 * @param repository Source of the entries.
 * @param client Uploads files to Nextcloud.
 * @param settings Saved connection and backup status.
 * @param clock Source of the current time (replaceable in tests).
 */
class BackupManager(
    private val repository: WeightRepository,
    private val client: NextcloudClient,
    private val settings: BackupSettingsStore,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /** Checks [config] without saving anything. */
    suspend fun testConnection(config: NextcloudConfig): NextcloudResult =
        client.testConnection(config)

    /**
     * Uploads the complete history as this month's backup file,
     * replacing an earlier backup from the same month.
     * The result is recorded in [BackupSettingsStore.status].
     */
    suspend fun backupNow(): NextcloudResult {
        val config = settings.config.first()
        val result = if (config == null) {
            NextcloudResult.Failure(BackupError.NOT_CONFIGURED)
        } else {
            val csv = WeightCsv.write(repository.getAllEntries()).toByteArray(Charsets.UTF_8)
            client.upload(config, BACKUP_FOLDER, fileNameFor(LocalDate.now(clock)), csv)
        }

        when (result) {
            NextcloudResult.Success -> settings.recordSuccess(Instant.now(clock))
            is NextcloudResult.Failure -> settings.recordFailure(result.error)
        }
        return result
    }

    /**
     * Backs up only if no backup has succeeded yet in the current calendar month.
     * Called daily by the background job.
     *
     * @return The backup result, or null if no backup was needed.
     */
    suspend fun backupIfDue(): NextcloudResult? {
        val lastSuccess = settings.status.first().lastSuccessAt
        if (!isBackupDue(lastSuccess, Instant.now(clock), clock.zone)) return null
        return backupNow()
    }

    companion object {
        /**
         * Folder in the user's Nextcloud where backups are stored. (nested levels allowed).
         * Debug builds use a separate folder so test data never
         * overwrites real backups.
         */
        val BACKUP_FOLDER = if (BuildConfig.DEBUG)
            "myapps/weighter/backups-debug" else "myapps/weighter/backups"

        /** Backup file name for the month of [date], e.g. "weight-tracker-2026-10.csv". */
        fun fileNameFor(date: LocalDate): String =
            "weighter-${YearMonth.from(date)}.csv"

        /**
         * True if no backup has succeeded in the calendar month of [now].
         *
         * @param lastSuccess Time of the last successful backup, or null if never.
         * @param now The current time.
         * @param zone Time zone that defines where a month starts and ends.
         */
        fun isBackupDue(lastSuccess: Instant?, now: Instant, zone: ZoneId): Boolean {
            if (lastSuccess == null) return true
            return YearMonth.from(lastSuccess.atZone(zone)) != YearMonth.from(now.atZone(zone))
        }
    }
}