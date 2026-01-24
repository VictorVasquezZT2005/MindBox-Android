package xyz.zt.mindbox.ui.screens.certificates

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll // IMPORTANTE: Este faltaba
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import xyz.zt.mindbox.data.model.Certificate
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCertificateScreen(navController: NavController) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Estados del formulario
    var title by remember { mutableStateOf("") }
    var platformType by remember { mutableStateOf("Carlos Slim") }
    var customPlatform by remember { mutableStateOf("") }
    var idInput by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }

    // Estados de control y fecha
    var isSaving by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedDateText by remember { mutableStateOf("Seleccionar fecha") }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPdfUri = uri
    }

    // Lógica del Diálogo de Fecha (Calendario Material You)
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = datePickerState.selectedDateMillis
                    if (dateMillis != null) {
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        selectedDateText = formatter.format(Date(dateMillis))
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Registrar Logro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, "Cerrar")
                    }
                },
                actions = {
                    if (!isSaving) {
                        IconButton(
                            onClick = {
                                isSaving = true
                                val finalPlatform = if (platformType == "Otro") customPlatform else platformType
                                val certId = UUID.randomUUID().toString()

                                // Función para guardar en Firestore
                                fun uploadAndSave(pdfUrl: String?) {
                                    val cert = Certificate(
                                        id = certId,
                                        title = title,
                                        platform = finalPlatform,
                                        issueDate = if (selectedDateText == "Seleccionar fecha") "" else selectedDateText,
                                        folio = idInput,
                                        score = score,
                                        notes = notes,
                                        pdfUrl = pdfUrl
                                    )

                                    db.collection("users").document(userId)
                                        .collection("certificates").document(certId)
                                        .set(cert)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Logro guardado", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                        .addOnFailureListener {
                                            isSaving = false
                                            Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                        }
                                }

                                if (selectedPdfUri != null) {
                                    val fileName = "${title.replace(" ", "_").lowercase()}_${certId.take(5)}.pdf"
                                    val storageRef = storage.reference.child("users/$userId/certificates/$fileName")

                                    storageRef.putFile(selectedPdfUri!!)
                                        .addOnSuccessListener {
                                            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                                uploadAndSave(downloadUrl.toString())
                                            }
                                        }
                                        .addOnFailureListener {
                                            isSaving = false
                                            Toast.makeText(context, "Error al subir PDF", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    uploadAndSave(null)
                                }
                            },
                            enabled = title.isNotBlank() && !isSaving
                        ) {
                            Icon(Icons.Default.Done, "Guardar", tint = colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colorScheme.surface)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            // Sección de Plataforma con horizontalScroll corregido
            Text("Plataforma / Emisor", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("Carlos Slim", "Credly", "Otro").forEach { option ->
                    FilterChip(
                        selected = platformType == option,
                        onClick = { platformType = option },
                        label = { Text(option) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            if (platformType == "Otro") {
                OutlinedTextField(
                    value = customPlatform,
                    onValueChange = { customPlatform = it },
                    label = { Text("Nombre de la Empresa") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Título del curso
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título del curso") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // --- SELECTOR DE FECHA (UI) ---
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Fecha de entrega", style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
                        Text(
                            text = selectedDateText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedDateText == "Seleccionar fecha") FontWeight.Normal else FontWeight.Bold
                        )
                    }
                }
            }

            // Folio e ID
            OutlinedTextField(
                value = idInput,
                onValueChange = { idInput = it },
                label = { Text("Folio / ID de Certificado") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // PDF
            Text("Archivo de respaldo", style = MaterialTheme.typography.labelLarge)
            OutlinedCard(
                onClick = { pdfLauncher.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        null,
                        tint = if (selectedPdfUri != null) Color(0xFFF44336) else colorScheme.outline
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (selectedPdfUri != null) "PDF seleccionado" else "Seleccionar archivo PDF",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Notas
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas adicionales") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}