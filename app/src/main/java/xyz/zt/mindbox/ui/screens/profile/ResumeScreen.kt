package xyz.zt.mindbox.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import xyz.zt.mindbox.data.model.*
import xyz.zt.mindbox.utils.ResumeHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeScreen(navController: NavController) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Estado para los certificados cargados desde Firebase
    var certificates by remember { mutableStateOf<List<Certificate>>(emptyList()) }
    var isLoadingCertificates by remember { mutableStateOf(true) }

    // Cargar certificados desde Firebase
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("certificates")
                .get()
                .addOnSuccessListener { snapshot ->
                    certificates = snapshot.documents.mapNotNull { doc ->
                        Certificate(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            platform = doc.getString("platform") ?: "",
                            issueDate = doc.getString("issueDate") ?: ""
                        )
                    }.sortedByDescending { cert ->
                        // Convierte la fecha DD/MM/YYYY a un formato comparable
                        try {
                            val parts = cert.issueDate.split("/")
                            if (parts.size == 3) {
                                // Formato: YYYYMMDD para ordenar correctamente
                                "${parts[2]}${parts[1].padStart(2, '0')}${parts[0].padStart(2, '0')}"
                            } else {
                                cert.issueDate
                            }
                        } catch (e: Exception) {
                            cert.issueDate
                        }
                    }
                    isLoadingCertificates = false
                }
                .addOnFailureListener {
                    isLoadingCertificates = false
                }
        } else {
            isLoadingCertificates = false
        }
    }

    // Estado del currículum
    var resumeData by remember { mutableStateOf(ResumeData()) }

    val scrollState = rememberScrollState()

    // Estado para calendario
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
                        resumeData = resumeData.copy(personalInfo = resumeData.personalInfo.copy(birthDate = date))
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

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        resumeData = resumeData.copy(photoUri = uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Curriculum", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (resumeData.photoUri == null) {
                        Toast.makeText(context, "Sube tu foto para continuar", Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            // Genera el PDF incluyendo los certificados de Firebase
                            ResumeHelper.generateResumePdf(context, resumeData, certificates)
                            Toast.makeText(context, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                icon = { Icon(Icons.Rounded.FileDownload, null) },
                text = { Text("Guardar en Descargas") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- FOTO OBLIGATORIA ---
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(100.dp, 130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (resumeData.photoUri != null) {
                            AsyncImage(model = resumeData.photoUri, contentDescription = null, contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Rounded.AddAPhoto, "Subir", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fotografía Formal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Formato tamaño título. Indispensable para el PDF.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // --- DATOS PERSONALES ---
            CVHeaderM3("Datos Personales", Icons.Rounded.Badge)
            M3TextField(resumeData.personalInfo.name, "Nombre completo", Icons.Rounded.Person) {
                resumeData = resumeData.copy(personalInfo = resumeData.personalInfo.copy(name = it))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = resumeData.personalInfo.birthDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nacimiento") },
                    modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                    enabled = false,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    trailingIcon = { Icon(Icons.Rounded.CalendarMonth, null) }
                )

                var sexExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = sexExp, onExpandedChange = { sexExp = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = resumeData.personalInfo.gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sexo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sexExp) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(expanded = sexExp, onDismissRequest = { sexExp = false }) {
                        listOf("Masculino", "Femenino").forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = {
                                resumeData = resumeData.copy(personalInfo = resumeData.personalInfo.copy(gender = s))
                                sexExp = false
                            })
                        }
                    }
                }
            }

            var ecExp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = ecExp, onExpandedChange = { ecExp = it }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = resumeData.personalInfo.maritalStatus,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Estado Civil") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ecExp) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(expanded = ecExp, onDismissRequest = { ecExp = false }) {
                    listOf("Soltero", "Casado").forEach { ec ->
                        DropdownMenuItem(text = { Text(ec) }, onClick = {
                            resumeData = resumeData.copy(personalInfo = resumeData.personalInfo.copy(maritalStatus = ec))
                            ecExp = false
                        })
                    }
                }
            }

            M3TextField(resumeData.personalInfo.professionalId, "Cédula Profesional (Opcional)", Icons.Rounded.Verified) {
                resumeData = resumeData.copy(personalInfo = resumeData.personalInfo.copy(professionalId = it))
            }
            M3TextField(resumeData.personalInfo.email, "Correo electrónico", Icons.Rounded.AlternateEmail) {
                resumeData = resumeData.copy(personalInfo = resumeData.personalInfo.copy(email = it))
            }
            M3TextField(resumeData.personalInfo.phone, "Número celular", Icons.Rounded.PhoneAndroid) {
                resumeData = resumeData.copy(personalInfo = resumeData.personalInfo.copy(phone = it))
            }
            M3TextField(resumeData.personalInfo.address, "Dirección actual", Icons.Rounded.Map) {
                resumeData = resumeData.copy(personalInfo = resumeData.personalInfo.copy(address = it))
            }

            // --- FORMACIÓN ACADÉMICA ---
            CVHeaderM3("Formación Académica", Icons.Rounded.School)

            EducationFieldM3(
                titleValue = resumeData.education.university,
                label = "Título Universitario / Universidad",
                onValueChange = { resumeData = resumeData.copy(education = resumeData.education.copy(university = it)) }
            )
            EducationFieldM3(
                titleValue = resumeData.education.postgraduate,
                label = "Posgrado / Maestría",
                onValueChange = { resumeData = resumeData.copy(education = resumeData.education.copy(postgraduate = it)) }
            )
            EducationFieldM3(
                titleValue = resumeData.education.secondary,
                label = "Educación Secundaria",
                onValueChange = { resumeData = resumeData.copy(education = resumeData.education.copy(secondary = it)) }
            )

            // --- CURSOS Y CERTIFICACIONES (Desde Firebase) ---
            if (isLoadingCertificates) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (certificates.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Cursos y Certificaciones",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                certificates.forEach { cert ->
                    ListItem(
                        headlineContent = { Text(cert.title, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("${cert.platform} • ${cert.issueDate}") },
                        leadingContent = { Icon(Icons.Rounded.TaskAlt, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // --- EXPERIENCIA LABORAL ---
            CVHeaderM3("Experiencia Laboral", Icons.Rounded.WorkOutline)
            resumeData.experiences.forEachIndexed { index, exp ->
                ExperienceCardM3(exp, onUpdate = { updated ->
                    val newList = resumeData.experiences.toMutableList()
                    newList[index] = updated
                    resumeData = resumeData.copy(experiences = newList)
                }, onRemove = {
                    val newList = resumeData.experiences.toMutableList()
                    newList.removeAt(index)
                    resumeData = resumeData.copy(experiences = newList)
                })
            }
            Button(
                onClick = { resumeData = resumeData.copy(experiences = resumeData.experiences + Experience()) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Añadir experiencia laboral")
            }

            // --- IDIOMAS ---
            CVHeaderM3("Idiomas", Icons.Rounded.Language)
            resumeData.languages.forEachIndexed { index, lang ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    M3TextField(lang.name, "Idioma", Icons.Rounded.Translate, modifier = Modifier.weight(1f)) { n ->
                        val l = resumeData.languages.toMutableList()
                        l[index] = lang.copy(name = n)
                        resumeData = resumeData.copy(languages = l)
                    }

                    var langExp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = langExp, onExpandedChange = { langExp = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = lang.level,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Nivel") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langExp) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(expanded = langExp, onDismissRequest = { langExp = false }) {
                            listOf("Básico", "Intermedio", "Avanzado").forEach { lvl ->
                                DropdownMenuItem(text = { Text(lvl) }, onClick = {
                                    val list = resumeData.languages.toMutableList()
                                    list[index] = lang.copy(level = lvl)
                                    resumeData = resumeData.copy(languages = list)
                                    langExp = false
                                })
                            }
                        }
                    }

                    IconButton(onClick = {
                        val l = resumeData.languages.toMutableList()
                        l.removeAt(index)
                        resumeData = resumeData.copy(languages = l)
                    }) { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) }
                }
            }
            TextButton(onClick = { resumeData = resumeData.copy(languages = resumeData.languages + Language()) }) {
                Icon(Icons.Rounded.Add, null)
                Text("Añadir idioma")
            }

            // --- HABILIDADES Y ADICIONAL ---
            CVHeaderM3("Habilidades e Información", Icons.Rounded.AutoAwesome)
            M3TextField(resumeData.skills, "Principales habilidades", Icons.Rounded.Star) {
                resumeData = resumeData.copy(skills = it)
            }
            M3TextField(resumeData.additionalInfo, "Información adicional", Icons.Rounded.Info) {
                resumeData = resumeData.copy(additionalInfo = it)
            }

            // --- REFERENCIAS ---
            CVHeaderM3("Referencia Personales", Icons.Rounded.ContactPhone)
            resumeData.references.forEachIndexed { index, ref ->
                ReferenceCardM3(ref, onUpdate = { updated ->
                    val newList = resumeData.references.toMutableList()
                    newList[index] = updated
                    resumeData = resumeData.copy(references = newList)
                }, onRemove = {
                    val newList = resumeData.references.toMutableList()
                    newList.removeAt(index)
                    resumeData = resumeData.copy(references = newList)
                })
            }
            FilledTonalButton(
                onClick = { resumeData = resumeData.copy(references = resumeData.references + Reference()) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.PersonAdd, null)
                Spacer(Modifier.width(8.dp))
                Text("Añadir referencia")
            }

            Spacer(Modifier.height(130.dp))
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationFieldM3(titleValue: String, label: String, onValueChange: (String) -> Unit) {
    var baseTitle by remember { mutableStateOf(titleValue.substringBefore(" (")) }
    var status by remember { mutableStateOf(if (titleValue.contains("En curso")) "En curso" else "Terminado") }
    var year by remember { mutableStateOf(if (titleValue.contains("(") && !titleValue.contains("En curso")) titleValue.substringAfter("(").substringBefore(")") else "") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = baseTitle,
            onValueChange = {
                baseTitle = it
                val suffix = if (status == "En curso") " (En curso)" else if (year.isNotBlank()) " ($year)" else ""
                onValueChange("$baseTitle$suffix")
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Terminado", "En curso").forEach { option ->
                FilterChip(
                    selected = status == option,
                    onClick = {
                        status = option
                        val suffix = if (status == "En curso") " (En curso)" else if (year.isNotBlank()) " ($year)" else ""
                        onValueChange("$baseTitle$suffix")
                    },
                    label = { Text(option) }
                )
            }

            if (status == "Terminado") {
                OutlinedTextField(
                    value = year,
                    onValueChange = {
                        year = it
                        onValueChange("$baseTitle ($year)")
                    },
                    label = { Text("Año") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

@Composable
fun CVHeaderM3(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun M3TextField(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ExperienceCardM3(exp: Experience, onUpdate: (Experience) -> Unit, onRemove: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Puesto Laboral", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onRemove) { Icon(Icons.Rounded.Cancel, null, tint = MaterialTheme.colorScheme.error) }
            }
            M3TextField(exp.company, "Nombre de la empresa", Icons.Rounded.Business) { onUpdate(exp.copy(company = it)) }
            M3TextField(exp.position, "Puesto / Cargo", Icons.Rounded.Badge) { onUpdate(exp.copy(position = it)) }
            M3TextField(exp.period, "Periodo (Año - Año)", Icons.Rounded.DateRange) { onUpdate(exp.copy(period = it)) }
            OutlinedTextField(
                value = exp.description,
                onValueChange = { onUpdate(exp.copy(description = it)) },
                label = { Text("Descripción de actividades") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 2
            )
        }
    }
}

@Composable
fun ReferenceCardM3(ref: Reference, onUpdate: (Reference) -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Referencia", fontWeight = FontWeight.Bold)
                IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, null) }
            }
            M3TextField(ref.name, "Nombre completo", Icons.Rounded.Person) { onUpdate(ref.copy(name = it)) }
            M3TextField(ref.email, "Correo electrónico", Icons.Rounded.Email) { onUpdate(ref.copy(email = it)) }
            M3TextField(ref.company, "Empresa / Compañía", Icons.Rounded.Business) { onUpdate(ref.copy(company = it)) }
            M3TextField(ref.phone, "Número de teléfono", Icons.Rounded.Phone) { onUpdate(ref.copy(phone = it)) }
        }
    }
}