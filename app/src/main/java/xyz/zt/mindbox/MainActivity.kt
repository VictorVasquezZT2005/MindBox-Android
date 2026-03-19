package xyz.zt.mindbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import xyz.zt.mindbox.utils.AppwriteHelper

class MainActivity : ComponentActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppwriteHelper.init(applicationContext)
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

fun Modifier.colorGlow(
    color: Color,
    radius: Float = 80f,
    alpha: Float = 0.25f,
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(radius, 0f, 4f, color.copy(alpha = alpha).toArgb())
            }
        }
        canvas.drawCircle(
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension / 2f,
            paint = paint
        )
    }
}

@Composable
fun LiquidBottomBar(
    navController: NavHostController,
    currentRoute: String?
) {
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()
    val surface = MaterialTheme.colorScheme.surface

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
            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow sutil detrás
        Box(
            modifier = Modifier
                .height(68.dp)
                .fillMaxWidth()
                .colorGlow(BrandOrange, radius = 40f, alpha = 0.18f)
        )

        // Barra — shadow da cuerpo en modo claro, glass encima
        Box(
            modifier = Modifier
                .height(68.dp)
                .fillMaxWidth()
                // Shadow ANTES de clip para que se vea en modo claro
                .shadow(
                    elevation = if (isDark) 6.dp else 12.dp,
                    shape = CircleShape,
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.30f)
                    else Color.Black.copy(alpha = 0.14f),
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.12f)
                    else Color.Black.copy(alpha = 0.07f)
                )
                .clip(CircleShape)
                // Claro: blanco casi opaco para destacar sobre fondo blanco
                // Oscuro: surface del tema
                .background(
                    if (isDark) surface else Color.White.copy(alpha = 0.93f)
                )
                // Glass: brillo sutil arriba
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.06f else 0.50f),
                            Color.Transparent
                        )
                    )
                )
                // Borde: claro usa negro muy sutil, oscuro usa blanco
                .border(
                    0.8.dp,
                    Brush.verticalGradient(
                        colors = if (isDark) listOf(
                            Color.White.copy(alpha = 0.18f), Color.Transparent
                        ) else listOf(
                            Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.02f)
                        )
                    ),
                    CircleShape
                )
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tabWidth = this.maxWidth / tabs.size
                val velocity = pillPosition.velocity
                val stretchX = 1f + (velocity * 0.0015f).coerceIn(-0.18f, 0.18f)
                val squishY  = 1f - (kotlin.math.abs(velocity) * 0.0004f).coerceIn(0f, 0.12f)

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val tw     = size.width / tabs.size
                            val pad    = 5.dp.toPx()
                            val l      = pillPosition.value * tw + pad
                            val t      = pad
                            val r      = l + tw - pad * 2
                            val b      = size.height - pad
                            val corner = (size.height - pad * 2) / 2f

                            val cx = (l + r) / 2f
                            val cy = (t + b) / 2f
                            val pw = (r - l) * stretchX * pillPressScale.value
                            val ph = (b - t) * squishY  * pillPressScale.value
                            val sl = cx - pw / 2f
                            val st = cy - ph / 2f
                            val sr = cx + pw / 2f
                            val sb = cy + ph / 2f
                            val rr = android.graphics.RectF(sl, st, sr, sb)

                            // Capa 1: relleno naranja
                            drawIntoCanvas { c ->
                                c.nativeCanvas.drawRoundRect(rr, corner, corner,
                                    android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        color = BrandOrange.copy(alpha = 0.35f).toArgb()
                                    })
                            }
                            // Capa 2: brillo glass blanco superior
                            drawIntoCanvas { c ->
                                c.nativeCanvas.drawRoundRect(rr, corner, corner,
                                    android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        setShader(android.graphics.LinearGradient(
                                            sl, st, sl, cy,
                                            intArrayOf(
                                                Color.White.copy(alpha = 0.25f).toArgb(),
                                                android.graphics.Color.TRANSPARENT
                                            ),
                                            null,
                                            android.graphics.Shader.TileMode.CLAMP
                                        ))
                                    })
                            }
                            // Capa 3: borde naranja sutil
                            drawIntoCanvas { c ->
                                c.nativeCanvas.drawRoundRect(rr, corner, corner,
                                    android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        color = BrandOrange.copy(alpha = 0.45f).toArgb()
                                        style = android.graphics.Paint.Style.STROKE
                                        strokeWidth = 1.dp.toPx()
                                    })
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedIndex == index
                        val onBackground = MaterialTheme.colorScheme.onBackground

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) BrandOrange
                            else onBackground.copy(alpha = 0.45f),
                            animationSpec = tween(300),
                            label = "color_$index"
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = 0.5f,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "scale_$index"
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
                                                spring(0.4f, Spring.StiffnessMedium)
                                            )
                                            tryAwaitRelease()
                                            pillPressScale.animateTo(
                                                1f,
                                                spring(0.35f, Spring.StiffnessLow)
                                            )
                                        },
                                        onTap = {
                                            if (!isSelected) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                        inclusive = false
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
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold
                                        else FontWeight.Normal,
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