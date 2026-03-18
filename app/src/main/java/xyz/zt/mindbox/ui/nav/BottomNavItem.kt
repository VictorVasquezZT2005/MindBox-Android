package xyz.zt.mindbox.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    object Dashboard : BottomNavItem("Inicio", Icons.Rounded.Home, "dashboard")

    object Notes : BottomNavItem("Notas", Icons.Rounded.Description, "notes")

    object Reminders : BottomNavItem("Alertas", Icons.Rounded.NotificationsActive, "reminders")

    object Passwords : BottomNavItem("Llaves", Icons.Rounded.VpnKey, "passwords")

    object Profile : BottomNavItem("Perfil", Icons.Rounded.AccountCircle, "profile")
}