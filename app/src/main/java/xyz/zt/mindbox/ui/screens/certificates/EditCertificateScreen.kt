package xyz.zt.mindbox.ui.screens.certificates

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch
import xyz.zt.mindbox.data.model.Certificate
import xyz.zt.mindbox.utils.AppwriteHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCertificateScreen(navController: NavController, certificateId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("Seleccionar fecha") }
    var platformType by remember { mutableStateOf("") }
    var idInput by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var currentPdfUrl by remember { mutableStateOf<String?>(null) }

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPdfUri = uri
    }

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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        calendar.timeInMillis = utcMillis
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        issueDate = formatter.format(calendar.time)
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
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
                                val bucketId = "69bb74c9003b6b2c2f98"
                                val projectId = "69bafc2400163f8e22ea"

                                scope.launch {
                                    try {
                                        var finalPdfUrl = currentPdfUrl

                                        if (selectedPdfUri != null) {
                                            val inputStream = context.contentResolver.openInputStream(selectedPdfUri!!)
                                            val bytes = inputStream?.readBytes() ?: byteArrayOf()
                                            val fileNamePath = "$userId/$certificateId/${title.replace(" ", "_").lowercase()}_rev.pdf"

                                            val inputFile = io.appwrite.models.InputFile.fromBytes(
                                                bytes = bytes,
                                                filename = fileNamePath,
                                                mimeType = "application/pdf"
                                            )

                                            val fileResponse = AppwriteHelper.storage.createFile(
                                                bucketId = bucketId,
                                                fileId = io.appwrite.ID.unique(),
                                                file = inputFile
                                            )

                                            // ✅ URL CORREGIDA CON ID Y MODE ADMIN
                                            finalPdfUrl = "https://sfo.cloud.appwrite.io/v1/storage/buckets/$bucketId/files/${fileResponse.id}/view?project=$projectId&mode=admin"
                                        }

                                        val updatedCert = Certificate(
                                            id = certificateId,
                                            title = title,
                                            platform = platformType,
                                            issueDate = if (issueDate == "Seleccionar fecha") "" else issueDate,
                                            folio = idInput,
                                            score = score,
                                            notes = notes,
                                            pdfUrl = finalPdfUrl
                                        )

                                        db.collection("users").document(userId)
                                            .collection("certificates").document(certificateId)
                                            .set(updatedCert)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Cambios guardados", Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            }
                                            .addOnFailureListener {
                                                isSaving = false
                                                Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                                            }
                                    } catch (e: Exception) {
                                        isSaving = false
                                        Toast.makeText(context, "Error Appwrite: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
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

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del curso") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditCalendar, null, tint = colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Fecha de entrega", style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
                            Text(issueDate, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("Documento de respaldo", style = MaterialTheme.typography.labelLarge)
                OutlinedCard(
                    onClick = { pdfLauncher.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            null,
                            tint = if (selectedPdfUri != null || currentPdfUrl != null) Color(0xFFF44336) else colorScheme.outline
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            val text = if (selectedPdfUri != null) "Nuevo archivo" else if (currentPdfUrl != null) "Archivo actual" else "Sin archivo"
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