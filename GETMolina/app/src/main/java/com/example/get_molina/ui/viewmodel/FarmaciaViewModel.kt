package com.example.get_molina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.get_molina.data.repository.FarmaciaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de la gestión del estado de la pantalla de farmacias.
 *
 * Utiliza [viewModelScope] para la ejecución de corrutinas en el hilo secundario
 * y expone la UI a través de [StateFlow] de manera reactiva y segura contra cambios de configuración.
 */
class FarmaciaViewModel(
    private val repository: FarmaciaRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    /**
     * Consulta de búsqueda actual. Al modificar este valor, el ViewModel realiza
     * la consulta con LIKE directamente en la base de datos de Room en tiempo real.
     */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    /**
     * Estado que indica si hay una operación de refresco o sincronización en curso.
     */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _sinConexion = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow<FarmaciaUiState>(FarmaciaUiState.Cargando)
    /**
     * Estado principal expuesto a la UI (Compose) de forma reactiva.
     */
    val uiState: StateFlow<FarmaciaUiState> = _uiState.asStateFlow()

    init {
        // Observamos el Flow de Room directo desde la BD ejecutando la consulta LIKE cuando cambia _searchQuery
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            val farmaciasFlow = _searchQuery.flatMapLatest { query ->
                repository.buscarPorNombre(query)
            }

            combine(
                farmaciasFlow,
                _sinConexion,
                _errorMessage
            ) { listaFarmacias, estaOffline, error ->
                when {
                    // Si no hay conexión pero existen datos previamente guardados en Room
                    estaOffline && listaFarmacias.isNotEmpty() -> {
                        FarmaciaUiState.SinConexion(farmacias = listaFarmacias)
                    }
                    // Si ocurrió un error y la base de datos está totalmente vacía
                    error != null && listaFarmacias.isEmpty() -> {
                        FarmaciaUiState.Error(mensaje = error, farmacias = emptyList())
                    }
                    // Si tenemos datos en Room y la última sincronización fue exitosa o hay datos cargados
                    listaFarmacias.isNotEmpty() -> {
                        FarmaciaUiState.Exito(farmacias = listaFarmacias)
                    }
                    // Mientras no haya datos y no se haya determinado error
                    else -> {
                        FarmaciaUiState.Cargando
                    }
                }
            }.collect { estadoCalculado ->
                _uiState.value = estadoCalculado
            }
        }

        // Intenta sincronizar automáticamente al crear el ViewModel
        refrescar(forzar = true)
    }

    /**
     * Actualiza la consulta de búsqueda que dispara la consulta SQL LIKE directamente en Room.
     *
     * @param nuevaConsulta Texto ingresado por el usuario en la barra de búsqueda.
     */
    fun onSearchQueryChanged(nuevaConsulta: String) {
        _searchQuery.value = nuevaConsulta
    }

    /**
     * Solicita al repositorio refrescar o sincronizar los datos de las farmacias desde la API.
     *
     * @param forzar Si es true, exige hacer la petición HTTP a Retrofit aunque ya existan datos en Room.
     */
    fun refrescar(forzar: Boolean = true) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val resultado = repository.refrescar(forzar = forzar)

            if (resultado.isSuccess) {
                _sinConexion.value = false
            } else {
                // Si la sincronización falla (por ej. falta de conexión o servidor caído)
                _sinConexion.value = true
                _errorMessage.value = resultado.exceptionOrNull()?.localizedMessage
                    ?: "Sin conexión con el servidor"
            }

            _isRefreshing.value = false
        }
    }
}

/**
 * Factoría (ViewModelProvider.Factory) simple para instanciar [FarmaciaViewModel]
 * inyectando manualmente el repositorio sin necesidad de usar Hilt.
 */
class FarmaciaViewModelFactory(
    private val repository: FarmaciaRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FarmaciaViewModel::class.java)) {
            return FarmaciaViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel no reconocido: ${modelClass.name}")
    }
}
