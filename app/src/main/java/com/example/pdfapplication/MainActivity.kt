package com.example.pdfapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.pdfapplication.model.DocumentFile
import com.example.pdfapplication.ui.theme.PdfApplicationTheme
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestStoragePermissions()

        setContent {
            PdfApplicationTheme {
                var reloadFiles by remember { mutableStateOf(false) }
                var files by remember { mutableStateOf(loadAllOfficeFiles(this)) }

                LaunchedEffect(reloadFiles) {
                    if (reloadFiles) {
                        files = loadAllOfficeFiles(this@MainActivity)
                        reloadFiles = false
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FilterBar(
                        files = files,
                        onDelete = { filePath ->
                            val file = File(filePath)
                            val isDeleted = deleteFile(file)
                            if (isDeleted) {
                                Toast.makeText(this, "File Deleted", Toast.LENGTH_SHORT).show()
                                reloadFiles = true
                            } else {
                                Toast.makeText(this, "Failed to Delete File", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${packageName}")
                )
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        }
    }

    override fun onResume() {
        super.onResume()
        requestStoragePermissions()
    }
}

fun loadAllOfficeFiles(context: Context): List<DocumentFile> {
    val files = mutableListOf<DocumentFile>()

    val uri: Uri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.MIME_TYPE
    )

    val mimeTypes = listOf(
        "application/pdf",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    )

    val selection =
        "${MediaStore.Files.FileColumns.MIME_TYPE} IN (${mimeTypes.joinToString { "?" }})"
    val selectionArgs = mimeTypes.toTypedArray()

    context.contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
    )?.use { cursor ->
        val dataColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
        val displayNameColumnIndex =
            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val dateModifiedColumnIndex =
            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
        val sizeColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

        while (cursor.moveToNext()) {
            val filePath = cursor.getString(dataColumnIndex)
            val fileName = cursor.getString(displayNameColumnIndex) ?: "Unknown"
            val dateModified = cursor.getLong(dateModifiedColumnIndex)
            val fileSize = cursor.getLong(sizeColumnIndex)

            if (!filePath.isNullOrEmpty() && File(filePath).exists()) {
                files.add(
                    DocumentFile(
                        filePath = filePath,
                        fileName = fileName,
                        dateModified = formatDate(dateModified),
                        fileSize = fileSize
                    )
                )
            }
        }
    }

    files.addAll(loadFilesFromDownloads())

    return files.distinctBy { it.filePath }
}

fun loadFilesFromDownloads(): List<DocumentFile> {
    val files = mutableListOf<DocumentFile>()
    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    if (downloadDir.exists() && downloadDir.isDirectory) {
        downloadDir.listFiles()?.forEach { file ->
            if (file.isFile && isSupportedFile(file)) {
                files.add(
                    DocumentFile(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        dateModified = formatDate(file.lastModified()),
                        fileSize = file.length()
                    )
                )
            }
        }
    }

    return files
}

fun isSupportedFile(file: File): Boolean {
    val supportedExtensions = listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx")
    val fileExtension = file.extension.lowercase()

    return supportedExtensions.contains(fileExtension)
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy")
        .format(Date(timestamp))
}

@Composable
fun FileList(
    files: List<DocumentFile>,
    onOptionsClick: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: (DocumentFile) -> Unit
) {
    LazyColumn(modifier = modifier) {
        items(files) { file -> FileItem(file, onOptionsClick, onEditClick = onEditClick) }
    }
}

@Composable
fun FileItem(
    file: DocumentFile,
    onOptionsClick: (DocumentFile) -> Unit,
    onEditClick: (DocumentFile) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onEditClick(file) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.fileName, style = MaterialTheme.typography.headlineSmall)
            Row {
                Text(text = file.dateModified, style = MaterialTheme.typography.labelSmall)
                Text(text = " * ", style = MaterialTheme.typography.labelSmall)
                Text(text = formatSize(file.fileSize), style = MaterialTheme.typography.labelSmall)
            }
        }
        IconButton(onClick = { onOptionsClick(file) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
        }
    }
}

