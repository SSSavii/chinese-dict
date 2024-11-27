package ru.example.dictionary

data class GraphemeRequest(
    val graphemes: List<String>
)

data class GraphemeResponse(
    val available_graphemes: List<String>
)

data class HieroglyphResponse(
    val hieroglyph: String
)

data class TranslationRequest(
    val text: String
)

data class TranslationResponse(
    val tokens: List<TokenDetail>? = null,
    // Добавляем поля для одиночного токена
    val token: String? = null,
    val pinyin: String? = null,
    val meanings: List<String>? = null
)

data class TokenDetail(
    val token: String,
    val pinyin: String,
    val meanings: List<String>
)
