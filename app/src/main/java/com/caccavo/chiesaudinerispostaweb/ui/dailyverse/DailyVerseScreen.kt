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
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caccavo.chiesaudinerispostaweb.audio.BibleAudioManager
import com.caccavo.chiesaudinerispostaweb.dailyverse.DailyVerse
import com.caccavo.chiesaudinerispostaweb.dailyverse.DailyVerseViewModel
import com.caccavo.chiesaudinerispostaweb.share.ShareUtils
import com.caccavo.chiesaudinerispostaweb.video.AudioCombiner
import com.caccavo.chiesaudinerispostaweb.video.VerseVideoFactory
import com.caccavo.chiesaudinerispostaweb.video.VideoBackground
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun DailyVerse.candidateAudioIds(): List<String> = buildList {
    titleAudioID?.let { add(it) }
    addAll(audioIDs.orEmpty())
    commentAudioID?.let { add(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyVerseScreen(
    onClose: () -> Unit,
    viewModel: DailyVerseViewModel = viewModel()
) {
    val context = LocalContext.current
    val audioManager = remember { BibleAudioManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val verse = viewModel.dailyVerse
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN) }

    var isPreparingAudio by remember { mutableStateOf(false) }
    var isPreparingVideo by remember { mutableStateOf(false) }
    var isSavingVideo by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    suspend fun combinedAudioFile(dailyVerse: DailyVerse): File? {
        val files = audioManager.prepareAudioFiles(dailyVerse.candidateAudioIds())
        if (files.isEmpty()) return null
        val output = File(context.cacheDir, "versetto-del-giorno-${dailyVerse.day ?: 0}.mp3")
        return AudioCombiner.combine(context, files, output)
    }

    fun shareAudio(dailyVerse: DailyVerse) {
        if (isPreparingAudio) return
        isPreparingAudio = true
        statusMessage = null
        scope.launch {
            val file = combinedAudioFile(dailyVerse)
            isPreparingAudio = false
            if (file == null) {
                statusMessage = "Audio del versetto del giorno non disponibile."
                return@launch
            }
            ShareUtils.shareFile(context, file, "audio/mpeg", "Invia audio versetto", dailyVerse.reference)
        }
    }

    fun makeVideo(dailyVerse: DailyVerse, onReady: (File) -> Unit) {
        val reference = dailyVerse.reference ?: "Versetto del giorno"
        val body = listOfNotNull(dailyVerse.verseText, dailyVerse.reflection).joinToString("\n\n")
        val background = VideoBackground.forIndex(dailyVerse.day ?: 0)
        scope.launch {
            val audioFile = combinedAudioFile(dailyVerse)
            if (audioFile == null) {
                statusMessage = "Audio del versetto del giorno non disponibile."
                isPreparingVideo = false
                isSavingVideo = false
                return@launch
            }
            val outputFile = File(context.cacheDir, "versetto-del-giorno-video-${dailyVerse.day ?: 0}.mp4")
            val videoFile = VerseVideoFactory.makeVideo(context, reference, body, audioFile, background, outputFile)
            isPreparingVideo = false
            isSavingVideo = false
            if (videoFile == null) {
                statusMessage = "Non sono riuscito a creare il video."
            } else {
                onReady(videoFile)
            }
        }
    }

    fun shareVideo(dailyVerse: DailyVerse) {
        if (isPreparingVideo) return
        isPreparingVideo = true
        statusMessage = null
        makeVideo(dailyVerse) { file ->
            ShareUtils.shareFile(context, file, "video/mp4", "Invia video versetto")
        }
    }

    fun saveVideo(dailyVerse: DailyVerse) {
        if (isSavingVideo) return
        isSavingVideo = true
        statusMessage = null
        makeVideo(dailyVerse) { file ->
            val saved = ShareUtils.saveVideoToGallery(context, file, "versetto-del-giorno-${dailyVerse.day ?: 0}.mp4")
            statusMessage = if (saved) "Video salvato in Galleria." else "Non sono riuscito a salvare il video."
        }
    }

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
                IconButton(
                    onClick = { audioManager.stop(); viewModel.goToPreviousDay() },
                    enabled = viewModel.canGoToPreviousDay
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Giorno precedente")
                }

                TextButton(onClick = { openDatePicker() }) {
                    Text(viewModel.selectedDate.format(dateFormatter))
                }

                IconButton(
                    onClick = { audioManager.stop(); viewModel.goToNextDay() },
                    enabled = viewModel.canGoToNextDay
                ) {
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
                    IconButton(onClick = { audioManager.speak(verse) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Ascolta")
                    }
                    IconButton(onClick = { audioManager.stop() }, enabled = audioManager.isSpeaking) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    }
                }

                if (audioManager.isSpeaking) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = audioManager.playbackProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (audioManager.lastError.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        audioManager.lastError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShareActionButton(
                        icon = if (isPreparingAudio) Icons.Filled.HourglassEmpty else Icons.Filled.Share,
                        label = if (isPreparingAudio) "Preparo..." else "Invia audio",
                        enabled = !isPreparingAudio,
                        onClick = { shareAudio(verse) }
                    )
                    ShareActionButton(
                        icon = if (isPreparingVideo) Icons.Filled.HourglassEmpty else Icons.Filled.Videocam,
                        label = if (isPreparingVideo) "Preparo..." else "Invia video",
                        enabled = !isPreparingVideo,
                        onClick = { shareVideo(verse) }
                    )
                    ShareActionButton(
                        icon = if (isSavingVideo) Icons.Filled.HourglassEmpty else Icons.Filled.Download,
                        label = if (isSavingVideo) "Salvo..." else "Salva video",
                        enabled = !isSavingVideo,
                        onClick = { saveVideo(verse) }
                    )
                }

                statusMessage?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareActionButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun LocalDate.atStartOfDayEpochMilli(): Long =
    this.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
