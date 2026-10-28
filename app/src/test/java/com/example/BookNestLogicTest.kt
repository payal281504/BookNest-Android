package com.example

import com.example.data.model.BookFormat
import com.example.data.model.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookNestLogicTest {

    @Test
    fun testBookFormatDetection() {
        assertEquals(BookFormat.PDF, BookFormat.fromFileName("novel.pdf"))
        assertEquals(BookFormat.EPUB, BookFormat.fromFileName("wonderland.epub"))
        assertEquals(BookFormat.TXT, BookFormat.fromFileName("art_of_war.txt"))
        assertEquals(BookFormat.PDF, BookFormat.fromFileName("UPPERCASE.PDF"))
    }

    @Test
    fun testReadingProgressCalculation() {
        val currentPage = 125
        val totalPages = 800
        val percentage = ((currentPage.toFloat() / totalPages.toFloat()) * 100f)
        assertEquals(15.625f, percentage, 0.01f)

        val remainingPages = (totalPages - currentPage)
        assertEquals(675, remainingPages)
    }

    @Test
    fun testReadingStatusTransitions() {
        val status = ReadingStatus.fromString("CURRENTLY_READING")
        assertEquals(ReadingStatus.CURRENTLY_READING, status)
        assertEquals("Currently Reading", status.displayName)

        val completedStatus = ReadingStatus.fromString("COMPLETED")
        assertEquals("Completed", completedStatus.displayName)
    }
}
