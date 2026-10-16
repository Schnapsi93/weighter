package com.aljaz.weighttracker.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*
 * Material's DatePicker works with milliseconds at UTC midnight.
 * These helpers convert between that format and LocalDate.
 * Always use UTC here – using the phone's time zone can shift
 * the selected day by one.
 */

/** Converts this date to milliseconds at UTC midnight. */
fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Converts DatePicker milliseconds (UTC midnight) back to a [LocalDate]. */
fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()