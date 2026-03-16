package xyz.zt.mindbox.ui.screens.passwords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import xyz.zt.mindbox.data.model.Password
import xyz.zt.mindbox.ui.theme.* @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordsScreen(navController: NavController, openAddDirectly: Boolean = false) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var passwords by remember { mutableStateOf(emptyList<Password>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var passwordToDelete by remember { mutableStateOf<Password?>(null) }

    LaunchedEffect(openAddDirectly) {
        if (openAddDirectly) {
            navController.navigate("add_password")
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            db.collection("users").document(userId).collection("passwords")
                .addSnapshotListener { snapshot, _ ->
                    passwords = snapshot?.documents?.mapNotNull { it.toObject(Password::class.java) } ?: emptyList()
                    isLoading = false
                }
        }
    }

    var ticks by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            ticks++
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            // FAB CORREGIDO Y CENTRADO
            Box(
                modifier = Modifier
                    .size(56.dp) // Tamaño estándar de FAB
                    .shadow(12.dp, CircleShape)
                    .background(
                        brush = Brush.linearGradient(listOf(BrandOrange, BrandRust)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center // Esto centra el FAB y su icono
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_password") },
                    containerColor = Color.Transparent, // Transparente para ver el gradiente del Box
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp), // Quitamos sombra interna
                    shape = CircleShape,
                    modifier = Modifier.fillMaxSize() // Ocupa todo el Box para mantener el centro
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar",
                        modifier = Modifier.size(28.dp) // Tamaño del icono ajustado
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "Mis Accesos",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "Tus códigos 2FA y seguridad.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )

            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = BrandOrange) },
                placeholder = { Text("Buscar servicio...") },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(Modifier.height(24.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = BrandOrange)
                }
            } else {
                val filtered = passwords.filter { it.serviceName.contains(searchQuery, true) }

                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No se encontraron servicios",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            OtpCard(
                                password = item,
                                ticks = ticks,
                                onDeleteRequest = { passwordToDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (passwordToDelete != null) {
        AlertDialog(
            onDismissRequest = { passwordToDelete = null },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "¿Eliminar acceso?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Se borrará definitivamente el código para ${passwordToDelete?.serviceName}. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        passwordToDelete?.let {
                            db.collection("users").document(userId).collection("passwords").document(it.id).delete()
                        }
                        passwordToDelete = null
                    }
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { passwordToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}