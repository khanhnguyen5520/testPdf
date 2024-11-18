package com.example.pdfapplication

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.pdfapplication.view.ExcelViewer
import com.example.pdfapplication.view.PdfViewScreen
import com.example.pdfapplication.view.PdfViewer
import com.example.pdfapplication.view.PptViewer
import com.example.pdfapplication.view.WordViewer
import java.io.File

class EditFileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePath = intent.getStringExtra("filePath") ?: ""
        val file = File(filePath)

        val fileType = getFileType(file)
        val pageBitmaps = if (fileType == FileType.PDF) {
            renderPdfToBitmaps(file)
        } else {
            emptyList()
        }

        setContent {
            FileViewer(
                file = file,
                fileType = fileType,
                pageBitmaps = pageBitmaps,
                onBack = { finish() })
        }
    }

    private fun getFileType(file: File): FileType {
        val extension = file.extension.lowercase()
        return when (extension) {
            "pdf" -> FileType.PDF
            in listOf("doc", "docx") -> FileType.WORD
            in listOf("xls", "xlsx") -> FileType.EXCEL
            in listOf("ppt", "pptx") -> FileType.PPT
            else -> FileType.UNKNOWN
        }
    }

    private fun renderPdfToBitmaps(pdfFile: File): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val fileDescriptor =
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)

            for (pageIndex in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            pdfRenderer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmaps
    }
}

enum class FileType {
    PDF, WORD, EXCEL, PPT, UNKNOWN
}

@Composable
fun FileViewer(file: File, fileType: FileType, pageBitmaps: List<Bitmap>, onBack: () -> Unit) {
    when (fileType) {
        FileType.PDF -> PdfViewScreen(pageBitmaps, filePath = file.path, onBack = onBack)
        FileType.WORD -> WordViewer(file)
        FileType.EXCEL -> ExcelViewer(file)
        FileType.PPT -> PptViewer(file)
        else -> Text("Unsupported file type.")
    }
}