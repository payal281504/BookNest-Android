package com.example.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.data.entity.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

sealed class ImportResult {
    data class Success(val book: BookEntity) : ImportResult()
    data class Duplicate(val existingBook: BookEntity, val newBookCandidate: BookEntity) : ImportResult()
    data class Failure(val fileName: String, val error: String) : ImportResult()
}

class BookImporter(private val context: Context) {

    private val booksDir: File by lazy {
        File(context.filesDir, "books").apply { if (!exists()) mkdirs() }
    }

    private val coversDir: File by lazy {
        File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
    }

    suspend fun importFromUri(
        uri: Uri,
        skipDuplicateCheck: Boolean = false,
        existingBookChecker: (suspend (String, String) -> BookEntity?)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        val originalFileName = getFileNameFromUri(uri)
        val format = BookFormat.fromFileName(originalFileName)

        // Generate safe internal file target
        val sanitizedBase = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val internalFile = File(booksDir, "${System.currentTimeMillis()}_$sanitizedBase")

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(internalFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext ImportResult.Failure(originalFileName, "Could not open file input stream")

            val fileSizeBytes = internalFile.length()
            if (fileSizeBytes == 0L) {
                internalFile.delete()
                return@withContext ImportResult.Failure(originalFileName, "File is empty (0 bytes)")
            }

            // Extract metadata depending on format
            var title = originalFileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()
            var author = "Unknown Author"
            var description = ""
            var totalPages = 1
            var coverPath: String? = null

            when (format) {
                BookFormat.PDF -> {
                    try {
                        ParcelFileDescriptor.open(internalFile, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                            val renderer = PdfRenderer(pfd)
                            totalPages = renderer.pageCount.coerceAtLeast(1)

                            // Render cover from page 0
                            if (renderer.pageCount > 0) {
                                val page = renderer.openPage(0)
                                val width = 600
                                val height = (600f * (page.height.toFloat() / page.width.toFloat())).toInt().coerceIn(400, 900)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()

                                val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(coverFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                }
                                bitmap.recycle()
                                coverPath = coverFile.absolutePath
                            }
                            renderer.close()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                BookFormat.EPUB -> {
                    try {
                        val meta = EpubParser.extractMetadata(internalFile)
                        if (meta.title.isNotBlank()) title = meta.title
                        if (meta.author.isNotBlank()) author = meta.author
                        description = meta.description
                        totalPages = (meta.chaptersCount * 4).coerceAtLeast(1)

                        if (meta.coverBitmap != null) {
                            val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(coverFile).use { out ->
                                meta.coverBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            meta.coverBitmap.recycle()
                            coverPath = coverFile.absolutePath
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                BookFormat.TXT -> {
                    try {
                        val txtContent = TxtParser.parse(internalFile)
                        if (txtContent.title.isNotBlank()) title = txtContent.title
                        totalPages = txtContent.totalEstimatedPages
                        description = "Plain text document containing ${txtContent.chapters.size} sections/chapters."
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val candidate = BookEntity(
                title = title,
                author = author,
                format = format.name,
                filePath = internalFile.absolutePath,
                coverPath = coverPath,
                description = description,
                genre = "General",
                tags = "",
                status = ReadingStatus.NOT_STARTED.name,
                currentPage = 1,
                totalPages = totalPages,
                fileSizeBytes = fileSizeBytes,
                dateAdded = System.currentTimeMillis()
            )

            // Check for duplicate
            if (!skipDuplicateCheck && existingBookChecker != null) {
                val existing = existingBookChecker(candidate.title, candidate.author)
                if (existing != null) {
                    return@withContext ImportResult.Duplicate(existing, candidate)
                }
            }

            ImportResult.Success(candidate)

        } catch (e: Exception) {
            if (internalFile.exists()) internalFile.delete()
            ImportResult.Failure(originalFileName, e.localizedMessage ?: "Failed to import file")
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "book_${System.currentTimeMillis()}"
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val displayName = cursor.getString(nameIndex)
                        if (!displayName.isNullOrBlank()) {
                            name = displayName
                        }
                    }
                }
            } catch (_: Exception) {}
        } else {
            uri.lastPathSegment?.let { name = it }
        }
        return name
    }
}
