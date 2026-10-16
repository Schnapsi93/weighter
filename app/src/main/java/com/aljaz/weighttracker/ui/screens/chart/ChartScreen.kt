package com.aljaz.weighttracker.ui.screens.chart

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aljaz.weighttracker.R
import com.aljaz.weighttracker.ui.AppViewModelProvider
import com.aljaz.weighttracker.util.WeightFormatter

/** Height of the chart area. */
private val CHART_HEIGHT = 260.dp

/**
 * Chart screen (stateful): connects [ChartViewModel] to [ChartContent].
 *
 * @param modifier Optional modifier from the caller.
 * @param viewModel Created automatically; can be replaced in tests.
 */
@Composable
fun ChartScreen(
    modifier: Modifier = Modifier,
    viewModel: ChartViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChartContent(
        uiState = uiState,
        onRangeSelected = viewModel::onRangeSelected,
        modifier = modifier,
    )
}

/**
 * Chart screen layout (stateless): range chips, chart and statistics.
 *
 * @param uiState What to display.
 * @param onRangeSelected Called when a range chip is tapped.
 * @param modifier Optional modifier from the caller.
 */
@Composable
fun ChartContent(
    uiState: ChartUiState,
    onRangeSelected: (ChartRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RangeSelector(
            selected = uiState.selectedRange,
            onRangeSelected = onRangeSelected,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            if (uiState.entries.size >= 2) {
                WeightLineChart(
                    entries = uiState.entries,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CHART_HEIGHT)
                        .padding(12.dp),
                )
            } else if (!uiState.isLoading) {
                NotEnoughData()
            }
        }

        uiState.stats?.let { StatsCard(it) }
    }
}

/**
 * Row of chips for choosing the time range. Scrolls sideways on narrow screens.
 *
 * @param selected The currently selected range.
 * @param onRangeSelected Called with the tapped range.
 */
@Composable
private fun RangeSelector(
    selected: ChartRange,
    onRangeSelected: (ChartRange) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChartRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onRangeSelected(range) },
                label = { Text(stringResource(range.labelRes)) },
            )
        }
    }
}

/** Shown inside the chart card when there are fewer than 2 entries in the range. */
@Composable
private fun NotEnoughData() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.chart_not_enough_data),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/**
 * Card listing the statistics for the selected range.
 *
 * @param stats Values to display.
 */
@Composable
private fun StatsCard(stats: ChartStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.chart_stats_title),
                style = MaterialTheme.typography.titleMedium,
            )
            StatRow(R.string.chart_stat_latest, WeightFormatter.formatKg(stats.latest))
            StatRow(R.string.chart_stat_change, WeightFormatter.formatKgChange(stats.change))
            StatRow(R.string.chart_stat_lowest, WeightFormatter.formatKg(stats.lowest))
            StatRow(R.string.chart_stat_highest, WeightFormatter.formatKg(stats.highest))
            StatRow(R.string.chart_stat_average, WeightFormatter.formatKg(stats.average))
            StatRow(R.string.chart_stat_std_dev, WeightFormatter.formatKg(stats.standardDeviation))

        }
    }
}

/**
 * One "label ........ value" line in the statistics card.
 *
 * @param labelRes String resource for the label.
 * @param value Already formatted value.
 */
@Composable
private fun StatRow(labelRes: Int, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}