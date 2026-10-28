package com.example.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.entity.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object StarterBooksGenerator {

    suspend fun generateStarterBooks(context: Context): List<BookEntity> = withContext(Dispatchers.IO) {
        val booksDir = File(context.filesDir, "books").apply { if (!exists()) mkdirs() }
        val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }

        val starterBooks = mutableListOf<BookEntity>()

        // 1. Generate The Art of War (TXT)
        try {
            val artOfWarFile = File(booksDir, "the_art_of_war_sun_tzu.txt")
            if (!artOfWarFile.exists() || artOfWarFile.length() == 0L) {
                artOfWarFile.writeText(getArtOfWarText(), Charsets.UTF_8)
            }
            val txtContent = TxtParser.parse(artOfWarFile)

            starterBooks.add(
                BookEntity(
                    title = "The Art of War",
                    author = "Sun Tzu",
                    format = BookFormat.TXT.name,
                    filePath = artOfWarFile.absolutePath,
                    coverPath = null,
                    description = "An ancient Chinese military treatise dating from the Late Spring and Autumn Period. Composed of 13 chapters, each devoted to one aspect of warfare and military strategy.",
                    genre = "Philosophy & Strategy",
                    tags = "Classic, Strategy, Philosophy",
                    status = ReadingStatus.CURRENTLY_READING.name,
                    currentPage = 1,
                    totalPages = txtContent.totalEstimatedPages.coerceAtLeast(14),
                    progressPercent = 7.1f,
                    lastOpenedTimestamp = System.currentTimeMillis() - 3600_000 * 2,
                    totalReadingTimeMillis = 15 * 60 * 1000L,
                    personalRating = 5.0f,
                    personalNotes = "Masterpiece of strategic thinking. Applicable to business and life.",
                    fileSizeBytes = artOfWarFile.length(),
                    isSampleBook = true
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Generate BookNest Reader's Handbook (PDF)
        try {
            val pdfFile = File(booksDir, "booknest_readers_guide.pdf")
            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                createGuidePdf(pdfFile)
            }

            var coverPath: String? = null
            var totalPages = 4
            try {
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    totalPages = renderer.pageCount
                    if (renderer.pageCount > 0) {
                        val page = renderer.openPage(0)
                        val bmp = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        val coverFile = File(coversDir, "cover_guide.jpg")
                        FileOutputStream(coverFile).use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        bmp.recycle()
                        coverPath = coverFile.absolutePath
                    }
                    renderer.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            starterBooks.add(
                BookEntity(
                    title = "BookNest Reader's Guide",
                    author = "BookNest Editorial",
                    format = BookFormat.PDF.name,
                    filePath = pdfFile.absolutePath,
                    coverPath = coverPath,
                    description = "A beautifully designed companion guide to mastering deep reading, organizing personal book collections, and building a consistent reading habit with BookNest.",
                    genre = "Guide & Non-Fiction",
                    tags = "Reading, Guide, Productivity",
                    status = ReadingStatus.CURRENTLY_READING.name,
                    currentPage = 2,
                    totalPages = totalPages,
                    progressPercent = 50.0f,
                    lastOpenedTimestamp = System.currentTimeMillis() - 1800_000,
                    totalReadingTimeMillis = 18 * 60 * 1000L,
                    personalRating = 4.5f,
                    personalNotes = "Great tips on spaced repetition and mindful reading sessions.",
                    fileSizeBytes = pdfFile.length(),
                    isSampleBook = true
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Generate Alice in Wonderland (EPUB)
        try {
            val epubFile = File(booksDir, "alice_in_wonderland.epub")
            if (!epubFile.exists() || epubFile.length() == 0L) {
                createSampleEpub(epubFile)
            }
            val meta = EpubParser.extractMetadata(epubFile)

            starterBooks.add(
                BookEntity(
                    title = "Alice's Adventures in Wonderland",
                    author = "Lewis Carroll",
                    format = BookFormat.EPUB.name,
                    filePath = epubFile.absolutePath,
                    coverPath = null,
                    description = "The classic English tale of Alice falling through a rabbit hole into a fantasy world of anthropomorphic creatures. A timeless masterwork of literary nonsense.",
                    genre = "Classic Fiction",
                    tags = "Classic, Fantasy, Adventure",
                    status = ReadingStatus.NOT_STARTED.name,
                    currentPage = 1,
                    totalPages = (meta.chaptersCount * 4).coerceAtLeast(16),
                    progressPercent = 0.0f,
                    lastOpenedTimestamp = 0L,
                    totalReadingTimeMillis = 0L,
                    personalRating = 0f,
                    personalNotes = "",
                    fileSizeBytes = epubFile.length(),
                    isSampleBook = true
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        starterBooks
    }

    private fun createGuidePdf(outputFile: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#9A3412")
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        // Page 1: Cover
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = document.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Background
        val bgPaint = Paint().apply { color = Color.parseColor("#FBF0D9") }
        canvas1.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#C05621")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas1.drawRoundRect(RectF(40f, 40f, pageWidth - 40f, pageHeight - 40f), 12f, 12f, borderPaint)

        // Gold Banner
        val bannerPaint = Paint().apply { color = Color.parseColor("#1E293B") }
        canvas1.drawRoundRect(RectF(60f, 180f, pageWidth - 60f, 380f), 16f, 16f, bannerPaint)

        val coverTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas1.drawText("BOOKNEST", pageWidth / 2f, 250f, coverTitlePaint)

        val coverSubPaint = Paint().apply {
            color = Color.parseColor("#FDE047")
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas1.drawText("THE READER'S COMPANION & GUIDE", pageWidth / 2f, 290f, coverSubPaint)

        val coverDescPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 12f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas1.drawText("Mastering Deep Reading, Habits & Digital Libraries", pageWidth / 2f, 330f, coverDescPaint)

        val authorCenterPaint = Paint().apply {
            color = Color.parseColor("#78350F")
            textSize = 15f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas1.drawText("Curated by BookNest Editorial Studio", pageWidth / 2f, 520f, authorCenterPaint)
        canvas1.drawText("First Edition • 2026", pageWidth / 2f, 740f, subtitlePaint.apply { textAlign = Paint.Align.CENTER })

        document.finishPage(page1)

        // Page 2: Chapter 1
        val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = document.startPage(pageInfo2)
        val canvas2 = page2.canvas
        drawWhitePageBackground(canvas2, pageWidth, pageHeight)

        canvas2.drawText("Chapter 1: The Art of Deep Reading", 50f, 80f, headerPaint)
        var y = 120f
        val paragraphs1 = listOf(
            "Reading is not merely an act of decoding symbols on a page; it is a communion between two minds separated by time and space. When we enter a state of deep reading, our brain constructs rich mental landscapes, synthesizes novel connections, and nurtures empathy.",
            "In our hyper-connected modern environment, attention has become our rarest currency. Notifications, endless feeds, and micro-content fragment our cognitive capacity. The antidote is intentional, focused reading sessions.",
            "Setting aside even twenty to thirty minutes of distraction-free reading each day produces compound interest for the intellect. A reader who commits to 25 pages each morning will effortlessly complete 30 to 40 books each year.",
            "To cultivate this habit, treat reading as an appointment with yourself. Choose a quiet corner, silence external alerts, and immerse yourself in the author's world. Notice when your mind drifts, gently guide it back, and let the narrative unfold."
        )
        for (para in paragraphs1) {
            y = drawWrappedText(canvas2, para, 50f, y, pageWidth - 100f, bodyPaint, 20f) + 16f
        }
        drawPageNumber(canvas2, 2, pageWidth, pageHeight)
        document.finishPage(page2)

        // Page 3: Chapter 2
        val pageInfo3 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
        val page3 = document.startPage(pageInfo3)
        val canvas3 = page3.canvas
        drawWhitePageBackground(canvas3, pageWidth, pageHeight)

        canvas3.drawText("Chapter 2: Organizing Your Digital Library", 50f, 80f, headerPaint)
        y = 120f
        val paragraphs2 = listOf(
            "A well-maintained personal library reflects the trajectory of a person's intellectual journey. With BookNest, your reading life is structured into intuitive categories:",
            "• Currently Reading: Keep 1 to 3 books in active rotation to maintain momentum without overwhelming your cognitive bandwidth.",
            "• Want to Read: A curated wishlist of future discoveries. Capture titles when recommendations strike so you always have your next voyage planned.",
            "• Completed Shelf: Your digital monument of finished works. Seeing your list of completed books grow provides powerful psychological reinforcement.",
            "• Bookmarks & Reflections: Great books deserve active conversation. Use BookNest bookmarks and notes to record memorable passages, counterarguments, and personal reflections while they are fresh."
        )
        for (para in paragraphs2) {
            y = drawWrappedText(canvas2, para, 50f, y, pageWidth - 100f, bodyPaint, 20f) + 16f
        }
        drawPageNumber(canvas3, 3, pageWidth, pageHeight)
        document.finishPage(page3)

        // Page 4: Chapter 3
        val pageInfo4 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 4).create()
        val page4 = document.startPage(pageInfo4)
        val canvas4 = page4.canvas
        drawWhitePageBackground(canvas4, pageWidth, pageHeight)

        canvas4.drawText("Chapter 3: Tracking Goals & Building Streaks", 50f, 80f, headerPaint)
        y = 120f
        val paragraphs3 = listOf(
            "Consistency trumps intensity. Reading 15 minutes every single day produces vastly superior retention and enjoyment compared to sporadic 4-hour binge sessions.",
            "BookNest automatically logs your daily reading minutes and calculates streaks. When you witness your reading ring fill each evening, you build an identity as a dedicated reader.",
            "Tips for Sustaining Your Streak:",
            "1. Read First Thing or Before Sleep: Anchor your reading habit to an existing routine.",
            "2. Keep Different Formats Accessible: Have an informative non-fiction PDF and a captivating fiction EPUB ready for different moods.",
            "3. Celebrate Milestones: Review your monthly statistics chart and take pride in every page turned.",
            "Happy reading, and welcome to your personal literary sanctuary!"
        )
        for (para in paragraphs3) {
            y = drawWrappedText(canvas4, para, 50f, y, pageWidth - 100f, bodyPaint, 20f) + 16f
        }
        drawPageNumber(canvas4, 4, pageWidth, pageHeight)
        document.finishPage(page4)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun drawWhitePageBackground(canvas: Canvas, width: Int, height: Int) {
        val bgPaint = Paint().apply { color = Color.parseColor("#FDFBF7") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
    }

    private fun drawPageNumber(canvas: Canvas, pageNum: Int, width: Int, height: Int) {
        val numPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 10f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("BookNest Reader's Guide • Page $pageNum of 4", width / 2f, height - 35f, numPaint)
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, startY: Float, maxWidth: Float, paint: Paint, lineHeight: Float): Float {
        val words = text.split(" ")
        var currentY = startY
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine.toString(), x, currentY, paint)
                currentY += lineHeight
                currentLine = StringBuilder(word)
            } else {
                currentLine = StringBuilder(testLine)
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine.toString(), x, currentY, paint)
            currentY += lineHeight
        }
        return currentY
    }

    private fun createSampleEpub(outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // 1. mimetype (must be uncompressed)
            val mimeEntry = ZipEntry("mimetype")
            mimeEntry.method = ZipEntry.STORED
            val mimeBytes = "application/epub+zip".toByteArray(Charsets.US_ASCII)
            mimeEntry.size = mimeBytes.size.toLong()
            mimeEntry.crc = calculateCrc(mimeBytes)
            zos.putNextEntry(mimeEntry)
            zos.write(mimeBytes)
            zos.closeEntry()

            // 2. META-INF/container.xml
            zos.putNextEntry(ZipEntry("META-INF/container.xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. OEBPS/content.opf
            zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zos.write("""<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookID" version="2.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Alice's Adventures in Wonderland</dc:title>
    <dc:creator>Lewis Carroll</dc:creator>
    <dc:language>en</dc:language>
    <dc:description>A young girl named Alice falls down a rabbit hole into a fantasy realm full of anthropomorphic creatures.</dc:description>
  </metadata>
  <manifest>
    <item id="chapter1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
    <item id="chapter2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
    <item id="chapter3" href="chapter3.xhtml" media-type="application/xhtml+xml"/>
    <item id="chapter4" href="chapter4.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine toc="ncx">
    <itemref idref="chapter1"/>
    <itemref idref="chapter2"/>
    <itemref idref="chapter3"/>
    <itemref idref="chapter4"/>
  </spine>
</package>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. Chapter 1
            zos.putNextEntry(ZipEntry("OEBPS/chapter1.xhtml"))
            zos.write("""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Chapter 1: Down the Rabbit-Hole</title></head>
<body>
  <h1>Chapter 1: Down the Rabbit-Hole</h1>
  <p>Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, "and what is the use of a book," thought Alice "without pictures or conversations?"</p>
  <p>So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her.</p>
  <p>There was nothing so very remarkable in that; nor did Alice think it so very much out of the way to hear the Rabbit say to itself, "Oh dear! Oh dear! I shall be late!" But when the Rabbit actually took a watch out of its waistcoat-pocket, and looked at it, and then hurried on, Alice started to her feet, for it flashed across her mind that she had never before seen a rabbit with either a waistcoat-pocket, or a watch to take out of it, and burning with curiosity, she ran across the field after it, and fortunately was just in time to see it pop down a large rabbit-hole under the hedge.</p>
</body>
</html>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 5. Chapter 2
            zos.putNextEntry(ZipEntry("OEBPS/chapter2.xhtml"))
            zos.write("""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Chapter 2: The Pool of Tears</title></head>
<body>
  <h1>Chapter 2: The Pool of Tears</h1>
  <p>"Curiouser and curiouser!" cried Alice (she was so much surprised, that for the moment she quite forgot how to speak good English); "now I’m opening out like the largest telescope that ever was! Good-bye, feet!"</p>
  <p>Just then her head struck against the roof of the hall: in fact she was now more than nine feet high, and she at once took up the little golden key and hurried off to the garden door.</p>
  <p>Poor Alice! It was as much as she could do, lying down on one side, to look through into the garden with one eye; but to get through was more hopeless than ever: she sat down and began to cry again.</p>
</body>
</html>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 6. Chapter 3
            zos.putNextEntry(ZipEntry("OEBPS/chapter3.xhtml"))
            zos.write("""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Chapter 3: A Caucus-Race and a Long Tale</title></head>
<body>
  <h1>Chapter 3: A Caucus-Race and a Long Tale</h1>
  <p>They were indeed a queer-looking party that assembled on the bank—the birds with draggled feathers, the animals with their fur clinging close to them, and all dripping wet, cross, and uncomfortable.</p>
  <p>The first question of course was, how to get dry again: they had a consultation about this, and after a few minutes it seemed quite natural to Alice to find herself talking familiarly with them, as if she had known them all her life.</p>
</body>
</html>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 7. Chapter 4
            zos.putNextEntry(ZipEntry("OEBPS/chapter4.xhtml"))
            zos.write("""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Chapter 4: The Rabbit Sends in a Little Bill</title></head>
<body>
  <h1>Chapter 4: The Rabbit Sends in a Little Bill</h1>
  <p>It was the White Rabbit, trotting slowly back again, and looking anxiously about as it went, as if it had lost something; and she heard it muttering to itself, "The Duchess! The Duchess! Oh my dear paws! Oh my fur and whiskers! She’ll get me executed, as sure as ferrets are ferrets!"</p>
  <p>Alice guessed in a moment that it was looking for the fan and the pair of white kid gloves, and she very good-naturedly began hunting about for them, but they were nowhere to be seen.</p>
</body>
</html>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
    }

    private fun calculateCrc(bytes: ByteArray): Long {
        val crc = java.util.zip.CRC32()
        crc.update(bytes)
        return crc.value
    }

    private fun getArtOfWarText(): String = """THE ART OF WAR
By Sun Tzu (Translated by Lionel Giles)

CHAPTER I. LAYING PLANS
1. Sun Tzu said: The art of war is of vital importance to the State.
2. It is a matter of life and death, a road either to safety or to ruin. Hence it is a subject of inquiry which can on no account be neglected.
3. The moral law causes the people to be in complete accord with their ruler, so that they will follow him regardless of their lives, undismayed by any danger.
4. Heaven signifies night and day, cold and heat, times and seasons. Earth comprises distances, great and small; danger and security; open ground and narrow passes; the chances of life and death.
5. The Commander stands for the virtues of wisdom, sincerity, benevolence, courage and strictness.
6. By method and discipline are to be understood the marshaling of the army in its proper subdivisions, the graduations of rank among the officers, the maintenance of roads by which supplies may reach the army, and the control of military expenditure.
7. These five heads should be familiar to every general: he who knows them will be victorious; he who knows them not will fail.
8. All warfare is based on deception.
9. Hence, when able to attack, we must seem unable; when using our forces, we must seem inactive; when we are near, we must make the enemy believe we are far away; when far away, we must make him believe we are near.
10. Hold out baits to entice the enemy. Feign disorder, and crush him.
11. If he is secure at all points, be prepared for him. If he is in superior strength, evade him.
12. If your opponent is of choleric temper, seek to irritate him. Pretend to be weak, that he may grow arrogant.
13. If he is taking his ease, give him no rest. If his forces are united, separate them.
14. Attack him where he is unprepared, appear where you are not expected.

CHAPTER II. WAGING WAR
1. Sun Tzu said: In the operations of war, where there are in the field a thousand swift chariots, as many heavy chariots, and a hundred thousand mail-clad soldiers, with provisions enough to carry them a thousand li, the expenditure at home and at the front, including entertainment of guests, small items such as glue and paint, and sums spent on chariots and armor, will reach the total of a thousand ounces of silver per day. Such is the cost of raising an army of 100,000 men.
2. When you engage in actual fighting, if victory is long in coming, then men's weapons will grow dull and their ardor will be damped. If you lay siege to a town, you will exhaust your strength.
3. Again, if the campaign is protracted, the resources of the State will not be equal to the strain.
4. Now, when your weapons are dulled, your ardor damped, your strength exhausted and your treasure spent, other chieftains will spring up to take advantage of your extremity. Then no man, however wise, will be able to avert the consequences that must ensue.
5. Thus, though we have heard of stupid haste in war, cleverness has never been seen associated with long delays.
6. There is no instance of a country having benefited from prolonged warfare.
7. It is only one who is thoroughly acquainted with the evils of war that can thoroughly understand the profitable way of carrying it on.

CHAPTER III. ATTACK BY STRATAGEM
1. Sun Tzu said: In the practical art of war, the best thing of all is to take the enemy's country whole and intact; to shatter and destroy it is not so good. So, too, it is better to recapture an army entire than to destroy it.
2. Hence to fight and conquer in all your battles is not supreme excellence; supreme excellence consists in breaking the enemy's resistance without fighting.
3. Thus the highest form of generalship is to balk the enemy's plans; the next best is to prevent the junction of the enemy's forces; the next in order is to attack the enemy's army in the field; and the worst policy of all is to besiege walled cities.
4. Therefore the skillful leader subdues the enemy's troops without any fighting; he captures their cities without laying siege to them; he overthrows their kingdom without lengthy operations in the field.
5. With his forces intact he will dispute the mastery of the Empire, and thus, without losing a man, his triumph will be complete. This is the method of attacking by stratagem.
6. It is the rule in war, if our forces are ten to the enemy's one, to surround him; if five to one, to attack him; if twice as numerous, to divide our army into two.
7. If equally matched, we can offer battle; if slightly inferior in numbers, we can avoid the enemy; if quite unequal in every way, we can flee from him.
8. Hence, though an obstinate fight may be made by a small force, in the end it must be captured by the larger force.
9. Now the general is the bulwark of the State; if the bulwark is complete at all points; the State will be strong; if the bulwark is defective, the State will be weak.
10. He will win who knows when to fight and when not to fight.
11. He will win who knows how to handle both superior and inferior forces.
12. He will win whose army is animated by the same spirit throughout all its ranks.
13. He will win who, prepared himself, waits to take the enemy unprepared.
14. Hence the saying: If you know the enemy and know yourself, you need not fear the result of a hundred battles. If you know yourself but not the enemy, for every victory gained you will also suffer a defeat. If you know neither the enemy nor yourself, you will succumb in every battle.

CHAPTER IV. TACTICAL DISPOSITIONS
1. Sun Tzu said: The good fighters of old first put themselves beyond the possibility of defeat, and then waited for an opportunity of defeating the enemy.
2. To secure ourselves against defeat lies in our own hands, but the opportunity of defeating the enemy is provided by the enemy himself.
3. Thus the good fighter is able to secure himself against defeat, but cannot make certain of defeating the enemy.
4. Hence the saying: One may know how to conquer without being able to do it.
5. Security against defeat implies defensive tactics; ability to defeat the enemy means taking the offensive.
6. Standing on the defensive indicates insufficient strength; attacking, a superabundance of strength.
7. The general who is skilled in defense hides in the most secret recesses of the earth; he who is skilled in attack flashes forth from the topmost heights of heaven. Thus on the one hand we have ability to protect ourselves; on the other, a victory that is complete.
"""
}
