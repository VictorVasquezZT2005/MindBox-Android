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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.zt.mindbox.data.local.AppDatabase
import xyz.zt.mindbox.data.local.PasswordEntity
import xyz.zt.mindbox.ui.components.QRScannerView
import xyz.zt.mindbox.utils.TOTPHelper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordsScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.get(context) }

    val passwords by db.passwordDao()
        .getAll()
        .collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    var newService by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newSecret by remember { mutableStateOf("") }

    var ticks by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            ticks++
        }
    }

    val cameraGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) showScanner = true
        }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            Text("MindBox Auth", fontWeight = FontWeight.Bold)

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

            val filtered = passwords.filter {
                it.serviceName.contains(searchQuery, true)
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No hay cuentas", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered, key = { it.id }) {
                        OtpCard(it, ticks)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (showScanner) "Escanear QR" else "Nueva cuenta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    if (showScanner) {
                        Box(Modifier.size(260.dp)) {
                            QRScannerView {
                                TOTPHelper.parseQrCode(it)?.let { (i, a, s) ->
                                    newService = i
                                    newEmail = a
                                    newSecret = s
                                    showScanner = false
                                }
                            }
                        }
                    } else {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (cameraGranted) showScanner = true
                                else permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Escanear QR")
                        }
                    }

                    OutlinedTextField(newService, { newService = it }, label = { Text("Servicio") })
                    OutlinedTextField(newEmail, { newEmail = it }, label = { Text("Usuario") })
                    OutlinedTextField(newSecret, { newSecret = it }, label = { Text("Secreto") })
                }
            },
            confirmButton = {
                Button(
                    enabled = newService.isNotBlank() && newSecret.isNotBlank(),
                    onClick = {
                        scope.launch {
                            db.passwordDao().insert(
                                PasswordEntity(
                                    id = UUID.randomUUID().toString(),
                                    serviceName = newService,
                                    accountEmail = newEmail,
                                    secretKey = newSecret
                                )
                            )
                        }
                        showDialog = false
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
