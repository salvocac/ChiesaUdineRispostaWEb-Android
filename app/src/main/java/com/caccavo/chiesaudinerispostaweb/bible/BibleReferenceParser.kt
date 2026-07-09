package com.caccavo.chiesaudinerispostaweb.bible

data class BibleReference(
    val book: String,
    val chapter: Int?,
    val verse: Int?
)

object BibleReferenceParser {

    // Varianti italiane comuni, sigle CEI, numeri romani, "Prima/Seconda...".
    // Le chiavi sono già senza spazi/punti (rimossi prima del confronto più sotto).
    private val abbreviations: Map<String, String> = buildMap {
        // Pentateuco
        put("gen", "Genesi"); put("ge", "Genesi"); put("gn", "Genesi"); put("genesi", "Genesi")
        put("eso", "Esodo"); put("es", "Esodo"); put("esodo", "Esodo")
        put("lev", "Levitico"); put("lv", "Levitico"); put("levitico", "Levitico")
        put("num", "Numeri"); put("nm", "Numeri"); put("numeri", "Numeri")
        put("deu", "Deuteronomio"); put("dt", "Deuteronomio"); put("deut", "Deuteronomio"); put("deuteronomio", "Deuteronomio")
        // Storici
        put("gios", "Giosuè"); put("gs", "Giosuè"); put("giosue", "Giosuè")
        put("giu", "Giudici"); put("gdc", "Giudici"); put("giudici", "Giudici")
        put("rut", "Rut"); put("rt", "Rut")
        put("1sam", "1 Samuele"); put("1sa", "1 Samuele"); put("isam", "1 Samuele"); put("primasamuele", "1 Samuele")
        put("2sam", "2 Samuele"); put("2sa", "2 Samuele"); put("iisam", "2 Samuele"); put("secondasamuele", "2 Samuele")
        put("1re", "1 Re"); put("ire", "1 Re"); put("primare", "1 Re")
        put("2re", "2 Re"); put("iire", "2 Re"); put("secondare", "2 Re")
        put("1cr", "1 Cronache"); put("1cro", "1 Cronache"); put("icr", "1 Cronache"); put("primacronache", "1 Cronache")
        put("2cr", "2 Cronache"); put("2cro", "2 Cronache"); put("iicr", "2 Cronache"); put("secondacronache", "2 Cronache")
        put("esd", "Esdra"); put("esdra", "Esdra")
        put("neh", "Neemia"); put("nee", "Neemia"); put("neemia", "Neemia")
        put("est", "Ester"); put("ester", "Ester")
        // Sapienziali/poetici
        put("giob", "Giobbe"); put("gb", "Giobbe"); put("giobbe", "Giobbe")
        put("sal", "Salmi"); put("salmo", "Salmi"); put("salmi", "Salmi"); put("ps", "Salmi"); put("sl", "Salmi")
        put("prov", "Proverbi"); put("pr", "Proverbi"); put("proverbi", "Proverbi")
        put("ec", "Ecclesiaste"); put("eccl", "Ecclesiaste"); put("qo", "Ecclesiaste"); put("ecclesiaste", "Ecclesiaste")
        put("cant", "Cantico dei Cantici"); put("ct", "Cantico dei Cantici"); put("cantico", "Cantico dei Cantici")
        // Profeti maggiori/minori
        put("isa", "Isaia"); put("is", "Isaia"); put("isaia", "Isaia")
        put("ger", "Geremia"); put("geremia", "Geremia")
        put("lam", "Lamentazioni"); put("lamentazioni", "Lamentazioni")
        put("eze", "Ezechiele"); put("ez", "Ezechiele"); put("ezechiele", "Ezechiele")
        put("dan", "Daniele"); put("dn", "Daniele"); put("daniele", "Daniele")
        put("os", "Osea"); put("osea", "Osea")
        put("gioe", "Gioele"); put("gl", "Gioele"); put("gioele", "Gioele")
        put("amo", "Amos"); put("am", "Amos"); put("amos", "Amos")
        put("oba", "Abdia"); put("abd", "Abdia"); put("abdia", "Abdia")
        put("gio", "Giona"); put("giona", "Giona")
        put("mic", "Michea"); put("michea", "Michea")
        put("nah", "Naum"); put("na", "Naum"); put("naum", "Naum")
        put("ab", "Abacuc"); put("abc", "Abacuc"); put("abacuc", "Abacuc")
        put("sof", "Sofonia"); put("sofonia", "Sofonia")
        put("ag", "Aggeo"); put("agg", "Aggeo"); put("aggeo", "Aggeo")
        put("zac", "Zaccaria"); put("zc", "Zaccaria"); put("zaccaria", "Zaccaria")
        put("mal", "Malachia"); put("malachia", "Malachia")
        // Vangeli e Atti
        put("mt", "Matteo"); put("mat", "Matteo"); put("matteo", "Matteo")
        put("mc", "Marco"); put("mar", "Marco"); put("marco", "Marco")
        put("lc", "Luca"); put("luc", "Luca"); put("luca", "Luca")
        put("gv", "Giovanni"); put("giov", "Giovanni"); put("giovanni", "Giovanni")
        put("att", "Atti"); put("at", "Atti"); put("atti", "Atti")
        // Lettere paoline
        put("rm", "Romani"); put("rom", "Romani"); put("romani", "Romani")
        put("1cor", "1 Corinzi"); put("icor", "1 Corinzi"); put("primacorinzi", "1 Corinzi")
        put("2cor", "2 Corinzi"); put("iicor", "2 Corinzi"); put("secondacorinzi", "2 Corinzi")
        put("gal", "Galati"); put("galati", "Galati")
        put("ef", "Efesini"); put("efe", "Efesini"); put("efesini", "Efesini")
        put("fil", "Filippesi"); put("filippesi", "Filippesi")
        put("col", "Colossesi"); put("colossesi", "Colossesi")
        put("1tess", "1 Tessalonicesi"); put("1ts", "1 Tessalonicesi"); put("primatessalonicesi", "1 Tessalonicesi")
        put("2tess", "2 Tessalonicesi"); put("2ts", "2 Tessalonicesi"); put("secondatessalonicesi", "2 Tessalonicesi")
        put("1tim", "1 Timoteo"); put("itim", "1 Timoteo"); put("primatimoteo", "1 Timoteo")
        put("2tim", "2 Timoteo"); put("iitim", "2 Timoteo"); put("secondatimoteo", "2 Timoteo")
        put("tit", "Tito"); put("tito", "Tito")
        put("file", "Filemone"); put("flm", "Filemone"); put("filemone", "Filemone")
        // Ebrei e cattoliche
        put("ebr", "Ebrei"); put("eb", "Ebrei"); put("ebrei", "Ebrei")
        put("giac", "Giacomo"); put("gc", "Giacomo"); put("giacomo", "Giacomo")
        put("1pie", "1 Pietro"); put("1pt", "1 Pietro"); put("primapietro", "1 Pietro")
        put("2pie", "2 Pietro"); put("2pt", "2 Pietro"); put("secondapietro", "2 Pietro")
        put("1gv", "1 Giovanni"); put("1gio", "1 Giovanni"); put("igv", "1 Giovanni"); put("primagiovanni", "1 Giovanni")
        put("2gv", "2 Giovanni"); put("2gio", "2 Giovanni"); put("iigv", "2 Giovanni"); put("secondagiovanni", "2 Giovanni")
        put("3gv", "3 Giovanni"); put("3gio", "3 Giovanni"); put("iiigv", "3 Giovanni"); put("terzagiovanni", "3 Giovanni")
        put("giuda", "Giuda")
        // Apocalisse
        put("ap", "Apocalisse"); put("apoc", "Apocalisse"); put("apocalisse", "Apocalisse")
    }

