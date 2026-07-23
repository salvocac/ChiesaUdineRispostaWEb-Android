package com.caccavo.chiesaudinerispostaweb.ui.dailyverse

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
import com.caccavo.chiesaudinerispostaweb.ui.bible.VideoCustomizationDialog
import com.caccavo.chiesaudinerispostaweb.ui.bible.BackgroundOption
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

/** Cosa si sta ascoltando adesso nella coda del versetto del giorno: serve a
 * evidenziare e seguire il testo come fa il lettore della Bibbia. */
private sealed interface DailySegment {
    data object Title : DailySegment
    data object Verses : DailySegment
    data class Comment(val sentence: Int) : DailySegment
}

/** Spezza il commento in frasi (i "versetti" del commento): ognuna è una riga
 * evidenziabile singolarmente durante l'ascolto. */
private fun splitIntoSentences(text: String): List<String> {
    val closers = setOf('"', '”', '»', '\'', ')')
    val enders = setOf('.', '!', '?', '…')
    val sentences = mutableListOf<String>()
    val current = StringBuilder()
    for (char in text) {
        if (char.isWhitespace()) {
            val lastMeaningful = current.lastOrNull { it !in closers }
            if (lastMeaningful != null && lastMeaningful in enders) {
                val trimmed = current.toString().trim()
                if (trimmed.isNotEmpty()) sentences.add(trimmed)
                current.clear()
                continue
            }
        }
        current.append(char)
    }
    val trimmed = current.toString().trim()
    if (trimmed.isNotEmpty()) sentences.add(trimmed)
    return sentences
}

/** Il commento è un unico file audio: stima la frase corrente in proporzione ai
 * caratteri già "letti" rispetto all'avanzamento della riproduzione. */
