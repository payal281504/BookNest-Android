package com.example.parser

import java.io.File
import java.nio.charset.Charset

data class TxtChapter(
    val index: Int,
    val title: String,
    val content: String,
    val startCharIndex: Int,
    val estimatedPages: Int
)

data class TxtBookContent(
    val title: String,
    val chapters: List<TxtChapter>,
    val totalEstimatedPages: Int,
    val fullTextLength: Int
)

object TxtParser {
    private const val CHARS_PER_PAGE = 1800

    fun parse(file: File): TxtBookContent {
        val rawText = readTextWithFallback(file)
        val title = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')

        // Look for chapter headings
        val lines = rawText.lines()
        val chapterRegex = Regex("^(chapter|act|part|book|section)\\s+([0-9ivxlcdm]+|[a-z]+)?:?.*$", RegexOption.IGNORE_CASE)

        val chapterIndices = mutableListOf<Pair<Int, String>>() // line index, title

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isNotEmpty() && line.length < 80 && chapterRegex.matches(line)) {
                chapterIndices.add(Pair(i, line))
            }
        }

        val chapters = mutableListOf<TxtChapter>()

        if (chapterIndices.size >= 2) {
            for (c in chapterIndices.indices) {
                val startLine = chapterIndices[c].first
                val endLine = if (c < chapterIndices.size - 1) chapterIndices[c + 1].first else lines.size
                val chapterLines = lines.subList(startLine, endLine)
                val chapterText = chapterLines.joinToString("\n").trim()
                val estimatedPages = (chapterText.length / CHARS_PER_PAGE).coerceAtLeast(1)

                chapters.add(
                    TxtChapter(
                        index = c + 1,
                        title = chapterIndices[c].second,
                        content = chapterText,
                        startCharIndex = 0,
                        estimatedPages = estimatedPages
                    )
                )
            }
        } else {
            // No explicit chapters, divide into chunks of ~5000 chars or keep as unified chapters
            val totalPages = (rawText.length / CHARS_PER_PAGE).coerceAtLeast(1)
            if (totalPages > 10) {
                // Split into sections of ~3 pages each
                val chunkSize = CHARS_PER_PAGE * 3
                var currentPos = 0
                var partIndex = 1
                while (currentPos < rawText.length) {
                    val nextPos = (currentPos + chunkSize).coerceAtMost(rawText.length)
                    val chunk = rawText.substring(currentPos, nextPos)
                    chapters.add(
                        TxtChapter(
                            index = partIndex,
                            title = "Part $partIndex",
                            content = chunk,
                            startCharIndex = currentPos,
                            estimatedPages = (chunk.length / CHARS_PER_PAGE).coerceAtLeast(1)
                        )
                    )
                    currentPos = nextPos
                    partIndex++
                }
            } else {
                chapters.add(
                    TxtChapter(
                        index = 1,
                        title = "Full Text",
                        content = rawText,
                        startCharIndex = 0,
                        estimatedPages = totalPages
                    )
                )
            }
        }

        val totalPages = chapters.sumOf { it.estimatedPages }.coerceAtLeast(1)

        return TxtBookContent(
            title = title,
            chapters = chapters,
            totalEstimatedPages = totalPages,
            fullTextLength = rawText.length
        )
    }

    private fun readTextWithFallback(file: File): String {
        return try {
            file.readText(Charsets.UTF_8)
        } catch (_: Exception) {
            try {
                file.readText(Charset.forName("ISO-8859-1"))
            } catch (_: Exception) {
                file.readText(Charset.defaultCharset())
            }
        }
    }
}
