package com.caccavo.chiesaudinerispostaweb.dailyverse

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val TAG = "DailyVerseManager"

/** Data fissa di ancoraggio per il calcolo del versetto del giorno: scritta nel codice,
 * non nelle preferenze, così il versetto di un dato giorno è sempre lo stesso per tutti
 * gli utenti e non si azzera mai, nemmeno reinstallando l'app o pubblicando aggiornamenti. */
private val REFERENCE_START_DATE: LocalDate = LocalDate.of(2026, 7, 9)

class DailyVerseManager(private val context: Context) {

    private val gson = Gson()
    private var cachedVerses: List<DailyVerse>? = null

    fun loadVerses(): List<DailyVerse> {
        cachedVerses?.let { return it }

        val json = try {
            context.assets.open("daily_verses.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "❌ daily_verses.json non trovato", e)
            return emptyList()
        }

        val type = object : TypeToken<List<DailyVerse>>() {}.type
        val verses: List<DailyVerse> = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore JSON", e)
            return emptyList()
        }

        Log.d(TAG, "✅ DailyVerses caricati: ${verses.size}")
        cachedVerses = verses
        return verses
    }

    /** Data di inizio del programma di letture: fissa, mai più cambiata. Serve come limite
     * per non poter tornare indietro a prima che il programma iniziasse. */
    fun programStartDate(): LocalDate = REFERENCE_START_DATE

    fun verseOfToday(): DailyVerse? = verse(LocalDate.now())

    /** Il ciclo di versetti/commenti si ripete da capo una volta esaurito (elapsedDays % count),
     * quindi non serve mai aggiungerne altri: quando finiscono, si ricomincia automaticamente. */
    fun verse(date: LocalDate): DailyVerse? {
        val verses = loadVerses()
        if (verses.isEmpty()) {
            Log.e(TAG, "❌ NESSUN VERSETTO CARICATO")
            return null
        }

        val elapsedDays = ChronoUnit.DAYS.between(programStartDate(), date).coerceAtLeast(0)
        val index = (elapsedDays % verses.size).toInt()
        return verses[index]
    }

    companion object {
        @Volatile private var instance: DailyVerseManager? = null

        fun getInstance(context: Context): DailyVerseManager =
            instance ?: synchronized(this) {
                instance ?: DailyVerseManager(context.applicationContext).also { instance = it }
            }
    }
}
