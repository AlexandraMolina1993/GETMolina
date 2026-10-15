package com.example.get_molina

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.get_molina.data.local.AppDatabase
import com.example.get_molina.data.repository.FarmaciaRepository
import com.example.get_molina.ui.theme.GETMolinaTheme
import com.example.get_molina.ui.viewmodel.FarmaciaUiState
import com.example.get_molina.ui.viewmodel.FarmaciaViewModel
import com.example.get_molina.ui.viewmodel.FarmaciaViewModelFactory
import kotlinx.coroutines.launch

val WebYellow = Color(0xFFFFD54F)
val CardBackground = Color.White
val VerifiedGreen = Color(0xFF2E7D32)

class MainActivity : ComponentActivity() {

    // ViewModel para gestionar las farmacias con Room + Retrofit
    private lateinit var farmaciaViewModel: FarmaciaViewModel

    // Respuesta híbrida de la búsqueda de negocios
    private var hybridResponse by mutableStateOf<HybridSearchResponse?>(null)
    private var isSearchingNegocios by mutableStateOf(value = false)
    private var lastQuery by mutableStateOf("")

    private var searchText by mutableStateOf("")
    private var farmaciaSeleccionada by mutableStateOf<Farmacia?>(null)
    private var negocioSeleccionado by mutableStateOf<SearchResult?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializamos la base de datos de Room, el Repositorio y el ViewModel (sin Hilt)
        val database = AppDatabase.getInstance(applicationContext)
        val repository = FarmaciaRepository(
            dao = database.farmaciaDao(),
            apiService = RetrofitClient.instance,
            database = database
        )
        val factory = FarmaciaViewModelFactory(repository)
        farmaciaViewModel = ViewModelProvider(this, factory)[FarmaciaViewModel::class.java]

