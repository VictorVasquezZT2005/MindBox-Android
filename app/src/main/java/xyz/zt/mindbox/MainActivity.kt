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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import xyz.zt.mindbox.ui.login.LoginScreen
import xyz.zt.mindbox.ui.login.RegisterScreen
import xyz.zt.mindbox.ui.login.ForgotPasswordScreen
import xyz.zt.mindbox.ui.theme.MindBoxTheme
import xyz.zt.mindbox.ui.nav.BottomNavItem
import xyz.zt.mindbox.ui.dashboard.screens.notes.*
import xyz.zt.mindbox.ui.screens.passwords.PasswordsScreen
import xyz.zt.mindbox.ui.dashboard.screens.reminders.RemindersScreen
import xyz.zt.mindbox.ui.dashboard.screens.dashboard.DashboardScreen
import xyz.zt.mindbox.ui.screens.profile.ProfileScreen

class MainActivity : ComponentActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MindBoxTheme {
                val navController = rememberNavController()
                val notesViewModel: NotesViewModel = viewModel()
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val mainRoutes = listOf(
                    BottomNavItem.Dashboard.route,
                    BottomNavItem.Notes.route,
                    BottomNavItem.Reminders.route,
                    BottomNavItem.Passwords.route,
                    BottomNavItem.Profile.route
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
                    NavHost(
                        navController = navController,
                        startDestination = if (!isLoggedIn) "login" else BottomNavItem.Dashboard.route,
                        modifier = Modifier.padding(padding)
                    ) {
                        // --- AUTH ---
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    isLoggedIn = true
                                    navController.navigate(BottomNavItem.Dashboard.route) {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = { navController.navigate("register") },
                                onNavigateToForgotPassword = { navController.navigate("forgot_password") }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    isLoggedIn = true
                                    navController.navigate(BottomNavItem.Dashboard.route) {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("forgot_password") {
                            ForgotPasswordScreen(onBack = { navController.popBackStack() })
                        }

                        // --- MAIN ---
                        composable(BottomNavItem.Dashboard.route) {
                            DashboardScreen(navController, notesViewModel) {
                                auth.signOut()
                                isLoggedIn = false
                                navController.navigate("login") { popUpTo(0) }
                            }
                        }

                        composable(BottomNavItem.Notes.route) {
                            NotesScreen(navController, notesViewModel)
                        }

                        composable(BottomNavItem.Reminders.route) { RemindersScreen() }
                        composable(BottomNavItem.Passwords.route) { PasswordsScreen() }

                        composable(BottomNavItem.Profile.route) {
                            ProfileScreen(onLogout = {
                                auth.signOut()
                                isLoggedIn = false
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            })
                        }

                        // --- PANTALLAS DE NOTAS ---
                        composable("new_note") {
                            NewNoteScreen(navController, notesViewModel)
                        }

                        // NUEVA RUTA: Detalle de nota (acepta ID como argumento)
                        composable(
                            route = "note_detail/{noteId}",
                            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                            NoteDetailScreen(navController, notesViewModel, noteId)
                        }
                    }
                }
            }
        }
    }
}