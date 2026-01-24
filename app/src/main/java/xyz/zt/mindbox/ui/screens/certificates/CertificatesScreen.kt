package xyz.zt.mindbox.ui.screens.certificates

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import xyz.zt.mindbox.data.model.Certificate
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificatesScreen(navController: NavController) {
    val colorScheme = MaterialTheme.colorScheme
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var certificates by remember { mutableStateOf(emptyList<Certificate>()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatformFilter by remember { mutableStateOf("Todas") }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val dynamicPlatforms = remember(certificates) {
        val uniquePlatforms = certificates.map { it.platform }.distinct().sorted()
        listOf("Todas") + uniquePlatforms
    }

    LaunchedEffect(dynamicPlatforms) {
        if (selectedPlatformFilter !in dynamicPlatforms) {
            selectedPlatformFilter = "Todas"
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            db.collection("users").document(userId).collection("certificates")
                .addSnapshotListener { snapshot, _ ->
                    certificates = snapshot?.documents?.mapNotNull { it.toObject(Certificate::class.java) } ?: emptyList()
                    isLoading = false
                }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_certificate") },
                containerColor = colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Agregar Logro") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                Text("Mis Logros", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar certificado...") },
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
                dynamicPlatforms.forEach { platform ->
                    FilterChip(
                        selected = selectedPlatformFilter == platform,
                        onClick = { selectedPlatformFilter = platform },
                        label = { Text(platform) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredAndSorted = remember(certificates, searchQuery, selectedPlatformFilter) {
                certificates
                    .filter { cert ->
                        val matchesSearch = cert.title.contains(searchQuery, ignoreCase = true)
                        val matchesPlatform = if (selectedPlatformFilter == "Todas") true else cert.platform == selectedPlatformFilter
                        matchesSearch && matchesPlatform
                    }
                    .sortedByDescending { cert ->
                        try {
                            if (cert.issueDate.isNotBlank()) dateFormat.parse(cert.issueDate) else Date(0)
                        } catch (e: Exception) {
                            Date(0)
                        }
                    }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else if (filteredAndSorted.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No se encontraron certificados", color = colorScheme.outline)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredAndSorted, key = { it.id }) { cert ->
                        CertificateCard(
                            cert = cert,
                            onClick = { navController.navigate("certificate_detail/${cert.id}") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateCard(cert: Certificate, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    val badgeTintColor = when(cert.platform) {
        "Credly" -> Color(0xFF2196F3)
        "Carlos Slim" -> Color(0xFF4CAF50)
        else -> colorScheme.primary
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(badgeTintColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = badgeTintColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cert.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = cert.platform,
                    style = MaterialTheme.typography.bodyMedium,
                    color = badgeTintColor
                )
            }
        }
    }
}