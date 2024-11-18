package com.example.pdfapplication.view

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewScreen(pageBitmaps: List<Bitmap>, filePath: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels

    val pdfView = remember {
        PDFView(context,null)
    }

    var searchQuery by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<List<Int>>>(emptyList()) }

    var isEditing by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    val selectionsByPage by remember {
        mutableStateOf<MutableMap<Int, MutableList<Pair<Offset, Offset>>>>(
            mutableMapOf()
        )
    }

// Toggle draw mode
    var drawMode by remember { mutableStateOf(false) }

    // Store drawing paths per page
    val drawPaths = remember { mutableStateOf(mutableMapOf<Int, MutableList<Path>>()) }

    // Cache for rendered bitmaps
    val bitmapCache = remember { LruCache<Int, Bitmap>(10) }

    suspend fun renderPageAsync(pageIndex: Int): Bitmap {
        return withContext(Dispatchers.IO) {
            val pdfRenderer = PdfRenderer(
                ParcelFileDescriptor.open(
                    File(filePath),
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
            )
            val page = pdfRenderer.openPage(pageIndex)
            val height = (page.height * (screenWidth.toFloat() / page.width)).toInt()
            val bitmap = Bitmap.createBitmap(screenWidth, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pdfRenderer.close()
            bitmap
        }
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

                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Refresh, contentDescription = "Inversion")
                    }

                    IconButton(onClick = { drawMode = !drawMode }) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = "Draw",
                            tint = if (drawMode) Color.Red else Color.Black
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PdfViewer(filePath, drawMode)
            }
//            val pageCount = remember { getNumberOfPages(filePath) }
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(paddingValues)
//            ) {
//                items(pageCount) { pageIndex ->
//                    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
//                    var isLoading by remember { mutableStateOf(true) }
//
//                    LaunchedEffect(pageIndex) {
//                        isLoading = true
//                        bitmap = bitmapCache[pageIndex] ?: run {
//                            val loadedBitmap = renderPageAsync(pageIndex)
//                            bitmapCache.put(pageIndex, loadedBitmap)
//                            loadedBitmap
//                        }
//                        isLoading = false
//                    }
//
//                    if (!isLoading) {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(8.dp)
//                                .aspectRatio(bitmap!!.width.toFloat() / bitmap!!.height.toFloat())
//                                .pointerInput(isEditing || drawMode) {
//                                    if (isEditing) {
//                                        detectDragGestures(
//                                            onDragStart = { offset ->
//                                                dragStart = offset
//                                                dragEnd = null
//                                            },
//                                            onDragEnd = {
//                                                dragEnd?.let { end ->
//                                                    dragStart?.let { start ->
//                                                        val pageSelections =
//                                                            selectionsByPage[pageIndex]
//                                                                ?: mutableListOf()
//                                                        pageSelections.add(start to end)
//                                                        selectionsByPage[pageIndex] =
//                                                            pageSelections
//                                                    }
//                                                }
//                                                dragStart = null
//                                                dragEnd = null
//                                            },
//                                            onDrag = { change, _ ->
//                                                dragEnd = change.position
//                                            }
//                                        )
//                                    } else if (drawMode) {
//                                        // Handle drawing on the canvas
//                                        detectDragGestures(
//                                            onDragStart = { offset ->
//                                                val path = Path().apply { moveTo(offset.x, offset.y) }
//                                                // Add new path to the drawing state
//                                                drawPaths.value[pageIndex] =
//                                                    (drawPaths.value[pageIndex] ?: mutableListOf()).apply {
//                                                        add(path)
//                                                    }
//                                            },
//                                            onDrag = { change, _ ->
//                                                drawPaths.value[pageIndex]?.lastOrNull()?.lineTo(change.position.x, change.position.y)
//                                            },
//                                            onDragEnd = {
//                                            }
//                                        )
//                                    }
//                                }
//                        ) {
//                            val matches = searchResults.getOrNull(pageIndex).orEmpty()
//
//                            Image(bitmap!!.asImageBitmap(), contentDescription = null)
//
////                            DisposableEffect(bitmap) {
////                                onDispose {
////                                    bitmap.recycle()
////                                }
////                            }
//
////                            Canvas(modifier = Modifier.fillMaxSize()) {
////                                drawImage(bitmap!!.asImageBitmap())
////
////                                matches.forEach { matchStartIndex ->
////                                    drawRect(
////                                        color = Color.Yellow.copy(alpha = 0.5f),
////                                        topLeft = Offset(matchStartIndex.toFloat(), 0f),
////                                        size = Size(100f, 20f)
////                                    )
////                                }
////                            }
//
//                            Canvas(modifier = Modifier.fillMaxSize()) {
//                                // Draw all paths drawn for the current page
//                                drawPaths.value[pageIndex]?.forEach { path ->
//                                    drawPath(path, color = Color.Red, style = Stroke(width = 5f))
//                                }
//
//                                val pageSelections =
//                                    selectionsByPage[pageIndex] ?: emptyList()
//                                pageSelections.forEach { (start, end) ->
//                                    drawRect(
//                                        color = Color.Black.copy(alpha = 0.5f),
//                                        topLeft = start,
//                                        size = Size(end.x - start.x, end.y - start.y)
//                                    )
//                                }
//
//                                // Draw current drag rectangle
//                                if (dragStart != null && dragEnd != null) {
//                                    val start = dragStart!!
//                                    val end = dragEnd!!
//
//                                    drawRect(
//                                        color = Color.Blue.copy(alpha = 0.3f),
//                                        topLeft = start,
//                                        size = Size(end.x - start.x, end.y - start.y)
//                                    )
//                                }
//                            }
//                        }
//                    } else {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(8.dp)
//                                .aspectRatio(1f),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            CircularProgressIndicator()
//                        }
//                    }
//                }
//            }
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
    val pdfRenderer =
        PdfRenderer(ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY))
    val pageCount = pdfRenderer.pageCount
    pdfRenderer.close()
    return pageCount
}

@Composable
fun PdfViewer(filePath: String, drawMode: Boolean) {
    val context = LocalContext.current
    val pdfView = remember { PDFView(context, null) }

    // Update PDFView based on drawMode
    LaunchedEffect(filePath, drawMode) {
        Log.d("PdfViewer", "File path: $filePath, drawMode: $drawMode")

        pdfView.fromFile(File(filePath))
            .onPageError { page, t ->
                Log.e("PdfViewer", "Error loading page $page: ${t.message}")
            }
            .onError { t ->
                Log.e("PdfViewer", "Error loading PDF: ${t.message}")
            }
            .onLoad {
                Log.d("PdfViewer", "PDF loaded successfully")
            }
            .apply {
                if (drawMode) {
                    onDrawAll { canvas, pageWidth, pageHeight, _ ->
                        // Drawing logic: Example red circle in the center
                        val centerX = pageWidth / 2f
                        val centerY = pageHeight / 2f
                        val radius = 50f
                        canvas.drawCircle(
                            centerX,
                            centerY,
                            radius,
                            Paint().apply { color = Color.Red.value.toInt() }
                        )
                    }
                }
            }
            .pages(0, Int.MAX_VALUE)
            .enableSwipe(true)
            .load()
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        factory = { pdfView }
    )
}