        setContent {
            GETMolinaTheme {
                when {
                    negocioSeleccionado != null -> {
                        PantallaDetalleNegocio(negocioSeleccionado!!) {
                            negocioSeleccionado = null
                        }
                    }
                    farmaciaSeleccionada != null -> {
                        PantallaDetalle(farmaciaSeleccionada!!) {
                            farmaciaSeleccionada = null
                        }
                    }
                    else -> {
                        PantallaPrincipal(viewModel = farmaciaViewModel)
                    }
                }
            }
        }
    }

    @Composable
    fun PantallaPrincipal(viewModel: FarmaciaViewModel) {
        // Recolectamos el estado de forma reactiva y segura con el ciclo de vida
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

        // Extraemos la lista de farmacias según el estado actual de la UI.
        // NOTA: La lista de farmacias ya viene filtrada desde Room vía DAO con la query LIKE.
        val listaFarmacias = when (val state = uiState) {
            is FarmaciaUiState.Exito -> state.farmacias
            is FarmaciaUiState.SinConexion -> state.farmacias
            is FarmaciaUiState.Error -> state.farmacias
            is FarmaciaUiState.Cargando -> emptyList()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WebYellow)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeaderSection(
                    query = searchText,
                    onQueryChange = {
                        searchText = it
                        viewModel.onSearchQueryChanged(it)
                    },
                    onSearchClick = { 
                        lastQuery = it
                        performHybridSearch(it) 
                    }
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // Sección de Negocios
                        if (hybridResponse != null || isSearchingNegocios) {
                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text(
                                        text = buildAnnotatedString {
                                            append("¡Claro! Esto es lo que encontré para ")
                                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                append("\"$lastQuery\"")
                                            }
                                            append(":")
                                        },
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color.Red,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = hybridResponse?.location ?: "Villa del Rosario",
                                            fontSize = 12.sp,
                                            color = Color.DarkGray,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                                
                                if (isSearchingNegocios) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                        color = Color.Black
                                    )
                                } else {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        items(hybridResponse?.results ?: emptyList()) { negocio ->
                                            CardNegocio(negocio) {
                                                negocioSeleccionado = negocio
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        // Sección de Farmacias
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = CardBackground,
                                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                            ) {
                                Column(modifier = Modifier.padding(top = 24.dp)) {
                                    
                                    // Título de la sección con botón de refresco/actualización
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Farmacias de Turno",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { viewModel.refrescar(forzar = true) },
                                            enabled = !isRefreshing
                                        ) {
                                            if (isRefreshing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.Black
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Actualizar datos",
                                                    tint = Color.Black
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "(desde: 08:00 am hasta: 08:00 am del día siguiente)",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 4.dp),
                                        textAlign = TextAlign.Start
                                    )

                                    // Campo de Búsqueda de Farmacias directo con Room (DAO LIKE query)
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 8.dp),
                                        placeholder = { Text("Buscar farmacia por nombre o dirección...", fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Buscar farmacia",
                                                tint = Color.Gray
                                            )
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Limpiar búsqueda",
                                                        tint = Color.Gray
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8F9FA),
                                            unfocusedContainerColor = Color(0xFFF8F9FA),
                                            focusedBorderColor = WebYellow,
                                            unfocusedBorderColor = Color.LightGray
                                        )
                                    )

                                    // BANDEROLA / AVISO DE MODO OFFLINE ("Sin conexión, mostrando datos guardados")
                                    if (uiState is FarmaciaUiState.SinConexion) {
                                        val mensaje = (uiState as FarmaciaUiState.SinConexion).mensaje
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            color = Color(0xFFFFF3CD),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Info,
                                                        contentDescription = "Aviso Sin Conexión",
                                                        tint = Color(0xFF856404),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = mensaje,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF856404),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                TextButton(
                                                    onClick = { viewModel.refrescar(forzar = true) }
                                                ) {
                                                    Text(
                                                        text = "Actualizar",
                                                        color = Color(0xFF856404),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(top = 8.dp),
                                        thickness = 0.5.dp,
                                        color = Color.LightGray
                                    )
                                    
                                    if (uiState is FarmaciaUiState.Cargando) {
                                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                        }
                                    } else if (uiState is FarmaciaUiState.Error && listaFarmacias.isEmpty()) {
                                        val errorMsg = (uiState as FarmaciaUiState.Error).mensaje
                                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                            Column(
                                                modifier = Modifier.align(Alignment.Center),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Error: $errorMsg",
                                                    color = Color.Red,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(16.dp)
                                                )
                                                Button(onClick = { viewModel.refrescar(forzar = true) }) {
                                                    Text("Reintentar")
                                                }
                                            }
                                        }
                                    } else if (listaFarmacias.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                            Text(
                                                text = "No se encontraron resultados",
                                                modifier = Modifier.align(Alignment.Center),
                                                color = Color.Gray
                                            )
                                        }
                                    } else {
                                        listaFarmacias.forEach { farmacia ->
                                            Box(modifier = Modifier.clickable { farmaciaSeleccionada = farmacia }) {
                                                ItemFarmaciaWeb(farmacia)
                                            }
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                thickness = 0.5.dp,
                                                color = Color.LightGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CardNegocio(negocio: SearchResult, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .width(260.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                AsyncImage(
                    model = negocio.imagen,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = negocio.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = buildAnnotatedString {
                            append("Vendido por: ")
                            append(negocio.negocioNombre ?: negocio.nombre)
                            append(" ")
                            withStyle(style = SpanStyle(color = Color(0xFF4CAF50), fontSize = 10.sp)) {
                                append("(${negocio.fuente ?: "Google Places"})")
                            }
                        },
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = VerifiedGreen, modifier = Modifier.size(14.dp))
                            Text(text = "Negocio Verificado", fontSize = 11.sp, color = VerifiedGreen, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = negocio.precio ?: "Consultar precio", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, null, tint = Color(0xFFE91E63), modifier = Modifier.size(14.dp))
                            Text(text = negocio.telefono ?: "", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = WebYellow, modifier = Modifier.size(14.dp))
                            Text(text = negocio.rating?.toString() ?: "0.0", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PantallaDetalleNegocio(negocio: SearchResult, onBack: () -> Unit) {
        BackHandler { onBack() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Detalle del Negocio") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WebYellow, titleContentColor = Color.Black)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color.White).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(model = negocio.imagen, contentDescription = null, modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = negocio.nombre, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!negocio.descripcion.isNullOrBlank()) { InfoRow(icon = Icons.Default.Info, label = "Descripción", value = negocio.descripcion) }
                    if (!negocio.ubicacion.isNullOrBlank()) { InfoRow(icon = Icons.Default.LocationOn, label = "Dirección", value = negocio.ubicacion) }
                    if (!negocio.emailContacto.isNullOrBlank()) { InfoRow(icon = null, label = "Email", value = negocio.emailContacto) }
                    if (!negocio.telefono.isNullOrBlank()) { InfoRow(icon = Icons.Default.Phone, label = "Teléfono", value = negocio.telefono) }
                    if (negocio.rating != null) { InfoRow(icon = Icons.Default.Star, label = "Calificación", value = "${negocio.rating} / 5.0") }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PantallaDetalle(farmacia: Farmacia, onBack: () -> Unit) {
        BackHandler { onBack() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Detalle de Farmacia") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WebYellow, titleContentColor = Color.Black)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color.White).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(model = farmacia.imagen, contentDescription = null, modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)), contentScale = ContentScale.Fit)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = farmacia.nombre, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFFFFCDD2))
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoRow(icon = Icons.Default.LocationOn, label = "Dirección", value = farmacia.getDireccionSolo())
                    val telefono = farmacia.getTelefonoExtraido()
                    if (!telefono.isNullOrBlank()) { InfoRow(icon = Icons.Default.Phone, label = "Teléfono", value = telefono) }
                    InfoRow(icon = null, label = "Turno", value = farmacia.fecha_raw)
                }
            }
        }
    }

    private fun performHybridSearch(query: String) {
        if (query.isBlank()) return

        val normalizedQuery = query.lowercase().trim()
        
        val hardwareKeywords = listOf("ferreter", "bulon", "herramient")
        val foodKeywords = listOf("comid", "restaur", "pizz", "hamburg", "lomit", "empanad", "parrill", "gastronom", "bar", "helad", "cafe", "café")

        if (hardwareKeywords.any { normalizedQuery.contains(it) }) {
            hybridResponse = HybridSearchResponse(
                results = listOf(
                    SearchResult(
                        id = "mock_1",
                        nombre = "Ferretería San Martín",
                        descripcion = "Herramientas y materiales para construcción",
                        precio = "Consultar",
                        imagen = "https://picsum.photos/400/300?random=139",
                        negocioNombre = "Ferretería San Martín",
                        emailContacto = "contacto@demo.com",
                        telefono = "3573-444111",
                        fuente = "mock",
                        ubicacion = "San Martín 250, Villa del Rosario",
                        rating = 3.5,
                        placeId = "MOCK_PLACE_6a3d662306d1d"
                    ),
                    SearchResult(
                        id = "mock_2",
                        nombre = "El Tornillo Feliz",
                        descripcion = "Bulonería y ferretería industrial",
                        precio = "Consultar",
                        imagen = "https://picsum.photos/400/300?random=929",
                        negocioNombre = "El Tornillo Feliz",
                        emailContacto = "contacto@demo.com",
                        telefono = "3573-444222",
                        fuente = "mock",
                        ubicacion = "Belgrano 120, Villa del Rosario",
                        rating = 4.1,
                        placeId = "MOCK_PLACE_6a3d662306d23"
                    ),
                    SearchResult(
                        id = "mock_3",
                        nombre = "Ferretería Centro",
                        descripcion = "Pinturas y herramientas eléctricas",
                        precio = "Consultar",
                        imagen = "https://picsum.photos/400/300?random=11",
                        negocioNombre = "Ferretería Centro",
                        emailContacto = "contacto@demo.com",
                        telefono = "3573-444333",
                        fuente = "mock",
                        ubicacion = "9 de Julio 450, Villa del Rosario",
                        rating = 4.7,
                        placeId = "MOCK_PLACE_6a3d662306d25"
                    )
                ),
                total = 3,
                localCount = 0,
                googleCount = 3,
                location = "Villa del Rosario",
                message = "Negocios cercanos en tu área"
            )
            return
        } else if (foodKeywords.any { normalizedQuery.contains(it) }) {
            hybridResponse = HybridSearchResponse(
                results = listOf(
                    SearchResult(
                        id = "mock_food_1",
                        nombre = "Pizzería Don Mario",
                        descripcion = "Pizza y empanadas artesanales al horno de barro",
                        precio = "$$$",
                        imagen = "https://picsum.photos/400/300?random=201",
                        negocioNombre = "Pizzería Don Mario",
                        emailContacto = "donmario@demo.com",
                        telefono = "3573-456789",
                        fuente = "mock",
                        ubicacion = "9 de Julio 120, Villa del Rosario",
                        rating = 4.6,
                        placeId = "MOCK_PLACE_FOOD_1"
                    ),
                    SearchResult(
                        id = "mock_food_2",
                        nombre = "Lomitería El Gordo",
                        descripcion = "Los mejores lomitos, hamburguesas y papas fritas",
                        precio = "$$",
                        imagen = "https://picsum.photos/400/300?random=202",
                        negocioNombre = "Lomitería El Gordo",
                        emailContacto = "elgordo@demo.com",
                        telefono = "3573-456790",
                        fuente = "mock",
                        ubicacion = "San Martín 480, Villa del Rosario",
                        rating = 4.4,
                        placeId = "MOCK_PLACE_FOOD_2"
                    ),
                    SearchResult(
                        id = "mock_food_3",
                        nombre = "Parrilla & Restaurante El Gaucho",
                        descripcion = "Carnes asadas, pastas caseras y minutas",
                        precio = "$$$$",
                        imagen = "https://picsum.photos/400/300?random=203",
                        negocioNombre = "Parrilla El Gaucho",
                        emailContacto = "elgaucho@demo.com",
                        telefono = "3573-456791",
                        fuente = "mock",
                        ubicacion = "Av. Rivadavia 350, Villa del Rosario",
                        rating = 4.8,
                        placeId = "MOCK_PLACE_FOOD_3"
                    ),
                    SearchResult(
                        id = "mock_food_4",
                        nombre = "Heladería & Café Tentación",
                        descripcion = "Helados artesanales, postres y cafetería",
                        precio = "$$",
                        imagen = "https://picsum.photos/400/300?random=204",
                        negocioNombre = "Tentación",
                        emailContacto = "tentacion@demo.com",
                        telefono = "3573-456792",
                        fuente = "mock",
                        ubicacion = "Belgrano 210, Villa del Rosario",
                        rating = 4.5,
                        placeId = "MOCK_PLACE_FOOD_4"
                    )
                ),
                total = 4,
                localCount = 0,
                googleCount = 4,
                location = "Villa del Rosario",
                message = "Lugares de comida y gastronomía cercanos"
            )
            return
        }

        isSearchingNegocios = true
        lifecycleScope.launch {
            try {
                val respuesta = RetrofitClient.instance.buscarNegocios(query)
                if (respuesta.isSuccessful) { hybridResponse = respuesta.body() }
            } catch (e: Exception) { Log.e("SEARCH_ERROR", "Error: ${e.message}") }
            finally { isSearchingNegocios = false }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector?, label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = WebYellow, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = value, fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
        }
    }
}

@Composable
fun HeaderSection(query: String, onQueryChange: (String) -> Unit, onSearchClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 24.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(model = "https://buscaya.com.ar/img/logo.png", contentDescription = "Logo BuscaYa", modifier = Modifier.height(70.dp).fillMaxWidth(), contentScale = ContentScale.Fit)
        Text(text = "Villa del Rosario", fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            placeholder = { Text("¿Qué buscas hoy?") },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { onSearchClick(query) }, modifier = Modifier.width(180.dp).height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(20.dp)) {
            Text(text = "Buscar", color = Color.White)
        }
    }
}

@Composable
fun ItemFarmaciaWeb(farmacia: Farmacia) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = farmacia.imagen, contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFF5F5F5)), contentScale = ContentScale.Fit)
        Spacer(modifier = Modifier.width(12.dp))
        val partes = farmacia.fecha_raw.split(" ")
        val diaNombre = partes.getOrNull(0) ?: ""
        val diaNum = partes.getOrNull(1) ?: ""
        val mesNom = partes.getOrNull(3)?.take(3) ?: ""
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(85.dp)) {
            Text(text = diaNombre, fontSize = 11.sp, color = Color.Gray)
            Text(text = diaNum, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(text = mesNom, fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = farmacia.nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(text = farmacia.getDireccionSolo(), fontSize = 14.sp, color = Color.DarkGray, lineHeight = 18.sp)
        }
    }
}
