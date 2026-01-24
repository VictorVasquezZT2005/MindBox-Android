package xyz.zt.mindbox.ui.screens.passwords

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import xyz.zt.mindbox.data.model.Password

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordsScreen(navController: NavController, openAddDirectly: Boolean = false) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var passwords by remember { mutableStateOf(emptyList<Password>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var passwordToDelete by remember { mutableStateOf<Password?>(null) }

    // EFECTO: Si venimos del Dashboard pidiendo agregar, navegamos de inmediato
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
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // CORRECCIÓN: Navegar a la pantalla completa en lugar de abrir el diálogo
                navController.navigate("add_password")
            }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Contraseñas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Buscar servicio") },
                shape = RoundedCornerShape(28.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else {
                val filtered = passwords.filter { it.serviceName.contains(searchQuery, true) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

    // El Diálogo de borrado sí puede ser flotante (es estándar en Android)
    if (passwordToDelete != null) {
        AlertDialog(
            onDismissRequest = { passwordToDelete = null },
            title = { Text("¿Eliminar acceso?") },
            text = { Text("Se borrará el código 2FA para ${passwordToDelete?.serviceName}.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        passwordToDelete?.let {
                            db.collection("users").document(userId).collection("passwords").document(it.id).delete()
                        }
                        passwordToDelete = null
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { passwordToDelete = null }) { Text("Cancelar") } }
        )
    }
}