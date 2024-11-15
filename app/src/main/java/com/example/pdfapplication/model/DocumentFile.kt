package com.example.pdfapplication.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DocumentFile(
    val filePath: String,
    val fileName: String,
    val dateModified: String,
    val fileSize: Long
) : Parcelable
