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
 * days with an empty weight (skipped silently)
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

    /** What a single CSV line turned out to contain. */
    private sealed interface LineResult {
        /** A valid date with a valid weight. */
        data class Entry(val entry: WeightEntry) : LineResult

        /** A valid date with no weight, e.g. "2026-08-19,": a day without a measurement. */
        data object EmptyDay : LineResult

        /** Anything that can't be read safely (or the header row). */
        data object Invalid : LineResult
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

            when (val result = parseLine(line)) {
                is LineResult.Entry -> byDate[result.entry.date] = result.entry
                LineResult.EmptyDay -> Unit  // no measurement that day: nothing to import
                LineResult.Invalid -> if (!isHeader(line)) skipped += index + 1
            }
        }

        return ParseResult(
            entries = byDate.values.sortedBy { it.date },
            skippedLines = skipped,
        )
    }

    /**
     * Parses one data line.
     *
     * The separator is `;` if the line contains one, otherwise `,`.
     * A valid date followed by an empty weight ("2026-08-19," or just
     * "2026-08-19") is a day without a measurement, not an error.
     * More than two non-empty fields are rejected, so "2026-10-16,79,5"
     * (ambiguous decimal comma) is never guessed.
     */
    private fun parseLine(line: String): LineResult {
        val separator = if (';' in line) ';' else ','
        val fields = line.split(separator)
            .map { it.trim().removeSurrounding("\"") }
            .filterIndexed { index, field -> index < 2 || field.isNotEmpty() }
        if (fields.size !in 1..2) return LineResult.Invalid

        val date = try {
            LocalDate.parse(fields[0])
        } catch (e: DateTimeParseException) {
            return LineResult.Invalid
        }

        val weightText = fields.getOrNull(1).orEmpty()
        if (weightText.isEmpty()) return LineResult.EmptyDay

        // Reuses the same validation as typed input (range check, comma or dot).
        return when (val weight = WeightParser.parse(weightText)) {
            is WeightParseResult.Valid -> LineResult.Entry(WeightEntry(date, weight.weightKg))
            is WeightParseResult.Invalid -> LineResult.Invalid
        }
    }

    /** True if [line] looks like a header row (e.g. "date,weight_kg"). */
    private fun isHeader(line: String): Boolean =
        line.removeSurrounding("\"").lowercase().startsWith("date")

    /** Formats a weight with a dot and no unnecessary zeros, e.g. 79.5 → "79.5", 80.0 → "80". */
    private fun formatWeight(weightKg: Double): String =
        BigDecimal.valueOf(weightKg).stripTrailingZeros().toPlainString()
}