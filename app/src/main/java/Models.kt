package ru.example.dictionary

data class GraphemeRequest(
    val graphemes: List<String>
)

data class GraphemeResponse(
    val available_graphemes: List<String>
)

data class ConfirmResponse(
    val confirm: Boolean
)

data class TranslationRequest(
    val text: String
)

data class TranslationResponse(
    val tokens: List<TokenDetail>
)

data class TokenDetail(
    val token: String,
    val pinyin: String,
    val meanings: List<String>
)
data class ConfirmGraphemesRequest(
    val hieroglyph: String,
    val graphemes: List<String>
)