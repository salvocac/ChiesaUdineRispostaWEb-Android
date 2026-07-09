package com.caccavo.chiesaudinerispostaweb.bible

import com.google.gson.annotations.SerializedName

data class BibleData(
    @SerializedName("verses") val verses: List<BibleVerse>
)

data class BibleVerse(
    @SerializedName("book") val book: Int,
    @SerializedName("book_name") val bookName: String,
    @SerializedName("chapter") val chapter: Int,
    @SerializedName("verse") val verse: Int,
    @SerializedName("text") val text: String,
    @Transient val translation: BibleTranslation = BibleTranslation.RIVEDUTA
) {
    val id: String
        get() = "$bookName${translation.audioIdSuffix}-$chapter-$verse"

    fun withTranslation(translation: BibleTranslation): BibleVerse = copy(translation = translation)
}
