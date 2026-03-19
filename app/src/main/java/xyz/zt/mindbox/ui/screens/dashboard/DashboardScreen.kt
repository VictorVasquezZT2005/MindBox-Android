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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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

private val NetworkBlue = Color(0xFF29B6F6)

// ─────────────────────────────────────────────────────────────
//  Liquid Glass Modifier
// ─────────────────────────────────────────────────────────────
fun Modifier.liquidGlassBackground(
    color: Color,
    surfaceColor: Color,
    shape: Shape,
    glassAlpha: Float = 0.13f,
    borderAlpha: Float = 0.35f,
    elevation: Dp = 8.dp,
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = color.copy(alpha = 0.18f),
        ambientColor = color.copy(alpha = 0.08f)
    )
    .background(surfaceColor, shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = glassAlpha + 0.04f),
                color.copy(alpha = glassAlpha * 0.3f)
            )
        ),
        shape = shape
    )
    .border(
        width = 0.8.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                color.copy(alpha = 0.15f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        ),
        shape = shape
    )

fun Modifier.colorGlow(
    color: Color,
    radius: Float = 80f,
    alpha: Float = 0.25f,
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val glowPaint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(radius, 0f, 4f, color.copy(alpha = alpha).toArgb())
            }
        }
        canvas.drawCircle(
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension / 2f,
            paint = glowPaint
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  Dashboard Screen
// ─────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    navController: NavController,
    notesViewModel: NotesViewModel,
    onLogout: () -> Unit
) {
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userName = currentUser?.displayName
        ?: currentUser?.email?.substringBefore("@")
        ?: "Usuario"

    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground
    val isDark = isSystemInDarkTheme()

    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── HEADER ────────────────────────────────────────────────
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
                            color = onBackground.copy(alpha = 0.6f)
                        )
                    )
                }

                // Perfil con halo de luz — Actualizado
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .colorGlow(BrandOrange, radius = 45f, alpha = 0.28f)
                        .shadow(
                            10.dp, CircleShape,
                            spotColor = BrandOrange.copy(alpha = 0.22f),
                            ambientColor = BrandOrange.copy(alpha = 0.10f)
                        )
                        .clip(CircleShape)
                        .clickable { navController.navigate(BottomNavItem.Profile.route) }
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
                color = onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── QUICK ACTIONS GRID ────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionCard(
                            title = "Mis Cursos",
                            icon = Icons.Rounded.School,
                            color = BrandOrange,
                            surfaceColor = surface,
                            onClick = { navController.navigate("certificates") }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionCard(
                            title = "Mi Red",
                            icon = Icons.Rounded.Hub,
                            color = NetworkBlue,
                            surfaceColor = surface,
                            onClick = { navController.navigate("stats") }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionCard(
                            title = "Escáner ID",
                            icon = Icons.Rounded.DocumentScanner,
                            color = Color(0xFFAB47BC),
                            surfaceColor = surface,
                            onClick = { navController.navigate("document_scanner") }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionCard(
                            title = "Mi CV",
                            icon = Icons.Rounded.ContactPage,
                            color = Color(0xFF66BB6A),
                            surfaceColor = surface,
                            onClick = { navController.navigate("resume") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            UpdateCard(
                githubOwner = "VictorVasquezZT2005",
                githubRepo = "MindBox",
                surfaceColor = surface,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Quick Action Card
// ─────────────────────────────────────────────────────────────
@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    surfaceColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val onBackground = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .colorGlow(color, radius = 55f, alpha = 0.22f)
            .liquidGlassBackground(
                color = color,
                surfaceColor = surfaceColor,
                shape = shape,
                glassAlpha = 0.13f,
                borderAlpha = 0.38f,
                elevation = 10.dp
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
                    .size(42.dp)
                    .colorGlow(color, radius = 30f, alpha = 0.30f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.28f),
                                color.copy(alpha = 0.10f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = onBackground,
                fontSize = 15.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Update Card
// ─────────────────────────────────────────────────────────────
@Composable
fun UpdateCard(
    githubOwner: String,
    githubRepo: String,
    surfaceColor: Color,
    isDark: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardShape = RoundedCornerShape(32.dp)
    val onBackground = MaterialTheme.colorScheme.onBackground
    val background = MaterialTheme.colorScheme.background

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasNotificationPermission = it
    }

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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .colorGlow(BrandOrange, radius = 60f, alpha = 0.18f)
            .liquidGlassBackground(
                color = BrandOrange,
                surfaceColor = surfaceColor,
                shape = cardShape,
                glassAlpha = 0.08f,
                borderAlpha = 0.32f,
                elevation = 12.dp
            )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .colorGlow(BrandOrange, radius = 28f, alpha = 0.28f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    BrandOrange.copy(alpha = 0.25f),
                                    BrandOrange.copy(alpha = 0.08f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.40f), Color.Transparent)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
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
                    .clip(CircleShape)
                    .background(
                        if (isDark) background.copy(alpha = 0.35f)
                        else onBackground.copy(alpha = 0.05f)
                    )
                    .border(
                        0.5.dp,
                        onBackground.copy(alpha = if (isDark) 0.08f else 0.10f),
                        CircleShape
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ACTUAL", style = MaterialTheme.typography.labelSmall, color = onBackground.copy(alpha = 0.45f))
                    Text(currentVersion, fontWeight = FontWeight.Bold, color = onBackground)
                }
                if (updateAvailable) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text("NUEVA", style = MaterialTheme.typography.labelSmall, color = BrandRust)
                        Text("v$latestVersion", fontWeight = FontWeight.Bold, color = BrandRust)
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
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = BrandOrange,
                    trackColor = BrandOrange.copy(alpha = 0.10f)
                )
                isDownloading -> Column {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = BrandOrange,
                        trackColor = BrandOrange.copy(alpha = 0.10f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${(downloadProgress * 100).toInt()}% descargado",
                        modifier = Modifier.align(Alignment.End),
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandOrange
                    )
                }
                downloadedFile != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .colorGlow(BrandDeepBlue, radius = 40f, alpha = 0.35f)
                            .clip(CircleShape)
                            .background(BrandDeepBlue.copy(alpha = 0.80f))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)
                                )
                            )
                            .border(
                                0.8.dp,
                                Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.40f), Color.Transparent),
                                    start = Offset(0f, 0f), end = Offset(200f, 100f)
                                ),
                                CircleShape
                            )
                            .clickable { installApk(context, downloadedFile!!) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.InstallMobile, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INSTALAR AHORA", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
                updateAvailable -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .colorGlow(BrandOrange, radius = 40f, alpha = 0.35f)
                            .clip(CircleShape)
                            .background(BrandOrange.copy(alpha = 0.80f))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)
                                )
                            )
                            .border(
                                0.8.dp,
                                Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                                    start = Offset(0f, 0f), end = Offset(200f, 100f)
                                ),
                                CircleShape
                            )
                            .clickable {
                                startDownload(context, downloadUrl, scope) { file, progress, downloading ->
                                    downloadedFile = file
                                    downloadProgress = progress
                                    isDownloading = downloading
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DESCARGAR ACTUALIZACIÓN", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
                else -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MindBox está al día", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
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