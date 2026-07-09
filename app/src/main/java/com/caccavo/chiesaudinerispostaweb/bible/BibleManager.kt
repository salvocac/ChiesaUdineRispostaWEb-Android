package com.caccavo.chiesaudinerispostaweb.bible

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "BibleManager"

class BibleManager(private val context: Context) {

    private val gson = Gson()
    private val versesByTranslation = ConcurrentHashMap<BibleTranslation, List<BibleVerse>>()

    /** Versetti della Riveduta (traduzione di default), mantenuti per compatibilità
     * con il codice che legge `verses` direttamente. */
    var verses: List<BibleVerse> = emptyList()
        private set

    fun loadVerses(translation: BibleTranslation = BibleTranslation.RIVEDUTA): List<BibleVerse> {
        versesByTranslation[translation]?.let { return it }

        val fileName = "${translation.jsonResourceName}.json"
        val json = try {
            context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "❌ $fileName NON trovato", e)
            return emptyList()
        }

        Log.d(TAG, "✅ $fileName trovato")

        val bible = gson.fromJson(json, BibleData::class.java)
        val loaded = bible.verses.map { it.withTranslation(translation) }

        versesByTranslation[translation] = loaded
        if (translation == BibleTranslation.RIVEDUTA) {
            verses = loaded
        }

        Log.d(TAG, "✅ Versetti caricati (${translation.displayName}): ${loaded.size}")
        return loaded
    }

    suspend fun loadVersesAsync(translation: BibleTranslation = BibleTranslation.RIVEDUTA): List<BibleVerse> =
        withContext(Dispatchers.IO) { loadVerses(translation) }

    fun verseOfToday(): BibleVerse? {
        if (verses.isEmpty()) {
            Log.e(TAG, "❌ Nessun versetto caricato")
            return null
        }

        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = (dayOfYear * 37) % verses.size

        return verses[index]
    }

    companion object {
        @Volatile private var instance: BibleManager? = null

        fun getInstance(context: Context): BibleManager =
            instance ?: synchronized(this) {
                instance ?: BibleManager(context.applicationContext).also { instance = it }
            }
    }
}
