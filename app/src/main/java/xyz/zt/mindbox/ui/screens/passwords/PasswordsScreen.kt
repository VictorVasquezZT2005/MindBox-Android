package xyz.zt.mindbox.ui.screens.passwords

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import xyz.zt.mindbox.data.model.Password
import xyz.zt.mindbox.ui.components.QRScannerView
import xyz.zt.mindbox.utils.TOTPHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordsScreen(openAddDialog: Boolean = false) { // Parámetro de acceso directo
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var passwords by remember { mutableStateOf(emptyList<Password>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // El estado inicial del diálogo depende de si venimos del Dashboard
    var showAddDialog by remember { mutableStateOf(openAddDialog) }
    var passwordToDelete by remember { mutableStateOf<Password?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    var newService by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newSecret by remember { mutableStateOf("") }

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

    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) showScanner = true }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                newService = ""; newEmail = ""; newSecret = ""; showAddDialog = true
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

    // Diálogo Confirmar Borrado
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

    // Diálogo Agregar Cuenta
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; showScanner = false },
            title = { Text(if (showScanner) "Escanear QR" else "Nueva cuenta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showScanner) {
                        Box(Modifier.size(260.dp).align(Alignment.CenterHorizontally)) {
                            QRScannerView {
                                TOTPHelper.parseQrCode(it)?.let { (s, a, k) ->
                                    newService = s; newEmail = a; newSecret = k
                                    showScanner = false
                                }
                            }
                        }
                    } else {
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            if (cameraGranted) showScanner = true else permissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Icon(Icons.Default.QrCodeScanner, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Escanear QR")
                        }
                        OutlinedTextField(newService, { newService = it }, label = { Text("Servicio") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(newEmail, { newEmail = it }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(newSecret, { newSecret = it }, label = { Text("Secreto") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                if (!showScanner) {
                    Button(
                        enabled = newService.isNotBlank() && newSecret.isNotBlank(),
                        onClick = {
                            val p = Password(serviceName = newService, accountEmail = newEmail, secretKey = newSecret)
                            db.collection("users").document(userId).collection("passwords").document(p.id).set(p)
                            showAddDialog = false
                        }
                    ) { Text("Guardar") }
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; showScanner = false }) { Text("Cancelar") } }
        )
    }
}