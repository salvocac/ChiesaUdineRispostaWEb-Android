package com.caccavo.chiesaudinerispostaweb.bible

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.Normalizer

class BibleViewModel(application: Application) : AndroidViewModel(application) {

    private val bibleManager = BibleManager.getInstance(application)
    private val prefs = application.getSharedPreferences("bible_preferences", Context.MODE_PRIVATE)

    var selectedTranslation by mutableStateOf(
        try {
            val saved = prefs.getString("selectedBibleTranslation", null)
            if (saved != null) BibleTranslation.valueOf(saved) else BibleTranslation.RIVEDUTA
        } catch (e: Exception) {
            BibleTranslation.RIVEDUTA
        }
    )
        private set
    var selectedBook by mutableStateOf("Genesi")
        private set
    var selectedChapter by mutableStateOf(1)
        private set
    var startVerse by mutableStateOf(1)
        private set
    var endVerse by mutableStateOf(1)
        private set
    var searchText by mutableStateOf("")
        private set

    var bookNames by mutableStateOf(listOf<String>())
        private set
    var chaptersForSelectedBook by mutableStateOf(listOf<Int>())
        private set
    var verseNumbersForSelectedChapter by mutableStateOf(listOf<Int>())
        private set
    var currentRangeVerses by mutableStateOf(listOf<BibleVerse>())
        private set
    var searchResultsCache by mutableStateOf(listOf<BibleVerse>())
        private set
    var searchOccurrenceCount by mutableStateOf(0)
        private set

    var isBibleLoading by mutableStateOf(true)
        private set
    var bibleLoadError by mutableStateOf("")
        private set

    var readerPayload by mutableStateOf<BibleReaderPayload?>(null)
        private set

    private var verses: List<BibleVerse> = emptyList()
    private var versesByBook: Map<String, List<BibleVerse>> = emptyMap()
    private var currentChapterVerses: List<BibleVerse> = emptyList()
    private var didStartBibleLoad = false

    init {
        loadBible()
    }

    val displayedVerses: List<BibleVerse>
        get() = if (searchText.isBlank()) currentRangeVerses else searchResultsCache

    private fun loadBible() {
        if (didStartBibleLoad) return
        didStartBibleLoad = true
        isBibleLoading = true
        bibleLoadError = ""

        val translationToLoad = selectedTranslation
        viewModelScope.launch {
            try {
                val loadedVerses = bibleManager.loadVersesAsync(translationToLoad)
                val groupedByBook = loadedVerses.groupBy { it.bookName }
                val orderedBooks = loadedVerses.groupBy { it.book }
                    .toSortedMap()
                    .mapNotNull { it.value.firstOrNull()?.bookName }

                verses = loadedVerses
                versesByBook = groupedByBook
                bookNames = orderedBooks

                if (!bookNames.contains(selectedBook)) {
                    bookNames.firstOrNull()?.let { selectedBook = it }
                }

                refreshSelectionData()
                verseNumbersForSelectedChapter.firstOrNull()?.let { startVerse = it }
                verseNumbersForSelectedChapter.lastOrNull()?.let { endVerse = it }
                refreshRangeData()

                isBibleLoading = false
            } catch (e: Exception) {
                bibleLoadError = e.message ?: "Errore sconosciuto"
                isBibleLoading = false
            }
        }
    }

    private fun refreshSelectionData(resetChapter: Boolean = true) {
        val bookVerses = versesByBook[selectedBook] ?: emptyList()
        val chapters = bookVerses.map { it.chapter }.toSortedSet().toList()
        chaptersForSelectedBook = chapters

        if (resetChapter || !chapters.contains(selectedChapter)) {
            selectedChapter = chapters.firstOrNull() ?: 1
        }

        currentChapterVerses = bookVerses
            .filter { it.chapter == selectedChapter }
            .sortedBy { it.verse }

        verseNumbersForSelectedChapter = currentChapterVerses.map { it.verse }
    }

    private fun refreshRangeData() {
        val lower = minOf(startVerse, endVerse)
        val upper = maxOf(startVerse, endVerse)

        currentRangeVerses = currentChapterVerses.filter { it.verse in lower..upper }

        if (searchText.isBlank()) {
            searchResultsCache = emptyList()
        }
    }

    fun selectTranslation(translation: BibleTranslation) {
        if (translation == selectedTranslation) return
        selectedTranslation = translation
        prefs.edit().putString("selectedBibleTranslation", translation.name).apply()
        searchText = ""
        searchResultsCache = emptyList()
        didStartBibleLoad = false
        loadBible()
    }

    fun selectBook(book: String) {
        if (book == selectedBook) return
        selectedBook = book
        refreshSelectionData()
        verseNumbersForSelectedChapter.firstOrNull()?.let { startVerse = it }
        verseNumbersForSelectedChapter.lastOrNull()?.let { endVerse = it }
        refreshRangeData()
    }

    fun selectChapter(chapter: Int) {
        if (chapter == selectedChapter) return
        selectedChapter = chapter
        refreshSelectionData(resetChapter = false)
        verseNumbersForSelectedChapter.firstOrNull()?.let { startVerse = it }
        verseNumbersForSelectedChapter.lastOrNull()?.let { endVerse = it }
        refreshRangeData()
    }

    fun moveChapterBackward() {
        val chapters = chaptersForSelectedBook
        val currentIndex = chapters.indexOf(selectedChapter)
        if (currentIndex <= 0) return
        selectChapter(chapters[currentIndex - 1])
    }