    fun parse(input: String): BibleReference? {
        val text = input.trim()
        if (text.isEmpty()) return null

        val lower = text.lowercase()
            .replace(":", " ")
            .replace(",", " ")
            .replace(";", " ")

        val collapsed = lower.replace(Regex("\\s+"), " ")
        val parts = collapsed.split(" ").filter { it.isNotEmpty() }.toMutableList()
        if (parts.isEmpty()) return null

        var verse: Int? = null
        var chapter: Int? = null

        parts.lastOrNull()?.toIntOrNull()?.let { value ->
            chapter = value
            parts.removeAt(parts.size - 1)
        }

        if (chapter != null) {
            parts.lastOrNull()?.toIntOrNull()?.let { value ->
                verse = chapter
                chapter = value
                parts.removeAt(parts.size - 1)
            }
        }

        val rawBook = parts.joinToString(" ").trim()
        if (rawBook.isEmpty()) return null

        var normalizedBook = rawBook
        val exactKey = normalizedBook.replace(".", "").replace(" ", "")
        val mapped = abbreviations[exactKey]
        if (mapped != null) {
            normalizedBook = mapped
        } else {
            val firstWord = normalizedBook.split(" ").firstOrNull()
            val key = firstWord?.replace(".", "")?.lowercase()
            val mappedFirst = key?.let { abbreviations[it] }
            if (mappedFirst != null) {
                normalizedBook = mappedFirst
            }
        }

        if (!abbreviations.values.contains(normalizedBook)) {
            normalizedBook = normalizedBook.replaceFirstChar { it.uppercase() }
        }

        return BibleReference(book = normalizedBook, chapter = chapter, verse = verse)
    }
}
