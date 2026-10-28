package com.example.data.model

enum class ReadingStatus(val displayName: String) {
    NOT_STARTED("Not Started"),
    CURRENTLY_READING("Currently Reading"),
    ON_HOLD("On Hold"),
    COMPLETED("Completed"),
    WISHLIST("Want to Read"),
    ABANDONED("Abandoned");

    companion object {
        fun fromString(value: String): ReadingStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: NOT_STARTED
        }
    }
}

enum class BookFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    EPUB("epub", "application/epub+zip"),
    TXT("txt", "text/plain");

    companion object {
        fun fromFileName(fileName: String): BookFormat {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "pdf" -> PDF
                "epub" -> EPUB
                "txt" -> TXT
                else -> TXT
            }
        }
    }
}

enum class ReaderTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SEPIA("Sepia")
}

enum class FontFamilyPreference(val displayName: String) {
    SERIF("Serif"),
    SANS_SERIF("Sans-Serif"),
    MONOSPACE("Monospace")
}

data class ReaderSettings(
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.5f,
    val horizontalMarginDp: Int = 20,
    val theme: ReaderTheme = ReaderTheme.SEPIA,
    val fontFamily: FontFamilyPreference = FontFamilyPreference.SERIF,
    val keepScreenOn: Boolean = true
)
