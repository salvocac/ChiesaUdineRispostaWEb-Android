package com.caccavo.chiesaudinerispostaweb.bible

data class BibleReaderPayload(
    val title: String,
    val verses: List<BibleVerse>,
    val isSearchResult: Boolean,
    val followsAudio: Boolean = false,
    val searchedWord: String? = null,
    val occurrenceCount: Int? = null,
    val audioAvailable: Boolean
)
