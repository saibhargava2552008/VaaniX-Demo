package com.example.vaanix.language

data class LanguageDetectionResult(
    val detectedLanguage: SupportedLanguage,
    val isMixedLanguage: Boolean,
    val confidence: Float,
    val reason: String
)

/**
 * Deterministic, offline language & script detector for Pan-India text and mixed speech.
 * Detects native Unicode scripts for all 12 Indian languages plus common Romanized
 * Hinglish / Tenglish / Tanglish / Kanglish / Manglish expressions.
 */
object LanguageDetector {

    private val hinglishMarkers = listOf(
        "mera", "mere", "meri", "kya", "aaya", "aayi", "karo", "karke", "sikhao",
        "mujhe", "batao", "kholo", "khol", "bhejo", "bhej", "awaz", "badhao",
        "kam karo", "samjhao", "padho", "namaste", "kaise", "hai", "hain", "mein", "ko", "chahiye"
    )

    private val tenglishMarkers = listOf(
        "naa", "kodukuki", "ammaki", "nannaki", "pampinchu", "cheppu", "teruvu",
        "chudali", "chupinchu", "em", "undi", "vachindi", "message pampinchu",
        "namaskaram", "ela", "cheyali", "penchu", "tagginchu", "nerpinchu", "chaduvu"
    )

    private val tanglishMarkers = listOf(
        "vanakkam", "sollu", "anuppu", "thira", "epdi", "enna", "irukku", "padikka", "sollunga"
    )

    private val kanglishMarkers = listOf(
        "namaskara", "helu", "kalisu", "tere", "yenu", "hege", "madodu", "oodu"
    )

    private val manglishMarkers = listOf(
        "namaskaram", "parayu", "ayakkuka", "thurakku", "entha", "engane", "vayikku"
    )

    private val marathiScriptHints = listOf("माझ्या", "करा", "उघडा", "सांगा", "पाठवा", "आहे", "शिकवा", "वाचा")

    fun detect(
        text: String,
        preferredLanguage: SupportedLanguage = LanguageCatalog.ENGLISH_INDIA
    ): LanguageDetectionResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return LanguageDetectionResult(preferredLanguage, false, 0f, "Empty input; using preferred language")
        }

        val hasLatin = trimmed.any { it in 'A'..'Z' || it in 'a'..'z' }
        val scriptCounts = mutableMapOf<String, Int>()

        for (ch in trimmed) {
            val code = ch.code
            when {
                code in 0x0C00..0x0C7F -> scriptCounts.increment(LanguageCatalog.TELUGU.bcp47Tag)
                code in 0x0900..0x097F -> scriptCounts.increment("devanagari")
                code in 0x0B80..0x0BFF -> scriptCounts.increment(LanguageCatalog.TAMIL.bcp47Tag)
                code in 0x0C80..0x0CFF -> scriptCounts.increment(LanguageCatalog.KANNADA.bcp47Tag)
                code in 0x0D00..0x0D7F -> scriptCounts.increment(LanguageCatalog.MALAYALAM.bcp47Tag)
                code in 0x0980..0x09FF -> scriptCounts.increment("bengali_assamese")
                code in 0x0A80..0x0AFF -> scriptCounts.increment(LanguageCatalog.GUJARATI.bcp47Tag)
                code in 0x0A00..0x0A7F -> scriptCounts.increment(LanguageCatalog.PUNJABI.bcp47Tag)
                code in 0x0B00..0x0B7F -> scriptCounts.increment(LanguageCatalog.ODIA.bcp47Tag)
                code in 0x0600..0x06FF -> scriptCounts.increment(LanguageCatalog.URDU.bcp47Tag)
            }
        }

        val dominantScript = scriptCounts.maxByOrNull { it.value }
        if (dominantScript != null && dominantScript.value >= 2) {
            val resolved = when (dominantScript.key) {
                "devanagari" -> {
                    val looksMarathi = marathiScriptHints.any { trimmed.contains(it) } ||
                        preferredLanguage.bcp47Tag == LanguageCatalog.MARATHI.bcp47Tag
                    if (looksMarathi) LanguageCatalog.MARATHI else LanguageCatalog.HINDI
                }
                "bengali_assamese" -> {
                    // Assamese uses 'ৰ' (U+09F0) and 'ৱ' (U+09F1) distinctively
                    val hasAssameseLetters = trimmed.any { it == '\u09F0' || it == '\u09F1' } ||
                        preferredLanguage.bcp47Tag == LanguageCatalog.ASSAMESE.bcp47Tag
                    if (hasAssameseLetters) LanguageCatalog.ASSAMESE else LanguageCatalog.BENGALI
                }
                else -> LanguageCatalog.findByTag(dominantScript.key)
            }
            return LanguageDetectionResult(
                detectedLanguage = resolved,
                isMixedLanguage = hasLatin,
                confidence = 0.92f,
                reason = "Detected ${resolved.scriptName} script" + if (hasLatin) " with English words" else ""
            )
        }

        // Check Romanized mixed-language patterns (Hinglish, Tenglish, Tanglish, etc.)
        val lower = trimmed.lowercase()
        val tokens = lower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

        val tenglishScore = countMarkerMatches(lower, tokens, tenglishMarkers)
        val hinglishScore = countMarkerMatches(lower, tokens, hinglishMarkers)
        val tanglishScore = countMarkerMatches(lower, tokens, tanglishMarkers)
        val kanglishScore = countMarkerMatches(lower, tokens, kanglishMarkers)
        val manglishScore = countMarkerMatches(lower, tokens, manglishMarkers)

        val scores = listOf(
            LanguageCatalog.TELUGU to tenglishScore,
            LanguageCatalog.HINDI to hinglishScore,
            LanguageCatalog.TAMIL to tanglishScore,
            LanguageCatalog.KANNADA to kanglishScore,
            LanguageCatalog.MALAYALAM to manglishScore
        )

        val bestMixed = scores.maxByOrNull { it.second }
        if (bestMixed != null && bestMixed.second >= 2) {
            return LanguageDetectionResult(
                detectedLanguage = bestMixed.first,
                isMixedLanguage = true,
                confidence = 0.78f,
                reason = "Detected ${bestMixed.first.mixedVariantLabel} phrasing"
            )
        }

        if (bestMixed != null && bestMixed.second == 1 && bestMixed.first.bcp47Tag == preferredLanguage.bcp47Tag) {
            return LanguageDetectionResult(
                detectedLanguage = bestMixed.first,
                isMixedLanguage = true,
                confidence = 0.70f,
                reason = "Matched preferred ${bestMixed.first.mixedVariantLabel}"
            )
        }

        return LanguageDetectionResult(
            detectedLanguage = preferredLanguage,
            isMixedLanguage = preferredLanguage.bcp47Tag != LanguageCatalog.ENGLISH_INDIA.bcp47Tag && hasLatin,
            confidence = 0.60f,
            reason = "Using selected language (${preferredLanguage.englishName})"
        )
    }

    private fun countMarkerMatches(lower: String, tokens: List<String>, markers: List<String>): Int {
        return markers.count { marker ->
            if (marker.contains(" ")) lower.contains(marker) else tokens.contains(marker)
        }
    }

    private fun MutableMap<String, Int>.increment(key: String) {
        this[key] = (this[key] ?: 0) + 1
    }
}
