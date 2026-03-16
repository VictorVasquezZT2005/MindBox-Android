package xyz.zt.mindbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import xyz.zt.mindbox.ui.nav.BottomNavItem
import xyz.zt.mindbox.ui.nav.MindBoxNavGraph
import xyz.zt.mindbox.ui.theme.*
import xyz.zt.mindbox.ui.dashboard.screens.notes.NotesViewModel
import xyz.zt.mindbox.ui.dashboard.screens.reminders.RemindersViewModel

class MainActivity : ComponentActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MindBoxTheme {
                val navController = rememberNavController()
                val notesViewModel: NotesViewModel = viewModel()
                val remindersViewModel: RemindersViewModel = viewModel()

                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val mainRoutes = listOf(
                    BottomNavItem.Dashboard.route,
                    BottomNavItem.Notes.route,
                    BottomNavItem.Reminders.route,
                    BottomNavItem.Passwords.route
                )

                Scaffold(
                    bottomBar = {
                        // Solo mostramos la barra si está logueado y en una ruta principal
                        if (isLoggedIn && currentRoute in mainRoutes) {
                            // CONTENEDOR FLOTANTE PARA EL NAVBAR
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                NavigationBar(
                                    modifier = Modifier
                                        .height(72.dp)
                                        .shadow(
                                            elevation = 15.dp,
                                            shape = RoundedCornerShape(24.dp),
                                            ambientColor = BrandOrange.copy(alpha = 0.5f)
                                        )
                                        .clip(RoundedCornerShape(24.dp)),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp
                                ) {
                                    val tabs = listOf(
                                        BottomNavItem.Dashboard,
                                        BottomNavItem.Notes,
                                        BottomNavItem.Reminders,
                                        BottomNavItem.Passwords
                                    )

                                    tabs.forEach { tab ->
                                        val isSelected = currentRoute == tab.route

                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = tab.title,
                                                    tint = if (isSelected) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = tab.title,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = BrandOrange.copy(alpha = 0.12f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0) // Manejamos los insets manualmente para el estilo flotante
                ) { padding ->
                    MindBoxNavGraph(
                        navController = navController,
                        isLoggedIn = isLoggedIn,
                        notesViewModel = notesViewModel,
                        remindersViewModel = remindersViewModel,
                        onLoginSuccess = {
                            isLoggedIn = true
                            navController.navigate(BottomNavItem.Dashboard.route) {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onLogout = {
                            auth.signOut()
                            isLoggedIn = false
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}