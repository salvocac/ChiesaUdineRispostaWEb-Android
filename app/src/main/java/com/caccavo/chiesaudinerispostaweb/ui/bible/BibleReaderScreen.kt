package com.caccavo.chiesaudinerispostaweb.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ContentCopy
import kotlinx.coroutines.delay
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caccavo.chiesaudinerispostaweb.audio.BibleAudioManager
import com.caccavo.chiesaudinerispostaweb.bible.BibleReaderPayload
import com.caccavo.chiesaudinerispostaweb.bible.BibleVerse
import com.caccavo.chiesaudinerispostaweb.share.ShareUtils
import com.caccavo.chiesaudinerispostaweb.share.VerseImageFactory
import com.caccavo.chiesaudinerispostaweb.video.AudioCombiner
import com.caccavo.chiesaudinerispostaweb.video.VideoBackground
import com.caccavo.chiesaudinerispostaweb.video.VerseVideoFactory
import com.caccavo.chiesaudinerispostaweb.ui.bible.VideoCustomizationDialog
import com.caccavo.chiesaudinerispostaweb.ui.bible.BackgroundOption
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    payload: BibleReaderPayload,
    allVerses: List<BibleVerse>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { BibleAudioManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var overrideVerses by remember { mutableStateOf<List<BibleVerse>?>(null) }
    var overrideTitle by remember { mutableStateOf<String?>(null) }
    var isSelecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isPreparingVideo by remember { mutableStateOf(false) }
    var videoStatus by remember { mutableStateOf<String?>(null) }
    var didCopyTexts by remember { mutableStateOf(false) }
    var showVideoCustomizationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(didCopyTexts) {
        if (didCopyTexts) {
            delay(2000)
            didCopyTexts = false
        }
    }

    val isFollowingAudio = payload.followsAudio && (audioManager.isSpeaking || audioManager.isPaused)
    val visibleVerses = if (isFollowingAudio) audioManager.currentReadingVerses else (overrideVerses ?: payload.verses)
    val visibleTitle = if (isFollowingAudio) audioManager.currentReadingTitle else (overrideTitle ?: payload.title)

    val selectedVerses = visibleVerses.filter { selectedIds.contains(it.id) }

    fun selectedReference(): String {
        val first = selectedVerses.firstOrNull() ?: return visibleTitle
        if (selectedVerses.size == 1) return "${first.bookName} ${first.chapter}:${first.verse}"
        val last = selectedVerses.last()
        return if (selectedVerses.all { it.bookName == first.bookName && it.chapter == first.chapter }) {
            "${first.bookName} ${first.chapter}:${first.verse}-${last.verse}"
        } else {
            "Versetti selezionati"
        }
    }

    fun selectedShareText(): String =
        selectedVerses.joinToString("\n\n") { "${it.bookName} ${it.chapter}:${it.verse} ${it.text}" }

    fun shareSelectedVerses() {
        if (selectedVerses.isEmpty()) return
        val bitmap = VerseImageFactory.makeVerseImage(context, selectedReference(), selectedShareText())
        ShareUtils.shareImage(context, bitmap)
    }

    fun shareSelectedVerseVideo() {
        val verses = selectedVerses
        if (verses.isEmpty() || isPreparingVideo) return
        showVideoCustomizationDialog = true
    }

    fun generateSelectedVerseVideo(backgroundOpt: BackgroundOption, fontSizeScale: Float, fontColor: Int) {
        val verses = selectedVerses
        if (verses.isEmpty() || isPreparingVideo) return
        isPreparingVideo = true
        videoStatus = null
        val first = verses.first()
        val titleId = audioManager.chapterTitleId(first.bookName, first.chapter, first.translation.audioIdSuffix)
        val candidateIds = listOf(titleId) + verses.map { it.id }
        val reference = selectedReference()
        val body = verses.joinToString(" ") { it.text }
        val background = backgroundOpt.gradientBackground ?: VideoBackground.forIndex(first.book * 31 + first.chapter)
        val bgImageResId = backgroundOpt.drawableResId

        scope.launch {
            val files = audioManager.prepareAudioFiles(candidateIds)
            if (files.isEmpty()) {
                isPreparingVideo = false
                videoStatus = "Audio dei versetti selezionati non disponibile."
                return@launch
            }
            val combined = AudioCombiner.combine(context, files, File(context.cacheDir, "versetti-audio-${System.currentTimeMillis()}.mp3"))
            if (combined == null) {
                isPreparingVideo = false
                videoStatus = "Non sono riuscito a preparare l'audio."
                return@launch
            }
            val outputFile = File(context.cacheDir, "versetti-video-${System.currentTimeMillis()}.mp4")
            val videoFile = VerseVideoFactory.makeVideo(
                context = context,
                reference = reference,
                body = body,
                audioFile = combined,
                background = background,
                bgImageResId = bgImageResId,
                fontSizeScale = fontSizeScale,
                fontColor = fontColor,
                outputFile = outputFile
            )
            isPreparingVideo = false
            if (videoFile == null) {
                videoStatus = "Non sono riuscito a creare il video."
            } else {
                ShareUtils.shareFile(context, videoFile, "video/mp4", "Invia video versetti")
            }
        }
    }

    fun orderedBookChapters(): List<Triple<Int, String, Int>> {
        val seen = HashSet<String>()
        val result = mutableListOf<Triple<Int, String, Int>>()
        for (verse in allVerses) {
            val key = "${verse.book}-${verse.chapter}"
            if (seen.add(key)) {
                result.add(Triple(verse.book, verse.bookName, verse.chapter))
            }
        }
        return result
    }

    fun changeChapter(delta: Int) {
        if (payload.isSearchResult || allVerses.isEmpty()) return
        val current = visibleVerses.firstOrNull() ?: return
        val chapters = orderedBookChapters()
        val currentIndex = chapters.indexOfFirst { it.first == current.book && it.third == current.chapter }
        if (currentIndex < 0) return
        val targetIndex = currentIndex + delta
        if (targetIndex !in chapters.indices) return
        val target = chapters[targetIndex]

        val newVerses = allVerses
            .filter { it.book == target.first && it.chapter == target.third }
            .sortedBy { it.verse }
        if (newVerses.isEmpty()) return

        audioManager.stop()
        overrideVerses = newVerses
        overrideTitle = "${target.second} ${target.third}"
    }

    fun playCurrentReading() {
        val first = visibleVerses.firstOrNull() ?: return
        val last = visibleVerses.lastOrNull() ?: return
        audioManager.speakContinuously(
            bookName = first.bookName,
            chapter = first.chapter,
            startVerse = first.verse,
            endVerse = last.verse,
            allVerses = allVerses
        )
    }

    val listState = rememberLazyListState()
    LaunchedEffect(audioManager.currentVerseId, isFollowingAudio) {
        if (!isFollowingAudio) return@LaunchedEffect
        val index = visibleVerses.indexOfFirst { it.id == audioManager.currentVerseId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(visibleTitle) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Chiudi")
                    }
                },
                actions = {
                    if (!payload.isSearchResult) {
                        IconButton(onClick = { changeChapter(-1) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Capitolo precedente")
                        }
                        IconButton(onClick = { changeChapter(1) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Capitolo successivo")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (visibleVerses.isNotEmpty() && !payload.isSearchResult) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = {
                            isSelecting = !isSelecting
                            if (!isSelecting) selectedIds = emptySet()
                        }) {
                            Icon(
                                if (isSelecting) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSelecting) "Fine" else "Seleziona versetti")
                        }

                        if (isSelecting) {
                            OutlinedButton(onClick = {
                                selectedIds = visibleVerses.map { it.id }.toSet()
                            }) {
                                Text("Seleziona tutti")
                            }
                        }
                    }

                    if (isSelecting) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Versetti selezionati", selectedShareText())
                                    clipboard.setPrimaryClip(clip)
                                    didCopyTexts = true
                                },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (didCopyTexts) Icons.Filled.CheckCircle else Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (didCopyTexts) "Copiato!" else "Copia")
                            }

                            Button(
                                onClick = { shareSelectedVerses() },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Condividi")
                            }

                            if (payload.audioAvailable) {
                                Button(
                                    onClick = { shareSelectedVerseVideo() },
                                    enabled = selectedIds.isNotEmpty() && !isPreparingVideo,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (isPreparingVideo) Icons.Filled.HourglassEmpty else Icons.Filled.Videocam,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isPreparingVideo) "Creo..." else "Video")
                                }
                            }
                        }
                        videoStatus?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (visibleVerses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Nessun versetto trovato", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (payload.isSearchResult && payload.searchedWord != null) {
                    Text(
                        text = "\"${payload.searchedWord}\" trovata ${payload.occurrenceCount ?: 0} volte in ${visibleVerses.size} versetti",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (payload.audioAvailable && !payload.isSearchResult) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        IconButton(onClick = { playCurrentReading() }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Ascolta")
                        }
                        IconButton(
                            onClick = { if (audioManager.isPaused) audioManager.resume() else audioManager.pause() },
                            enabled = audioManager.isSpeaking
                        ) {
                            Icon(
                                if (audioManager.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (audioManager.isPaused) "Riprendi" else "Pausa"
                            )
                        }
                        IconButton(onClick = { audioManager.stop() }, enabled = audioManager.isSpeaking) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop")
                        }
                    }
                    if (audioManager.isSpeaking) {
                        LinearProgressIndicator(
                            progress = audioManager.playbackProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    items(visibleVerses, key = { it.id }) { verse ->
                        val isPlaying = isFollowingAudio && verse.id == audioManager.currentVerseId
                        BibleVerseRow(
                            verse = verse,
                            isSearchResult = payload.isSearchResult,
                            isCurrentlyPlaying = isPlaying,
                            isSelecting = isSelecting,
                            isSelected = selectedIds.contains(verse.id),
                            onToggleSelected = {
                                selectedIds = if (selectedIds.contains(verse.id)) {
                                    selectedIds - verse.id
                                } else {
                                    selectedIds + verse.id
                                }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
        
        if (showVideoCustomizationDialog) {
            VideoCustomizationDialog(
                onDismiss = { showVideoCustomizationDialog = false },
                onConfirm = { backgroundOpt, fontSizeScale, fontColor ->
                    showVideoCustomizationDialog = false
                    generateSelectedVerseVideo(backgroundOpt, fontSizeScale, fontColor)
                }
            )
        }
    }
}

@Composable
private fun BibleVerseRow(
    verse: BibleVerse,
    isSearchResult: Boolean,
    isCurrentlyPlaying: Boolean = false,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelected: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .let { if (isSelecting) it.clickable { onToggleSelected() } else it }
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        if (isSelecting) {
            Icon(
                if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        if (isSearchResult) {
            Column(modifier = Modifier.width(90.dp)) {
                Text(
                    verse.bookName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${verse.chapter}:${verse.verse}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                "${verse.verse}",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(30.dp)
            )
        }

        Text(verse.text, style = MaterialTheme.typography.bodyLarge)
    }
}
