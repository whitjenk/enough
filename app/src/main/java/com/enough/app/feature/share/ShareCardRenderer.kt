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
 * Draws a share card (SPEC §7.6 Step 2) to a PNG and returns a FileProvider
 * content URI to hand to the OS share sheet. Two sizes: a square for the weekly
 * Progress card and a 9:16 **story** for the feeling-first daily card, which is
 * the format native to the surfaces the "fibermaxing" moment runs on (SPEC §0.8).
 *
 * A fixed, theme-independent light look: a shared image should read the same for
 * everyone. Only ever invoked from an explicit "share" tap — nothing runs on its
 * own, and nothing leaves the device except through the user's own share.
 */
object ShareCardRenderer {

    private const val SQUARE = 1080
    private const val STORY_W = 1080
    private const val STORY_H = 1920
    private const val MARGIN = 96f
    private val BACKGROUND = Color.parseColor("#EAF3EA")
    private val INK = Color.parseColor("#1E3A2B")
    private val ACCENT = Color.parseColor("#3B7A57")

    /** The weekly square card (Progress). */
    fun render(context: Context, lines: ShareCardLines): Uri = draw(context, lines, SQUARE, SQUARE)

    /** The feeling-first daily card in 9:16 story format (Today, at the check-in peak). */
    fun renderStory(context: Context, lines: ShareCardLines): Uri = draw(context, lines, STORY_W, STORY_H)

    private fun draw(context: Context, lines: ShareCardLines, width: Int, height: Int): Uri {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND)

        val title = textPaint(ACCENT, 44f, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
        val headline = textPaint(INK, 96f, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        val subline = textPaint(INK, 48f, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
        val footer = textPaint(ACCENT, 40f, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))

        canvas.drawText(lines.title, MARGIN, MARGIN + title.textSize, title)

        // A small accent motif above the headline gives the tall story card a focal point.
        val maxWidth = width - 2 * MARGIN
        val headlineLines = wrap(lines.headline, headline, maxWidth)
        val headlineBlock = headlineLines.size * headline.fontSpacing
        val centerY = height / 2f
        canvas.drawCircle(MARGIN + 18f, centerY - headlineBlock / 2f - 56f, 18f, textPaint(ACCENT, 1f, Typeface.DEFAULT))

        var y = centerY - headlineBlock / 2f
        headlineLines.forEach { line ->
            canvas.drawText(line, MARGIN, y + headline.textSize, headline)
            y += headline.fontSpacing
        }

        wrap(lines.subline, subline, maxWidth).forEach { line ->
            y += subline.fontSpacing * 0.35f
            canvas.drawText(line, MARGIN, y + subline.textSize, subline)
            y += subline.fontSpacing
        }

        canvas.drawText(lines.footer, MARGIN, height - MARGIN, footer)

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
        val file = File(dir, "enough-share.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