private fun currentSentenceIndex(progress: Float, sentences: List<String>): Int {
    val totalCharacters = sentences.sumOf { it.length }
    if (totalCharacters <= 0) return 0
    val target = totalCharacters * progress
    var cumulative = 0f
    sentences.forEachIndexed { index, sentence ->
        cumulative += sentence.length
        if (target < cumulative) return index
    }
    return (sentences.size - 1).coerceAtLeast(0)
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
    var showVideoCustomizationDialog by remember { mutableStateOf(false) }
    var pendingVideoAction by remember { mutableStateOf<String?>(null) } // "share" or "save"
    var customizationDailyVerse by remember { mutableStateOf<DailyVerse?>(null) }

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

    fun makeVideo(dailyVerse: DailyVerse, backgroundOpt: BackgroundOption, fontSizeScale: Float, fontColor: Int, onReady: (File) -> Unit) {
        val reference = dailyVerse.reference ?: "Versetto del giorno"
        val body = listOfNotNull(dailyVerse.verseText, dailyVerse.reflection).joinToString("\n\n")
        val background = backgroundOpt.gradientBackground ?: VideoBackground.forIndex(dailyVerse.day ?: 0)
        val bgImageResId = backgroundOpt.drawableResId
        scope.launch {
            val audioFile = combinedAudioFile(dailyVerse)
            if (audioFile == null) {
                statusMessage = "Audio del versetto del giorno non disponibile."
                isPreparingVideo = false
                isSavingVideo = false
                return@launch
            }
            val outputFile = File(context.cacheDir, "versetto-del-giorno-video-${dailyVerse.day ?: 0}.mp4")
            val videoFile = VerseVideoFactory.makeVideo(
                context = context,
                reference = reference,
                body = body,
                audioFile = audioFile,
                background = background,
                bgImageResId = bgImageResId,
                fontSizeScale = fontSizeScale,
                fontColor = fontColor,
                outputFile = outputFile
            )
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
        customizationDailyVerse = dailyVerse
        pendingVideoAction = "share"
        showVideoCustomizationDialog = true
    }

    fun saveVideo(dailyVerse: DailyVerse) {
        if (isSavingVideo) return
        customizationDailyVerse = dailyVerse
        pendingVideoAction = "save"
        showVideoCustomizationDialog = true
    }

    fun openDatePicker() {
        val date = viewModel.selectedDate
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                audioManager.stop()
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

    // Frasi del commento e segmento attualmente in ascolto: guidano evidenziazione e scroll.
    val sentences = remember(verse) { splitIntoSentences(verse?.reflection ?: "") }
    val playingSegment: DailySegment? = run {
        if (!audioManager.isSpeaking && !audioManager.isPaused) return@run null
        val currentVerse = verse ?: return@run null
        val ids = audioManager.dailyQueueAudioIds
        val currentId = ids.getOrNull(audioManager.dailyQueueIndex) ?: return@run null
        when (currentId) {
            currentVerse.titleAudioID -> DailySegment.Title
            currentVerse.commentAudioID -> DailySegment.Comment(
                currentSentenceIndex(audioManager.dailyItemProgress, sentences)
            )
            else -> DailySegment.Verses
        }
    }
    val scrollAnchorId: String? = when (playingSegment) {
        DailySegment.Title -> "daily-title"
        DailySegment.Verses -> "daily-verses"
        is DailySegment.Comment -> "daily-sentence-${playingSegment.sentence}"
        null -> null
    }

    val scrollState = rememberScrollState()
    val anchorPositions = remember { mutableStateMapOf<String, Float>() }

    LaunchedEffect(scrollAnchorId) {
        val anchorId = scrollAnchorId ?: return@LaunchedEffect
        val y = anchorPositions[anchorId] ?: return@LaunchedEffect
        // Tiene l'elemento evidenziato verso il centro dello schermo mentre la lettura avanza.
        val target = (y - 350f).toInt().coerceIn(0, scrollState.maxValue)
        scrollState.animateScrollTo(target)
    }

    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    fun Modifier.highlight(active: Boolean, anchorId: String): Modifier = this
        .onGloballyPositioned { anchorPositions[anchorId] = it.positionInParent().y }
        .background(if (active) highlightColor else Color.Transparent, RoundedCornerShape(8.dp))
        .padding(vertical = 4.dp, horizontal = 8.dp)

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
                .verticalScroll(scrollState)
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
                    textAlign = TextAlign.Center,
                    modifier = Modifier.highlight(playingSegment == DailySegment.Title, "daily-title")
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = verse.verseText ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.highlight(playingSegment == DailySegment.Verses, "daily-verses")
                )

                Spacer(Modifier.height(12.dp))

                // Barra di riproduzione sopra il commento, così i controlli restano
                // a portata di mano senza scorrere tutto il testo.
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    IconButton(onClick = { audioManager.speak(verse) }) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Ascolta")
                    }
                    IconButton(
                        onClick = {
                            if (audioManager.isPaused) audioManager.resume() else audioManager.pause()
                        },
                        enabled = audioManager.isSpeaking || audioManager.isPaused
                    ) {
                        Icon(
                            if (audioManager.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (audioManager.isPaused) "Riprendi" else "Pausa"
                        )
                    }
                    IconButton(onClick = { audioManager.stop() }) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    }
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

                Spacer(Modifier.height(12.dp))

                // Il commento frase per frase: durante l'ascolto la frase corrente viene
                // evidenziata e seguita, come i versetti nel lettore della Bibbia.
                sentences.forEachIndexed { index, sentence ->
                    Text(
                        text = sentence,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.highlight(
                            playingSegment == DailySegment.Comment(index),
                            "daily-sentence-$index"
                        )
                    )
                    if (index < sentences.lastIndex) Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(20.dp))

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

        if (showVideoCustomizationDialog) {
            VideoCustomizationDialog(
                onDismiss = {
                    showVideoCustomizationDialog = false
                    pendingVideoAction = null
                    customizationDailyVerse = null
                },
                onConfirm = { backgroundOpt, fontSizeScale, fontColor ->
                    showVideoCustomizationDialog = false
                    val action = pendingVideoAction
                    val v = customizationDailyVerse
                    pendingVideoAction = null
                    customizationDailyVerse = null
                    
                    if (v != null && action != null) {
                        if (action == "share") {
                            isPreparingVideo = true
                            statusMessage = null
                            makeVideo(v, backgroundOpt, fontSizeScale, fontColor) { file ->
                                ShareUtils.shareFile(context, file, "video/mp4", "Invia video versetto")
                            }
                        } else if (action == "save") {
                            isSavingVideo = true
                            statusMessage = null
                            makeVideo(v, backgroundOpt, fontSizeScale, fontColor) { file ->
                                val saved = ShareUtils.saveVideoToGallery(context, file, "versetto-del-giorno-${v.day ?: 0}.mp4")
                                statusMessage = if (saved) "Video salvato in Galleria." else "Non sono riuscito a salvare il video."
                            }
                        }
                    }
                }
            )
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
