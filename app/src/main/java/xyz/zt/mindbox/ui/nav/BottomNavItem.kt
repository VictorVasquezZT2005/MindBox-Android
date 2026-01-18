package xyz.zt.mindbox.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    // Iconos estilo 'Outlined' o 'Rounded' para un look más limpio
    object Dashboard : BottomNavItem("Inicio", Icons.Rounded.GridView, "dashboard")

    // 'Description' o 'EditNote' se ven mejor para libretas que el icono de 'Note' simple
    object Notes : BottomNavItem("Notas", Icons.Rounded.Description, "notes")

    // 'Notifications' es el estándar moderno para recordatorios/alertas
    object Reminders : BottomNavItem("Alertas", Icons.Rounded.NotificationsActive, "reminders")

    // 'Fingerprint' o 'VpnKey' sugieren más seguridad que un simple candado
    object Passwords : BottomNavItem("Llaves", Icons.Rounded.VpnKey, "passwords")

    object Profile : BottomNavItem("Perfil", Icons.Rounded.AccountCircle, "profile")
}