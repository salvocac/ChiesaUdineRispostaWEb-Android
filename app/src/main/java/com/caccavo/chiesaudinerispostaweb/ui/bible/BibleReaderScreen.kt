package com.caccavo.chiesaudinerispostaweb.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caccavo.chiesaudinerispostaweb.audio.BibleAudioManager
import com.caccavo.chiesaudinerispostaweb.bible.BibleReaderPayload
import com.caccavo.chiesaudinerispostaweb.bible.BibleVerse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    payload: BibleReaderPayload,
    allVerses: List<BibleVerse>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { BibleAudioManager.getInstance(context) }

    var overrideVerses by remember { mutableStateOf<List<BibleVerse>?>(null) }
    var overrideTitle by remember { mutableStateOf<String?>(null) }

    val isFollowingAudio = payload.followsAudio && (audioManager.isSpeaking || audioManager.isPaused)
    val visibleVerses = if (isFollowingAudio) audioManager.currentReadingVerses else (overrideVerses ?: payload.verses)
    val visibleTitle = if (isFollowingAudio) audioManager.currentReadingTitle else (overrideTitle ?: payload.title)

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
                        BibleVerseRow(verse = verse, isSearchResult = payload.isSearchResult, isCurrentlyPlaying = isPlaying)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BibleVerseRow(verse: BibleVerse, isSearchResult: Boolean, isCurrentlyPlaying: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
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
