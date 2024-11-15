package com.example.pdfapplication.view

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import java.io.File
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewer(pageBitmaps: List<Bitmap>, filePath: String, onBack: () -> Unit) {
    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<List<Int>>>(emptyList()) }

    val pageBitmapsWithDeviceWidth = remember(pageBitmaps) {
        pageBitmaps.map { bitmap ->
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                screenWidth,
                (bitmap.height * (screenWidth.toFloat() / bitmap.width)).toInt(),
                false
            )
            scaledBitmap
        }
    }

    val pageTexts: List<String> = remember(pageBitmaps) {
        extractTextFromPdf(filePath)
        pageBitmaps.mapIndexed { index, _ ->
            "Page ${index + 1}: Example text content for search."
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    fun performSearch() {
        searchResults = pageTexts.map { pageText ->
            if (searchQuery.isNotEmpty()) {
                Regex(searchQuery, RegexOption.IGNORE_CASE).findAll(pageText)
                    .map { it.range.first }
                    .toList()
            } else {
                emptyList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                }
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                itemsIndexed(pageBitmapsWithDeviceWidth) { index, bitmap ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .aspectRatio(1280f / 959f)
                    ) {
                        val pageText = pageTexts.getOrNull(index).orEmpty()
                        val matches = searchResults.getOrNull(index).orEmpty()

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawImage(bitmap.asImageBitmap())

                            matches.forEach { matchStartIndex ->
                                drawRect(
                                    color = Color.Yellow.copy(alpha = 0.5f),
                                    topLeft = Offset(matchStartIndex.toFloat(), 0f),
                                    size = Size(
                                        100f,
                                        20f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    )

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    performSearch()
                    showSearchDialog = false
                }) {
                    Text("Search")
                }
            },
            text = {
                Column {
                    Text("Enter search query:")
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

fun extractTextFromPdf(filePath: String): String {
    val reader = PdfReader(filePath)
    val pdfDocument = PdfDocument(reader)
    val stringBuilder = StringBuilder()

    val numberOfPages = getNumberOfPages(filePath)

    // Iterate through all pages and extract text
    for (i in 1..numberOfPages) {
        val page: PdfPage = pdfDocument.getPage(i) // Correctly retrieve the PdfPage
        val strategy = SimpleTextExtractionStrategy() // Text extraction strategy
        val pageText = PdfTextExtractor.getTextFromPage(page, strategy) // Extract text from page
        stringBuilder.append(pageText)
    }

    pdfDocument.close()
    reader.close()
    return stringBuilder.toString()
}

fun getNumberOfPages(filePath: String): Int {
    val file = File(filePath)
    val reader = PdfReader(filePath)
    val pdfDocument = PdfDocument(reader)
    val numberOfPages = pdfDocument.numberOfPages
    pdfDocument.close()
    reader.close()
    return numberOfPages
}
