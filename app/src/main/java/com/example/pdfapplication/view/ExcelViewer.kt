package com.example.pdfapplication.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileInputStream

@Composable
fun ExcelViewer(file: File) {
    val text = readExcelFile(file)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        BasicText(text = text)
    }
}

fun readExcelFile(file: File): String {
    val text = StringBuilder()
    try {
        val fis = FileInputStream(file)
        val workbook = WorkbookFactory.create(fis)
        val sheet = workbook.getSheetAt(0)
        for (row in sheet) {
            for (cell in row) {
                text.append(cell.toString()).append("\t")
            }
            text.append("\n")
        }
        fis.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return text.toString()
}