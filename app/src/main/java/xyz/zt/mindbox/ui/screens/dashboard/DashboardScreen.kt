package xyz.zt.mindbox.ui.dashboard.screens.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import xyz.zt.mindbox.R
import xyz.zt.mindbox.ui.dashboard.screens.notes.NotesViewModel
import xyz.zt.mindbox.ui.nav.BottomNavItem
import xyz.zt.mindbox.ui.theme.*
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // CABECERA ESTILO MINDBOX
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "MindBox",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Text(
                        text = "Hola, $userName",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )
                }

                // AVATAR LIMPIO - SOLO EL PNG CON TRANSPARENCIA
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(8.dp, CircleShape) // Sombra ligera para dar profundidad al icono
                        .clip(CircleShape)
                        .clickable { navController.navigate(BottomNavItem.Profile.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile),
                        contentDescription = "Perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Accesos rápidos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // GRID DE ACCIONES RÁPIDAS
            Box(modifier = Modifier.height(260.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false
                ) {
                    item {
                        QuickActionCard(
                            title = "Mis Cursos",
                            icon = Icons.Rounded.School,
                            brush = Brush.linearGradient(listOf(BrandOrange, BrandRust)),
                            onClick = { navController.navigate("certificates") }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = "Mi Red",
                            icon = Icons.Rounded.Hub,
                            brush = Brush.linearGradient(listOf(BrandDeepBlue, Color(0xFF4A90E2))),
                            onClick = { navController.navigate("stats") }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = "Escáner ID",
                            icon = Icons.Rounded.DocumentScanner,
                            brush = Brush.linearGradient(listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))),
                            onClick = { navController.navigate("document_scanner") }
                        )
                    }

                    item {
                        QuickActionCard(
                            title = "Mi CV",
                            icon = Icons.Rounded.ContactPage,
                            brush = Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))),
                            onClick = { navController.navigate("resume") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // TARJETA DE ACTUALIZACIÓN
            UpdateCard(
                githubOwner = "VictorVasquezZT2005",
                githubRepo = "MindBox"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    brush: Brush,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .shadow(12.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BrandOrange.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sistema de Software",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.background, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Actual", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(currentVersion, fontWeight = FontWeight.Bold)
                }
                if (updateAvailable) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Nueva", style = MaterialTheme.typography.labelSmall, color = BrandRust)
                        Text("v$latestVersion", fontWeight = FontWeight.Bold, color = BrandRust)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                checking -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = BrandOrange)
                isDownloading -> {
                    Column {
                        LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = BrandOrange)
                        Text("${(downloadProgress * 100).toInt()}%", modifier = Modifier.align(Alignment.End), style = MaterialTheme.typography.labelSmall)
                    }
                }
                downloadedFile != null -> {
                    Button(
                        onClick = { installApk(context, downloadedFile!!) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandDeepBlue)
                    ) {
                        Icon(Icons.Rounded.InstallMobile, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INSTALAR ACTUALIZACIÓN", fontWeight = FontWeight.Bold)
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
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Icon(Icons.Rounded.Download, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DESCARGAR AHORA", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MindBox está actualizado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// Lógica de descarga e instalación
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