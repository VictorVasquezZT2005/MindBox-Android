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
import kotlinx.coroutines.launch
import xyz.zt.mindbox.data.model.Certificate
import xyz.zt.mindbox.utils.AppwriteHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCertificateScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // --- Estados del Formulario ---
    var title by remember { mutableStateOf("") }
    var platformType by remember { mutableStateOf("Carlos Slim") }
    var customPlatform by remember { mutableStateOf("") }
    var idInput by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedDateText by remember { mutableStateOf("Seleccionar fecha") }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPdfUri = uri
    }

    // --- Función para guardar en Firestore ---
    fun saveCertificateData(certId: String, finalPlatform: String, pdfUrl: String?) {
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
                Toast.makeText(context, "Logro guardado con éxito", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            .addOnFailureListener {
                isSaving = false
                Toast.makeText(context, "Error al guardar en Firestore", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Diálogo de Fecha Corregido (Desfase UTC) ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        // Usamos UTC para que no reste horas por la zona horaria local
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        calendar.timeInMillis = utcMillis

                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        selectedDateText = formatter.format(calendar.time)
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
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
                    IconButton(
                        onClick = {
                            if (title.isBlank()) return@IconButton
                            isSaving = true
                            val certId = UUID.randomUUID().toString()
                            val finalPlatform = if (platformType == "Otro") customPlatform else platformType
                            val bucketId = "69bb74c9003b6b2c2f98"

                            scope.launch {
                                try {
                                    if (selectedPdfUri != null) {
                                        val inputStream = context.contentResolver.openInputStream(selectedPdfUri!!)
                                        val bytes = inputStream?.readBytes() ?: byteArrayOf()

                                        // Nombre con ruta: userId/certId/nombre.pdf
                                        val fileNamePath = "$userId/$certId/${title.replace(" ", "_").lowercase()}.pdf"

                                        val inputFile = io.appwrite.models.InputFile.fromBytes(
                                            bytes = bytes,
                                            filename = fileNamePath,
                                            mimeType = "application/pdf"
                                        )

                                        // 1. Subir a Appwrite
                                        val fileResponse = AppwriteHelper.storage.createFile(
                                            bucketId = bucketId,
                                            fileId = io.appwrite.ID.unique(),
                                            file = inputFile
                                        )

                                        // 2. URL de visualización corregida
                                        val fileUrl = "https://sfo.cloud.appwrite.io/v1/storage/buckets/$bucketId/files/${fileResponse.id}/view?project=69bafc2400163f8e22ea"

                                        saveCertificateData(certId, finalPlatform, fileUrl)
                                    } else {
                                        saveCertificateData(certId, finalPlatform, null)
                                    }
                                } catch (e: Exception) {
                                    isSaving = false
                                    Toast.makeText(context, "Error Appwrite: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = title.isNotBlank() && !isSaving
                    ) {
                        Icon(Icons.Default.Done, "Guardar", tint = colorScheme.primary)
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

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título del curso") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Selector de Fecha
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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

            OutlinedTextField(
                value = idInput,
                onValueChange = { idInput = it },
                label = { Text("Folio / ID de Certificado") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Selector de PDF
            Text("Archivo de respaldo", style = MaterialTheme.typography.labelLarge)
            OutlinedCard(
                onClick = { pdfLauncher.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        null,
                        tint = if (selectedPdfUri != null) Color(0xFFF44336) else colorScheme.outline
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(if (selectedPdfUri != null) "PDF seleccionado" else "Seleccionar archivo PDF")
                }
            }

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