@Composable
fun FileOptionsBottomSheet(
    file: DocumentFile,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPDF by remember { mutableStateOf(true) }
    isPDF = File(file.filePath).extension.contains("pdf")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                onClick = { onDismiss() },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(text = file.fileName, style = MaterialTheme.typography.titleLarge)
                Text(text = file.filePath, style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                //Rename button
                Button(
                    onClick = { renameFile(context, file.filePath) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Rename") }

                // Merge PDF Button
                AnimatedVisibility(
                    visible = isPDF
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(context, MergePdfActivity::class.java)
                            context.startActivity(intent)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Merge PDF")
                    }
                }

                // Split PDF Button
                AnimatedVisibility(
                    visible = isPDF
                ) {
                    Button(
                        onClick = {
                            // Handle file splitting
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Split PDF")
                    }
                }

                //Share PDF button
                Button(
                    onClick = {
                        sharePdf(file.filePath, context)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share")
                }

                //Print PDF button
                AnimatedVisibility(
                    visible = isPDF
                ) {
                    Button(
                        onClick = {
                            printPdf(file.filePath, context)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Print")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                //Delete PDF button
                Button(
                    onClick = {
                        onDelete(file.filePath)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete")
                }

                //Convert to Pdf
                AnimatedVisibility(
                    visible = !isPDF && File(file.filePath).extension.lowercase() !in listOf(
                        ".xls",
                        ".xlsx"
                    )
                ) {
                    Button(
                        onClick = {
                            convertWordToPdf(File(file.filePath))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val text =
                            if (File(file.filePath).extension.lowercase() in listOf("doc", "docx")
                            ) {
                                "Word to PDF"
                            } else {
                                ""
                            }
                        Text(text)
                    }
                }
            }
        }
    }
}

fun renameFile(context: Context, filePath: String) {
    val oldFile = File(filePath)
    val parentDirectory = oldFile.parentFile
    val newName = "testName2.pdf"
    val newFile = File(parentDirectory, newName)
    if (oldFile.renameTo(newFile)) {
        Toast.makeText(
            context,
            "Rename successfully",
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun convertWordToPdf(wordFile: File) {
    try {
        // Validate input file
        if (!wordFile.exists() || !wordFile.isFile || !wordFile.name.endsWith(".docx", true)) {
            throw IllegalArgumentException("Invalid Word file. Please provide a valid .docx file.")
        }

        val pdfFile = File(wordFile.parent, wordFile.nameWithoutExtension + ".pdf")

        Log.e("kkk", "Starting conversion. Output file: ${pdfFile.absolutePath}")

        FileInputStream(wordFile).use { inputStream ->
            val document = XWPFDocument(inputStream)

            FileOutputStream(pdfFile).use { outputStream ->
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val pdfLayoutDocument = Document(pdfDocument)

                for (paragraph in document.paragraphs) {
                    val text = paragraph.text.trim()
//                    Log.e("kkk", "Extracted Paragraph: $text")
                    if (text.isNotEmpty()) {
                        pdfLayoutDocument.add(Paragraph(text))
                    }
                }

                pdfLayoutDocument.close()
                pdfDocument.close()

                Log.e("kkk", "PDF created successfully at: ${pdfFile.absolutePath}")
            }

            document.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("kkk", "Error while converting Word to PDF: ${e.message}")
    }
}

fun sharePdf(filePath: String, context: Context) {
    val file = File(filePath)
    if (file.exists()) {
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Sharing PDF")
            putExtra(Intent.EXTRA_TEXT, "Here is the PDF file.")
        }

        val chooser = Intent.createChooser(shareIntent, "Share PDF")
        context.startActivity(chooser)
    } else {
        Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
    }
}

fun printPdf(filePath: String, context: Context) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    try {
        val printAdapter = PdfDocumentAdapter(context, filePath)
        printManager.print("Document", printAdapter, PrintAttributes.Builder().build())
    } catch (e: Exception) {
        Log.e("MainActivity", "printPDF:${e.message} ")
    }
}

fun deleteFile(file: File): Boolean {
    return try {
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun formatSize(size: Long): String {
    return when {
        size < 1024 -> "$size bytes"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}

@Preview(showBackground = true)
@Composable
fun FileTabsPreview() {
    val mockFiles = listOf(
        DocumentFile("path1", "Document1.pdf", "2024-11-01", 12345),
        DocumentFile("path2", "Document2.docx", "2024-11-02", 67890),
        DocumentFile("path3", "Spreadsheet.xlsx", "2024-11-03", 4567),
        DocumentFile("path4", "Presentation.pptx", "2024-11-04", 78901)
    )
    PdfApplicationTheme {
        FilterBar(files = mockFiles, onDelete = {})
    }
}