package xyz.zt.mindbox.ui.screens.passwords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import xyz.zt.mindbox.data.model.Password
import xyz.zt.mindbox.utils.TOTPHelper
import xyz.zt.mindbox.ui.theme.* @Composable
fun OtpCard(
    password: Password,
    ticks: Int,
    onDeleteRequest: () -> Unit
) {
    val code = remember(ticks) {
        if (password.secretKey.isNotBlank()) {
            try {
                TOTPHelper.generateCode(password.secretKey)
            } catch (e: Exception) { "000000" }
        } else "000000"
    }

    var progressFactor by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            val ms = System.currentTimeMillis() % 30000
            progressFactor = 1f - (ms / 30000f)
            delay(50)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = BrandOrange.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Información del servicio
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = password.serviceName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                )
                if (password.accountEmail.isNotBlank()) {
                    Text(
                        text = password.accountEmail,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // Área del Código corregida para Modo Oscuro
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val formatted = if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code

                    // Usamos BrandOrange para el texto, o Rojo si está por expirar
                    Text(
                        text = formatted,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (progressFactor < 0.2f) Color.Red else BrandOrange,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Barra de progreso sincronizada
                    LinearProgressIndicator(
                        progress = { progressFactor },
                        modifier = Modifier
                            .width(50.dp)
                            .height(4.dp)
                            .clip(CircleShape),
                        color = if (progressFactor < 0.2f) Color.Red else BrandOrange,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Botón eliminar con contraste mejorado
                IconButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}