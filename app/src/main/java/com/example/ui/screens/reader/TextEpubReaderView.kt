package com.example.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FontFamilyPreference
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.ui.theme.ReaderDarkBg
import com.example.ui.theme.ReaderDarkSurface
import com.example.ui.theme.ReaderDarkText
import com.example.ui.theme.ReaderDarkTextSecondary
import com.example.ui.theme.ReaderLightBg
import com.example.ui.theme.ReaderLightSurface
import com.example.ui.theme.ReaderLightText
import com.example.ui.theme.ReaderLightTextSecondary
import com.example.ui.theme.ReaderSepiaBg
import com.example.ui.theme.ReaderSepiaSurface
import com.example.ui.theme.ReaderSepiaText
import com.example.ui.theme.ReaderSepiaTextSecondary

@Composable
fun TextEpubReaderView(
    title: String,
    content: String,
    chapterTitle: String,
    currentChapterIndex: Int,
    totalChapters: Int,
    settings: ReaderSettings,
    onTap: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Reset scroll when chapter changes
    LaunchedEffect(currentChapterIndex) {
        scrollState.scrollTo(0)
    }

    // Determine colors from theme
    val (bgColor, surfaceColor, textColor, textSecondaryColor) = when (settings.theme) {
        ReaderTheme.SEPIA -> Quad(ReaderSepiaBg, ReaderSepiaSurface, ReaderSepiaText, ReaderSepiaTextSecondary)
        ReaderTheme.DARK -> Quad(ReaderDarkBg, ReaderDarkSurface, ReaderDarkText, ReaderDarkTextSecondary)
        ReaderTheme.LIGHT -> Quad(ReaderLightBg, ReaderLightSurface, ReaderLightText, ReaderLightTextSecondary)
    }

    val selectedFontFamily = when (settings.fontFamily) {
        FontFamilyPreference.SERIF -> FontFamily.Serif
        FontFamilyPreference.SANS_SERIF -> FontFamily.Default
        FontFamilyPreference.MONOSPACE -> FontFamily.Monospace
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onTap()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = settings.horizontalMarginDp.dp, vertical = 24.dp)
        ) {
            // Chapter Header
            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                color = surfaceColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "CHAPTER ${currentChapterIndex + 1} OF $totalChapters",
                    color = textSecondaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = chapterTitle,
                color = textColor,
                fontSize = (settings.fontSizeSp + 6).sp,
                lineHeight = (settings.fontSizeSp + 12).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = selectedFontFamily
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Text Body
            Text(
                text = content,
                color = textColor,
                fontSize = settings.fontSizeSp.sp,
                lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                fontFamily = selectedFontFamily,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Navigation Buttons between chapters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentChapterIndex > 0) {
                    OutlinedButton(
                        onClick = onPreviousChapter,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Previous Chapter")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (currentChapterIndex < totalChapters - 1) {
                    Button(
                        onClick = onNextChapter,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next Chapter")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
