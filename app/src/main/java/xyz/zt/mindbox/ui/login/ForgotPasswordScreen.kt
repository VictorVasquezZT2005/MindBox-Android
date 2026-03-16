package xyz.zt.mindbox.ui.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import xyz.zt.mindbox.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Animación de flotado (igual que en Login)
    val infiniteTransition = rememberInfiniteTransition(label = "iconFlotando")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )

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
            // Fila para el botón de retroceder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // LOGO/ICONO CON EFECTO 3D (Consistente con Login)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer { translationY = floatingOffset }
                    .shadow(20.dp, shape = RoundedCornerShape(30.dp), ambientColor = BrandOrange)
                    .background(
                        brush = Brush.linearGradient(listOf(BrandOrange, BrandRust)),
                        shape = RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (message != null && !isError) Icons.Default.MarkEmailRead else Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Recuperar acceso",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            )

            Text(
                text = "Ingresa tu correo para recibir un enlace de restablecimiento.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // CAMPO DE TEXTO
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; message = null },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // BOTÓN PRINCIPAL CON GRADIENTE
            Button(
                onClick = {
                    loading = true
                    message = null
                    auth.sendPasswordResetEmail(email.trim())
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                isError = false
                                message = "Enlace enviado. Revisa tu bandeja de entrada."
                            } else {
                                isError = true
                                message = task.exception?.localizedMessage ?: "Error al enviar"
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(18.dp)),
                contentPadding = PaddingValues(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = !loading && email.isNotBlank()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(listOf(BrandOrange, BrandRust)),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("ENVIAR ENLACE", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }

            if (message != null) {
                Text(
                    text = message!!,
                    color = if (isError) MaterialTheme.colorScheme.error else BrandOrange,
                    modifier = Modifier.padding(top = 24.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // FOOTER para volver
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Recordaste tu contraseña?")
                TextButton(onClick = onBack) {
                    Text("Inicia sesión", color = BrandOrange, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}