package com.caccavo.chiesaudinerispostaweb.dailyverse

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate

class DailyVerseViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = DailyVerseManager.getInstance(application)

    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    val dailyVerse: DailyVerse?
        get() = manager.verse(selectedDate)

    val canGoToPreviousDay: Boolean
        get() = selectedDate > manager.programStartDate()

    val canGoToNextDay: Boolean
        get() = selectedDate < LocalDate.now()

    val programStartDate: LocalDate
        get() = manager.programStartDate()

    fun goToPreviousDay() {
        if (!canGoToPreviousDay) return
        selectedDate = selectedDate.minusDays(1)
    }

    fun goToNextDay() {
        if (!canGoToNextDay) return
        selectedDate = selectedDate.plusDays(1)
    }

    fun selectDate(date: LocalDate) {
        val clamped = when {
            date < manager.programStartDate() -> manager.programStartDate()
            date > LocalDate.now() -> LocalDate.now()
            else -> date
        }
        selectedDate = clamped
    }
}
