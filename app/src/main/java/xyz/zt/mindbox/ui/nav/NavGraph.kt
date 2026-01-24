package xyz.zt.mindbox.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import xyz.zt.mindbox.ui.dashboard.screens.dashboard.DashboardScreen
import xyz.zt.mindbox.ui.dashboard.screens.notes.*
import xyz.zt.mindbox.ui.dashboard.screens.reminders.*
import xyz.zt.mindbox.ui.login.ForgotPasswordScreen
import xyz.zt.mindbox.ui.login.LoginScreen
import xyz.zt.mindbox.ui.login.RegisterScreen
import xyz.zt.mindbox.ui.screens.passwords.PasswordsScreen
import xyz.zt.mindbox.ui.screens.passwords.AddPasswordScreen
import xyz.zt.mindbox.ui.screens.profile.ProfileScreen
import xyz.zt.mindbox.ui.screens.certificates.*
import xyz.zt.mindbox.ui.screens.stats.StatsScreen
import xyz.zt.mindbox.ui.screens.documents.DocumentScannerScreen
import xyz.zt.mindbox.ui.screens.profile.ResumeScreen

@Composable
fun MindBoxNavGraph(
    navController: NavHostController,
    isLoggedIn: Boolean,
    notesViewModel: NotesViewModel,
    remindersViewModel: RemindersViewModel,
    onLoginSuccess: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = if (!isLoggedIn) "login" else BottomNavItem.Dashboard.route,
        modifier = modifier
    ) {
        // --- AUTENTICACIÓN ---
        composable("login") {
            LoginScreen(onLoginSuccess = onLoginSuccess, onNavigateToRegister = { navController.navigate("register") }, onNavigateToForgotPassword = { navController.navigate("forgot_password") })
        }
        composable("register") { RegisterScreen(onRegisterSuccess = onLoginSuccess, onBack = { navController.popBackStack() }) }
        composable("forgot_password") { ForgotPasswordScreen(onBack = { navController.popBackStack() }) }

        // --- PANTALLAS PRINCIPALES ---
        composable(BottomNavItem.Dashboard.route) { DashboardScreen(navController, notesViewModel, onLogout) }
        composable(BottomNavItem.Notes.route) { NotesScreen(navController, notesViewModel) }
        composable(BottomNavItem.Reminders.route) { RemindersScreen(navController, remindersViewModel) }
        composable(BottomNavItem.Passwords.route) { PasswordsScreen(navController = navController) }
        composable(BottomNavItem.Profile.route) { ProfileScreen(onLogout = onLogout) }

        // --- NUEVA RUTA: ESTADÍSTICAS (MI RED) ---
        composable("stats") { StatsScreen(navController) }

        // --- FLUJO DE CERTIFICADOS ---
        composable("certificates") { CertificatesScreen(navController) }
        composable("add_certificate") { AddCertificateScreen(navController) }

        composable("document_scanner") { DocumentScannerScreen(navController) }

        composable("resume") { ResumeScreen(navController = navController) }

        composable(
            route = "certificate_detail/{certId}",
            arguments = listOf(navArgument("certId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("certId") ?: ""
            CertificateDetailScreen(navController, id)
        }

        composable(
            route = "edit_certificate/{certId}",
            arguments = listOf(navArgument("certId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("certId") ?: ""
            EditCertificateScreen(navController, id)
        }

        // --- FLUJO DE CONTRASEÑAS (2FA) ---
        composable("add_password") { AddPasswordScreen(onBack = { navController.popBackStack() }) }

        // --- FLUJO DE NOTAS ---
        composable("new_note") { NewNoteScreen(navController, notesViewModel) }
        composable("note_detail/{noteId}", arguments = listOf(navArgument("noteId") { type = NavType.StringType })) {
            val id = it.arguments?.getString("noteId") ?: ""
            NoteDetailScreen(navController, notesViewModel, id)
        }

        // --- FLUJO DE RECORDATORIOS ---
        composable("add_reminder") { AddReminderScreen(onBack = { navController.popBackStack() }, viewModel = remindersViewModel) }
        composable("reminder_detail/{reminderId}", arguments = listOf(navArgument("reminderId") { type = NavType.StringType })) {
            val id = it.arguments?.getString("reminderId") ?: ""
            ReminderDetailScreen(navController, remindersViewModel, id)
        }
    }
}