package xyz.zt.mindbox.ui.screens.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import xyz.zt.mindbox.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val colorScheme = MaterialTheme.colorScheme

    var noteCount by remember { mutableIntStateOf(0) }
    var certCount by remember { mutableIntStateOf(0) }
    var passCount by remember { mutableIntStateOf(0) }
    var reminderCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Animación de flotado para el icono central (similar al login)
    val infiniteTransition = rememberInfiniteTransition(label = "statsIconFloating")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val userRef = db.collection("users").document(userId)

            userRef.collection("notes").get().addOnSuccessListener { noteCount = it.size() }
            userRef.collection("certificates").get().addOnSuccessListener { certCount = it.size() }
            userRef.collection("passwords").get().addOnSuccessListener { passCount = it.size() }

            userRef.collection("reminders").get()
                .addOnSuccessListener { reminderCount = it.size() }
                .addOnCompleteListener { isLoading = false }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Estadísticas",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandOrange)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 28.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(20.dp))

                    // ICONO DE RESUMEN ESTILO LOGIN
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .graphicsLayer { translationY = floatingOffset }
                            .shadow(15.dp, shape = RoundedCornerShape(25.dp), ambientColor = BrandOrange)
                            .background(
                                brush = Brush.linearGradient(listOf(BrandOrange, BrandRust)),
                                shape = RoundedCornerShape(25.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(45.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Tu Actividad Digital",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(Modifier.height(32.dp))

                    // FILAS DE ESTADÍSTICAS
                    StatRow("Notas Guardadas", noteCount.toString(), Icons.Rounded.Description, BrandOrange)
                    Spacer(Modifier.height(16.dp))

                    StatRow("Cursos y Logros", certCount.toString(), Icons.Rounded.School, BrandRust)
                    Spacer(Modifier.height(16.dp))

                    StatRow("Llaves de Acceso", passCount.toString(), Icons.Rounded.VpnKey, BrandDeepBlue)
                    Spacer(Modifier.height(16.dp))

                    StatRow("Recordatorios", reminderCount.toString(), Icons.Rounded.NotificationsActive, Color(0xFFE91E63))

                    Spacer(Modifier.height(40.dp))

                    // TARJETA DE RESUMEN FINAL CON GRADIENTE SUAVE
                    val totalItems = noteCount + certCount + passCount + reminderCount
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, shape = RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = BrandOrange)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Resumen Total",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Has sincronizado $totalItems elementos en tu MindBox. ¡Tu red de conocimiento sigue creciendo!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, icon: ImageVector, color: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}