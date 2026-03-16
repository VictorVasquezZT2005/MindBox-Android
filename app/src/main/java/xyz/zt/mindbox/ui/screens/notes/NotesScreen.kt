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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import xyz.zt.mindbox.data.model.Note
import xyz.zt.mindbox.ui.theme.*
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
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
                    onClick = { navController.navigate("new_note") },
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar",
                        modifier = Modifier.size(28.dp)
                    )
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

            Text(
                text = "Mis Notas",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary, // Color primario dinámico
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "Tu cuaderno, tus ideas y recordatorios.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )

            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = BrandOrange) },
                placeholder = { Text("Buscar nota o categoría...") },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.2f),
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface
                )
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dynamicTypes.forEach { type ->
                    val isSelected = viewModel.selectedTypeFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedTypeFilter = type },
                        label = { Text(type, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal) },
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

            val filteredNotes = viewModel.notes.filter { note ->
                val matchesSearch = note.content.contains(viewModel.searchQuery, ignoreCase = true)
                val matchesType = if (viewModel.selectedTypeFilter == "Todas") true else note.type == viewModel.selectedTypeFilter
                matchesSearch && matchesType
            }

            if (filteredNotes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay notas que mostrar", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteNotebookCard(
                            note = note,
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
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "¿Arrancar hoja?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Esta nota se eliminará permanentemente de tu MindBox. No podrás recuperarla después.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        noteToDelete?.let { viewModel.deleteNote(it.id) }
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
fun NoteNotebookCard(note: Note, onDelete: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val categoryColor = when (note.type) {
        "Trabajo" -> Color(0xFF4A90E2)
        "Idea"    -> BrandOrange
        "Urgente" -> Color(0xFFE74C3C)
        "Personal" -> Color(0xFF2ECC71)
        else       -> colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = categoryColor.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Column(
                modifier = Modifier
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(categoryColor, categoryColor.copy(alpha = 0.6f)))),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(4) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(colorScheme.surface.copy(alpha = 0.7f)))
                }
            }

            Column(modifier = Modifier.padding(18.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // CORRECCIÓN AQUÍ: Usamos colorScheme.onSurface para legibilidad en Dark Mode
                        Text(
                            text = note.content.lines().firstOrNull() ?: "Sin título",
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = note.type.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { ShareHelper.shareAsPdf(context, note) },
                            modifier = Modifier.background(colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape).size(34.dp)
                        ) {
                            Icon(Icons.Default.Share, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.background(Color(0xFFFFEBEE).copy(alpha = 0.1f), CircleShape).size(34.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (note.content.lines().size > 1) note.content.lines().drop(1).joinToString(" ") else "Nota vacía...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}