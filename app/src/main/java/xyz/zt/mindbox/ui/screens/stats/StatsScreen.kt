package xyz.zt.mindbox.ui.screens.stats

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import xyz.zt.mindbox.ui.components.MindGraphBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val colorScheme = MaterialTheme.colorScheme

    // Estados para los conteos
    var noteCount by remember { mutableIntStateOf(0) }
    var certCount by remember { mutableIntStateOf(0) }
    var passCount by remember { mutableIntStateOf(0) }
    var reminderCount by remember { mutableIntStateOf(0) } // <-- NUEVO ESTADO
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val userRef = db.collection("users").document(userId)

            // Consultas en paralelo para mayor velocidad
            userRef.collection("notes").get().addOnSuccessListener { noteCount = it.size() }
            userRef.collection("certificates").get().addOnSuccessListener { certCount = it.size() }
            userRef.collection("passwords").get().addOnSuccessListener { passCount = it.size() }

            // NUEVA CONSULTA: Recordatorios
            userRef.collection("reminders").get()
                .addOnSuccessListener { reminderCount = it.size() }
                .addOnCompleteListener { isLoading = false }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi Red Digital", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            MindGraphBackground(modifier = Modifier.fillMaxSize(), nodeCount = 20)

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Resumen de Conexiones",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(20.dp))

                    // --- TARJETAS DE ESTADÍSTICAS ---
                    StatRow("Notas Guardadas", noteCount.toString(), Icons.Rounded.Description, colorScheme.primary)
                    Spacer(Modifier.height(12.dp))

                    StatRow("Cursos y Logros", certCount.toString(), Icons.Rounded.School, colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))

                    StatRow("Llaves de Acceso", passCount.toString(), Icons.Rounded.VpnKey, colorScheme.tertiary)
                    Spacer(Modifier.height(12.dp))

                    // NUEVA FILA: Recordatorios
                    StatRow("Recordatorios", reminderCount.toString(), Icons.Rounded.NotificationsActive, Color(0xFFE91E63))

                    Spacer(Modifier.height(30.dp))

                    // Mensaje Dinámico
                    val totalItems = noteCount + certCount + passCount + reminderCount
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Tu MindBox tiene $totalItems puntos de información conectados. Sigue nutriendo tu red digital.",
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, icon: ImageVector, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
        }
    }
}