package com.aljaz.weighttracker.ui.screens.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.aljaz.weighttracker.model.WeightEntry
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/*
 * This is the ONLY file in the app that uses the Vico chart library.
 * The rest of the app passes in plain WeightEntry lists, so switching
 * chart libraries later would only require rewriting this file.
 */

/** Approximate number of date labels shown on the x-axis. */
private const val TARGET_X_LABELS = 4

/**
 * Line chart of weight over time.
 *
 * - x-axis: dates (as epoch days, so missing days appear as gaps)
 * - y-axis: weight in kg, zoomed to the data instead of starting at 0
 * - the whole range always fits on screen (no scrolling)
 *
 * @param entries Entries to draw, oldest first. Should contain at least 2.
 * @param modifier Optional modifier from the caller (set the height here).
 */
@Composable
fun WeightLineChart(
    entries: List<WeightEntry>,
    modifier: Modifier = Modifier,
) {
    // Holds the chart's data; created once and updated when entries change.
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(entries) {
        if (entries.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineModel {
                series(
                    entries.map { it.date.toEpochDay() },
                    entries.map { it.weightKg },
                )
            }
        }
    }

    // Show a label every N days so they never overlap, whatever the range.
    val daySpan = if (entries.size >= 2) {
        entries.last().date.toEpochDay() - entries.first().date.toEpochDay()
    } else {
        0L
    }
    val labelSpacing = max(1, (daySpan / TARGET_X_LABELS).toInt())

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(rangeProvider = WeightRangeProvider),
            startAxis = VerticalAxis.rememberStart(valueFormatter = WeightAxisFormatter),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = DateAxisFormatter,
                itemPlacer = remember(labelSpacing) {
                    HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing })
                },
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        // Fit all data on screen; the range chips control what is shown.
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
    )
}

/**
 * Sets the y-axis range to the data's min/max, rounded outward to whole kg
 * with 5 kg of padding – so small changes are clearly visible.
 */
private val WeightRangeProvider = object : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore) = floor(minY ) - 5
    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore) = ceil(maxY ) + 5
}

/** y-axis labels: weight without unit, e.g. "79,5". */
private val WeightAxisFormatter = CartesianValueFormatter { _, value, _ ->
    DecimalFormat("0.#").format(value)
}

/** Date format for x-axis labels, e.g. "16. okt." (follows the phone's language). */
private val dateLabelFormat = DateTimeFormatter.ofPattern("d. MMM")

/** x-axis labels: converts an epoch-day x-value back into a short date. */
private val DateAxisFormatter = CartesianValueFormatter { _, value, _ ->
    LocalDate.ofEpochDay(value.toLong()).format(dateLabelFormat)
}