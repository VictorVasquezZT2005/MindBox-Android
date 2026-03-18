package xyz.zt.mindbox.ui.dashboard.screens.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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

            // Header
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

                Image(
                    painter = painterResource(id = R.drawable.ic_profile),
                    contentDescription = "Perfil",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable { navController.navigate(BottomNavItem.Profile.route) },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Accesos rápidos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ FIX: Column + Rows en lugar de LazyVerticalGrid
            // LazyVerticalGrid dentro de verticalScroll causa crash por constraints infinitos
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionCard("Mis Cursos", Icons.Rounded.School, BrandOrange) {
                            navController.navigate("certificates")
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionCard("Mi Red", Icons.Rounded.Hub, BrandDeepBlue) {
                            navController.navigate("stats")
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionCard("Escáner ID", Icons.Rounded.DocumentScanner, Color(0xFFAB47BC)) {
                            navController.navigate("document_scanner")
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionCard("Mi CV", Icons.Rounded.ContactPage, Color(0xFF66BB6A)) {
                            navController.navigate("resume")
                        }
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
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                spotColor = color.copy(alpha = 0.4f)
            )
            .background(surfaceColor, shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.30f),
                        color.copy(alpha = 0.10f)
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.Transparent,
                        color.copy(alpha = 0.2f)
                    )
                ),
                shape = shape
            )
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun UpdateCard(githubOwner: String, githubRepo: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardShape = RoundedCornerShape(32.dp)

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasNotificationPermission = it }

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
                if (assets.length() > 0) {
                    downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            checking = false
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = cardShape,
                spotColor = BrandOrange.copy(alpha = 0.2f)
            ),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
            )
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BrandOrange.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        null,
                        tint = BrandOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Sistema de Software",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .border(0.5.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ACTUAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(currentVersion, fontWeight = FontWeight.Bold)
                }
                if (updateAvailable) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "NUEVA",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandRust
                        )
                        Text(
                            "v$latestVersion",
                            fontWeight = FontWeight.Bold,
                            color = BrandRust
                        )
                    }
                } else {
                    Text(
                        "ACTUALIZADO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                checking -> LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = BrandOrange,
                    trackColor = BrandOrange.copy(alpha = 0.1f)
                )
                isDownloading -> {
                    Column {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = BrandOrange,
                            trackColor = BrandOrange.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${(downloadProgress * 100).toInt()}% descargado",
                            modifier = Modifier.align(Alignment.End),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandOrange
                        )
                    }
                }
                downloadedFile != null -> {
                    Button(
                        onClick = { installApk(context, downloadedFile!!) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, CircleShape, spotColor = BrandDeepBlue),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandDeepBlue)
                    ) {
                        Icon(Icons.Rounded.InstallMobile, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INSTALAR AHORA", fontWeight = FontWeight.ExtraBold)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, CircleShape, spotColor = BrandOrange),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Icon(Icons.Rounded.Download, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DESCARGAR ACTUALIZACIÓN", fontWeight = FontWeight.ExtraBold)
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "MindBox está al día",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun startDownload(
    context: Context,
    urlStr: String?,
    scope: kotlinx.coroutines.CoroutineScope,
    onUpdate: (File?, Float, Boolean) -> Unit
) {
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
                if (fileLength > 0) withContext(Dispatchers.Main) {
                    onUpdate(null, total.toFloat() / fileLength, true)
                }
                output.write(data, 0, count)
            }
            output.close()
            input.close()
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