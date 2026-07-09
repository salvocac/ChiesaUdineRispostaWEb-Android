package com.caccavo.chiesaudinerispostaweb.ui.dailyverse

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caccavo.chiesaudinerispostaweb.dailyverse.DailyVerseViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyVerseScreen(
    onClose: () -> Unit,
    viewModel: DailyVerseViewModel = viewModel()
) {
    val context = LocalContext.current
    val verse = viewModel.dailyVerse
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN) }

    fun openDatePicker() {
        val date = viewModel.selectedDate
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                viewModel.selectDate(LocalDate.of(year, month + 1, dayOfMonth))
            },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
        val start = viewModel.programStartDate
        val today = LocalDate.now()
        dialog.datePicker.minDate = start.atStartOfDayEpochMilli()
        dialog.datePicker.maxDate = today.atStartOfDayEpochMilli()
        dialog.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Versetto del giorno") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Chiudi")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = viewModel::goToPreviousDay, enabled = viewModel.canGoToPreviousDay) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Giorno precedente")
                }

                TextButton(onClick = { openDatePicker() }) {
                    Text(viewModel.selectedDate.format(dateFormatter))
                }

                IconButton(onClick = viewModel::goToNextDay, enabled = viewModel.canGoToNextDay) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Giorno successivo")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (verse == null) {
                Text("Nessun versetto disponibile", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    text = verse.reference ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = verse.verseText ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = verse.reflection ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Ascolta")
                    }
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "L'ascolto audio arriverà in un prossimo aggiornamento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShareActionButton(icon = Icons.Filled.Share, label = "Invia audio")
                    ShareActionButton(icon = Icons.Filled.Videocam, label = "Invia video")
                    ShareActionButton(icon = Icons.Filled.Download, label = "Salva video")
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Condivisione audio/video del versetto in arrivo prossimamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ShareActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = {}, enabled = false) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun LocalDate.atStartOfDayEpochMilli(): Long =
    this.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
