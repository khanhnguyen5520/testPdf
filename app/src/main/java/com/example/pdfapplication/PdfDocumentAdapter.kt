package com.example.pdfapplication

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class PdfDocumentAdapter(context: Context, path: String) : PrintDocumentAdapter() {
    internal var context: Context? = null
    internal var path = ""

    init {
        this.context = context
        this.path = path
    }

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback!!.onLayoutCancelled()
        } else {
            val builder = PrintDocumentInfo.Builder("file name")
            builder.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            callback!!.onLayoutFinished(builder.build(), newAttributes != oldAttributes)
        }
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        var input: InputStream? = null
        var output: OutputStream? = null
        try {
            val file = File(path)
            input = FileInputStream(file)
            output = FileOutputStream(destination!!.fileDescriptor)
            if (!cancellationSignal!!.isCanceled) {
                input.copyTo(output)
                callback!!.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } else {
                callback!!.onWriteCancelled()
            }
        } catch (e: Exception) {
            callback!!.onWriteFailed(e.message)
            Log.e("PDF Document Adapter", "onWrite: ${e.message}")
        } finally {
            try {
                input!!.close()
                output!!.close()
            } catch (e: IOException) {
                Log.e("PDF Document Adapter", "onWrite: ${e.message}")
            }
        }
    }
}