package com.aljaz.weighttracker.data.csv

import android.content.ContentResolver
import android.net.Uri
import com.aljaz.weighttracker.data.repository.WeightRepository
import com.aljaz.weighttracker.model.WeightEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Exports and imports weight data as CSV files chosen by the user.
 *
 * Files are addressed by content [Uri]s from Android's file picker, so the
 * app needs no storage permission. All file work runs on [Dispatchers.IO]
 * so the UI never freezes.
 *
 * @param contentResolver Android's gateway for reading/writing picked files.
 * @param repository Source of the entries to export and target of imports.
 */
class CsvTransfer(
    private val contentResolver: ContentResolver,
    private val repository: WeightRepository,
) {

    /**
     * Writes all entries to the file at [uri] (replacing its content).
     * @return Number of entries written.
     * @throws IOException if the file cannot be written.
     */
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val entries = repository.getAllEntries()
        val bytes = WeightCsv.write(entries).toByteArray(Charsets.UTF_8)

        // "wt" = write and truncate, so an old longer file leaves no leftovers.
        contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: throw IOException("Cannot open $uri for writing")

        entries.size
    }

    /**
     * Reads and parses the file at [uri] without saving anything,
     * so the user can review it first.
     * @throws IOException if the file cannot be read.
     */
    suspend fun read(uri: Uri): WeightCsv.ParseResult = withContext(Dispatchers.IO) {
        val text = contentResolver.openInputStream(uri)?.use {
            it.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IOException("Cannot open $uri for reading")

        WeightCsv.parse(text)
    }

    /**
     * Saves [entries] in one database transaction.
     * Days that already exist are replaced by the imported values.
     */
    suspend fun import(entries: List<WeightEntry>) {
        repository.saveEntries(entries)
    }
}