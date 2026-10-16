package com.aljaz.weighttracker.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Tests for [BackupManager.isBackupDue]. */
class BackupDueTest {

    private val zone = ZoneId.of("Europe/Ljubljana")

    /** Local time in Ljubljana → Instant. */
    private fun at(localDateTime: String): Instant =
        LocalDateTime.parse(localDateTime).atZone(zone).toInstant()

    @Test
    fun neverBackedUp_isDue() {
        assertTrue(BackupManager.isBackupDue(null, at("2026-10-16T10:00"), zone))
    }

    @Test
    fun backedUpEarlierThisMonth_isNotDue() {
        assertFalse(BackupManager.isBackupDue(at("2026-10-01T03:00"), at("2026-10-31T23:59"), zone))
    }

    @Test
    fun backedUpLastMonth_isDue() {
        assertTrue(BackupManager.isBackupDue(at("2026-09-30T23:59"), at("2026-10-01T00:01"), zone))
    }

    @Test
    fun sameMonthLastYear_isDue() {
        assertTrue(BackupManager.isBackupDue(at("2025-10-15T12:00"), at("2026-10-15T12:00"), zone))
    }
}