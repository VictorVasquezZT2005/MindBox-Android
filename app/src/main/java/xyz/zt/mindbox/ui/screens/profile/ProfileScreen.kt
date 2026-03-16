package xyz.zt.mindbox.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import xyz.zt.mindbox.R
import xyz.zt.mindbox.ui.theme.*

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

    // Animación de flotado para el avatar
    val infiniteTransition = rememberInfiniteTransition(label = "avatarFlotando")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )

    fun saveUserData() {
        val currentUser = auth.currentUser ?: return
        isLoading = true

        // 1. Actualizar Perfil en Firebase Auth (DisplayName)
        val profileUpdates = userProfileChangeRequest {
            displayName = name
        }

        currentUser.updateProfile(profileUpdates).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {

                // 2. ¿Actualizar Contraseña? Solo si el usuario escribió algo
                if (password.isNotEmpty()) {
                    currentUser.updatePassword(password).addOnFailureListener {
                        Toast.makeText(context, "Error al actualizar contraseña. Re-inicia sesión.", Toast.LENGTH_LONG).show()
                    }
                }

                // 3. Actualizar Datos en Firestore
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
                        password = "" // Limpiar el campo de contraseña por seguridad
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BARRA SUPERIOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }

                Text(
                    text = "Mi Perfil",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (isEditing) {
                    IconButton(
                        onClick = { saveUserData() },
                        enabled = !isLoading,
                        modifier = Modifier.background(BrandOrange.copy(alpha = 0.1f), CircleShape)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BrandOrange
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Guardar", tint = BrandOrange)
                        }
                    }
                } else {
                    IconButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AVATAR CON EFECTO FLOTANTE
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer { translationY = floatingOffset }
                    .shadow(15.dp, CircleShape, ambientColor = BrandOrange)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_profile),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (name.isNotEmpty()) "¡Hola, $name!" else "Tu Perfil",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // CAMPOS DE TEXTO
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre Completo") },
                enabled = isEditing,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandOrange) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                enabled = false,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandOrange.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Nueva Contraseña") },
                enabled = isEditing,
                placeholder = { Text("Dejar en blanco para mantener") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandOrange) },
                trailingIcon = {
                    if (isEditing) {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            // BOTÓN DE CERRAR SESIÓN
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("CERRAR SESIÓN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}