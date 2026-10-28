# BookNest – Smart Book Library & Reading Tracker

BookNest is a native, modern Android application for managing personal digital book libraries, reading books offline (PDF, EPUB, TXT), tracking reading progress automatically, and achieving daily reading goals.

## Core Features

### 1. Multi-Format Book Import & Storage
- **Supported Formats**: PDF, EPUB, TXT.
- **Storage Access Framework**: Multi-document picker allowing batch import of multiple books at once.
- **Metadata Extraction**:
  - Automatically extracts book title, author, description, and chapter counts.
  - Extracts cover images from EPUB archives (`cover-image`, `meta name="cover"`).
  - Renders high-resolution cover page 0 from PDFs using Android's native `PdfRenderer`.
  - Procedurally generates elegant gradient book covers with serif typography when no cover image exists.
- **Duplicate Detection**: Identifies existing titles and authors, providing options to skip or replace.
- **Reliable Local Storage**: Copies imported files directly into the app's internal private storage directory (`files/books/`), preventing URI permission revocation across app restarts or device reboots.

### 2. Built-in Multi-Format Readers
- **PDF Reader**:
  - Native hardware-accelerated rendering using Android's `android.graphics.pdf.PdfRenderer`.
  - Smooth pinch-to-zoom and double-tap zoom gestures.
  - Page-by-page navigation, previous/next controls, page scrubber slider, and direct page number jump.
- **EPUB & TXT Reader**:
  - Pure Kotlin ZIP & OPF parser for EPUBs and smart chapter detector for TXT files.
  - Three reading themes: **Light**, **Dark**, and **Sepia**.
  - Adjustable font size (14sp – 28sp), font family (Serif, Sans-Serif, Monospace), and line spacing (Compact, Normal, Relaxed).
  - Table of Contents / Chapter selector dialog.
- **Distraction-Free Immersion**: Tap the reading surface to hide/show top and bottom toolbars.

### 3. Automatic Reading Progress & Session Tracking
- **Granular Position Tracking**: Real-time progress updates tracking current page, total pages, percentage (e.g. 15.6%), and remaining pages.
- **Foreground Session Timer**: Accurately tracks reading duration and logs reading sessions into Room database.
- **Last Read Location Restore**: Automatically reopens books at the exact saved page/chapter.
- **End-of-Book Celebration**: Prompts to mark books as Completed when reaching the final page.

### 4. Reading Analytics & Habit Building
- **Daily Reading Goal**: Configurable daily target (e.g., 30 minutes) with animated circular progress indicator.
- **Weekly Reading Activity Chart**: Visual bar chart displaying minutes read across each day of the past week.
- **Reading Streaks**: Consecutive days calculation to encourage daily reading consistency.
- **Monthly Challenge**: Track books completed this month against a monthly volume target.
- **Session History Log**: Chronological log of reading sessions showing book title, timestamp, duration, and pages read.

### 5. Library Organization & Wishlist
- **Reading Status Categories**:
  - *Currently Reading*
  - *Want to Read (Wishlist)*
  - *Not Started*
  - *Completed*
  - *On Hold*
  - *Abandoned*
- **Powerful Search**: Filter by title, author, genre, tags, or personal notes.
- **Flexible Sorting**: Sort by Recently Read, Title (A-Z), Author (A-Z), Recently Added, Reading Progress, or Reading Time.
- **Grid and List Views**: Seamless toggle between multi-column grid and detailed list view.
- **Annotations**: Page bookmarks and personal notes with rating (1–5 stars).

## Technology Stack
- **Language**: Kotlin 2.2+
- **UI Framework**: Jetpack Compose with Material 3 Design
- **Architecture**: MVVM with Repository Pattern
- **Local Database**: Room 2.7.0 (with KSP)
- **Image Loading**: Coil 2.7.0
- **Navigation**: Jetpack Navigation Compose
- **Document Engines**: Native `PdfRenderer`, custom streaming `ZipFile` EPUB parser, and regex-powered `TxtParser`

## Build & Testing
To compile and build the app:
```bash
gradle assembleDebug
```
To run unit and Robolectric tests:
```bash
gradle :app:testDebugUnitTest
```
