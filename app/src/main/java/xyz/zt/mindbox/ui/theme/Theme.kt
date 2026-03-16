package xyz.zt.mindbox.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // Este es el Color de Compose
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    secondary = BrandRust,
    tertiary = BrandDeepBlue,
    background = DeepBlack,
    surface = Color(0xFF162024),
    onPrimary = DeepBlack,
    onBackground = Color.White, // Ahora sí encontrará White
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BrandDeepBlue,
    secondary = BrandOrange,
    tertiary = BrandRust,
    background = SoftWhite,
    surface = SoftWhite,
    onPrimary = Color.White,
    onBackground = BrandDeepBlue,
    onSurface = BrandDeepBlue
)

@Composable
fun MindBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // USAMOS LA REFERENCIA EXPLÍCITA PARA EL SISTEMA:
            // android.graphics.Color.TRANSPARENT (es un Int, no un objeto Color de Compose)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}