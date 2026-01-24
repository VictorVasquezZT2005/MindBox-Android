package xyz.zt.mindbox.ui.screens.certificates

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
fun EditCertificateScreen(navController: NavController, certificateId: String) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Estados de los campos extraídos del modelo
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    var platformType by remember { mutableStateOf("") }
    var idInput by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var currentPdfUrl by remember { mutableStateOf<String?>(null) }

    // Estados de control de la UI
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Estado del Calendario (DatePicker)
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPdfUri = uri
    }

    // Cargar datos del certificado al iniciar
    LaunchedEffect(certificateId) {
        db.collection("users").document(userId).collection("certificates").document(certificateId)
            .get().addOnSuccessListener { doc ->
                val cert = doc.toObject(Certificate::class.java)
                cert?.let {
                    title = it.title
                    notes = it.notes ?: ""
                    issueDate = it.issueDate.ifBlank { "Seleccionar fecha" }
                    platformType = it.platform
                    idInput = it.folio ?: it.credlyId ?: ""
                    score = it.score ?: ""
                    currentPdfUrl = it.pdfUrl
                }
                isLoading = false
            }.addOnFailureListener {
                Toast.makeText(context, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
    }

    // Diálogo del Calendario
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        issueDate = formatter.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
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
                title = { Text("Editar Detalles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (!isLoading && !isSaving) {
                        IconButton(
                            onClick = {
                                isSaving = true

                                // Función para actualizar Firestore
                                fun finalUpdate(pdfUrl: String?) {
                                    val updatedCert = Certificate(
                                        id = certificateId,
                                        title = title,
                                        platform = platformType,
                                        issueDate = if (issueDate == "Seleccionar fecha") "" else issueDate,
                                        folio = idInput,
                                        score = score,
                                        notes = notes,
                                        pdfUrl = pdfUrl
                                    )

                                    db.collection("users").document(userId)
                                        .collection("certificates").document(certificateId)
                                        .set(updatedCert)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Cambios guardados", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                        .addOnFailureListener { isSaving = false }
                                }

                                // Lógica de PDF: Subir nuevo o mantener actual
                                if (selectedPdfUri != null) {
                                    val fileName = "${title.replace(" ", "_").lowercase()}_${certificateId.take(5)}_rev.pdf"
                                    val storageRef = storage.reference.child("users/$userId/certificates/$fileName")

                                    storageRef.putFile(selectedPdfUri!!).addOnSuccessListener {
                                        storageRef.downloadUrl.addOnSuccessListener { newUrl ->
                                            finalUpdate(newUrl.toString())
                                        }
                                    }.addOnFailureListener { isSaving = false }
                                } else {
                                    finalUpdate(currentPdfUrl)
                                }
                            },
                            enabled = title.isNotBlank()
                        ) {
                            Icon(Icons.Default.Done, "Guardar", tint = colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
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

                // Cabecera informativa (No editable)
                Text("Plataforma: $platformType", style = MaterialTheme.typography.labelLarge, color = colorScheme.primary)

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
                        Icon(Icons.Default.EditCalendar, null, tint = colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Fecha de entrega", style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
                            Text(issueDate, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Documento PDF
                Text("Documento de respaldo", style = MaterialTheme.typography.labelLarge)
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
                            tint = if (selectedPdfUri != null || currentPdfUrl != null) Color(0xFFF44336) else colorScheme.outline
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            val text = if (selectedPdfUri != null) "Nuevo archivo seleccionado"
                            else if (currentPdfUrl != null) "Archivo actual guardado"
                            else "No hay archivo adjunto"
                            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Toca para cambiar", style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}