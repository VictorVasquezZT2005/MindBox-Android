package xyz.zt.mindbox.ui.dashboard.screens.notes

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import xyz.zt.mindbox.data.model.Note
import xyz.zt.mindbox.utils.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: NavController, viewModel: NotesViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    var showDeleteDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    val dynamicTypes = remember(viewModel.notes) {
        val uniqueTypes = viewModel.notes.map { it.type }.distinct().sorted()
        listOf("Todas") + uniqueTypes
    }

    LaunchedEffect(dynamicTypes) {
        if (viewModel.selectedTypeFilter != "Todas" && viewModel.selectedTypeFilter !in dynamicTypes) {
            viewModel.selectedTypeFilter = "Todas"
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("new_note") },
                containerColor = colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Nueva Nota") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Mis Notas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar en el cuaderno...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(28.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dynamicTypes.forEach { type ->
                    FilterChip(
                        selected = viewModel.selectedTypeFilter == type,
                        onClick = { viewModel.selectedTypeFilter = type },
                        label = { Text(type) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primaryContainer,
                            selectedLabelColor = colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredNotes = viewModel.notes.filter { note ->
                val matchesSearch = note.content.contains(viewModel.searchQuery, ignoreCase = true)
                val matchesType = if (viewModel.selectedTypeFilter == "Todas") true else note.type == viewModel.selectedTypeFilter
                matchesSearch && matchesType
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    NoteNotebookCard(
                        note = note,
                        colorScheme = colorScheme,
                        onDelete = {
                            noteToDelete = note
                            showDeleteDialog = true
                        },
                        onClick = { navController.navigate("note_detail/${note.id}") }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Arrancar hoja?") },
            text = { Text("¿Estás seguro de que quieres eliminar esta nota del cuaderno?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteToDelete?.let { viewModel.deleteNote(it.id) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun NoteNotebookCard(note: Note, colorScheme: ColorScheme, onDelete: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val spiralColor = when (note.type) {
        "Trabajo" -> Color(0xFF2196F3)
        "Idea"    -> Color(0xFF8BC34A)
        "Urgente" -> Color(0xFFF44336)
        "Personal" -> Color(0xFFFF9800)
        else       -> colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Column(
                modifier = Modifier.width(24.dp).fillMaxHeight().background(spiralColor),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(5) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(colorScheme.surface))
                }
            }

            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.content.lines().firstOrNull() ?: "Sin título",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Row {
                        IconButton(onClick = { ShareHelper.shareAsPdf(context, note) }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Compartir PDF",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = spiralColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}