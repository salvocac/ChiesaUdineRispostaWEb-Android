package com.caccavo.chiesaudinerispostaweb.ui.bible

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caccavo.chiesaudinerispostaweb.bible.BibleReaderPayload
import com.caccavo.chiesaudinerispostaweb.bible.BibleVerse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    payload: BibleReaderPayload,
    allVerses: List<BibleVerse>,
    onClose: () -> Unit
) {
    var overrideVerses by remember { mutableStateOf<List<BibleVerse>?>(null) }
    var overrideTitle by remember { mutableStateOf<String?>(null) }

    val visibleVerses = overrideVerses ?: payload.verses
    val visibleTitle = overrideTitle ?: payload.title

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

        overrideVerses = newVerses
        overrideTitle = "${target.second} ${target.third}"
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

                LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    items(visibleVerses, key = { it.id }) { verse ->
                        BibleVerseRow(verse = verse, isSearchResult = payload.isSearchResult)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BibleVerseRow(verse: BibleVerse, isSearchResult: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
