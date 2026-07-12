package com.caccavo.chiesaudinerispostaweb.bible

enum class BibleTranslation {
    RIVEDUTA,
    LLC;

    val displayName: String
        get() = when (this) {
            RIVEDUTA -> "Luzzi"
            LLC -> "Luzzi, Linguaggio Corrente"
        }

    val jsonResourceName: String
        get() = when (this) {
            RIVEDUTA -> "riveduta"
            LLC -> "bibbia-llc-app"
        }

    /** Aggiunto al nome del libro per costruire l'audioId (es. "Genesi-LLC-1-1"),
     * così i due id non collidono mai con quelli della Riveduta già registrata. */
    val audioIdSuffix: String
        get() = when (this) {
            RIVEDUTA -> ""
            LLC -> "-LLC"
        }

    /** Se una traduzione non ha audio registrato, l'app nasconde i controlli di
     * ascolto/invio audio/invio video invece di fallire in silenzio. */
    val hasAudio: Boolean
        get() = when (this) {
            RIVEDUTA -> true
            LLC -> true
        }
}
