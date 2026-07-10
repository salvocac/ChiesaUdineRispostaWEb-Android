package com.caccavo.chiesaudinerispostaweb.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.caccavo.chiesaudinerispostaweb.R

/** Genera l'immagine "brandizzata" del versetto da condividere (stesso stile della card
 * dell'app iOS: sfondo sfumato blu-nero, logo, riferimento in giallo, testo bianco). */
object VerseImageFactory {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    fun makeVerseImage(context: Context, reference: String, verse: String): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                Color.argb(242, 25, 118, 210),
                Color.BLACK,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), backgroundPaint)

        val centerX = WIDTH / 2f
        var y = 260f

        val logoBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        }.getOrNull()
        if (logoBitmap != null) {
            val logoWidth = 260
            val logoHeight = (logoBitmap.height.toFloat() / logoBitmap.width * logoWidth).toInt()
            val scaled = Bitmap.createScaledBitmap(logoBitmap, logoWidth, logoHeight, true)
            canvas.drawBitmap(scaled, centerX - logoWidth / 2f, y, null)
            y += logoHeight + 70f
        }

        val referencePaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.YELLOW
            textSize = 56f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(reference, centerX, y, referencePaint)
        y += 90f

        val versePaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 46f
        }
        val quoted = "“$verse”"
        val textWidth = WIDTH - 2 * 170
        val layout = StaticLayout.Builder
            .obtain(quoted, 0, quoted.length, versePaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.15f)
            .build()
        canvas.save()
        canvas.translate((WIDTH - textWidth) / 2f, y)
        layout.draw(canvas)
        canvas.restore()

        val churchPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.argb(230, 255, 255, 255)
            textSize = 40f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Chiesa Cristiana Evangelica", centerX, HEIGHT - 190f, churchPaint)
        canvas.drawText("Friulana di Udine", centerX, HEIGHT - 140f, churchPaint)

        val urlPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.argb(180, 255, 255, 255)
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("www.chiesacristianaudine.it", centerX, HEIGHT - 90f, urlPaint)

        return bitmap
    }
}
