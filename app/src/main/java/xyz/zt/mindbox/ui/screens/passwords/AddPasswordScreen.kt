package xyz.zt.mindbox.ui.screens.passwords

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import xyz.zt.mindbox.data.model.Password
import xyz.zt.mindbox.ui.components.QRScannerView
import xyz.zt.mindbox.utils.TOTPHelper
import xyz.zt.mindbox.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var service by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    // Control de permisos para el visor integrado
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    // Pedir permiso al entrar si no se tiene
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Nuevo Acceso",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- CUADRO DE ESCÁNER INTEGRADO ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = BrandOrange)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .border(2.dp, Brush.linearGradient(listOf(BrandOrange, BrandRust)), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    QRScannerView { result ->
                        TOTPHelper.parseQrCode(result)?.let { (s, a, k) ->
                            service = s
                            email = a
                            secret = k
                        }
                    }

                    // Guía visual minimalista sobre la cámara
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(100.dp)
                    )
                } else {
                    Text(
                        "Se requiere permiso de cámara para escanear",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Divisor Estilizado
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
                Text(
                    " O INGRESA MANUAL ",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- FORMULARIO MANUAL ---
            OutlinedTextField(
                value = service,
                onValueChange = { service = it },
                label = { Text("Nombre del Servicio") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo o Usuario") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text("Clave Secreta (Key)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- BOTÓN GUARDAR CON GRADIENTE (Estilo Login) ---
            Button(
                onClick = {
                    val p = Password(serviceName = service, accountEmail = email, secretKey = secret)
                    db.collection("users").document(userId).collection("passwords").document(p.id).set(p)
                    onBack()
                },
                enabled = service.isNotBlank() && secret.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(18.dp)),
                contentPadding = PaddingValues(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                val isEnabled = service.isNotBlank() && secret.isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (isEnabled) Brush.horizontalGradient(listOf(BrandOrange, BrandRust))
                            else Brush.horizontalGradient(listOf(Color.Gray, Color.LightGray)),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("GUARDAR CUENTA", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}