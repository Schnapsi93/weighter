package com.aljaz.weighttracker.ui.screens.chart

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.ui.AppViewModelProvider
import com.aljaz.weighttracker.ui.components.editor.WeightEditorDialog
import com.aljaz.weighttracker.util.WeightFormatter
import com.aljaz.weighttracker.ui.components.editor.WeightEditorDialog

/** Height of the chart area. */
private val CHART_HEIGHT = 260.dp

/**
 * Chart screen (stateful): connects [ChartViewModel] to [ChartContent]
 * and shows the add/edit dialog when it is open.
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
        onTodayClick = viewModel::onTodayClick,
        modifier = modifier,
    )

    uiState.editor?.let { editorState ->
        // No onDelete here: deleting (with Undo) is done in History.
        WeightEditorDialog(
            state = editorState,
            onDateChange = viewModel.editor::onDateChange,
            onInputChange = viewModel.editor::onInputChange,
            onSave = viewModel.editor::save,
            onDismiss = viewModel.editor::dismiss,
        )
    }
}

/**
 * Chart screen layout (stateless): range chips, chart, statistics
 * and the "today" floating button.
 *
 * @param uiState What to display.
 * @param onRangeSelected Called when a range chip is tapped.
 * @param onTodayClick Called when the floating button is tapped.
 * @param modifier Optional modifier from the caller.
 */
@Composable
fun ChartContent(
    uiState: ChartUiState,
    onRangeSelected: (ChartRange) -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        // The app's root Scaffold already handles system bar insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            TodayButton(todayEntry = uiState.todayEntry, onClick = onTodayClick)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            // Space so the floating button never covers the statistics.
            Spacer(Modifier.height(72.dp))
        }
    }
}

/**
 * Floating button for today's weight: "Add today", or "Today: 79,5 kg"
 * with an edit icon once today's weight is saved.
 *
 * @param todayEntry Today's entry, or null if not entered yet.
 * @param onClick Called when the button is tapped.
 */
@Composable
private fun TodayButton(
    todayEntry: WeightEntry?,
    onClick: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = {
            Icon(
                imageVector = if (todayEntry != null) Icons.Filled.Edit else Icons.Filled.Add,
                contentDescription = null,
            )
        },
        text = {
            Text(
                if (todayEntry != null) {
                    stringResource(
                        R.string.chart_fab_today_value,
                        WeightFormatter.formatKg(todayEntry.weightKg),
                    )
                } else {
                    stringResource(R.string.chart_fab_add_today)
                }
            )
        },
    )
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