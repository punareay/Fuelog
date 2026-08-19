package com.fuelog.droid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fuelog.droid.data.RefuelEntry
import com.fuelog.droid.ui.components.formatRiel
import com.fuelog.droid.ui.viewmodel.FuelViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: FuelViewModel, onEdit: () -> Unit) {
    val rawEntries by viewModel.filteredEntries.collectAsState(initial = emptyList())
    val entries = remember(rawEntries) {
        rawEntries.sortedWith(compareByDescending<RefuelEntry> { it.date }.thenByDescending { it.id })
    }
    val volumeUnit by viewModel.volumeUnit.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var entryToDelete by remember { mutableStateOf<RefuelEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refuel History") },
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
                            Icon(Icons.Default.Refresh, contentDescription = "Sync")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries) { entry ->
                HistoryItem(
                    entry = entry,
                    volumeUnit = volumeUnit,
                    onEdit = {
                        viewModel.selectEntry(entry)
                        onEdit()
                    },
                    onDelete = { entryToDelete = entry }
                )
            }
        }

        entryToDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                title = { Text("Delete Entry") },
                text = { Text("Are you sure you want to delete this record?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteEntry(entry)
                            entryToDelete = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryItem(
    entry: RefuelEntry,
    volumeUnit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displayDate = remember(entry.date) {
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = isoFormat.parse(entry.date)
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            entry.date
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${entry.id ?: "N/A"} - $displayDate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (entry.isSynced) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    if (!entry.isSynced) {
                        Text(
                            text = "Unsynced",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${entry.vehicleType} - ${entry.fuelType}", fontSize = 14.sp)
                    val stationDisplay = if (entry.stationName.length > 16) "Station:\n${entry.stationName}" else "Station: ${entry.stationName}"
                    Text(text = stationDisplay, fontSize = 14.sp)
                }
                Text(
                    text = formatRiel(entry.totalCost),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = "Consumed: ${entry.fuelConsumed}$volumeUnit @ ${formatRiel(entry.fuelPrice)}/$volumeUnit",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
