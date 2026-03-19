package xyz.zt.mindbox.ui.screens.certificates

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import xyz.zt.mindbox.data.model.Certificate
import xyz.zt.mindbox.ui.theme.BrandOrange
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────
//  Liquid Glass Modifier — puro, sin contexto @Composable.
//  surfaceColor se recibe como parámetro (captúralo con
//  MaterialTheme.colorScheme.surface dentro del composable).
// ─────────────────────────────────────────────────────────────
fun Modifier.liquidGlassBackground(
    color: Color,
    surfaceColor: Color,
    shape: Shape,
    glassAlpha: Float = 0.13f,
    borderAlpha: Float = 0.35f,
    elevation: Dp = 8.dp,
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = color.copy(alpha = 0.18f),
        ambientColor = color.copy(alpha = 0.08f)
    )
    .background(surfaceColor, shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = glassAlpha + 0.04f),
                color.copy(alpha = glassAlpha * 0.3f)
            )
        ),
        shape = shape
    )
    .border(
        width = 0.8.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                color.copy(alpha = 0.15f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        ),
        shape = shape
    )

// ─────────────────────────────────────────────────────────────
//  Color Glow — halo difuso usando setShadowLayer en Canvas.
//  No necesita @Composable, no usa BlurMaskFilter.
// ─────────────────────────────────────────────────────────────
fun Modifier.colorGlow(
    color: Color,
    radius: Float = 80f,
    alpha: Float = 0.25f,
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val glowPaint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(radius, 0f, 4f, color.copy(alpha = alpha).toArgb())
            }
        }
        canvas.drawCircle(
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension / 2f,
            paint = glowPaint
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  Pantalla principal
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CertificatesScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var certificates by remember { mutableStateOf(emptyList<Certificate>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatformFilter by remember { mutableStateOf("Todas") }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val listState = rememberLazyListState()

    // Colapso del header inspirado en el toolbar nativo del artículo
    val headerCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
        }
    }

    val dynamicPlatforms = remember(certificates) {
        listOf("Todas") + certificates.map { it.platform }.distinct().sorted()
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            db.collection("users").document(userId).collection("certificates")
                .addSnapshotListener { snapshot, _ ->
                    certificates = snapshot?.documents
                        ?.mapNotNull { it.toObject(Certificate::class.java) }
                        ?: emptyList()
                    isLoading = false
                }
        }
    }

    // Capturamos los colores del tema aquí, en contexto @Composable,
    // para pasarlos como parámetros normales a las funciones puras.
    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurface = MaterialTheme.colorScheme.onSurface
    val isDark = isSystemInDarkTheme()
    // En modo claro el glass necesita más opacidad para ser visible
    val glassBaseAlpha = if (isDark) 0.40f else 0.12f
    val glassBrightAlpha = if (isDark) 0.12f else 0.06f
    val glassRimAlpha = if (isDark) 0.25f else 0.35f

    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                val fabShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        // 1. Halo de color difuso
                        .colorGlow(BrandOrange, radius = 70f, alpha = 0.45f)
                        // 2. Sombra con color
                        .shadow(16.dp, fabShape, spotColor = BrandOrange.copy(alpha = 0.5f), ambientColor = BrandOrange.copy(alpha = 0.2f))
                        // 3. Base semitransparente (no sólida) — aquí está la diferencia
                        .background(BrandOrange.copy(alpha = 0.45f), fabShape)
                        // 4. Gradiente blanco de arriba a abajo (brillo glass)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            ),
                            fabShape
                        )
                        // 5. Rim light — borde brillante diagonal
                        .border(
                            width = 0.8.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(130f, 130f)
                            ),
                            shape = fabShape
                        )
                        .clip(fabShape)
                        .clickable { navController.navigate("add_certificate") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Agregar Logro",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val filteredAndSorted = remember(certificates, searchQuery, selectedPlatformFilter) {
                    certificates
                        .filter { cert ->
                            val matchesSearch = cert.title.contains(searchQuery, ignoreCase = true)
                            val matchesPlatform = selectedPlatformFilter == "Todas" ||
                                    cert.platform == selectedPlatformFilter
                            matchesSearch && matchesPlatform
                        }
                        .sortedByDescending { cert ->
                            try {
                                if (cert.issueDate.isNotBlank()) dateFormat.parse(cert.issueDate)
                                else Date(0)
                            } catch (e: Exception) { Date(0) }
                        }
                }

                // ── LISTA ──────────────────────────────────────────────
                when {
                    isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = BrandOrange)
                    }
                    filteredAndSorted.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No hay certificados aún",
                                color = onBackground.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else -> LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 220.dp,
                            bottom = 100.dp,
                            start = 20.dp,
                            end = 20.dp
                        )
                    ) {
                        items(filteredAndSorted, key = { it.id }) { cert ->
                            CertificateQuickCard(
                                cert = cert,
                                surfaceColor = surface,
                                onClick = { navController.navigate("certificate_detail/${cert.id}") }
                            )
                        }
                    }
                }

                // ── HEADER FLOTANTE con efecto glass ──────────────────
                // Gradiente semitransparente: las cards "sangran" visualmente
                // por debajo del header, igual que en la imagen de referencia.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to background.copy(alpha = 0.86f),
                                    0.55f to background.copy(alpha = 0.70f),
                                    0.80f to background.copy(alpha = 0.20f),
                                    1.00f to background.copy(alpha = 0.00f)
                                )
                            )
                        )
                ) {
                    Spacer(Modifier.height(20.dp))

                    // Título + back + badge — con su propio padding horizontal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                // Base: adaptada a modo claro/oscuro
                                .background(
                                    if (isDark) surface.copy(alpha = 0.40f)
                                    else onBackground.copy(alpha = 0.08f)
                                )
                                // Brillo glass
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = glassBrightAlpha),
                                            Color.Transparent
                                        )
                                    )
                                )
                                // Rim-light
                                .border(
                                    0.8.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            onBackground.copy(alpha = glassRimAlpha),
                                            Color.Transparent
                                        ),
                                        start = Offset(0f, 0f), end = Offset(80f, 80f)
                                    ),
                                    CircleShape
                                )
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.ChevronLeft,
                                contentDescription = "Volver",
                                modifier = Modifier.size(26.dp),
                                tint = onBackground
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Text(
                            text = "Mis Logros",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )

                        Spacer(Modifier.weight(1f))

                        AnimatedVisibility(
                            visible = certificates.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            val badgeShape = RoundedCornerShape(12.dp)
                            Box(
                                modifier = Modifier
                                    .shadow(6.dp, badgeShape, spotColor = BrandOrange.copy(alpha = 0.35f))
                                    .background(BrandOrange.copy(alpha = 0.80f), badgeShape)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)
                                        ), badgeShape
                                    )
                                    .border(
                                        0.8.dp,
                                        Brush.linearGradient(
                                            listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                                            start = Offset(0f, 0f), end = Offset(80f, 80f)
                                        ),
                                        badgeShape
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${certificates.size}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Buscador estilo iOS — cápsula + botón lupa separado
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val searchShape = RoundedCornerShape(28.dp)
                        // Campo de texto (cápsula)
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(
                                    if (isDark) surface.copy(alpha = 0.30f)
                                    else onBackground.copy(alpha = 0.07f),
                                    searchShape
                                )
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)
                                    ), searchShape
                                )
                                .border(
                                    0.8.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            onBackground.copy(alpha = if (searchQuery.isNotEmpty()) 0.20f else 0.10f),
                                            Color.Transparent
                                        ),
                                        start = Offset(0f, 0f), end = Offset(200f, 80f)
                                    ),
                                    searchShape
                                )
                                .padding(horizontal = 18.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = onBackground,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(BrandOrange),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Buscar...",
                                            color = onBackground.copy(alpha = 0.30f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        // Botón lupa circular — clip primero para evitar hexágono en modo claro
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                // Base adaptativa claro/oscuro
                                .background(
                                    when {
                                        searchQuery.isNotEmpty() -> BrandOrange.copy(alpha = 0.75f)
                                        isDark -> surface.copy(alpha = 0.40f)
                                        else -> onBackground.copy(alpha = 0.08f)
                                    }
                                )
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                                    )
                                )
                                .border(
                                    0.8.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            onBackground.copy(alpha = if (searchQuery.isNotEmpty()) 0.30f else glassRimAlpha),
                                            Color.Transparent
                                        ),
                                        start = Offset(0f, 0f), end = Offset(80f, 80f)
                                    ),
                                    CircleShape
                                )
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = if (searchQuery.isNotEmpty()) Color.White
                                else onBackground.copy(alpha = 0.55f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Filtros — SIN padding en el Row, para que el scroll llegue al borde.
                    // El primer chip ya no queda cortado porque el contenedor es fillMaxWidth
                    // sin restricción de padding lateral.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dynamicPlatforms.forEach { platform ->
                            LiquidGlassFilterChip(
                                label = platform,
                                isSelected = selectedPlatformFilter == platform,
                                accentColor = BrandOrange,
                                surfaceColor = surface,
                                onSurfaceColor = onSurface,
                                onClick = { selectedPlatformFilter = platform }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Filter Chip con liquid glass
// ─────────────────────────────────────────────────────────────
@Composable
fun LiquidGlassFilterChip(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onClick: () -> Unit,
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale"
    )
    val chipShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(8.dp, chipShape, spotColor = accentColor.copy(alpha = 0.45f))
                        // Base más opaca para que el color naranja se vea bien
                        .background(accentColor.copy(alpha = 0.75f), chipShape)
                        // Brillo glass encima
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)
                            ), chipShape
                        )
                        .border(
                            0.8.dp,
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                                start = Offset(0f, 0f), end = Offset(100f, 100f)
                            ),
                            chipShape
                        )
                } else {
                    Modifier
                        .shadow(3.dp, chipShape, spotColor = Color.Black.copy(alpha = 0.15f))
                        .background(surfaceColor.copy(alpha = 0.55f), chipShape)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), chipShape)
                }
            )
            .clip(chipShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else onSurfaceColor.copy(alpha = 0.65f),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 13.sp,
            letterSpacing = if (isSelected) 0.3.sp else 0.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  Card con Liquid Glass
// ─────────────────────────────────────────────────────────────
@Composable
fun CertificateQuickCard(
    cert: Certificate,
    surfaceColor: Color,
    onClick: () -> Unit,
) {
    val platformColor = when (cert.platform) {
        "Credly"      -> Color(0xFF2196F3)
        "Carlos Slim" -> Color(0xFF4CAF50)
        "Udemy"       -> Color(0xFFA435F0)
        else          -> BrandOrange
    }

    val onBackground = MaterialTheme.colorScheme.onBackground
    val shape = RoundedCornerShape(24.dp)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entryScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardEntry"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = entryScale; scaleY = entryScale; alpha = entryScale }
            .fillMaxWidth()
            .liquidGlassBackground(
                color = platformColor,
                surfaceColor = surfaceColor,
                shape = shape,
                glassAlpha = 0.11f,
                borderAlpha = 0.38f,
                elevation = 10.dp
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Ícono con glow de color
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .colorGlow(platformColor, radius = 45f, alpha = 0.28f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                platformColor.copy(alpha = 0.22f),
                                platformColor.copy(alpha = 0.07f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                platformColor.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = null,
                    tint = platformColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cert.title,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    color = onBackground
                )
                Spacer(Modifier.height(4.dp))
                // Pill de plataforma
                Box(
                    modifier = Modifier
                        .background(platformColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, platformColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = cert.platform,
                        style = MaterialTheme.typography.labelMedium,
                        color = platformColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (cert.issueDate.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = cert.issueDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = onBackground.copy(alpha = 0.38f)
                    )
                }
            }

        }
    }
}