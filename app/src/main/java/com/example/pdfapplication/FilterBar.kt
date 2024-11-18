package com.example.pdfapplication

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.pdfapplication.model.DocumentFile

@Composable
fun FilterBar(
    files: List<DocumentFile>,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedTab = FileTab.entries[selectedTabIndex]
    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<DocumentFile?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                FileTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab.title) }
                    )
                }
            }

            val filteredFiles = files.filter {
                selectedTab.extensions.isEmpty() || selectedTab.extensions.any { ext ->
                    it.filePath.endsWith(ext, ignoreCase = true)
                }
            }

            FileList(
                files = filteredFiles,
                onOptionsClick = { file ->
                    selectedFile = file
                    showBottomSheet = true
                },
                modifier = Modifier.weight(1f),
                onEditClick = { file ->
                    val intent = Intent(context, EditFileActivity::class.java).apply {
                        putExtra("filePath", file.filePath)
                    }
                    context.startActivity(intent)
                }
            )
        }

        if (showBottomSheet && selectedFile != null) {
            FileOptionsBottomSheet(
                file = selectedFile!!,
                onDelete = onDelete,
                onDismiss = { showBottomSheet = false }
            )
        }
    }
}