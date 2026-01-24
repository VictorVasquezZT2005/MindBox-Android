package xyz.zt.mindbox.ui.dashboard.screens.reminders

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.zt.mindbox.data.model.Reminder
import xyz.zt.mindbox.utils.CalendarHelper
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(onBack: () -> Unit, viewModel: RemindersViewModel) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }

    var hasDate by remember { mutableStateOf(false) }
    var hasTime by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("Seleccionar fecha") }
    var selectedTime by remember { mutableStateOf("Seleccionar hora") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val availableLists = remember { mutableStateListOf("Imbox", "Personal", "Trabajo") }
    var selectedList by remember { mutableStateOf("Imbox") }
    var showListMenu by remember { mutableStateOf(false) }
    var showNewListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    var showDetailsField by remember { mutableStateOf(false) }
    var detailText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.POST_NOTIFICATIONS
        ))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Recordatorio", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas rápidas") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, placeholder = { Text("https://...") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasDate, onCheckedChange = { hasDate = it; if(it) showDatePicker = true })
                Text("Fecha")
                if (hasDate) TextButton(onClick = { showDatePicker = true }) { Text(selectedDate) }

                Spacer(modifier = Modifier.width(16.dp))

                Checkbox(checked = hasTime, onCheckedChange = { hasTime = it; if(it) showTimePicker = true })
                Text("Hora")
                if (hasTime) TextButton(onClick = { showTimePicker = true }) { Text(selectedTime) }
            }

            Text("Categorización", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    FilterChip(
                        selected = true,
                        onClick = { showListMenu = true },
                        label = { Text("Lista: $selectedList") }
                    )
                    DropdownMenu(expanded = showListMenu, onDismissRequest = { showListMenu = false }) {
                        availableLists.forEach { list ->
                            DropdownMenuItem(text = { Text(list) }, onClick = { selectedList = list; showListMenu = false })
                        }
                        Divider()
                        DropdownMenuItem(text = { Text("+ Nueva lista") }, onClick = { showListMenu = false; showNewListDialog = true })
                    }
                }

                FilterChip(
                    selected = showDetailsField,
                    onClick = { showDetailsField = !showDetailsField },
                    label = { Text("Detalles") }
                )
            }

            AnimatedVisibility(visible = showDetailsField) {
                OutlinedTextField(
                    value = detailText,
                    onValueChange = { detailText = it },
                    label = { Text("Detalles adicionales") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                Text("Marcar como urgente", color = if(isUrgent) Color.Red else Color.Unspecified)
            }

            Button(
                onClick = {
                    val finalNotes = if(showDetailsField && detailText.isNotBlank()) {
                        "$notes\n\nDetalles: $detailText"
                    } else notes

                    val reminder = Reminder(
                        title = title,
                        notes = finalNotes,
                        url = url,
                        date = if (hasDate) selectedDate else "",
                        time = if (hasTime) selectedTime else "",
                        isUrgent = isUrgent,
                        listCategory = selectedList
                    )

                    viewModel.addReminder(reminder)

                    if (hasDate && hasTime) {
                        CalendarHelper.addEventToCalendar(
                            context = context,
                            title = title,
                            notes = finalNotes,
                            dateStr = selectedDate,
                            timeStr = selectedTime
                        )
                    }
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank()
            ) {
                Text("Guardar y Notificar")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showNewListDialog) {
        AlertDialog(onDismissRequest = { showNewListDialog = false }, title = { Text("Nueva Lista") }, text = { OutlinedTextField(value = newListName, onValueChange = { newListName = it }, label = { Text("Nombre") }) }, confirmButton = { TextButton(onClick = { if(newListName.isNotBlank()) { availableLists.add(newListName); selectedList = newListName; showNewListDialog = false } }) { Text("Crear") } })
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = millis
                        }
                        selectedDate = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val currentTime = Calendar.getInstance()
        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
            initialMinute = currentTime.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    selectedTime = "$hour:$minute"
                    showTimePicker = false
                }) { Text("OK") }
            },
            text = {
                Box(modifier = Modifier.padding(16.dp)) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}