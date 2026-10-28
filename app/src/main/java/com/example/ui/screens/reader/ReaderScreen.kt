package com.example.ui.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FontFamilyPreference
import com.example.data.model.ReaderTheme
import com.example.ui.theme.ReaderDarkBg
import com.example.ui.theme.ReaderDarkText
import com.example.ui.theme.ReaderLightBg
import com.example.ui.theme.ReaderLightText
import com.example.ui.theme.ReaderSepiaBg
import com.example.ui.theme.ReaderSepiaText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    viewModel: ReaderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.endReadingSession()
        }
    }

    val book by viewModel.book.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val epubChapters by viewModel.epubChapters.collectAsStateWithLifecycle()
    val currentChapterIndex by viewModel.currentChapterIndex.collectAsStateWithLifecycle()
    val txtContent by viewModel.txtContent.collectAsStateWithLifecycle()
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val isControlsVisible by viewModel.isControlsVisible.collectAsStateWithLifecycle()
    val showCompletionDialog by viewModel.showCompletionDialog.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    var showAppearanceSheet by remember { mutableStateOf(false) }
    var showTocDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }

    val currentBook = book
    if (currentBook == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Opening reader...")
        }
        return
    }

    // Completion Dialog
    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCompletionDialog,
            title = { Text("Finished Reading!") },
            text = {
                Text("Congratulations! You've reached the end of \"${currentBook.title}\". Would you like to mark this book as Completed in your library?")
            },
            confirmButton = {
                Button(onClick = viewModel::markBookCompleted) {
                    Text("Yes, Mark Completed")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCompletionDialog) {
                    Text("Keep in Progress")
                }
            }
        )
    }

    // Jump to Page Dialog
    if (showJumpDialog) {
        var pageInput by remember { mutableStateOf(currentPage.toString()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                Column {
                    Text("Enter page number (1 to $totalPages):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("jump_page_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = pageInput.toIntOrNull() ?: currentPage
                        viewModel.onPageChanged(target, totalPages)
                        showJumpDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Table of Contents Dialog (for EPUB & TXT)
    if (showTocDialog) {
        AlertDialog(
            onDismissRequest = { showTocDialog = false },
            title = { Text("Table of Contents") },
            text = {
                val chapterTitles = when (currentBook.format.uppercase()) {
                    "EPUB" -> epubChapters.map { it.title }
                    "TXT" -> txtContent?.chapters?.map { it.title } ?: emptyList()
                    else -> (1..totalPages).map { "Page $it" }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    itemsIndexed(chapterTitles) { index, title ->
                        val isSelected = if (currentBook.format.uppercase() == "PDF") index + 1 == currentPage else index == currentChapterIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (currentBook.format.uppercase() == "EPUB") {
                                        viewModel.onChapterSelected(index)
                                    } else if (currentBook.format.uppercase() == "TXT") {
                                        viewModel.onTxtChapterSelected(index)
                                    } else {
                                        viewModel.onPageChanged(index + 1, totalPages)
                                    }
                                    showTocDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTocDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Bookmarks Dialog
    if (showBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarksDialog = false },
            title = { Text("Bookmarks (${bookmarks.size})") },
            text = {
                if (bookmarks.isEmpty()) {
                    Text("No bookmarks added yet for this book.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        items(bookmarks, key = { it.id }) { bm ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.onPageChanged(bm.pageNumber, totalPages)
                                        showBookmarksDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "p.${bm.pageNumber}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = bm.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteBookmark(bm.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarksDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Appearance Modal Bottom Sheet
    if (showAppearanceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAppearanceSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Reader Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Themes
                Text("Theme", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReaderTheme.entries.forEach { theme ->
                        val (bg, fg) = when (theme) {
                            ReaderTheme.LIGHT -> Pair(ReaderLightBg, ReaderLightText)
                            ReaderTheme.DARK -> Pair(ReaderDarkBg, ReaderDarkText)
                            ReaderTheme.SEPIA -> Pair(ReaderSepiaBg, ReaderSepiaText)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bg,
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (readerSettings.theme == theme) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.updateTheme(theme) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(theme.displayName, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Font Size Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size", style = MaterialTheme.typography.labelMedium)
                        Text("${readerSettings.fontSizeSp.toInt()} sp", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = readerSettings.fontSizeSp,
                        onValueChange = viewModel::updateFontSize,
                        valueRange = 14f..28f,
                        steps = 7
                    )
                }

                // Font Family
                Text("Font Family", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontFamilyPreference.entries.forEach { family ->
                        FilledTonalButton(
                            onClick = { viewModel.updateFontFamily(family) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (readerSettings.fontFamily == family) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (readerSettings.fontFamily == family) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(family.displayName, fontSize = 12.sp)
                        }
                    }
                }

                // Line Height Spacing
                Text("Line Spacing", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(Pair("Compact", 1.3f), Pair("Normal", 1.5f), Pair("Relaxed", 1.8f)).forEach { (label, multiplier) ->
                        FilledTonalButton(
                            onClick = { viewModel.updateLineSpacing(multiplier) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (readerSettings.lineHeightMultiplier == multiplier) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (readerSettings.lineHeightMultiplier == multiplier) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(label, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("reader_screen")
    ) {
        // Document Viewer depending on format
        when (currentBook.format.uppercase()) {
            "PDF" -> {
                PdfReaderView(
                    filePath = currentBook.filePath,
                    currentPage = currentPage,
                    onTotalPagesDiscovered = { total ->
                        viewModel.onPageChanged(currentPage, total)
                    },
                    onTap = viewModel::toggleControls,
                    onNextPage = {
                        if (currentPage < totalPages) {
                            viewModel.onPageChanged(currentPage + 1, totalPages)
                        }
                    },
                    onPreviousPage = {
                        if (currentPage > 1) {
                            viewModel.onPageChanged(currentPage - 1, totalPages)
                        }
                    }
                )
            }
            "EPUB" -> {
                val chapter = epubChapters.getOrNull(currentChapterIndex)
                TextEpubReaderView(
                    title = currentBook.title,
                    content = chapter?.plainText ?: "Loading chapter...",
                    chapterTitle = chapter?.title ?: "Chapter ${currentChapterIndex + 1}",
                    currentChapterIndex = currentChapterIndex,
                    totalChapters = epubChapters.size.coerceAtLeast(1),
                    settings = readerSettings,
                    onTap = viewModel::toggleControls,
                    onPreviousChapter = {
                        if (currentChapterIndex > 0) {
                            viewModel.onChapterSelected(currentChapterIndex - 1)
                        }
                    },
                    onNextChapter = {
                        if (currentChapterIndex < epubChapters.size - 1) {
                            viewModel.onChapterSelected(currentChapterIndex + 1)
                        }
                    }
                )
            }
            "TXT" -> {
                val chapters = txtContent?.chapters ?: emptyList()
                val chapter = chapters.getOrNull(currentChapterIndex)
                TextEpubReaderView(
                    title = currentBook.title,
                    content = chapter?.content ?: "Loading text...",
                    chapterTitle = chapter?.title ?: "Chapter ${currentChapterIndex + 1}",
                    currentChapterIndex = currentChapterIndex,
                    totalChapters = chapters.size.coerceAtLeast(1),
                    settings = readerSettings,
                    onTap = viewModel::toggleControls,
                    onPreviousChapter = {
                        if (currentChapterIndex > 0) {
                            viewModel.onTxtChapterSelected(currentChapterIndex - 1)
                        }
                    },
                    onNextChapter = {
                        if (currentChapterIndex < chapters.size - 1) {
                            viewModel.onTxtChapterSelected(currentChapterIndex + 1)
                        }
                    }
                )
            }
        }

        // Overlay: Top App Bar
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 4.dp
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentBook.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "By ${currentBook.author}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Reader")
                        }
                    },
                    actions = {
                        // Table of Contents
                        IconButton(onClick = { showTocDialog = true }) {
                            Icon(Icons.Default.List, contentDescription = "Chapters")
                        }

                        // Appearance Settings
                        IconButton(onClick = { showAppearanceSheet = true }) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Appearance")
                        }

                        // Bookmark
                        IconButton(onClick = { viewModel.addBookmark() }) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark Page")
                        }

                        // View Bookmarks
                        IconButton(onClick = { showBookmarksDialog = true }) {
                            Icon(Icons.Default.Bookmark, contentDescription = "View Bookmarks")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }

        // Overlay: Bottom Controls Bar
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPage > 1) viewModel.onPageChanged(currentPage - 1, totalPages)
                            },
                            enabled = currentPage > 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous")
                        }

                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { newPage ->
                                viewModel.onPageChanged(newPage.toInt(), totalPages)
                            },
                            valueRange = 1f..totalPages.coerceAtLeast(1).toFloat(),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (currentPage < totalPages) viewModel.onPageChanged(currentPage + 1, totalPages)
                            },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next")
                        }
                    }

                    // Page count & jump button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showJumpDialog = true }) {
                            Icon(Icons.Default.Numbers, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Page $currentPage of $totalPages", fontWeight = FontWeight.Bold)
                        }

                        val progressPercent = ((currentPage.toFloat() / totalPages.coerceAtLeast(1).toFloat()) * 100f).coerceIn(0f, 100f)
                        Text(
                            text = "${String.format("%.1f", progressPercent)}% read",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
