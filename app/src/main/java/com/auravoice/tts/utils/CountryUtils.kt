package com.auravoice.tts.utils

object CountryUtils {

    private val NON_COUNTRY_KEYWORDS = setOf(
        "hindi", "gujarati", "punjabi", "tamil", "telugu", "marathi", "bengali",
        "kannada", "malayalam", "odia", "urdu", "assamese", "spanish", "french",
        "german", "japanese", "chinese", "korean", "italian", "portuguese",
        "russian", "arabic", "english", "turkish", "dutch", "swedish", "polish",
        "indonesian", "vietnamese", "thai", "female", "male", "studio female",
        "studio male", "neutral", "default", "neural", "standard", "wavenet",
        "polyglot", "chirp", "voice", "aura"
    )

    fun isRealCountryName(name: String): Boolean {
        if (name.isBlank()) return false
        val lower = name.trim().lowercase()
        if (NON_COUNTRY_KEYWORDS.contains(lower)) return false
        if (lower.contains("studio") || lower.contains("female") || lower.contains("male")) return false
        return true
    }

    fun deriveCountry(language: String, accent: String, name: String, id: String): String {
        val langLower = language.lowercase()
        val accentLower = accent.lowercase()
        val nameLower = name.lowercase()
        val idLower = id.lowercase()

        // Check if accent itself is a valid country name
        if (isRealCountryName(accent)) {
            return accent.trim()
        }

        return when {
            // India
            langLower.contains("india") || langLower.contains("hindi") || langLower.contains("punjabi") ||
                    langLower.contains("tamil") || langLower.contains("telugu") || langLower.contains("marathi") ||
                    langLower.contains("bengali") || langLower.contains("gujarati") || langLower.contains("kannada") ||
                    langLower.contains("malayalam") || langLower.contains("odia") || langLower.contains("urdu") ||
                    langLower.contains("assamese") || accentLower.contains("indian") || accentLower.contains("hindi") ||
                    idLower.contains("-in") || nameLower.contains("india") -> "India"

            // United States
            langLower.contains("united states") || langLower.contains("american") || langLower.contains("en-us") ||
                    accentLower.contains("american") || idLower.contains("en-us") || nameLower.contains("us") -> "United States"

            // United Kingdom
            langLower.contains("united kingdom") || langLower.contains("british") || langLower.contains("en-gb") ||
                    accentLower.contains("british") || idLower.contains("en-gb") || nameLower.contains("uk") -> "United Kingdom"

            // Spain
            langLower.contains("spain") || langLower.contains("spanish") || langLower.contains("es-es") ||
                    accentLower.contains("spanish") || idLower.contains("es-es") -> "Spain"

            // France
            langLower.contains("france") || langLower.contains("french") || langLower.contains("fr-fr") ||
                    accentLower.contains("french") || idLower.contains("fr-fr") -> "France"

            // Germany
            langLower.contains("germany") || langLower.contains("german") || langLower.contains("de-de") ||
                    accentLower.contains("german") || idLower.contains("de-de") -> "Germany"

            // Japan
            langLower.contains("japan") || langLower.contains("japanese") || langLower.contains("ja-jp") ||
                    accentLower.contains("japanese") || idLower.contains("ja-jp") -> "Japan"

            // China
            langLower.contains("china") || langLower.contains("chinese") || langLower.contains("zh-cn") ||
                    langLower.contains("zh-tw") || langLower.contains("mandarin") || langLower.contains("cantonese") ||
                    accentLower.contains("chinese") || idLower.contains("zh-cn") -> "China"

            // Brazil
            langLower.contains("brazil") || langLower.contains("portuguese") || langLower.contains("pt-br") ||
                    accentLower.contains("brazilian") || idLower.contains("pt-br") -> "Brazil"

            // Italy
            langLower.contains("italy") || langLower.contains("italian") || langLower.contains("it-it") ||
                    accentLower.contains("italian") || idLower.contains("it-it") -> "Italy"

            // South Korea
            langLower.contains("korea") || langLower.contains("korean") || langLower.contains("ko-kr") ||
                    accentLower.contains("korean") || idLower.contains("ko-kr") -> "South Korea"

            // Canada
            langLower.contains("canada") || langLower.contains("canadian") || langLower.contains("en-ca") ||
                    idLower.contains("en-ca") -> "Canada"

            // Australia
            langLower.contains("australia") || langLower.contains("australian") || langLower.contains("en-au") ||
                    idLower.contains("en-au") -> "Australia"

            // Russia
            langLower.contains("russia") || langLower.contains("russian") || langLower.contains("ru-ru") ||
                    idLower.contains("ru-ru") -> "Russia"

            // Mexico
            langLower.contains("mexico") || langLower.contains("mexican") || langLower.contains("es-mx") ||
                    idLower.contains("es-mx") -> "Mexico"

            // Saudi Arabia
            langLower.contains("saudi") || langLower.contains("arabic") || langLower.contains("ar-sa") ||
                    idLower.contains("ar-sa") -> "Saudi Arabia"

            else -> "United States"
        }
    }
}
