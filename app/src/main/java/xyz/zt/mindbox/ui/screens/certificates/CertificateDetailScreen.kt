package xyz.zt.mindbox.ui.screens.certificates

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import xyz.zt.mindbox.data.model.Certificate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateDetailScreen(navController: NavController, certId: String) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var certificate by remember { mutableStateOf<Certificate?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(certId) {
        db.collection("users").document(userId).collection("certificates").document(certId)
            .get().addOnSuccessListener { doc ->
                certificate = doc.toObject(Certificate::class.java)
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalle del Logro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("edit_certificate/$certId") }) {
                        Icon(Icons.Default.Edit, "Editar", tint = colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            certificate?.let { cert ->
                val badgeColor = when (cert.platform) {
                    "Credly" -> Color(0xFF2196F3)
                    "Carlos Slim" -> Color(0xFF4CAF50)
                    else -> colorScheme.primary
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(20.dp))

                    // Halo de la Insignia
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(badgeColor.copy(alpha = 0.05f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = badgeColor.copy(alpha = 0.1f),
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Verified,
                                    null,
                                    modifier = Modifier.size(60.dp),
                                    tint = badgeColor
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = cert.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = cert.platform,
                        style = MaterialTheme.typography.titleMedium,
                        color = badgeColor,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(40.dp))

                    // TARJETA DE DATOS TÉCNICOS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {

                            // ID del Logro (Dinámico)
                            InfoSection(
                                label = "ID DEL LOGRO / FOLIO",
                                value = if (!cert.folio.isNullOrBlank()) cert.folio
                                else if (!cert.credlyId.isNullOrBlank()) cert.credlyId
                                else "No registrado"
                            )

                            // --- FECHA DE ENTREGA ---
                            if (!cert.issueDate.isNullOrBlank()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = colorScheme.outline.copy(alpha = 0.2f)
                                )
                                InfoSection(
                                    label = "FECHA DE EMISIÓN",
                                    value = cert.issueDate
                                )
                            }

                            // Calificación (Opcional)
                            if (!cert.score.isNullOrBlank()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = colorScheme.outline.copy(alpha = 0.2f)
                                )
                                InfoSection(
                                    label = "CALIFICACIÓN",
                                    value = cert.score!!
                                )
                            }
                        }
                    }

                    // --- SECCIÓN DE PDF ---
                    if (!cert.pdfUrl.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedCard(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cert.pdfUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFF44336), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Ver Certificado PDF", fontWeight = FontWeight.Bold)
                                    Text("Toca para abrir documento", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.OpenInNew, null, tint = colorScheme.outline, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // SECCIÓN DE NOTAS
                    if (!cert.notes.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    "DESCRIPCIÓN Y NOTAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = cert.notes!!,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            } ?: run {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Certificado no encontrado")
                }
            }
        }
    }
}

// Componente reutilizable para los items de la tarjeta
@Composable
fun InfoSection(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}