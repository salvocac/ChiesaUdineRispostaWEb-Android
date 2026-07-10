package com.caccavo.chiesaudinerispostaweb.audio

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.caccavo.chiesaudinerispostaweb.bible.BibleVerse
import com.caccavo.chiesaudinerispostaweb.dailyverse.DailyVerse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "BibleAudioManager"
private const val AUDIO_BASE_URL = "https://pub-44310709cc6e4e4cb1a8d3f21dcac883.r2.dev"
private const val PREFS_NAME = "bible_audio_history"
private const val KEY_AUDIO_ID = "lastBibleAudioID"
private const val KEY_ELAPSED = "lastBibleAudioElapsed"

private data class ChapterPlayback(
    val bookName: String,
    val chapter: Int,
    val verses: List<BibleVerse>,
    val announcedStartVerse: Int,
    val announcedEndVerse: Int
)

/**
 * Riproduzione narrata dei versetti, semplificata rispetto alla versione iOS: qui non c'è
 * lettura "a campione" (si legge tutto), gli annunci titolo/intervallo vengono accodati come
 * clip separate invece che unite in un unico file, e non c'è ancora un MediaSession per i
 * controlli dalla schermata di blocco: la riproduzione continua finché l'app resta in
 * esecuzione. Il resto (audio remoti dallo stesso bucket, cache locale, ripresa dell'ultima
 * lettura) è portato dalla versione iOS.
 */
class BibleAudioManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cacheDir: File by lazy {
        File(appContext.cacheDir, "BibleAudio").apply { mkdirs() }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var isSpeaking by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var playbackProgress by mutableStateOf(0f)
        private set
    var currentReadingTitle by mutableStateOf("")
        private set
    var currentReadingVerses by mutableStateOf(listOf<BibleVerse>())
        private set
    var currentVerseId by mutableStateOf<String?>(null)
        private set
    var lastError by mutableStateOf("")
        private set
    var hasSavedReading by mutableStateOf(false)
        private set
    var savedReadingDescription by mutableStateOf("")
        private set

    private var player: ExoPlayer? = null
    private var progressJob: Job? = null
    private var playbackToken = 0

    /** Verso associato a ogni voce della playlist attuale; null per le clip di annuncio
     * (titolo capitolo, "versetti da X a Y") che non corrispondono a un versetto vero. */
    private var playlistVerses: List<BibleVerse?> = emptyList()

    private var chapters: List<ChapterPlayback> = emptyList()
    private var chapterIndex = 0
    private var shouldAdvanceChapters = false

    init {
        refreshSavedReading()
    }

