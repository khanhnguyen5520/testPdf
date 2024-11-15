package com.example.pdfapplication.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream

@Composable
fun WordViewer(file: File) {
    val text = readWordFile(file)
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        BasicText(text = text)
    }
}

fun readWordFile(file: File): String {
    val text = StringBuilder()
    try {
        val fis = FileInputStream(file)
        val document = XWPFDocument(fis)
        for (paragraph in document.paragraphs) {
            text.append(paragraph.text).append("\n")
        }
        fis.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return text.toString()
}