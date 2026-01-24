package xyz.zt.mindbox.ui.screens.documents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(navController: NavController) {
    val context = LocalContext.current
    var frontUri by remember { mutableStateOf<Uri?>(null) }
    var backUri by remember { mutableStateOf<Uri?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var capturingFront by remember { mutableStateOf(true) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            if (capturingFront) frontUri = uri else backUri = uri
        } else if (result.error != null) {
            Toast.makeText(context, "Error: ${result.error?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val cropOptions = CropImageOptions().apply {
        imageSourceIncludeGallery = true
        imageSourceIncludeCamera = true

        guidelines = CropImageView.Guidelines.ON
        autoZoomEnabled = true
        fixAspectRatio = false

        // Interfaz en modo oscuro
        backgroundColor = android.graphics.Color.BLACK
        activityBackgroundColor = android.graphics.Color.BLACK
        toolbarColor = android.graphics.Color.BLACK
        toolbarTitleColor = android.graphics.Color.WHITE
        toolbarBackButtonColor = android.graphics.Color.WHITE
        activityMenuIconColor = android.graphics.Color.WHITE

        activityTitle = "Ajustar Documento"
        cropperLabelText = "" // Eliminado el texto de instrucción del recorte
        showCropOverlay = true
        showProgressBar = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Escáner a 150%", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "Captura la foto y ajusta los bordes. Confirma la selección usando el icono de la barra superior para continuar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            DocumentCard(
                label = "Parte Frontal",
                uri = frontUri,
                icon = Icons.Default.Badge
            ) {
                capturingFront = true
                cropLauncher.launch(CropImageContractOptions(null, cropOptions))
            }

            Spacer(Modifier.height(16.dp))

            DocumentCard(
                label = "Parte Trasera",
                uri = backUri,
                icon = Icons.Default.CreditCard
            ) {
                capturingFront = false
                cropLauncher.launch(CropImageContractOptions(null, cropOptions))
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (frontUri != null && backUri != null) {
                        isGenerating = true
                        generate150PdfInDownloads(context, frontUri!!, backUri!!)
                        isGenerating = false
                    }
                },
                enabled = frontUri != null && backUri != null && !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(12.dp))
                    Text("Generar PDF en Descargas", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun DocumentCard(label: String, uri: Uri?, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (uri != null) colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (uri != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(label, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, modifier = Modifier.size(40.dp), tint = colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun generate150PdfInDownloads(context: Context, frontUri: Uri, backUri: Uri) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val frontBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(frontUri))
        val backBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(backUri))

        val targetW = 365
        val targetH = 230

        val fScaled = Bitmap.createScaledBitmap(frontBitmap, targetW, targetH, true)
        val bScaled = Bitmap.createScaledBitmap(backBitmap, targetW, targetH, true)

        canvas.drawBitmap(fScaled, 123.5f, 100f, null)
        canvas.drawBitmap(bScaled, 123.5f, 380f, null)

        pdfDocument.finishPage(page)

        val fileName = "Copia_ID_150_${System.currentTimeMillis()}.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(context, "Archivo guardado", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}