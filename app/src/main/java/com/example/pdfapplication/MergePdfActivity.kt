package com.example.pdfapplication

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pdfapplication.model.DocumentFile
import com.example.pdfapplication.ui.theme.PdfApplicationTheme

class MergePdfActivity : ComponentActivity() {

    private lateinit var selectedFiles: MutableState<MutableList<DocumentFile>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PdfApplicationTheme {
                val isMerge = remember { mutableStateOf(false) }
                val context = LocalContext.current
                val pdfFiles = remember { mutableStateOf(loadAllPdfFiles(context)) }

                selectedFiles = remember { mutableStateOf(mutableListOf()) }

                val title = remember { mutableStateOf("${selectedFiles.value.size} Selected") }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Back button
                        IconButton(onClick = { finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        Text(
                            title.value,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(pdfFiles.value) { file ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val isSelected = selectedFiles.value.any { it.filePath == file.filePath }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            selectedFiles.value = selectedFiles.value.toMutableList().apply { add(file) }
                                        } else {
                                            selectedFiles.value = selectedFiles.value.toMutableList().apply { remove(file) }
                                        }
                                        title.value = "${selectedFiles.value.size} Selected"
                                    }
                                )
                                Text(file.fileName, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Merge button
                    Button(
                        onClick = {
                            isMerge.value = true
                            Toast.makeText(
                                context,
                                "${selectedFiles.value.size} Selected",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedFiles.value.size > 1
                    ) {
                        Text("Continue")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isMerge.value && selectedFiles.value.size > 1) {
                    LaunchedEffect(Unit) {
                        val mergeIntent = Intent(context, MergeActivity::class.java)
                        mergeIntent.putExtra("list", ArrayList(selectedFiles.value))
                        context.startActivity(mergeIntent)
                        isMerge.value = false
                    }
                }
            }
        }
    }

    private fun loadAllPdfFiles(context: Context): List<DocumentFile> {
        val files = mutableListOf<DocumentFile>()
        val uri: Uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            "${MediaStore.Files.FileColumns.MIME_TYPE} = ?",
            arrayOf("application/pdf"),
            null
        )

        cursor?.use {
            val dataColumnIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val displayNameColumnIndex =
                it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

            while (it.moveToNext()) {
                val filePath = it.getString(dataColumnIndex)
                val fileName = it.getString(displayNameColumnIndex)
                files.add(DocumentFile(filePath, fileName, "", 0))
            }
        }

        return files
    }
}