package xyz.zt.mindbox.ui.dashboard.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import xyz.zt.mindbox.data.model.Reminder
import xyz.zt.mindbox.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(navController: NavController, viewModel: RemindersViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    val reminders = viewModel.reminders

    val dynamicCategories = remember(reminders) {
        val uniqueCategories = reminders.map { it.listCategory }.distinct().sorted()
        listOf("Todas") + uniqueCategories
    }

    var selectedCategory by remember { mutableStateOf("Todas") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            // FAB Estilo MindBox (como en Passwords/Login)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(12.dp, CircleShape)
                    .background(
                        brush = Brush.linearGradient(listOf(BrandOrange, BrandRust)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_reminder") },
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.Add, "Agregar", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // Títulos estilo MindBox
            Text(
                text = "Recordatorios",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "No dejes pasar lo importante.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )

            Spacer(Modifier.height(28.dp))

            // Buscador estilo MindBox (Shape 18dp)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = BrandOrange) },
                placeholder = { Text("Buscar recordatorios...") },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.2f),
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface
                )
            )

            Spacer(Modifier.height(20.dp))

            // Chips de categorías
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dynamicCategories.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandOrange,
                            selectedLabelColor = Color.White,
                            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = null
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            val filteredList = reminders.filter { reminder ->
                val matchesSearch = reminder.title.contains(searchQuery, ignoreCase = true)
                val matchesCategory = if (selectedCategory == "Todas") true else reminder.listCategory == selectedCategory
                matchesSearch && matchesCategory
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Notifications, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
                        Text("Todo al día", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredList, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onDelete = {
                                reminderToDelete = reminder
                                showDeleteDialog = true
                            },
                            onClick = { navController.navigate("reminder_detail/${reminder.id}") }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("¿Eliminar?", fontWeight = FontWeight.Bold, color = colorScheme.onSurface) },
            text = { Text("¿Estás seguro de borrar \"${reminderToDelete?.title}\"?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        reminderToDelete?.let { viewModel.deleteReminder(it.id) }
                        showDeleteDialog = false
                    }
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ReminderCard(reminder: Reminder, onDelete: () -> Unit, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (reminder.isUrgent) Color.Red.copy(alpha = 0.2f) else BrandOrange.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador lateral de urgencia/estado
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 40.dp)
                    .clip(CircleShape)
                    .background(if (reminder.isUrgent) Color.Red else BrandOrange)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )

                if (reminder.notes.isNotBlank()) {
                    Text(
                        text = reminder.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (reminder.date.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📅 ${reminder.date} • ${reminder.time}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reminder.isUrgent) Color.Red else BrandOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .background(colorScheme.errorContainer.copy(alpha = 0.1f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Delete, null, tint = colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}