package xyz.zt.mindbox.ui.screens.passwords

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import xyz.zt.mindbox.data.model.Password
import xyz.zt.mindbox.utils.TOTPHelper

@Composable
fun OtpCard(
    password: Password,
    ticks: Int,
    onDeleteRequest: () -> Unit
) {
    // Generación del código OTP sincronizado con los ticks del sistema
    val code = remember(ticks) {
        if (password.secretKey.isNotBlank()) {
            try {
                TOTPHelper.generateCode(password.secretKey)
            } catch (e: Exception) { "000000" }
        } else "000000"
    }

    // Estado para el progreso fluido (0.0 a 1.0)
    var progressFactor by remember { mutableFloatStateOf(1f) }

    // Hilo de actualización constante para la barra de progreso
    LaunchedEffect(Unit) {
        while (true) {
            val ms = System.currentTimeMillis() % 30000
            progressFactor = 1f - (ms / 30000f)
            delay(50) // 20 actualizaciones por segundo para fluidez total
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Información del servicio (Izquierda)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = password.serviceName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (password.accountEmail.isNotBlank()) {
                    Text(
                        text = password.accountEmail,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Lógica y botones (Derecha)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val formatted = if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code

                    Text(
                        text = formatted,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // INDICADOR CIRCULAR CORREGIDO
                    CircularProgressIndicator(
                        progress = { progressFactor },
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = if (progressFactor < 0.2f) Color.Red else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Botón de eliminar
                IconButton(onClick = onDeleteRequest) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}