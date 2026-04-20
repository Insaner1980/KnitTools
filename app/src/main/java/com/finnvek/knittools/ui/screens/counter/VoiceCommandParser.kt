package com.finnvek.knittools.ui.screens.counter

internal object VoiceCommandParser {
    fun parse(text: String): VoiceCommand? {
        val normalized = normalize(text)
        if (normalized.isEmpty()) return null

        when {
            normalized in STOP_WORDS -> return VoiceCommand.StopListening
            normalized in HELP_WORDS -> return VoiceCommand.Help
            normalized in STITCH_INCREMENT_WORDS -> return VoiceCommand.StitchIncrement
            normalized in STITCH_DECREMENT_WORDS -> return VoiceCommand.StitchDecrement
            normalized in UNDO_WORDS -> return VoiceCommand.Undo
            normalized in RESET_WORDS -> return VoiceCommand.Reset
        }

        parseCountedCommand(normalized)?.let { return it }

        return when {
            normalized in INCREMENT_WORDS -> VoiceCommand.Increment()
            normalized in DECREMENT_WORDS -> VoiceCommand.Decrement()
            else -> null
        }
    }

    private fun parseCountedCommand(text: String): VoiceCommand? {
        val words = text.split("\\s+".toRegex())
        val firstWord = words.firstOrNull() ?: return null
        val isIncrement = firstWord in INCREMENT_WORDS
        val isDecrement = firstWord in DECREMENT_WORDS
        if (!isIncrement && !isDecrement) return null

        val count = words.drop(1).firstNotNullOfOrNull(::parseNumber) ?: return null
        return if (isIncrement) VoiceCommand.Increment(count) else VoiceCommand.Decrement(count)
    }

    private fun parseNumber(word: String): Int? {
        word.toIntOrNull()?.let { if (it in 1..100) return it }
        return WORD_TO_NUMBER[word]
    }

