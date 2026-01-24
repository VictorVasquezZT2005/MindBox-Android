package xyz.zt.mindbox.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import xyz.zt.mindbox.data.model.Note
import java.io.File
import java.io.FileOutputStream

object ShareHelper {

    fun shareAsPdf(context: Context, note: Note) {
        val pdfDocument = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val categoryColor = when (note.type) {
            "Trabajo" -> 0xFF2196F3.toInt()
            "Idea"    -> 0xFF8BC34A.toInt()
            "Urgente" -> 0xFFF44336.toInt()
            "Personal" -> 0xFFFF9800.toInt()
            else       -> 0xFF9E9E9E.toInt()
        }

        val paintTitle = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }

        val paintBody = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 16f
        }

        val paintLine = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1f
        }

        canvas.drawRect(0f, 0f, 25f, 842f, Paint().apply { color = categoryColor })

        var yPos = 120f
        while (yPos < 800f) {
            canvas.drawLine(40f, yPos, 560f, yPos, paintLine)
            yPos += 35f
        }

        canvas.drawText(note.type.uppercase(), 50f, 50f, paintBody.apply { textSize = 10f; isFakeBoldText = true })

        val lines = note.content.lines()
        val title = lines.firstOrNull() ?: "Sin título"
        canvas.drawText(title, 50f, 90f, paintTitle)

        var yText = 145f
        if (lines.size > 1) {
            lines.drop(1).forEach { line ->
                if (yText < 800f) {
                    canvas.drawText(line, 50f, yText, paintBody.apply { textSize = 16f; isFakeBoldText = false })
                    yText += 35f
                }
            }
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "MindBox_${note.id}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Nota como PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}