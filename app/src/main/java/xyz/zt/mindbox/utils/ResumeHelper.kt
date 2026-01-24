package xyz.zt.mindbox.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import xyz.zt.mindbox.data.model.Certificate
import xyz.zt.mindbox.data.model.ResumeData
import java.io.File
import java.io.FileOutputStream

object ResumeHelper {
    fun generateResumePdf(context: Context, data: ResumeData, certificates: List<Certificate>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.color = Color.parseColor("#263238")
        canvas.drawRect(0f, 0f, 200f, 792f, paint)

        data.photoUri?.let { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val scaled = Bitmap.createScaledBitmap(bitmap, 140, 180, true)
                canvas.drawBitmap(scaled, 30f, 40f, null)
            } catch (e: Exception) { e.printStackTrace() }
        }

        paint.color = Color.WHITE
        paint.textSize = 10f
        var ySide = 250f

        if (data.personalInfo.phone.isNotEmpty() || data.personalInfo.email.isNotEmpty()) {
            canvas.drawText("CONTACTO", 30f, ySide, paint.apply { isFakeBoldText = true }); ySide += 20f
            paint.isFakeBoldText = false
            if (data.personalInfo.phone.isNotEmpty()) {
                canvas.drawText(data.personalInfo.phone, 30f, ySide, paint); ySide += 15f
            }
            if (data.personalInfo.email.isNotEmpty()) {
                canvas.drawText(data.personalInfo.email, 30f, ySide, paint); ySide += 15f
            }
            ySide += 15f
        }

        if (data.personalInfo.address.isNotEmpty()) {
            canvas.drawText("DIRECCIÓN", 30f, ySide, paint.apply { isFakeBoldText = true }); ySide += 20f
            paint.isFakeBoldText = false
            val addressLines = data.personalInfo.address.chunked(25)
            addressLines.forEach { line -> canvas.drawText(line, 30f, ySide, paint); ySide += 15f }
            ySide += 5f
        }

        if (data.languages.isNotEmpty()) {
            ySide += 20f
            canvas.drawText("IDIOMAS", 30f, ySide, paint.apply { isFakeBoldText = true }); ySide += 20f
            paint.isFakeBoldText = false
            data.languages.forEach {
                canvas.drawText("${it.name}: ${it.level}", 30f, ySide, paint); ySide += 15f
            }
        }

        paint.color = Color.parseColor("#1A237E")
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText(data.personalInfo.name.uppercase(), 220f, 70f, paint)

        paint.textSize = 12f
        paint.color = Color.DKGRAY
        paint.isFakeBoldText = false
        if (data.personalInfo.professionalId.isNotEmpty()) {
            canvas.drawText("Cédula: ${data.personalInfo.professionalId}", 220f, 90f, paint)
        }

        var y = 140f

        if (data.experiences.isNotEmpty()) {
            drawHeader(canvas, paint, "EXPERIENCIA LABORAL", 220f, y)
            y += 30f
            data.experiences.forEach { exp ->
                paint.textSize = 11f; paint.isFakeBoldText = true; paint.color = Color.BLACK
                canvas.drawText(exp.position, 220f, y, paint); y += 15f
                paint.textSize = 10f; paint.isFakeBoldText = false; paint.color = Color.parseColor("#5C6BC0")
                canvas.drawText("${exp.company} | ${exp.period}", 220f, y, paint); y += 15f
                paint.color = Color.BLACK; paint.textSize = 9f
                val descLines = exp.description.chunked(50)
                descLines.forEach { line ->
                    canvas.drawText(line, 220f, y, paint)
                    y += 12f
                }
                y += 10f
            }
            y += 10f
        }

        drawHeader(canvas, paint, "FORMACIÓN ACADÉMICA", 220f, y)
        y += 30f
        paint.textSize = 10f
        paint.color = Color.BLACK
        paint.isFakeBoldText = false

        if (data.education.university.isNotEmpty()) {
            paint.isFakeBoldText = true
            canvas.drawText("Universidad:", 220f, y, paint)
            paint.isFakeBoldText = false
            y += 14f
            canvas.drawText(data.education.university, 220f, y, paint)
            y += 18f
        }

        if (data.education.postgraduate.isNotEmpty()) {
            paint.isFakeBoldText = true
            canvas.drawText("Posgrado:", 220f, y, paint)
            paint.isFakeBoldText = false
            y += 14f
            canvas.drawText(data.education.postgraduate, 220f, y, paint)
            y += 18f
        }

        if (data.education.secondary.isNotEmpty()) {
            paint.isFakeBoldText = true
            canvas.drawText("Secundaria:", 220f, y, paint)
            paint.isFakeBoldText = false
            y += 14f
            canvas.drawText(data.education.secondary, 220f, y, paint)
            y += 18f
        }

        if (certificates.isNotEmpty()) {
            y += 10f
            paint.textSize = 11f
            paint.isFakeBoldText = true
            paint.color = Color.parseColor("#1A237E")
            canvas.drawText("CURSOS Y CERTIFICACIONES", 220f, y, paint)
            y += 20f

            paint.textSize = 9f
            paint.isFakeBoldText = false
            paint.color = Color.BLACK

            certificates.forEach { cert ->
                canvas.drawText("• ${cert.title}", 220f, y, paint)
                y += 13f
                paint.color = Color.parseColor("#5C6BC0")
                canvas.drawText("  ${cert.platform} | ${cert.issueDate}", 225f, y, paint)
                paint.color = Color.BLACK
                y += 16f
            }
            y += 5f
        }

        if (data.skills.isNotEmpty()) {
            y += 15f
            drawHeader(canvas, paint, "HABILIDADES", 220f, y)
            y += 25f
            paint.textSize = 9f
            paint.color = Color.BLACK
            paint.isFakeBoldText = false
            val skillLines = data.skills.chunked(60)
            skillLines.forEach { line ->
                canvas.drawText(line, 220f, y, paint)
                y += 13f
            }
        }

        if (data.references.isNotEmpty()) {
            y += 15f
            drawHeader(canvas, paint, "REFERENCIAS", 220f, y)
            y += 25f
            paint.textSize = 9f
            paint.color = Color.BLACK

            data.references.forEach { ref ->
                paint.isFakeBoldText = true
                canvas.drawText(ref.name, 220f, y, paint)
                y += 13f
                paint.isFakeBoldText = false
                canvas.drawText("${ref.company} | ${ref.phone}", 220f, y, paint)
                y += 12f
                canvas.drawText(ref.email, 220f, y, paint)
                y += 18f
            }
        }

        pdfDocument.finishPage(page)

        val fileName = "CV_${data.personalInfo.name.replace(" ", "_")}.pdf"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "CV guardado en Descargas/$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawHeader(canvas: Canvas, paint: Paint, text: String, x: Float, y: Float) {
        paint.color = Color.parseColor("#1A237E")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText(text, x, y, paint)
        paint.strokeWidth = 2f
        canvas.drawLine(x, y + 5f, 580f, y + 5f, paint)
    }
}