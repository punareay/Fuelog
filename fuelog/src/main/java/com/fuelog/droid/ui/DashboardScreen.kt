package com.fuelog.droid.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.fuelog.droid.R
import com.fuelog.droid.ui.components.formatRiel
import com.fuelog.droid.ui.viewmodel.FuelViewModel
import com.fuelog.droid.ui.viewmodel.ReportFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FuelViewModel, onAddClick: () -> Unit) {
    val entries by viewModel.filteredEntries.collectAsState()
    val vehicleDetails by viewModel.vehicleDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentFilter by viewModel.reportFilter.collectAsState()
    val customRange by viewModel.customDateRange.collectAsState()
    
    val totalExpense = entries.sumOf { it.totalCost }

    var showDateRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.sync))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_record))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Vehicle Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.vehicle_identity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.plate_label, vehicleDetails.plateNumber.ifBlank { "N/A" }))
                    Text(text = stringResource(R.string.model_label, vehicleDetails.model.ifBlank { "N/A" }))
                    Text(text = stringResource(R.string.year_label, vehicleDetails.year.ifBlank { "N/A" }))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Section
            Text(
                text = stringResource(R.string.report_range),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = {
                            if (filter == ReportFilter.CUSTOM) {
                                showDateRangePicker = true
                            } else {
                                viewModel.setReportFilter(filter)
                            }
                        },
                        label = {
                            val labelRes = when (filter) {
                                ReportFilter.ALL -> R.string.filter_all
                                ReportFilter.WEEKLY -> R.string.filter_weekly
                                ReportFilter.MONTHLY -> R.string.filter_monthly
                                ReportFilter.QUARTERLY -> R.string.filter_quarterly
                                ReportFilter.SEMESTER -> R.string.filter_semester
                                ReportFilter.YEARLY -> R.string.filter_yearly
                                ReportFilter.CUSTOM -> R.string.filter_custom
                            }
                            Text(stringResource(labelRes))
                        }
                    )
                }
            }

            if (currentFilter == ReportFilter.CUSTOM && customRange != null) {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                Text(
                    text = "${sdf.format(Date(customRange!!.first))} - ${sdf.format(Date(customRange!!.second))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.total_fuel_expense),
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatRiel(totalExpense),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.filtered_records, entries.size),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showDateRangePicker) {
            val dateRangePickerState = rememberDateRangePickerState()
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val start = dateRangePickerState.selectedStartDateMillis
                            val end = dateRangePickerState.selectedEndDateMillis
                            if (start != null && end != null) {
                                viewModel.setCustomDateRange(start, end)
                                showDateRangePicker = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    title = { Text(stringResource(R.string.select_date_range), modifier = Modifier.padding(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
