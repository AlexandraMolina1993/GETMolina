package com.example.get_molina

import com.google.gson.annotations.SerializedName

/**
 * Data class representing the response from the pharmacies API.
 */
data class FarmaciaResponse(
    @SerializedName("farmacias")
    val farmacias: List<Farmacia>
)

/**
 * Data class representing an individual pharmacy with all fields from the API.
 */
data class Farmacia(
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("direccion")
    val direccion: String,
    @SerializedName("fecha_raw")
    val fecha_raw: String,
    @SerializedName("fecha_iso")
    val fecha_iso: String? = null,
    @SerializedName("dia_semana")
    val dia_semana: String? = null,
    @SerializedName("telefono")
    val telefono: String? = null, // Some items might have it separate in other API versions
    @SerializedName("imagen")
    val imagen: String? = null
) {
    /**
     * Helper to separate the address from the phone number if they come combined in the 'direccion' field.
     */
    fun getDireccionSolo(): String {
        return if (direccion.contains(" – ")) {
            direccion.split(" – ")[0]
        } else {
            direccion
        }
    }

    /**
     * Helper to extract the phone number from the 'direccion' field if present.
     */
    fun getTelefonoExtraido(): String? {
        return if (direccion.contains(" – ")) {
            direccion.split(" – ").getOrNull(1)
        } else {
            telefono
        }
    }
}