    fun prepare() {
        if (player == null) {
            player = ExoPlayer.Builder(appContext).build().also { exo ->
                exo.addListener(playerListener)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentVerseForCurrentItem()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                onQueueFinished()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Errore player", error)
            lastError = "Errore durante la riproduzione audio."
        }
    }

    fun speakContinuously(
        bookName: String,
        chapter: Int,
        startVerse: Int,
        endVerse: Int,
        allVerses: List<BibleVerse>
    ) {
        stopInternal(clearDisplay = false)
        lastError = ""

        val chapterVerses = allVerses
            .filter { it.bookName == bookName && it.chapter == chapter }
            .sortedBy { it.verse }
        val selectedVerses = chapterVerses.filter { it.verse in startVerse..endVerse }

        val firstVerse = chapterVerses.firstOrNull()?.verse
        val lastVerse = chapterVerses.lastOrNull()?.verse
        if (firstVerse == null || lastVerse == null || selectedVerses.isEmpty()) {
            lastError = "Nessun versetto da leggere."
            return
        }

        val wholeChapter = startVerse <= firstVerse && endVerse >= lastVerse
        if (wholeChapter) {
            chapters = continuousChapters(bookName, chapter, startVerse, allVerses)
            shouldAdvanceChapters = true
        } else {
            chapters = listOf(
                ChapterPlayback(
                    bookName = bookName,
                    chapter = chapter,
                    verses = selectedVerses,
                    announcedStartVerse = selectedVerses.first().verse,
                    announcedEndVerse = selectedVerses.last().verse
                )
            )
            shouldAdvanceChapters = false
        }

        chapterIndex = 0
        playCurrentChapter()
    }

    fun speak(dailyVerse: DailyVerse) {
        stopInternal(clearDisplay = false)
        lastError = ""
        val token = playbackToken

        val candidateIDs = buildList {
            dailyVerse.titleAudioID?.let { add(it) }
            addAll(dailyVerse.audioIDs.orEmpty())
            dailyVerse.commentAudioID?.let { add(it) }
        }

        scope.launch {
            ensureCached(candidateIDs)
            if (token != playbackToken) return@launch

            val audioIds = buildList {
                dailyVerse.titleAudioID?.let { if (recordedAudioFile(it) != null) add(it) }
                addAll(dailyVerse.audioIDs.orEmpty())
                dailyVerse.commentAudioID?.let { if (recordedAudioFile(it) != null) add(it) }
            }

            if (audioIds.isEmpty()) {
                lastError = "Audio del versetto del giorno non disponibile."
                return@launch
            }

            chapters = emptyList()
            shouldAdvanceChapters = false
            currentReadingTitle = dailyVerse.reference ?: "Versetto del giorno"
            currentReadingVerses = emptyList()

            playAudioIds(audioIds, verses = audioIds.map { null }, onComplete = { finishPlayback() })
        }
    }

    fun resumeLastReading(allVerses: List<BibleVerse>) {
        val audioId = prefs.getString(KEY_AUDIO_ID, null)
        val savedVerse = audioId?.let { id -> allVerses.firstOrNull { it.id == id } }
        if (savedVerse == null) {
            lastError = "Nessuna lettura precedente disponibile."
            refreshSavedReading()
            return
        }

        val pendingSeekSeconds = prefs.getFloat(KEY_ELAPSED, 0f).coerceAtLeast(0f)

        stopInternal(clearDisplay = false)
        var built = continuousChapters(savedVerse.bookName, savedVerse.chapter, savedVerse.verse, allVerses)
        if (built.isEmpty()) {
            lastError = "Impossibile riprendere la lettura salvata."
            return
        }
        val first = built.first()
        built = listOf(
            first.copy(
                verses = first.verses.filter { it.verse >= savedVerse.verse },
                announcedStartVerse = savedVerse.verse
            )
        ) + built.drop(1)

        chapters = built
        shouldAdvanceChapters = true
        chapterIndex = 0
        playCurrentChapter(initialSeekSeconds = pendingSeekSeconds)
    }

    fun pause() {
        val exo = player ?: return
        if (!isSpeaking) return
        exo.pause()
        isPaused = true
        saveCurrentPosition()
    }

    fun resume() {
        val exo = player ?: return
        if (!isPaused) return
        exo.play()
        isSpeaking = true
        isPaused = false
    }

    fun stop() {
        stopInternal(clearDisplay = true)
    }

    private fun stopInternal(clearDisplay: Boolean) {
        playbackToken++
        saveCurrentPosition()
        progressJob?.cancel()
        progressJob = null
        player?.stop()
        player?.clearMediaItems()
        chapters = emptyList()
        chapterIndex = 0
        playlistVerses = emptyList()
        shouldAdvanceChapters = false
        isSpeaking = false
        isPaused = false
        playbackProgress = 0f
        if (clearDisplay) {
            currentReadingTitle = ""
            currentReadingVerses = emptyList()
            currentVerseId = null
        }
    }

    private fun playCurrentChapter(initialSeekSeconds: Float = 0f) {
        val chapter = chapters.getOrNull(chapterIndex) ?: run {
            finishPlayback()
            return
        }

        val verseAudioIds = chapter.verses.map { it.id }
        val titleAudioId = chapterTitleAudioId(
            chapter.bookName,
            chapter.chapter,
            chapter.verses.firstOrNull()?.translation?.audioIdSuffix.orEmpty()
        )
        val rangeAudioIds = verseRangeAnnouncementAudioIds(chapter.announcedStartVerse, chapter.announcedEndVerse)
        val token = playbackToken

        scope.launch {
            ensureCached(verseAudioIds + titleAudioId + rangeAudioIds)
            if (token != playbackToken) return@launch

            if (!verseAudioIds.all { recordedAudioFile(it) != null }) {
                lastError = "Audio mancante per ${chapter.bookName} ${chapter.chapter}."
                finishPlayback()
                return@launch
            }

            currentReadingTitle = "${chapter.bookName} ${chapter.chapter}"
            currentReadingVerses = chapter.verses

            val announcementIds = buildList {
                if (recordedAudioFile(titleAudioId) != null) add(titleAudioId)
                if (rangeAudioIds.isNotEmpty() && rangeAudioIds.all { recordedAudioFile(it) != null }) {
                    addAll(rangeAudioIds)
                }
            }

            val allIds = announcementIds + verseAudioIds
            val allVersesForIds: List<BibleVerse?> = announcementIds.map { null as BibleVerse? } + chapter.verses
            playAudioIds(allIds, verses = allVersesForIds, onComplete = { onChapterFinished() }, initialSeekSeconds = initialSeekSeconds)
        }
    }

    private fun onChapterFinished() {
        if (shouldAdvanceChapters) {
            chapterIndex++
            playCurrentChapter()
        } else {
            finishPlayback()
        }
    }

    private fun playAudioIds(
        audioIds: List<String>,
        verses: List<BibleVerse?>,
        onComplete: () -> Unit,
        initialSeekSeconds: Float = 0f
    ) {
        val files = audioIds.map { recordedAudioFile(it) }
        if (files.any { it == null } || files.isEmpty()) {
            lastError = "Uno o più file audio non sono disponibili."
            return
        }

        prepare()
        val exo = player ?: return
        exo.clearMediaItems()
        playlistVerses = verses
        pendingCompletion = onComplete

        files.filterNotNull().forEach { file ->
            exo.addMediaItem(MediaItem.fromUri(file.toURI().toString()))
        }
        exo.prepare()
        if (initialSeekSeconds > 0f) {
            exo.seekTo(0, (initialSeekSeconds * 1000).toLong())
        }
        exo.playWhenReady = true
        isSpeaking = true
        isPaused = false
        playbackProgress = 0f
        updateCurrentVerseForCurrentItem()
        startProgressUpdates()
    }

    private var pendingCompletion: (() -> Unit)? = null

    private fun onQueueFinished() {
        val completion = pendingCompletion
        pendingCompletion = null
        progressJob?.cancel()
        completion?.invoke()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                updateProgress()
                delay(250)
            }
        }
    }

    private fun updateProgress() {
        val exo = player ?: return
        val itemCount = exo.mediaItemCount.coerceAtLeast(1)
        val duration = exo.duration
        val itemProgress = if (duration > 0) {
            (exo.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f
        playbackProgress = ((exo.currentMediaItemIndex + itemProgress) / itemCount).coerceIn(0f, 1f)
        saveCurrentPosition()
    }

    private fun updateCurrentVerseForCurrentItem() {
        val exo = player ?: return
        val index = exo.currentMediaItemIndex
        val verse = playlistVerses.getOrNull(index) ?: return
        currentVerseId = verse.id
        prefs.edit()
            .putString(KEY_AUDIO_ID, verse.id)
            .putFloat(KEY_ELAPSED, 0f)
            .apply()
        hasSavedReading = true
        savedReadingDescription = "${verse.bookName} ${verse.chapter}:${verse.verse}"
    }

    private fun saveCurrentPosition() {
        val exo = player ?: return
        val index = exo.currentMediaItemIndex
        val verse = playlistVerses.getOrNull(index) ?: return
        prefs.edit()
            .putString(KEY_AUDIO_ID, verse.id)
            .putFloat(KEY_ELAPSED, exo.currentPosition / 1000f)
            .apply()
    }

    private fun finishPlayback() {
        progressJob?.cancel()
        progressJob = null
        player?.stop()
        player?.clearMediaItems()
        isSpeaking = false
        isPaused = false
        playbackProgress = 1f
        pendingCompletion = null
    }

    private fun refreshSavedReading() {
        val audioId = prefs.getString(KEY_AUDIO_ID, null)
        if (audioId == null) {
            hasSavedReading = false
            savedReadingDescription = ""
            return
        }
        val parts = audioId.split("-")
        if (parts.size < 3) {
            hasSavedReading = true
            savedReadingDescription = audioId
            return
        }
        val verse = parts.last()
        val chapter = parts[parts.size - 2]
        val book = parts.dropLast(2).joinToString("-")
        hasSavedReading = true
        savedReadingDescription = "$book $chapter:$verse"
    }

    /** Scarica (se serve) e restituisce i file locali di questi audio, nello stesso ordine,
     * saltando quelli non disponibili: usato dalla generazione video per assemblare la
     * narrazione (titolo capitolo + versetti) in un unico file da mettere sotto il video. */
    suspend fun prepareAudioFiles(audioIds: List<String>): List<File> {
        ensureCached(audioIds)
        return audioIds.mapNotNull { recordedAudioFile(it) }
    }

    fun chapterTitleId(bookName: String, chapter: Int, audioIdSuffix: String = ""): String =
        chapterTitleAudioId(bookName, chapter, audioIdSuffix)

    private fun chapterTitleAudioId(bookName: String, chapter: Int, audioIdSuffix: String = ""): String =
        "$bookName$audioIdSuffix-$chapter-titolo"

    private fun verseRangeAnnouncementAudioIds(startVerse: Int, endVerse: Int): List<String> {
        if (startVerse !in 1..176 || endVerse !in 1..176) return emptyList()
        return if (startVerse == endVerse) {
            listOf("parola-versetto", "numero-$startVerse")
        } else {
            listOf("parola-versetti", "parola-da", "numero-$startVerse", "parola-a", "numero-$endVerse")
        }
    }

    private fun recordedAudioFile(audioId: String): File? {
        val file = File(cacheDir, "$audioId.mp3")
        return if (file.exists()) file else null
    }

    private suspend fun ensureCached(audioIds: List<String>) = withContext(Dispatchers.IO) {
        for (audioId in audioIds.distinct()) {
            val destination = File(cacheDir, "$audioId.mp3")
            if (destination.exists()) continue

            try {
                val connection = URL("$AUDIO_BASE_URL/$audioId.mp3").openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.connect()
                if (connection.responseCode != 200) {
                    connection.disconnect()
                    continue
                }
                val tempFile = File(cacheDir, "$audioId.mp3.tmp")
                connection.inputStream.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                connection.disconnect()
                tempFile.renameTo(destination)
            } catch (e: Exception) {
                Log.w(TAG, "Audio non disponibile: $audioId", e)
            }
        }
    }

    private fun continuousChapters(
        bookName: String,
        chapter: Int,
        startVerse: Int,
        allVerses: List<BibleVerse>
    ): List<ChapterPlayback> {
        val startBookOrder = allVerses.firstOrNull { it.bookName == bookName }?.book ?: Int.MAX_VALUE

        return allVerses
            .groupBy { "${it.book}|${it.chapter}" }
            .values
            .mapNotNull { verses ->
                val first = verses.firstOrNull() ?: return@mapNotNull null
                Quad(first.book, first.bookName, first.chapter, verses.sortedBy { it.verse })
            }
            .filter { it.book > startBookOrder || (it.bookName == bookName && it.chapter >= chapter) }
            .sortedWith(compareBy({ it.book }, { it.chapter }))
            .mapNotNull { item ->
                val verses = if (item.bookName == bookName && item.chapter == chapter) {
                    item.verses.filter { it.verse >= startVerse }
                } else {
                    item.verses
                }
                if (verses.isEmpty()) return@mapNotNull null
                ChapterPlayback(
                    bookName = item.bookName,
                    chapter = item.chapter,
                    verses = verses,
                    announcedStartVerse = verses.first().verse,
                    announcedEndVerse = verses.last().verse
                )
            }
    }

    private data class Quad(val book: Int, val bookName: String, val chapter: Int, val verses: List<BibleVerse>)

    companion object {
        @Volatile private var instance: BibleAudioManager? = null

        fun getInstance(context: Context): BibleAudioManager =
            instance ?: synchronized(this) {
                instance ?: BibleAudioManager(context.applicationContext).also { instance = it }
            }
    }
}
