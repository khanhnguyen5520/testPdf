package com.example.pdfapplication.view

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewer(pageBitmaps: List<Bitmap>, filePath: String, onBack: () -> Unit) {
    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels
    val extractedText = remember(filePath) { extractTextFromPdf(filePath) }
    var searchQuery by remember { mutableStateOf("") }

    // Scaled bitmaps based on device width
    val pageBitmapsWithDeviceWidth by remember(pageBitmaps, screenWidth) {
        derivedStateOf {
            pageBitmaps.map { bitmap ->
                Bitmap.createScaledBitmap(
                    bitmap,
                    screenWidth,
                    (bitmap.height * (screenWidth.toFloat() / bitmap.width)).toInt(),
                    false
                )
            }
        }
    }

    // Highlight ranges based on the search query
    val highlightedRanges by remember(searchQuery, extractedText) {
        derivedStateOf {
            extractedText.mapIndexed { index, pageText ->
                searchText(searchQuery, pageText).map { (start, end) ->
                    calculateHighlightCoordinates(
                        index,
                        start,
                        end,
                        pageText,
                        pageBitmapsWithDeviceWidth[index].width,
                        pageBitmapsWithDeviceWidth[index].height
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Viewer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
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
                    HighlightedImage(
                        bitmap = bitmap,
                        highlights = highlightedRanges[index]
                    )
                }

                item {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}

fun extractTextFromPdf(filePath: String): List<String> {
    return PdfReader(filePath).use { reader ->
        PdfDocument(reader).use { pdfDocument ->
            (1..pdfDocument.numberOfPages).map { pageIndex ->
                PdfTextExtractor.getTextFromPage(
                    pdfDocument.getPage(pageIndex),
                    SimpleTextExtractionStrategy()
                )
            }
        }
    }
}

fun searchText(query: String, text: String): List<Pair<Int, Int>> {
    val matches = mutableListOf<Pair<Int, Int>>()
    var startIndex = 0
    while (true) {
        val index = text.indexOf(query, startIndex, ignoreCase = true)
        if (index == -1) break
        matches.add(index to index + query.length)
        startIndex = index + query.length
    }
    return matches
}

fun calculateHighlightCoordinates(
    pageIndex: Int,
    startOffset: Int,
    endOffset: Int,
    text: String,
    bitmapWidth: Int,
    bitmapHeight: Int
): Rect {
    val totalCharacters = text.length
    val proportionX = bitmapWidth.toFloat() / totalCharacters
    val lineCount = text.lines().size
    val lineHeight = bitmapHeight / lineCount

    val lineNumber = text.substring(0, startOffset).lines().size - 1
    val startX = (startOffset % totalCharacters) * proportionX
    val startY = lineNumber * lineHeight

    val endX = startX + (endOffset - startOffset) * proportionX
    val endY = startY + lineHeight

    return Rect(
        startX.toInt(),
        startY.toInt(),
        endX.toInt(),
        endY.toInt()
    )
}

@Composable
fun HighlightedImage(bitmap: Bitmap, highlights: List<Rect>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        drawImage(bitmap.asImageBitmap())

        highlights.forEach { rect ->
            drawRect(
                color = Color.Yellow.copy(alpha = 0.5f),
                topLeft = Offset(x = rect.left.toFloat(), y = rect.top.toFloat()),
                size = Size(width = rect.width().toFloat(), height = rect.height().toFloat())
            )
        }
    }
}