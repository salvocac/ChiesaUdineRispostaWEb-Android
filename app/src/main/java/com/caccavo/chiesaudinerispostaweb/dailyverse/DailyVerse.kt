package com.caccavo.chiesaudinerispostaweb.dailyverse

import com.google.gson.annotations.SerializedName

data class DailyVerse(
    @SerializedName("day") val day: Int?,
    @SerializedName("year") val year: Int?,
    @SerializedName("reference") val reference: String?,
    @SerializedName("verseText") val verseText: String?,
    @SerializedName("reflection") val reflection: String?,
    @SerializedName("theme") val theme: String?,
    @SerializedName("audioIDs") val audioIDs: List<String>?,
    @SerializedName("commentAudioID") val commentAudioID: String?
) {
    /** ID atteso per l'audio del titolo (es. "giorno-0001-titolo"), da registrare in futuro
     * con lo stesso strumento usato per gli altri audio. */
    val titleAudioID: String?
        get() = day?.let { "giorno-%04d-titolo".format(it) }
}
