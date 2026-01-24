package xyz.zt.mindbox.ui.dashboard.screens.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import xyz.zt.mindbox.BuildConfig
import xyz.zt.mindbox.ui.dashboard.screens.notes.NotesViewModel
import xyz.zt.mindbox.ui.nav.BottomNavItem
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun DashboardScreen(
    navController: NavController,
    notesViewModel: NotesViewModel,
    onLogout: () -> Unit
) {
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Usuario"

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MindBox",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { navController.navigate(BottomNavItem.Profile.route) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = "Perfil",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Hola, $userName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Bienvenido de vuelta a tu cuaderno digital.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Accesos rápidos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(modifier = Modifier.height(240.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    item {
                        QuickActionCard(
                            title = "Mis Cursos",
                            icon = Icons.Rounded.School,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            onClick = { navController.navigate("certificates") }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = "Mi Red",
                            icon = Icons.Rounded.Hub,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = { navController.navigate("stats") }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = "Escáner ID (150%)",
                            icon = Icons.Rounded.DocumentScanner,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { navController.navigate("document_scanner") }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = "Mi CV",
                            icon = Icons.Rounded.ContactPage,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            onClick = { navController.navigate("resume") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            UpdateCard(
                githubOwner = "VictorVasquezZT2005",
                githubRepo = "MindBox"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun UpdateCard(githubOwner: String, githubRepo: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var hasNotificationPermission by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasNotificationPermission = it }
    val currentVersion = BuildConfig.VERSION_NAME

    var latestVersion by remember { mutableStateOf<String?>(null) }
    var downloadUrl by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    val updateAvailable = latestVersion != null && latestVersion != currentVersion

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        checking = true
        try {
            withContext(Dispatchers.IO) {
                val url = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
                val json = JSONObject(URL(url).readText())
                latestVersion = json.getString("tag_name").removePrefix("v")
                val assets = json.getJSONArray("assets")
                if (assets.length() > 0) downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
            }
        } catch (e: Exception) { e.printStackTrace() } finally { checking = false }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Software", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Versión actual", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                Text(currentVersion, fontWeight = FontWeight.SemiBold)
            }

            if (latestVersion != null && updateAvailable) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Nueva versión", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                    Text("v$latestVersion", fontWeight = FontWeight.Bold, color = colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when {
                checking -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                isDownloading -> {
                    Column {
                        LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                        Text("${(downloadProgress * 100).toInt()}%", modifier = Modifier.align(Alignment.End), style = MaterialTheme.typography.labelSmall)
                    }
                }
                downloadedFile != null -> {
                    Button(
                        onClick = { installApk(context, downloadedFile!!) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.tertiary)
                    ) {
                        Icon(Icons.Rounded.InstallMobile, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instalar v$latestVersion")
                    }
                }
                updateAvailable -> {
                    Button(
                        onClick = {
                            startDownload(context, downloadUrl, scope) { file, progress, downloading ->
                                downloadedFile = file
                                downloadProgress = progress
                                isDownloading = downloading
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Download, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Descargar actualización")
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MindBox está actualizado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(28.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

private fun startDownload(context: Context, urlStr: String?, scope: kotlinx.coroutines.CoroutineScope, onUpdate: (File?, Float, Boolean) -> Unit) {
    if (urlStr == null) return
    onUpdate(null, 0f, true)
    scope.launch(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            val fileLength = connection.contentLength
            val input = connection.inputStream
            val file = File(context.getExternalFilesDir(null), "MindBox_Update.apk")
            val output = FileOutputStream(file)
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) withContext(Dispatchers.Main) { onUpdate(null, total.toFloat() / fileLength, true) }
                output.write(data, 0, count)
            }
            output.close(); input.close()
            withContext(Dispatchers.Main) { onUpdate(file, 1f, false) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onUpdate(null, 0f, false)
                Toast.makeText(context, "Fallo en descarga", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun installApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}