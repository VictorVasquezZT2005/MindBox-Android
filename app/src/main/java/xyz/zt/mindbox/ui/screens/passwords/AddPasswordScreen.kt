package xyz.zt.mindbox.ui.screens.passwords

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var isLoading by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    val infiniteTransition = rememberInfiniteTransition(label = "scannerFloating")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )

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
            // CABECERA (Centrada, sin el botón de la palomita)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }

                Text(
                    text = "Nuevo Acceso",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ESCÁNER CON EFECTO FLOTANTE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .graphicsLayer { translationY = floatingOffset }
                    .shadow(15.dp, RoundedCornerShape(28.dp), ambientColor = BrandOrange)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black)
                    .border(2.dp, Brush.linearGradient(listOf(BrandOrange, BrandRust)), RoundedCornerShape(28.dp)),
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
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    )
                } else {
                    Text("Cámara requerida", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Configuración de Cuenta",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Text(
                text = "Escanea el código QR o ingresa los datos",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = service,
                onValueChange = { service = it },
                label = { Text("Nombre del Servicio") },
                placeholder = { Text("Ej: Google, GitHub...") },
                leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null, tint = BrandOrange) },
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
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandOrange) },
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
                leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null, tint = BrandOrange) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            // BOTÓN GUARDAR
            Button(
                onClick = {
                    isLoading = true
                    val p = Password(serviceName = service, accountEmail = email, secretKey = secret)
                    db.collection("users").document(userId).collection("passwords").document(p.id).set(p)
                        .addOnSuccessListener { onBack() }
                },
                enabled = service.isNotBlank() && secret.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(18.dp)),
                contentPadding = PaddingValues(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                val isEnabled = service.isNotBlank() && secret.isNotBlank() && !isLoading
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (isEnabled) Brush.horizontalGradient(listOf(BrandOrange, BrandRust))
                            else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.5f), Color.LightGray.copy(alpha = 0.5f))),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("GUARDAR CUENTA", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}