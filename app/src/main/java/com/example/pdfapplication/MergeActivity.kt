@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.pdfapplication

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfapplication.model.DocumentFile
import com.example.pdfapplication.ui.theme.PdfApplicationTheme
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import java.io.File

class MergeActivity : ComponentActivity() {

    private lateinit var files: ArrayList<DocumentFile>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        files = intent.getSerializableExtra("list") as? ArrayList<DocumentFile> ?: arrayListOf()

        setContent {
            PdfApplicationTheme {
                FileListScreen(
                    files = files,
                    onBack = { finish() }, context = this
                )
            }
        }
    }
}

@Composable
fun FileListScreen(
    files: ArrayList<DocumentFile>,
    onBack: () -> Unit,
    context: Context
) {
    var fileList by remember { mutableStateOf(files.toMutableList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reorder and Merge Files") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    itemsIndexed(fileList, key = { _, file -> file.filePath }) { index, file ->
                        FileListItem(
                            file = file,
                            onDelete = {
                                fileList = fileList.toMutableList().apply { remove(file) }
                            },
                            onMove = { fromIndex, toIndex ->
                                fileList = fileList.toMutableList().apply {
                                    val movedFile = removeAt(fromIndex)
                                    add(toIndex, movedFile)
                                }
                            },
                            currentIndex = index,
                            totalItems = fileList.size
                        )
                    }
                }

                Button(
                    onClick = { mergeSelectedPdfs(files, context = context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = fileList.size > 1
                ) {
                    Text("Merge Files")
                }
            }
        }
    )
}

@Composable
fun FileListItem(
    file: DocumentFile,
    onDelete: () -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    currentIndex: Int,
    totalItems: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag Handle
        Icon(
            painter = painterResource(id = R.drawable.ic_drag_handle),
            contentDescription = "Drag",
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    if (currentIndex > 0) onMove(currentIndex, currentIndex - 1)
                    if (currentIndex < totalItems - 1) onMove(
                        currentIndex,
                        currentIndex + 1
                    )
                }
        )

        Spacer(modifier = Modifier.width(16.dp))

        // File Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = file.fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.dateModified,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
            Text(
                text = "${file.fileSize / 1024} KB",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Delete Icon
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Close, contentDescription = "Close")
        }
    }
}

private fun mergeSelectedPdfs(files: ArrayList<DocumentFile>, context: Context) {
    if (files.size > 1) {
        mergePdfFiles(context, files) { success ->
            if (success) {
                Toast.makeText(context, "Successfully Merged", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to Merge", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        Toast.makeText(context, "Please select at least two files", Toast.LENGTH_SHORT).show()
    }
}

private fun mergePdfFiles(
    context: Context,
    files: ArrayList<DocumentFile>,
    onComplete: (Boolean) -> Unit
) {
    try {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        if (outputDir?.exists() != true) {
            outputDir?.mkdirs()
        }

        val outputFile = File(outputDir, "merged.pdf")

        if (outputFile.exists()) {
            outputFile.delete()
        }

        val pdfDocument = PdfDocument(PdfWriter(outputFile))

        // Merge the PDFs
        files.forEach { file ->
            val reader = PdfReader(file.filePath)
            PdfDocument(reader).apply {
                copyPagesTo(1, numberOfPages, pdfDocument)
                close()
            }
        }

        pdfDocument.close()

        if (outputFile.exists()) {
            onComplete(true)
            Log.e("kkk", "Output File Path: ${outputFile.absolutePath}")
            Toast.makeText(context, "Successfully merged", Toast.LENGTH_SHORT).show()
        } else {
            onComplete(false)
            Toast.makeText(context, "Merge failed, file not created", Toast.LENGTH_SHORT).show()
        }

    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(false)
        Toast.makeText(context, "Merge failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    PdfApplicationTheme {
//        Greeting("Android")
//    }
//}