package ru.example.dictionary

import retrofit2.http.*

interface ApiService {
    @POST("/hieroglyphs/get_available_graphemes")
    suspend fun getAvailableGraphemes(@Body request: GraphemeRequest): GraphemeResponse

    @POST("/hieroglyphs/get_hieroglyph")
    suspend fun getHieroglyph(
        @Body request: GraphemeRequest
    ): HieroglyphResponse
    @POST("/translation/translate/")
    suspend fun getTranslation(@Body request: TranslationRequest): TranslationResponse
}