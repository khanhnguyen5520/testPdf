package com.example.pdfapplication.view

import android.app.Presentation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PptViewer(file: File) {
    val text = readPptxFile(file)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        BasicText(text = text)
    }
}

fun readPptxFile(file: File): String {
    val text = StringBuilder()
    try {
        //val presentation = Presentation(file.path)

        // Loop through all slides
//        for (slideIndex in 0 until presentation.slides.size) {
//            val slide = presentation.slides[slideIndex]
//
//            // Loop through all shapes in each slide
//            for (shape in slide.shapes) {
//                if (shape.hasTextFrame()) {
//                    val textFrame = shape.textFrame
//                    for (paragraph in textFrame.paragraphs) {
//                        for (portion in paragraph.portions) {
//                            text.append(portion.text).append("\n")
//                        }
//                    }
//                }
//            }
//        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return text.toString()
}