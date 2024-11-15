package com.example.pdfapplication

enum class FileTab(val title: String, val extensions: List<String>) {
    All("All", emptyList()),
    PDF("PDF", listOf(".pdf")),
    Word("Word", listOf(".doc", ".docx")),
    Excel("Excel", listOf(".xls", ".xlsx")),
    PPT("PPT", listOf(".ppt", ".pptx"))
}