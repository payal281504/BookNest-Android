package com.example.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Html
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.regex.Pattern

data class EpubMetadata(
    val title: String,
    val author: String,
    val description: String,
    val coverBitmap: Bitmap? = null,
    val chaptersCount: Int = 1
)

data class EpubChapter(
    val index: Int,
    val title: String,
    val plainText: String,
    val htmlContent: String
)

object EpubParser {

    fun extractMetadata(epubFile: File): EpubMetadata {
        var title = epubFile.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
        var author = "Unknown Author"
        var description = ""
        var coverBitmap: Bitmap? = null
        var chaptersCount = 1

        try {
            ZipFile(epubFile).use { zip ->
                val opfPath = findOpfPath(zip) ?: return EpubMetadata(title, author, description)
                val opfEntry = zip.getEntry(opfPath) ?: return EpubMetadata(title, author, description)

                val opfContent = zip.getInputStream(opfEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }

                // Extract title
                val titleMatch = Regex("<dc:title[^>]*>(.*?)</dc:title>", RegexOption.DOT_MATCHES_ALL).find(opfContent)
                if (titleMatch != null) {
                    title = cleanXmlText(titleMatch.groupValues[1])
                }

                // Extract author
                val authorMatch = Regex("<dc:creator[^>]*>(.*?)</dc:creator>", RegexOption.DOT_MATCHES_ALL).find(opfContent)
                if (authorMatch != null) {
                    author = cleanXmlText(authorMatch.groupValues[1])
                }

                // Extract description
                val descMatch = Regex("<dc:description[^>]*>(.*?)</dc:description>", RegexOption.DOT_MATCHES_ALL).find(opfContent)
                if (descMatch != null) {
                    description = cleanXmlText(descMatch.groupValues[1])
                }

                // Find spine count
                val spineItemMatches = Regex("<itemref[^>]+idref=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).findAll(opfContent).toList()
                if (spineItemMatches.isNotEmpty()) {
                    chaptersCount = spineItemMatches.size
                }

                // Find cover image
                coverBitmap = extractCoverImage(zip, opfPath, opfContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return EpubMetadata(
            title = title.ifBlank { epubFile.nameWithoutExtension },
            author = author.ifBlank { "Unknown Author" },
            description = description,
            coverBitmap = coverBitmap,
            chaptersCount = chaptersCount.coerceAtLeast(1)
        )
    }

    private fun findOpfPath(zip: ZipFile): String? {
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
        val containerXml = zip.getInputStream(containerEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val match = Regex("full-path=[\"']([^\"']+\\.opf)[\"']", RegexOption.IGNORE_CASE).find(containerXml)
        return match?.groupValues?.get(1)
    }

    private fun extractCoverImage(zip: ZipFile, opfPath: String, opfContent: String): Bitmap? {
        try {
            val opfDir = if (opfPath.contains('/')) opfPath.substringBeforeLast('/') + "/" else ""

            // Strategy 1: Look for properties="cover-image"
            var coverHref: String? = null
            val coverPropMatch = Regex("<item[^>]+properties=[\"'][^\"']*cover-image[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(opfContent)
                ?: Regex("<item[^>]+href=[\"']([^\"']+)[\"'][^>]+properties=[\"'][^\"']*cover-image[^\"']*[\"']", RegexOption.IGNORE_CASE).find(opfContent)

            if (coverPropMatch != null) {
                coverHref = coverPropMatch.groupValues[1]
            }

            // Strategy 2: Look for <meta name="cover" content="id" />
            if (coverHref == null) {
                val metaCover = Regex("<meta[^>]+name=[\"']cover[\"'][^>]+content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(opfContent)
                val coverId = metaCover?.groupValues?.get(1)
                if (coverId != null) {
                    val idMatch = Regex("<item[^>]+id=[\"']$coverId[\"'][^>]+href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(opfContent)
                    coverHref = idMatch?.groupValues?.get(1)
                }
            }

            // Strategy 3: Check item id="cover" or href contains "cover"
            if (coverHref == null) {
                val genericMatch = Regex("<item[^>]+href=[\"']([^\"']*(?:cover|title)[^\"']*\\.(?:jpg|jpeg|png))[\"']", RegexOption.IGNORE_CASE).find(opfContent)
                coverHref = genericMatch?.groupValues?.get(1)
            }

            if (coverHref != null) {
                val fullCoverPath = opfDir + coverHref
                var entry = zip.getEntry(fullCoverPath)
                if (entry == null) {
                    // Try without prefix or search for href
                    entry = zip.entries().asSequence().firstOrNull { it.name.endsWith(coverHref.substringAfterLast('/'), ignoreCase = true) }
                }
                if (entry != null) {
                    zip.getInputStream(entry).use { stream ->
                        return BitmapFactory.decodeStream(stream)
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun loadChapters(epubFile: File): List<EpubChapter> {
        val chapters = mutableListOf<EpubChapter>()
        try {
            ZipFile(epubFile).use { zip ->
                val opfPath = findOpfPath(zip) ?: return chapters
                val opfEntry = zip.getEntry(opfPath) ?: return chapters
                val opfDir = if (opfPath.contains('/')) opfPath.substringBeforeLast('/') + "/" else ""
                val opfContent = zip.getInputStream(opfEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }

                // Map items id -> href
                val manifestMap = mutableMapOf<String, String>()
                val itemRegex = Regex("<item\\s+[^>]*id=[\"']([^\"']+)[\"'][^>]*href=[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE)
                val itemRegexAlt = Regex("<item\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*id=[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE)

                itemRegex.findAll(opfContent).forEach {
                    manifestMap[it.groupValues[1]] = it.groupValues[2]
                }
                itemRegexAlt.findAll(opfContent).forEach {
                    manifestMap[it.groupValues[2]] = it.groupValues[1]
                }

                // Read spine
                val spineMatches = Regex("<itemref[^>]+idref=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).findAll(opfContent).toList()

                var chapterIdx = 1
                for (match in spineMatches) {
                    val idref = match.groupValues[1]
                    val href = manifestMap[idref] ?: continue
                    val fullPath = opfDir + href
                    val entry = zip.getEntry(fullPath)
                        ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(href.substringAfterLast('/'), ignoreCase = true) }

                    if (entry != null) {
                        val rawHtml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val plainText = htmlToPlainText(rawHtml)
                        if (plainText.isNotBlank()) {
                            val title = extractChapterTitle(rawHtml, "Chapter $chapterIdx")
                            chapters.add(
                                EpubChapter(
                                    index = chapterIdx,
                                    title = title,
                                    plainText = plainText,
                                    htmlContent = rawHtml
                                )
                            )
                            chapterIdx++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (chapters.isEmpty()) {
            listOf(
                EpubChapter(
                    index = 1,
                    title = "Chapter 1",
                    plainText = "Could not parse EPUB chapters, or document is empty.",
                    htmlContent = "<p>Could not parse EPUB chapters, or document is empty.</p>"
                )
            )
        } else {
            chapters
        }
    }

    private fun extractChapterTitle(html: String, fallback: String): String {
        val h1Match = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
        if (h1Match != null) {
            val title = cleanXmlText(h1Match.groupValues[1])
            if (title.isNotBlank() && title.length < 80) return title
        }
        val titleTag = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
        if (titleTag != null) {
            val title = cleanXmlText(titleTag.groupValues[1])
            if (title.isNotBlank() && title.length < 80) return title
        }
        return fallback
    }

    private fun cleanXmlText(text: String): String {
        return text.replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }

    fun htmlToPlainText(html: String): String {
        // Strip scripts and styles
        var cleaned = html.replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")

        // Replace headers and paragraph endings with newlines
        cleaned = cleaned.replace(Regex("</?(h[1-6]|p|div|br|li|blockquote)[^>]*>", RegexOption.IGNORE_CASE), "\n\n")

        // Parse remaining entities and tags
        val spanned = Html.fromHtml(cleaned, Html.FROM_HTML_MODE_COMPACT)
        return spanned.toString().replace(Regex("\n{3,}"), "\n\n").trim()
    }
}
