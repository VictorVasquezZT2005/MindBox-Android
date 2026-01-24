package xyz.zt.mindbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import xyz.zt.mindbox.ui.nav.BottomNavItem
import xyz.zt.mindbox.ui.nav.MindBoxNavGraph
import xyz.zt.mindbox.ui.theme.MindBoxTheme
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
                        if (isLoggedIn && currentRoute in mainRoutes) {
                            NavigationBar {
                                val tabs = listOf(
                                    BottomNavItem.Dashboard,
                                    BottomNavItem.Notes,
                                    BottomNavItem.Reminders,
                                    BottomNavItem.Passwords
                                )
                                tabs.forEach { tab ->
                                    NavigationBarItem(
                                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                                        label = { Text(tab.title) },
                                        selected = currentRoute == tab.route,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets.navigationBars
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
                            navController.navigate("login") { popUpTo(0) { inclusive = true } }
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
        }
    }