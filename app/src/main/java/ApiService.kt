package ru.example.dictionary

import retrofit2.http.POST
import retrofit2.http.Body

interface ApiService {
    @POST("get_available_graphemes")
    suspend fun getAvailableGraphemes(@Body request: GraphemeRequest): GraphemeResponse
}

data class GraphemeRequest(val graphemes: List<String>)
data class GraphemeResponse(val available_graphemes: List<String>)