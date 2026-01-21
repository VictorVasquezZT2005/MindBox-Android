package xyz.zt.mindbox.ui.dashboard.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import xyz.zt.mindbox.ui.dashboard.screens.notes.NotesViewModel
import xyz.zt.mindbox.ui.nav.BottomNavItem

@Composable
fun DashboardScreen(
    navController: NavController,
    notesViewModel: NotesViewModel,
    onLogout: () -> Unit
) {
    // Obtenemos los datos del usuario actual de Firebase
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Usuario"

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Espaciador superior para alinear con el resto de pantallas
            Spacer(modifier = Modifier.height(56.dp))

            // --- CABECERA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MindBox",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { navController.navigate(BottomNavItem.Profile.route) }) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = "Perfil",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BIENVENIDA ---
            Text(
                text = "Hola, $userName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tienes ${notesViewModel.notes.size} notas guardadas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Accesos rápidos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // --- GRID DE ACCIONES RÁPIDAS ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Acción: Nueva Nota
                item {
                    QuickActionCard(
                        title = "Nueva Nota",
                        icon = Icons.Rounded.Add,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { navController.navigate("new_note") }
                    )
                }

                // Acción: Recordatorios
                item {
                    QuickActionCard(
                        title = "Recordatorio",
                        icon = Icons.Rounded.NotificationsActive,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = { navController.navigate(BottomNavItem.Reminders.route) }
                    )
                }

                // Acción: Nueva Contraseña (Acceso directo al diálogo)
                item {
                    QuickActionCard(
                        title = "Contraseña",
                        icon = Icons.Rounded.VpnKey,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = {
                            // Navega a Passwords pasándole el parámetro para abrir el diálogo de creación
                            navController.navigate("${BottomNavItem.Passwords.route}?showAdd=true")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}