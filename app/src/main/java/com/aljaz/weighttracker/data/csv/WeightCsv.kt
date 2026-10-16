package com.aljaz.weighttracker.data.csv

import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.util.WeightParseResult
import com.aljaz.weighttracker.util.WeightParser
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Converts weight entries to and from CSV text.
 *
 * Written format (always the same, independent of phone language):
 * ```
 * date,weight_kg
 * 2026-10-16,79.5
 * ```
 * Reading is tolerant: it also accepts `;` as separator with a decimal comma
 * (what Excel saves on Slovenian systems), a header row, blank lines,
 * quotes around values, and a leading byte-order mark.
 */
object WeightCsv {

    /** First line of every exported file. */
    const val HEADER = "date,weight_kg"

    /** Invisible "byte order mark" some programs (Excel) put at the start of a file. */
    private const val BOM = "\uFEFF"

    /**
     * Result of [parse].
     *
     * @property entries Valid entries, oldest first, at most one per day.
     * @property skippedLines 1-based line numbers that could not be read.
     */
    data class ParseResult(
        val entries: List<WeightEntry>,
        val skippedLines: List<Int>,
    )

    /**
     * Converts [entries] to CSV text, including the header line.
     * @param entries Entries to write (written in the given order).
     */
    fun write(entries: List<WeightEntry>): String = buildString {
        appendLine(HEADER)
        entries.forEach { entry ->
            appendLine("${entry.date},${formatWeight(entry.weightKg)}")
        }
    }

    /**
     * Reads entries from CSV [text].
     *
     * If the same date appears more than once, the last occurrence wins.
     * Lines that cannot be read are reported in [ParseResult.skippedLines]
     * instead of guessing their meaning.
     */
    fun parse(text: String): ParseResult {
        val byDate = LinkedHashMap<LocalDate, WeightEntry>()
        val skipped = mutableListOf<Int>()

        text.removePrefix(BOM).lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed

            val entry = parseLine(line)
            when {
                entry != null -> byDate[entry.date] = entry
                isHeader(line) -> Unit  // header row: ignore silently
                else -> skipped += index + 1
            }
        }

        return ParseResult(
            entries = byDate.values.sortedBy { it.date },
            skippedLines = skipped,
        )
    }

    /**
     * Parses one data line, or returns null if it is not a valid entry.
     *
     * The separator is `;` if the line contains one, otherwise `,`.
     * Exactly two non-empty fields are required (trailing empty fields, which
     * Excel sometimes adds, are allowed). This rejects "2026-10-16,79,5":
     * with a comma separator the decimal comma makes the line ambiguous.
     */
    private fun parseLine(line: String): WeightEntry? {
        val separator = if (';' in line) ';' else ','
        val fields = line.split(separator)
            .map { it.trim().removeSurrounding("\"") }
            .filterIndexed { index, field -> index < 2 || field.isNotEmpty() }
        if (fields.size != 2) return null

        val date = try {
            LocalDate.parse(fields[0])
        } catch (e: DateTimeParseException) {
            return null
        }

        // Reuses the same validation as typed input (range check, comma or dot).
        return when (val weight = WeightParser.parse(fields[1])) {
            is WeightParseResult.Valid -> WeightEntry(date, weight.weightKg)
            is WeightParseResult.Invalid -> null
        }
    }

    /** True if [line] looks like a header row (e.g. "date,weight_kg"). */
    private fun isHeader(line: String): Boolean =
        line.removeSurrounding("\"").lowercase().startsWith("date")

    /** Formats a weight with a dot and no unnecessary zeros, e.g. 79.5 → "79.5", 80.0 → "80". */
    private fun formatWeight(weightKg: Double): String =
        BigDecimal.valueOf(weightKg).stripTrailingZeros().toPlainString()
}