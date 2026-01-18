package xyz.zt.mindbox.ui.screens.passwords

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.zt.mindbox.data.local.PasswordEntity
import xyz.zt.mindbox.utils.TOTPHelper

@Composable
fun OtpCard(password: PasswordEntity, ticks: Int) {

    val code = remember(ticks) {
        TOTPHelper.generateCode(password.secretKey)
    }

    val progress by animateFloatAsState(
        targetValue = TOTPHelper.getProgress(),
        label = "otp_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    password.serviceName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                if (password.accountEmail.isNotBlank()) {
                    Text(
                        password.accountEmail,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {

                val formatted =
                    if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}"
                    else code

                Text(
                    formatted,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(6.dp))

                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 3.dp,
                    color = if (progress < 0.2f) Color.Red
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}
