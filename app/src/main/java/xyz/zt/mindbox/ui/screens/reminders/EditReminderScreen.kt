package xyz.zt.mindbox.ui.dashboard.screens.reminders

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.zt.mindbox.data.model.Reminder
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(onBack: () -> Unit, viewModel: RemindersViewModel, reminderId: String) {
    val originalReminder = viewModel.reminders.find { it.id == reminderId }

    var title by remember { mutableStateOf(originalReminder?.title ?: "") }
    var notes by remember { mutableStateOf(originalReminder?.notes ?: "") }
    var url by remember { mutableStateOf(originalReminder?.url ?: "") }
    var isUrgent by remember { mutableStateOf(originalReminder?.isUrgent ?: false) }
    var selectedList by remember { mutableStateOf(originalReminder?.listCategory ?: "Imbox") }

    var hasDate by remember { mutableStateOf(originalReminder?.date?.isNotBlank() == true) }
    var hasTime by remember { mutableStateOf(originalReminder?.time?.isNotBlank() == true) }
    var selectedDate by remember { mutableStateOf(originalReminder?.date ?: "Seleccionar fecha") }
    var selectedTime by remember { mutableStateOf(originalReminder?.time ?: "Seleccionar hora") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }
    val availableLists = remember { mutableStateListOf("Imbox", "Personal", "Trabajo") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Recordatorio") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = {
                        val updated = originalReminder?.copy(
                            title = title,
                            notes = notes,
                            url = url,
                            date = if (hasDate) selectedDate else "",
                            time = if (hasTime) selectedTime else "",
                            isUrgent = isUrgent,
                            listCategory = selectedList
                        )
                        if (updated != null) {
                            viewModel.updateReminder(updated)
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasDate, onCheckedChange = { hasDate = it })
                Text("Fecha")
                if (hasDate) TextButton(onClick = { showDatePicker = true }) { Text(selectedDate) }
                Spacer(Modifier.width(8.dp))
                Checkbox(checked = hasTime, onCheckedChange = { hasTime = it })
                Text("Hora")
                if (hasTime) TextButton(onClick = { showTimePicker = true }) { Text(selectedTime) }
            }

            Box {
                FilterChip(selected = true, onClick = { showListMenu = true }, label = { Text("Lista: $selectedList") })
                DropdownMenu(expanded = showListMenu, onDismissRequest = { showListMenu = false }) {
                    availableLists.forEach { list ->
                        DropdownMenuItem(text = { Text(list) }, onClick = { selectedList = list; showListMenu = false })
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                Text("Urgente", color = if (isUrgent) Color.Red else Color.Unspecified)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
                    selectedDate = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
                }
                showDatePicker = false
            }) { Text("OK") }
        }) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(is24Hour = true)
        AlertDialog(onDismissRequest = { showTimePicker = false }, confirmButton = {
            TextButton(onClick = {
                selectedTime = "${timePickerState.hour.toString().padStart(2, '0')}:${timePickerState.minute.toString().padStart(2, '0')}"
                showTimePicker = false
            }) { Text("OK") }
        }, text = { TimePicker(state = timePickerState) })
    }
}