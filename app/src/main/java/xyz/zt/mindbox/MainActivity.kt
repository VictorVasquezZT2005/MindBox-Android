package xyz.zt.mindbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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
                        if (isLoggedIn && currentRoute in mainRoutes) {
                            LiquidBottomBar(navController, currentRoute)
                        }
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
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

@Composable
fun LiquidBottomBar(
    navController: NavHostController,
    currentRoute: String?
) {
    val haptic = LocalHapticFeedback.current
    val tabs = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Notes,
        BottomNavItem.Reminders,
        BottomNavItem.Passwords
    )

    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val pillPosition = remember { Animatable(selectedIndex.toFloat()) }
    val pillPressScale = remember { Animatable(1f) }

    LaunchedEffect(selectedIndex) {
        pillPosition.animateTo(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .height(68.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    spotColor = BrandOrange.copy(alpha = 0.3f)
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
                )
            )
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tabWidth = this.maxWidth / tabs.size

                val velocity = pillPosition.velocity
                val stretchX = 1f + (velocity * 0.0015f).coerceIn(-0.18f, 0.18f)
                val squishY = 1f - (kotlin.math.abs(velocity) * 0.0004f).coerceIn(0f, 0.12f)

                // --- INDICADOR LÍQUIDO ---
                Box(
                    modifier = Modifier
                        .offset(x = tabWidth * pillPosition.value)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = stretchX * pillPressScale.value,
                                scaleY = squishY * pillPressScale.value
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        BrandOrange.copy(alpha = 0.22f),
                                        BrandOrange.copy(alpha = 0.06f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(1.dp, BrandOrange.copy(alpha = 0.35f), CircleShape)
                    )
                }

                // --- ICONOS ---
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedIndex == index

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) BrandOrange else Color.Gray.copy(alpha = 0.6f),
                            animationSpec = tween(400),
                            label = "tab_color_$index"
                        )

                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = 0.5f,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "icon_scale_$index"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .pointerInput(index) {
                                    detectTapGestures(
                                        onPress = {
                                            pillPressScale.animateTo(
                                                0.88f,
                                                spring(
                                                    dampingRatio = 0.4f,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                            tryAwaitRelease()
                                            pillPressScale.animateTo(
                                                1f,
                                                spring(
                                                    dampingRatio = 0.35f,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        },
                                        onTap = {
                                            if (!isSelected) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = contentColor,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer(
                                            scaleX = iconScale,
                                            scaleY = iconScale,
                                            translationY = if (isSelected) (-2).dp.value else 0f
                                        )
                                )

                                // ✅ Labels siempre visibles, solo cambian color y peso
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = contentColor,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}