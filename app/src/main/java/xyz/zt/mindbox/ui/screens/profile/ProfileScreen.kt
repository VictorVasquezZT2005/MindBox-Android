package xyz.zt.mindbox.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import xyz.zt.mindbox.R
import xyz.zt.mindbox.ui.theme.*

fun Modifier.liquidGlassBackground(
    color: Color,
    surfaceColor: Color,
    shape: Shape,
    glassAlpha: Float = 0.13f,
    borderAlpha: Float = 0.35f,
    elevation: Dp = 8.dp,
): Modifier = this
    .shadow(elevation, shape,
        spotColor = color.copy(alpha = 0.18f),
        ambientColor = color.copy(alpha = 0.08f))
    .background(surfaceColor, shape)
    .background(
        Brush.verticalGradient(listOf(
            color.copy(alpha = glassAlpha + 0.04f),
            color.copy(alpha = glassAlpha * 0.3f)
        )), shape)
    .border(0.8.dp,
        Brush.linearGradient(listOf(
            Color.White.copy(alpha = borderAlpha),
            color.copy(alpha = 0.15f),
            Color.Transparent
        ), start = Offset(0f, 0f), end = Offset(400f, 400f)),
        shape)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, onLogout: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current

    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground

    val fieldBg = if (isDark) surface else Color.White
    val fieldBorder = if (isDark) onBackground.copy(alpha = 0.12f) else Color(0xFFDDDDDD)

    val infiniteTransition = rememberInfiniteTransition(label = "avatarFlotando")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )

    fun saveUserData() {
        val currentUser = auth.currentUser ?: return
        isLoading = true
        val profileUpdates = userProfileChangeRequest { displayName = name }
        currentUser.updateProfile(profileUpdates).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                if (password.isNotEmpty()) {
                    currentUser.updatePassword(password).addOnFailureListener {
                        Toast.makeText(context, "Error al actualizar contraseña. Re-inicia sesión.", Toast.LENGTH_LONG).show()
                    }
                }
                val userData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "lastUpdate" to com.google.firebase.Timestamp.now()
                )
                db.collection("users").document(currentUser.uid)
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        isLoading = false
                        isEditing = false
                        password = ""
                        Toast.makeText(context, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        isLoading = false
                        Toast.makeText(context, "Error al guardar en base de datos", Toast.LENGTH_SHORT).show()
                    }
            } else {
                isLoading = false
                Toast.makeText(context, "Error al actualizar nombre de usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ── BARRA SUPERIOR ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) surface.copy(alpha = 0.40f)
                            else onBackground.copy(alpha = 0.07f)
                        )
                        .background(Brush.verticalGradient(listOf(
                            Color.White.copy(alpha = if (isDark) 0.10f else 0.45f),
                            Color.Transparent
                        )))
                        .border(0.8.dp, Brush.linearGradient(listOf(
                            onBackground.copy(alpha = if (isDark) 0.22f else 0.12f),
                            Color.Transparent
                        ), Offset(0f, 0f), Offset(80f, 80f)), CircleShape)
                        .clickable { navController.navigateUp() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ChevronLeft, "Volver",
                        tint = onBackground, modifier = Modifier.size(26.dp))
                }

                Text("Mi Perfil",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold))

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BrandOrange.copy(alpha = if (isEditing) 0.75f else 0.12f))
                        .background(Brush.verticalGradient(listOf(
                            Color.White.copy(alpha = if (isEditing) 0.20f else 0.08f),
                            Color.Transparent
                        )))
                        .border(0.8.dp, Brush.linearGradient(listOf(
                            Color.White.copy(alpha = if (isEditing) 0.40f else 0.20f),
                            Color.Transparent
                        ), Offset(0f, 0f), Offset(80f, 80f)), CircleShape)
                        .clickable { if (isEditing) saveUserData() else isEditing = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = if (isEditing) Color.White else BrandOrange)
                    } else {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            if (isEditing) "Guardar" else "Editar",
                            tint = if (isEditing) Color.White else BrandOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── AVATAR ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer { translationY = floatingOffset }
                    .colorGlow(BrandOrange, radius = 60f, alpha = 0.30f)
                    .shadow(16.dp, CircleShape,
                        spotColor = BrandOrange.copy(alpha = 0.25f),
                        ambientColor = BrandOrange.copy(alpha = 0.10f))
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = R.drawable.ic_profile),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (name.isNotEmpty()) "¡Hola, $name!" else "Tu Perfil",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(text = email,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = onBackground.copy(alpha = 0.55f)))

            Spacer(Modifier.height(40.dp))

            // ── CAMPOS con label ARRIBA ────────────────────────────────
            LiquidTextField(
                value = name, onValueChange = { name = it },
                label = "Nombre Completo", enabled = isEditing,
                leadingIcon = {
                    Icon(Icons.Default.Person, null,
                        tint = BrandOrange.copy(alpha = if (isEditing) 1f else 0.45f),
                        modifier = Modifier.size(20.dp))
                },
                fieldBg = fieldBg, fieldBorder = fieldBorder,
                isEditing = isEditing, onBackground = onBackground
            )

            Spacer(Modifier.height(14.dp))

            LiquidTextField(
                value = email, onValueChange = {},
                label = "Correo Electrónico", enabled = false,
                leadingIcon = {
                    Icon(Icons.Default.Email, null,
                        tint = BrandOrange.copy(alpha = 0.40f),
                        modifier = Modifier.size(20.dp))
                },
                fieldBg = fieldBg, fieldBorder = fieldBorder,
                isEditing = false, onBackground = onBackground
            )

            Spacer(Modifier.height(14.dp))

            LiquidTextField(
                value = password, onValueChange = { password = it },
                label = "Nueva Contraseña", enabled = isEditing,
                placeholder = "Dejar en blanco para mantener",
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                leadingIcon = {
                    Icon(Icons.Default.Lock, null,
                        tint = BrandOrange.copy(alpha = if (isEditing) 1f else 0.45f),
                        modifier = Modifier.size(20.dp))
                },
                trailingIcon = if (isEditing) ({
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                            .background(BrandOrange.copy(alpha = 0.10f))
                            .clickable { passwordVisible = !passwordVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            null, tint = onBackground.copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp))
                    }
                }) else null,
                fieldBg = fieldBg, fieldBorder = fieldBorder,
                isEditing = isEditing, onBackground = onBackground
            )

            Spacer(Modifier.height(48.dp))

            // ── CERRAR SESIÓN ──────────────────────────────────────────
            val errorColor = MaterialTheme.colorScheme.error
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(58.dp)
                    .colorGlow(errorColor, radius = 40f, alpha = 0.20f)
                    .liquidGlassBackground(
                        color = errorColor, surfaceColor = surface,
                        shape = RoundedCornerShape(18.dp),
                        glassAlpha = 0.08f, borderAlpha = 0.30f, elevation = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onLogout() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, null,
                        tint = errorColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("CERRAR SESIÓN", fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp, color = errorColor, letterSpacing = 0.5.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  LiquidTextField — label ARRIBA del campo como texto separado
// ─────────────────────────────────────────────────────────────
@Composable
fun LiquidTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    fieldBg: Color,
    fieldBorder: Color,
    isEditing: Boolean,
    onBackground: Color,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)

    Column {
        // Label arriba — limpio, sin flotar sobre el borde
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.3.sp
            ),
            color = if (isEditing) BrandOrange.copy(alpha = 0.85f)
            else onBackground.copy(alpha = 0.45f),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = if (placeholder.isNotEmpty()) ({
                Text(placeholder, color = onBackground.copy(alpha = 0.30f))
            }) else null,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isEditing) 6.dp else 2.dp,
                    shape = shape,
                    spotColor = if (isEditing) BrandOrange.copy(alpha = 0.12f)
                    else Color.Black.copy(alpha = 0.04f),
                    ambientColor = Color.Black.copy(alpha = 0.03f)
                ),
            shape = shape,
            singleLine = true,
            // Sin label dentro del campo — ya está arriba
            label = null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = fieldBg,
                unfocusedContainerColor = fieldBg,
                disabledContainerColor  = fieldBg,
                errorContainerColor     = fieldBg,
                focusedBorderColor      = BrandOrange.copy(alpha = 0.65f),
                unfocusedBorderColor    = fieldBorder,
                disabledBorderColor     = fieldBorder.copy(alpha = 0.55f),
                focusedTextColor        = onBackground,
                unfocusedTextColor      = onBackground,
                disabledTextColor       = onBackground.copy(alpha = 0.55f),
                cursorColor             = BrandOrange,
                selectionColors         = TextSelectionColors(
                    handleColor     = BrandOrange,
                    backgroundColor = BrandOrange.copy(alpha = 0.20f)
                ),
            )
        )
    }
}