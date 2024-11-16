package ru.example.dictionary

import retrofit2.http.*

interface ApiService {
    @POST("/hieroglyphs/get_available_graphemes")
    suspend fun getAvailableGraphemes(@Body request: GraphemeRequest): GraphemeResponse

    @POST("/hieroglyphs/confirm")
    suspend fun confirmGraphemes(
        @Query("hieroglyph") hieroglyph: String,
        @Body selected_graphemes: GraphemeRequest
    ): ConfirmResponse

    @GET("/hieroglyphs/random_hieroglyph")
    suspend fun getRandomHieroglyph(): String

    @POST("/translation/translate/")
    suspend fun getTranslation(@Body request: TranslationRequest): TranslationResponse
}