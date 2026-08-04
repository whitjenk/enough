package com.enough.app.feature.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** The resolved (localized) lines to draw — kept out of the pure content model. */
data class ShareCardLines(
    val title: String,
    val headline: String,
    val subline: String,
    val footer: String,
)

/**
 * Draws the opt-in share card (SPEC §7.6 Step 2) to a PNG and returns a
 * FileProvider content URI to hand to the OS share sheet. Deliberately a fixed,
 * theme-independent light look: a shared image should read the same for everyone
 * who sees it, regardless of the sharer's or viewer's app theme.
 *
 * This is only ever invoked from an explicit "share" tap — nothing here runs on
 * its own, and nothing leaves the device except through the user's own share.
 */
object ShareCardRenderer {

    private const val SIZE = 1080
    private const val MARGIN = 96f
    private val BACKGROUND = Color.parseColor("#EAF3EA")
    private val INK = Color.parseColor("#1E3A2B")
    private val ACCENT = Color.parseColor("#3B7A57")

    /** Render [lines] to a cached PNG and return its shareable URI. */
    fun render(context: Context, lines: ShareCardLines): Uri {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND)

        val title = textPaint(ACCENT, 44f, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
        val headline = textPaint(INK, 92f, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        val subline = textPaint(INK, 46f, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
        val footer = textPaint(ACCENT, 40f, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))

        canvas.drawText(lines.title, MARGIN, MARGIN + title.textSize, title)

        // Headline (wrapped) sits centered in the card's vertical middle.
        val maxWidth = SIZE - 2 * MARGIN
        val headlineLines = wrap(lines.headline, headline, maxWidth)
        val headlineBlockHeight = headlineLines.size * headline.fontSpacing
        var y = (SIZE / 2f) - headlineBlockHeight / 2f
        headlineLines.forEach { line ->
            canvas.drawText(line, MARGIN, y + headline.textSize, headline)
            y += headline.fontSpacing
        }

        // Subline just below the headline block.
        wrap(lines.subline, subline, maxWidth).forEach { line ->
            y += subline.fontSpacing * 0.3f
            canvas.drawText(line, MARGIN, y + subline.textSize, subline)
            y += subline.fontSpacing
        }

        canvas.drawText(lines.footer, MARGIN, SIZE - MARGIN, footer)

        return writeAndShareUri(context, bitmap)
    }

    private fun textPaint(color: Int, size: Float, typeface: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        this.typeface = typeface
    }

    /** Greedy word-wrap so long headlines/sublines don't overflow the card. */
    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = StringBuilder(candidate)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun writeAndShareUri(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(dir, "fiber-week.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
