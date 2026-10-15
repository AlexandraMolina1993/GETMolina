package com.example.get_molina

import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    /**
     * Fetches the list of pharmacies on duty from the server.
     * 
     * @return A [Response] containing a [FarmaciaResponse] object.
     */
    @GET("api/farmacias_turno_android")
    suspend fun getFarmacias(): Response<FarmaciaResponse>

    /**
     * Performs a hybrid search for businesses.
     * 
     * @param query The search query (e.g., "ferreteria").
     * @return A [Response] containing [HybridSearchResponse].
     */
    @GET("api/hybrid-search-android")
    suspend fun buscarNegocios(
        @retrofit2.http.Query("q") query: String
    ): Response<HybridSearchResponse>
}
