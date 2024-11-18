package com.example.pdfapplication.view

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import com.itextpdf.kernel.geom.Point
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewer(pageBitmaps: List<Bitmap>, filePath: String, onBack: () -> Unit) {
    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels
    val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels

    var searchQuery by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<List<Int>>>(emptyList()) }

    var isEditing by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    val selections by remember { mutableStateOf<MutableList<Pair<Offset, Offset>>>(mutableListOf()) }

    val pageBitmapsWithDeviceWidth = remember(pageBitmaps) {
        pageBitmaps.map { bitmap ->
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                screenWidth,
                screenHeight,
                false
            )
            scaledBitmap
        }
    }

    fun applyEditsToPdf() {
        val pdfReader = PdfReader(filePath)
        val pdfDocument = PdfDocument(pdfReader)
        val pdfWriter = PdfWriter(filePath.replace(".pdf", "_edited.pdf"))
        val editedDocument = PdfDocument(pdfWriter)

        for (i in 1..pdfDocument.numberOfPages) {
            val page = pdfDocument.getPage(i)
            val pageRect = page.pageSize

            // Process all selections for the page
            selections.forEach { (start, end) ->
                val startPdfCoords = mapToPdfCoordinates(start, pageRect, screenWidth)
                val endPdfCoords = mapToPdfCoordinates(end, pageRect, screenWidth)

                val rect = Rectangle(
                    startPdfCoords.x.toFloat(),
                    startPdfCoords.y.toFloat(),
                    endPdfCoords.x.toFloat(),
                    endPdfCoords.y.toFloat()
                )
                val canvas = PdfCanvas(page)
                canvas.setFillColor(ColorConstants.BLACK)
                canvas.rectangle(rect)
                canvas.fill()
            }
        }

        pdfDocument.close()
        pdfReader.close()
        editedDocument.close()
    }

    val pageTexts: List<String> = remember(pageBitmaps) {
        extractTextFromPdf(filePath)
        List(pageBitmaps.size) { index ->
            "Page ${index + 1}: Example text content for search."
        }
    }

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
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = if (isEditing) Color.Red else Color.Black
                        )
                    }

                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
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
                            .pointerInput(isEditing) {
                                if (isEditing) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            dragStart = offset
                                            dragEnd = null
                                        },
                                        onDragEnd = {
                                            dragEnd?.let { end ->
                                                dragStart?.let { start ->
                                                    selections.add(start to end)
                                                }
                                            }
                                            dragStart = null
                                            dragEnd = null
                                        },
                                        onDrag = { change, dragAmount ->
                                            dragEnd = change.position
                                        }
                                    )
                                }
                            }
                    ) {
                        val matches = searchResults.getOrNull(index).orEmpty()

                        Image(bitmap.asImageBitmap(), contentDescription = null)

                        DisposableEffect(bitmap) {
                            onDispose {
                                bitmap.recycle()
                            }
                        }

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

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            selections.forEach { (start, end) ->
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    topLeft = start,
                                    size = Size(end.x - start.x, end.y - start.y)
                                )
                            }

                            // Draw current drag rectangle
                            if (dragStart != null && dragEnd != null) {
                                drawRect(
                                    color = Color.Blue.copy(alpha = 0.3f),
                                    topLeft = dragStart!!,
                                    size = Size(
                                        dragEnd!!.x - dragStart!!.x,
                                        dragEnd!!.y - dragStart!!.y
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

fun mapToPdfCoordinates(offset: Offset, pageRect: Rectangle, screenWidth: Int): Point {
    val pdfWidth = pageRect.width
    val pdfHeight = pageRect.height

    val xRatio = pdfWidth / screenWidth
    val yRatio = pdfHeight / screenWidth

    return Point(
        offset.x.toDouble() * xRatio,
        pdfHeight - (offset.y.toDouble() * yRatio)
    )
}

fun extractTextFromPdf(filePath: String): String {
    val reader = PdfReader(filePath)
    val pdfDocument = PdfDocument(reader)
    val stringBuilder = StringBuilder()

    val numberOfPages = getNumberOfPages(filePath)

    for (i in 1..numberOfPages) {
        val page: PdfPage = pdfDocument.getPage(i)
        val strategy = SimpleTextExtractionStrategy()
        val pageText = PdfTextExtractor.getTextFromPage(page, strategy)
        stringBuilder.append(pageText)
    }

    pdfDocument.close()
    reader.close()
    return stringBuilder.toString()
}

fun getNumberOfPages(filePath: String): Int {
    val reader = PdfReader(filePath)
    val pdfDocument = PdfDocument(reader)
    val numberOfPages = pdfDocument.numberOfPages
    pdfDocument.close()
    reader.close()
    return numberOfPages
}