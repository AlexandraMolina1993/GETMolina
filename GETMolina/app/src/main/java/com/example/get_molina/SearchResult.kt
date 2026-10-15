package com.example.get_molina

import com.google.gson.annotations.SerializedName

/**
 * Respuesta exacta de la API híbrida de BuscaYa.
 */
data class HybridSearchResponse(
    @SerializedName("results")
    val results: List<SearchResult>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("local_count")
    val localCount: Int,
    @SerializedName("google_count")
    val googleCount: Int,
    @SerializedName("location")
    val location: String?,
    @SerializedName("message")
    val message: String?
)

/**
 * Estructura de cada negocio según el JSON provisto.
 */
data class SearchResult(
    @SerializedName("id")
    val id: String,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("descripcion")
    val descripcion: String?,
    @SerializedName("precio")
    val precio: String?,
    @SerializedName("imagen")
    val imagen: String?,
    @SerializedName("negocio_nombre")
    val negocioNombre: String?,
    @SerializedName("email_contacto")
    val emailContacto: String?,
    @SerializedName("telefono")
    val telefono: String?,
    @SerializedName("fuente")
    val fuente: String?,
    @SerializedName("ubicacion")
    val ubicacion: String?,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("place_id")
    val placeId: String?
)