    private fun normalize(text: String): String =
        text
            .lowercase()
            .trim()
            .replace(Regex("[^\\p{L}\\d\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private val STOP_WORDS =
        setOf(
            "stop",
            "stop listening",
            "stop voice",
            "lopeta",
            "lopeta kuuntelu",
            "pysähdy",
            "stopp",
            "stoppa",
            "sluta",
            "sluta lyssna",
            "anhalten",
            "stoppen",
            "hör auf",
            "hoer auf",
            "beenden",
            "arrête",
            "arrete",
            "arrêter",
            "arreter",
            "stopper",
            "parar",
            "deten",
            "detente",
            "detente ya",
            "deja de escuchar",
            "pare",
            "terminar",
            "ferma",
            "fermarsi",
            "hou op",
            "slutt",
            "slutt å lytte",
            "slutt a lytte",
            "stop med at lytte",
        )

    private val HELP_WORDS =
        setOf(
            "help",
            "commands",
            "what can i say",
            "apua",
            "komennot",
            "ohje",
            "hjälp",
            "hjalp",
            "kommandon",
            "hilfe",
            "befehle",
            "aide",
            "commandes",
            "ayuda",
            "comandos",
            "ajuda",
            "aiuto",
            "help me",
            "opdrachten",
            "kommandoer",
            "kommandoar",
        )

    private val STITCH_INCREMENT_WORDS =
        setOf(
            "stitch",
            "next stitch",
            "count stitch",
            "mark stitch",
            "silmukka",
            "seuraava silmukka",
            "maska",
            "nästa maska",
            "naesta maska",
            "masche",
            "nächste masche",
            "naechste masche",
            "maille",
            "maille suivante",
            "punto",
            "siguiente punto",
            "próximo ponto",
            "proximo ponto",
            "ponto seguinte",
            "ponto",
            "neste maske",
            "næste maske",
            "naeste maske",
            "volgende steek",
            "steek",
            "maglia",
            "maglia successiva",
        )

    private val STITCH_DECREMENT_WORDS =
        setOf(
            "back stitch",
            "previous stitch",
            "undo stitch",
            "edellinen silmukka",
            "takaisin silmukka",
            "föregående maska",
            "foregaende maska",
            "tidigare maska",
            "vorige masche",
            "zurück masche",
            "zurueck masche",
            "maille précédente",
            "maille precedente",
            "punto anterior",
            "retroceder punto",
            "ponto anterior",
            "forrige maske",
            "forrige maska",
            "forrige steek",
            "steek terug",
            "maglia precedente",
            "punto indietro",
        )

    private val INCREMENT_WORDS =
        setOf(
            "plus",
            "more",
            "next",
            "add",
            "forward",
            "continue",
            "seuraava",
            "lisää",
            "lisaa",
            "plussa",
            "eteenpäin",
            "eteenpain",
            "nästa",
            "naesta",
            "framåt",
            "framat",
            "lägg",
            "lagg",
            "plus",
            "mehr",
            "weiter",
            "vor",
            "hinzu",
            "addiere",
            "suivant",
            "avance",
            "ajoute",
            "plus",
            "siguiente",
            "avanza",
            "suma",
            "añade",
            "anade",
            "más",
            "mas",
            "seguinte",
            "avança",
            "avanca",
            "soma",
            "adiciona",
            "mais",
            "neste",
            "legg",
            "tilføj",
            "tilfoj",
            "næste",
            "naeste",
            "læg",
            "laeg",
            "volgende",
            "verder",
            "tel",
            "voeg",
            "avanti",
            "aggiungi",
            "più",
            "piu",
        )

    private val DECREMENT_WORDS =
        setOf(
            "minus",
            "back",
            "remove",
            "previous",
            "miinus",
            "takaisin",
            "vähennä",
            "vahenna",
            "pois",
            "minus",
            "tillbaka",
            "ta bort",
            "förra",
            "forra",
            "zurück",
            "zurueck",
            "minus",
            "entferne",
            "retour",
            "recule",
            "retire",
            "moins",
            "atrás",
            "atras",
            "retrocede",
            "quita",
            "menos",
            "voltar",
            "recuar",
            "tirar",
            "menos",
            "tilbake",
            "trekk",
            "minus",
            "tilbage",
            "fjern",
            "minus",
            "terug",
            "verwijder",
            "min",
            "indietro",
            "togli",
            "meno",
        )

    private val UNDO_WORDS =
        setOf(
            "undo",
            "oops",
            "that was wrong",
            "kumoa",
            "peru",
            "ångra",
            "angra",
            "rückgängig",
            "ruckgangig",
            "annuler",
            "défaire",
            "defaire",
            "deshacer",
            "desfaz",
            "angre",
            "fortryd",
            "ongedaan maken",
            "annulla",
        )

    private val RESET_WORDS =
        setOf(
            "reset",
            "start over",
            "zero",
            "nollaa",
            "aloita alusta",
            "börja om",
            "borja om",
            "nollställ",
            "nollstall",
            "zurücksetzen",
            "zuruecksetzen",
            "null",
            "réinitialiser",
            "reinitialiser",
            "remettre à zéro",
            "remettre a zero",
            "restablecer",
            "reiniciar",
            "repor",
            "reiniciar",
            "tilbakestill",
            "nulstil",
            "resetten",
            "azzera",
            "reimposta",
        )

    private val WORD_TO_NUMBER =
        mapOf(
            // English
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "six" to 6,
            "seven" to 7,
            "eight" to 8,
            "nine" to 9,
            "ten" to 10,
            "eleven" to 11,
            "twelve" to 12,
            "thirteen" to 13,
            "fourteen" to 14,
            "fifteen" to 15,
            "sixteen" to 16,
            "seventeen" to 17,
            "eighteen" to 18,
            "nineteen" to 19,
            "twenty" to 20,
            // Finnish
            "yksi" to 1,
            "kaksi" to 2,
            "kolme" to 3,
            "neljä" to 4,
            "nelja" to 4,
            "viisi" to 5,
            "kuusi" to 6,
            "seitsemän" to 7,
            "seitseman" to 7,
            "kahdeksan" to 8,
            "yhdeksän" to 9,
            "yhdeksan" to 9,
            "kymmenen" to 10,
            // Swedish
            "ett" to 1,
            "en" to 1,
            "två" to 2,
            "tva" to 2,
            "tre" to 3,
            "fyra" to 4,
            "fem" to 5,
            "sex" to 6,
            "sju" to 7,
            "åtta" to 8,
            "atta" to 8,
            "nio" to 9,
            "tio" to 10,
            // German
            "eins" to 1,
            "zwei" to 2,
            "drei" to 3,
            "vier" to 4,
            "fünf" to 5,
            "funf" to 5,
            "sechs" to 6,
            "sieben" to 7,
            "acht" to 8,
            "neun" to 9,
            "zehn" to 10,
            // French
            "un" to 1,
            "deux" to 2,
            "trois" to 3,
            "quatre" to 4,
            "cinq" to 5,
            "six" to 6,
            "sept" to 7,
            "huit" to 8,
            "neuf" to 9,
            "dix" to 10,
            // Spanish
            "uno" to 1,
            "dos" to 2,
            "tres" to 3,
            "cuatro" to 4,
            "cinco" to 5,
            "seis" to 6,
            "siete" to 7,
            "ocho" to 8,
            "nueve" to 9,
            "diez" to 10,
            // Portuguese
            "um" to 1,
            "dois" to 2,
            "três" to 3,
            "tres" to 3,
            "quatro" to 4,
            "cinco" to 5,
            "seis" to 6,
            "sete" to 7,
            "oito" to 8,
            "nove" to 9,
            "dez" to 10,
            // Norwegian / Danish
            "en" to 1,
            "to" to 2,
            "tre" to 3,
            "fire" to 4,
            "fem" to 5,
            "seks" to 6,
            "syv" to 7,
            "sju" to 7,
            "otte" to 8,
            "åtte" to 8,
            "atte" to 8,
            "ni" to 9,
            "ti" to 10,
            // Dutch
            "een" to 1,
            "twee" to 2,
            "drie" to 3,
            "vier" to 4,
            "vijf" to 5,
            "zes" to 6,
            "zeven" to 7,
            "acht" to 8,
            "negen" to 9,
            "tien" to 10,
            // Italian
            "uno" to 1,
            "due" to 2,
            "tre" to 3,
            "quattro" to 4,
            "cinque" to 5,
            "sei" to 6,
            "sette" to 7,
            "otto" to 8,
            "nove" to 9,
            "dieci" to 10,
        )
}