    fun moveChapterForward() {
        val chapters = chaptersForSelectedBook
        val currentIndex = chapters.indexOf(selectedChapter)
        if (currentIndex < 0 || currentIndex + 1 >= chapters.size) return
        selectChapter(chapters[currentIndex + 1])
    }

    fun updateStartVerse(verse: Int) {
        startVerse = verse
        refreshRangeData()
    }

    fun updateEndVerse(verse: Int) {
        endVerse = verse
        refreshRangeData()
    }

    fun updateSearchText(text: String) {
        searchText = text
        if (text.isBlank()) {
            searchResultsCache = emptyList()
        }
    }

    fun performSearch() {
        val query = searchText.trim()
        if (query.isEmpty()) return

        val ref = BibleReferenceParser.parse(query)
        val resolvedBook = ref?.let { resolveBookName(it.book) }

        if (ref != null && resolvedBook != null && applyReferenceSelection(resolvedBook, ref.chapter, ref.verse)) {
            // Libro/capitolo/versetto restano selezionati nella pagina principale, e si apre
            // subito la lettura: altrimenti l'utente non vede nessun risultato visibile e
            // pensa che la ricerca per riferimento non funzioni.
            val title = if (startVerse == endVerse) {
                "$selectedBook $selectedChapter:$startVerse"
            } else {
                "$selectedBook $selectedChapter:$startVerse-$endVerse"
            }
            openReader(title = title, verses = currentRangeVerses, isSearchResult = false)
        } else {
            runTextSearch(query)
            openReader(
                title = "Risultati ricerca",
                verses = searchResultsCache,
                isSearchResult = true,
                searchedWord = query,
                occurrenceCount = searchOccurrenceCount
            )
        }
    }

    fun performVerseSearch() {
        if (endVerse < startVerse) {
            val tmp = startVerse
            startVerse = endVerse
            endVerse = tmp
        }
        if (searchText.isNotEmpty()) {
            searchText = ""
            searchResultsCache = emptyList()
        }
        refreshRangeData()
        val title = if (startVerse == endVerse) {
            "$selectedBook $selectedChapter:$startVerse"
        } else {
            "$selectedBook $selectedChapter:$startVerse-$endVerse"
        }
        openReader(title = title, verses = currentRangeVerses, isSearchResult = false)
    }

    private fun runTextSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            searchResultsCache = emptyList()
            searchOccurrenceCount = 0
            return
        }

        searchResultsCache = verses.filter {
            it.text.contains(trimmed, ignoreCase = true) ||
                it.bookName.contains(trimmed, ignoreCase = true) ||
                "${it.bookName} ${it.chapter}:${it.verse}".contains(trimmed, ignoreCase = true)
        }
        searchOccurrenceCount = searchResultsCache.sumOf { countOccurrences(trimmed, it.text) }
    }

    private fun countOccurrences(needle: String, haystack: String): Int {
        if (needle.isEmpty()) return 0
        var count = 0
        var index = 0
        while (true) {
            val found = haystack.indexOf(needle, index, ignoreCase = true)
            if (found < 0) break
            count++
            index = found + needle.length
        }
        return count
    }

    private fun applyReferenceSelection(book: String, chapter: Int?, verse: Int?): Boolean {
        val bookVerses = versesByBook[book] ?: emptyList()
        val availableChapters = bookVerses.map { it.chapter }.toSortedSet().toList()
        if (availableChapters.isEmpty()) return false

        val resolvedChapter = when {
            chapter != null && availableChapters.contains(chapter) -> chapter
            chapter == null -> availableChapters.first()
            else -> return false
        }

        val chapterVerses = bookVerses.filter { it.chapter == resolvedChapter }.sortedBy { it.verse }
        val availableVerses = chapterVerses.map { it.verse }
        val firstVerse = availableVerses.firstOrNull() ?: return false
        val lastVerse = availableVerses.lastOrNull() ?: return false

        if (verse != null && !availableVerses.contains(verse)) return false

        selectedBook = book
        selectedChapter = resolvedChapter
        chaptersForSelectedBook = availableChapters
        currentChapterVerses = chapterVerses
        verseNumbersForSelectedChapter = availableVerses

        val rangeStart = verse ?: firstVerse
        val rangeEnd = verse ?: lastVerse
        startVerse = rangeStart
        endVerse = rangeEnd
        currentRangeVerses = chapterVerses.filter { it.verse in rangeStart..rangeEnd }
        searchText = ""
        searchResultsCache = emptyList()

        return true
    }

    private fun resolveBookName(parsedBook: String): String? {
        val wanted = normalizedSearchKey(parsedBook)
        if (wanted.isEmpty()) return null

        bookNames.firstOrNull { normalizedSearchKey(it) == wanted }?.let { return it }

        bookNames.firstOrNull {
            normalizedSearchKey(it).replace(" ", "") == wanted.replace(" ", "")
        }?.let { return it }

        return bookNames.firstOrNull { normalizedSearchKey(it).startsWith(wanted) }
    }

    private fun normalizedSearchKey(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return normalized
            .lowercase()
            .replace(".", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun openReader(
        title: String,
        verses: List<BibleVerse>,
        isSearchResult: Boolean,
        followsAudio: Boolean = false,
        searchedWord: String? = null,
        occurrenceCount: Int? = null
    ) {
        readerPayload = BibleReaderPayload(
            title = title,
            verses = verses,
            isSearchResult = isSearchResult,
            followsAudio = followsAudio,
            searchedWord = searchedWord,
            occurrenceCount = occurrenceCount,
            audioAvailable = selectedTranslation.hasAudio
        )
    }

    fun closeReader() {
        readerPayload = null
    }

    fun allVerses(): List<BibleVerse> = verses
}
