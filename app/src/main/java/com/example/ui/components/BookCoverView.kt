package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.abs

@Composable
fun BookCoverView(
    title: String,
    author: String,
    format: String,
    coverPath: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    elevation: Dp = 4.dp
) {
    val fileExists = remember(coverPath) {
        coverPath != null && File(coverPath).exists()
    }

    Surface(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius)
    ) {
        if (fileExists && coverPath != null) {
            AsyncImage(
                model = File(coverPath),
                contentDescription = "Cover of $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ProceduralBookCover(
                title = title,
                author = author,
                format = format
            )
        }
    }
}

@Composable
fun ProceduralBookCover(
    title: String,
    author: String,
    format: String,
    modifier: Modifier = Modifier
) {
    // Generate harmonious gradient based on title hash
    val gradients = listOf(
        listOf(Color(0xFF1E293B), Color(0xFF0F172A)), // Deep Midnight Slate
        listOf(Color(0xFF7C2D12), Color(0xFF431407)), // Warm Terracotta Umber
        listOf(Color(0xFF064E3B), Color(0xFF022C22)), // Deep Forest Emerald
        listOf(Color(0xFF4C1D95), Color(0xFF2E1065)), // Royal Amethyst
        listOf(Color(0xFF1E3A8A), Color(0xFF172554)), // Deep Sapphire
        listOf(Color(0xFF831843), Color(0xFF500724))  // Rich Bordeaux
    )
    val colorIndex = abs((title + author).hashCode()) % gradients.size
    val gradientColors = gradients[colorIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        // Spine shadow simulation on left edge
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0x66000000), Color(0x00000000))
                    )
                )
        )

        // Format Badge at top right
        Surface(
            color = Color(0x40FFFFFF),
            shape = RoundedCornerShape(bottomStart = 6.dp),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(
                text = format.uppercase(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Title and Author in center
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = title,
                color = Color(0xFFFFFBEB),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtle divider
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(Color(0x80FDE047))
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = author,
                color = Color(0xFFE2E8F0),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1.2f))
        }
    }
}
