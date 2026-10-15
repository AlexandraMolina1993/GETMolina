package com.example.get_molina.ui.viewmodel

import com.example.get_molina.Farmacia

/**
 * Estado sellado (Sealed Interface) que representa los estados posibles de la interfaz de usuario.
 * Permite manejar de forma segura y exhaustiva los estados mediante expresiones `when`.
 */
sealed interface FarmaciaUiState {

    /**
     * Estado de carga inicial mientras se obtiene la información de Room o la red.
     */
    object Cargando : FarmaciaUiState

    /**
     * Estado de éxito cuando los datos se sincronizaron con éxito desde la API o la base local.
     *
     * @param farmacias Lista de farmacias a mostrar en la pantalla.
     */
    data class Exito(val farmacias: List<Farmacia>) : FarmaciaUiState

    /**
     * Estado de modo offline/sin conexión. Informa al usuario que se están mostrando los datos guardados localmente.
     *
     * @param farmacias Lista de farmacias recuperada desde Room.
     * @param mensaje Mensaje explicativo para la alerta o banner en la UI.
     */
    data class SinConexion(
        val farmacias: List<Farmacia>,
        val mensaje: String = "Sin conexión, mostrando datos guardados"
    ) : FarmaciaUiState

    /**
     * Estado de error cuando falla la sincronización y la base de datos está vacía.
     *
     * @param mensaje Detalle del fallo ocurrido.
     * @param farmacias Lista vacía o datos de respaldo si existieran.
     */
    data class Error(
        val mensaje: String,
        val farmacias: List<Farmacia> = emptyList()
    ) : FarmaciaUiState
}
