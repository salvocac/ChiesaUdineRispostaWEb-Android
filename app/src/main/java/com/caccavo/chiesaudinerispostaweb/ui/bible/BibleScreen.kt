package com.caccavo.chiesaudinerispostaweb.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caccavo.chiesaudinerispostaweb.audio.BibleAudioManager
import com.caccavo.chiesaudinerispostaweb.bible.BibleTranslation
import com.caccavo.chiesaudinerispostaweb.bible.BibleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(
    onClose: () -> Unit,
    viewModel: BibleViewModel = viewModel()
) {
    val context = LocalContext.current
    val audioManager = remember { BibleAudioManager.getInstance(context) }
    val readerPayload = viewModel.readerPayload

    if (readerPayload != null) {
        BibleReaderScreen(
            payload = readerPayload,
            allVerses = viewModel.allVerses(),
            onClose = { viewModel.closeReader() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bibbia") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Chiudi")
                    }
                }
            )
        }
    ) { padding ->
        when {
            viewModel.isBibleLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Caricamento Bibbia...")
                    }
                }
            }
            viewModel.bibleLoadError.isNotEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Non riesco a caricare la Bibbia", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(viewModel.bibleLoadError, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    if (audioManager.hasSavedReading) {
                        Button(
                            onClick = {
                                audioManager.resumeLastReading(viewModel.allVerses())
                                viewModel.openReader(
                                    title = audioManager.currentReadingTitle,
                                    verses = audioManager.currentReadingVerses,
                                    isSearchResult = false,
                                    followsAudio = true
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Text("Riprendi da ${audioManager.savedReadingDescription}")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    SearchField(
                        text = viewModel.searchText,
                        onTextChange = viewModel::updateSearchText,
                        onSearch = viewModel::performSearch
                    )

                    Spacer(Modifier.height(8.dp))

                    SimpleDropdownSelector(
                        label = { it.displayName },
                        options = BibleTranslation.entries,
                        selected = viewModel.selectedTranslation,
                        onSelected = viewModel::selectTranslation,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    SimpleDropdownSelector(
                        label = { it },
                        options = viewModel.bookNames,
                        selected = viewModel.selectedBook,
                        onSelected = viewModel::selectBook,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    ChapterVersePickerBar(
                        chapters = viewModel.chaptersForSelectedBook,
                        verseNumbers = viewModel.verseNumbersForSelectedChapter,
                        selectedChapter = viewModel.selectedChapter,
                        startVerse = viewModel.startVerse,
                        endVerse = viewModel.endVerse,
                        onChapterSelected = viewModel::selectChapter,
                        onChapterBack = viewModel::moveChapterBackward,
                        onChapterForward = viewModel::moveChapterForward,
                        onStartVerseSelected = viewModel::updateStartVerse,
                        onEndVerseSelected = viewModel::updateEndVerse,
                        onSearch = viewModel::performVerseSearch
                    )

                    Spacer(Modifier.height(12.dp))

                    if (viewModel.selectedTranslation.hasAudio) {
                        BibleAudioActionBar(
                            isPaused = audioManager.isPaused,
                            isSpeaking = audioManager.isSpeaking,
                            onSpeak = {
                                val title = if (viewModel.startVerse == viewModel.endVerse) {
                                    "${viewModel.selectedBook} ${viewModel.selectedChapter}:${viewModel.startVerse}"
                                } else {
                                    "${viewModel.selectedBook} ${viewModel.selectedChapter}:${viewModel.startVerse}-${viewModel.endVerse}"
                                }
                                viewModel.openReader(
                                    title = title,
                                    verses = viewModel.currentRangeVerses,
                                    isSearchResult = false,
                                    followsAudio = true
                                )
                                audioManager.speakContinuously(
                                    bookName = viewModel.selectedBook,
                                    chapter = viewModel.selectedChapter,
                                    startVerse = viewModel.startVerse,
                                    endVerse = viewModel.endVerse,
                                    allVerses = viewModel.allVerses()
                                )
                            },
                            onPauseResume = {
                                if (audioManager.isPaused) audioManager.resume() else audioManager.pause()
                            },
                            onStop = { audioManager.stop() }
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.VolumeOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Audio non ancora disponibile per questa traduzione.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Usa Cerca, scegli capitolo/versetti o premi Ascolta: il testo si apre sempre in una schermata separata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    text: String,
    onTextChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp)
        )
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Cerca una parola nella Bibbia...") },
            singleLine = true
        )
        TextButton(onClick = onSearch) {
            Text("Cerca")
        }
    }
}

@Composable
private fun ChapterVersePickerBar(
    chapters: List<Int>,
    verseNumbers: List<Int>,
    selectedChapter: Int,
    startVerse: Int,
    endVerse: Int,
    onChapterSelected: (Int) -> Unit,
    onChapterBack: () -> Unit,
    onChapterForward: () -> Unit,
    onStartVerseSelected: (Int) -> Unit,
    onEndVerseSelected: (Int) -> Unit,
    onSearch: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(onClick = onChapterBack) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Capitolo precedente")
        }

        Column {
            Text("Cap.", style = MaterialTheme.typography.labelSmall)
            SimpleDropdownSelector(
                label = { it.toString() },
                options = chapters,
                selected = selectedChapter,
                onSelected = onChapterSelected
            )
        }

        IconButton(onClick = onChapterForward) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Capitolo successivo")
        }

        Column {
            Text("Da", style = MaterialTheme.typography.labelSmall)
            SimpleDropdownSelector(
                label = { it.toString() },
                options = verseNumbers,
                selected = startVerse,
                onSelected = onStartVerseSelected
            )
        }

        Column {
            Text("A", style = MaterialTheme.typography.labelSmall)
            SimpleDropdownSelector(
                label = { it.toString() },
                options = verseNumbers,
                selected = endVerse,
                onSelected = onEndVerseSelected
            )
        }

        TextButton(onClick = onSearch) {
            Text("Cerca")
        }
    }
}

@Composable
private fun BibleAudioActionBar(
    isPaused: Boolean,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AudioActionButton(icon = Icons.Filled.PlayCircle, label = "Ascolta", onClick = onSpeak)
        AudioActionButton(
            icon = if (isPaused) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle,
            label = if (isPaused) "Riprendi" else "Pausa",
            onClick = onPauseResume
        )
        AudioActionButton(icon = Icons.Filled.StopCircle, label = "Stop", onClick = onStop)
    }
}

@Composable
private fun AudioActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, modifier = Modifier.padding(4.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
