package com.caccavo.chiesaudinerispostaweb.video

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
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.caccavo.chiesaudinerispostaweb.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

enum class VideoBackground(private val startColor: Int, private val endColor: Int) {
    BLU(Color.rgb(13, 28, 77), Color.rgb(36, 77, 153)),
    TRAMONTO(Color.rgb(77, 36, 66), Color.rgb(204, 107, 61)),
    NOTTE(Color.rgb(8, 10, 26), Color.rgb(26, 41, 71));

    fun shader(width: Float, height: Float): LinearGradient =
        LinearGradient(0f, 0f, width, height, startColor, endColor, Shader.TileMode.CLAMP)

    companion object {
        fun forIndex(index: Int): VideoBackground = entries[index.mod(entries.size)]
    }
}

/** Genera un video "cartolina" del versetto (semplificato rispetto alla versione iOS: qui una
 * sola immagine statica per tutta la durata, invece delle dissolvenze testo-per-testo animate),
 * componendo immagine + audio narrato con Media3 Transformer. */
object VerseVideoFactory {

    private const val WIDTH = 1080
    private const val HEIGHT = 1440

    private fun renderCardBitmap(context: Context, reference: String, body: String, background: VideoBackground): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint().apply {
            shader = background.shader(WIDTH.toFloat(), HEIGHT.toFloat())
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), backgroundPaint)

        val centerX = WIDTH / 2f
        var y = 260f

        val logoBitmap = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.logo) }.getOrNull()
        if (logoBitmap != null) {
            val logoWidth = 220
            val logoHeight = (logoBitmap.height.toFloat() / logoBitmap.width * logoWidth).toInt()
            val scaled = Bitmap.createScaledBitmap(logoBitmap, logoWidth, logoHeight, true)
            canvas.drawBitmap(scaled, centerX - logoWidth / 2f, y, null)
            y += logoHeight + 70f
        }

        val referencePaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 68f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(reference, centerX, y, referencePaint)
        y += 110f

        val bodyPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 52f
        }
        val textWidth = WIDTH - 2 * 150
        val layout = StaticLayout.Builder
            .obtain(body, 0, body.length, bodyPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .build()
        canvas.save()
        canvas.translate((WIDTH - textWidth) / 2f, y)
        layout.draw(canvas)
        canvas.restore()

        val footerPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.argb(200, 255, 255, 255)
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Chiesa Cristiana Evangelica Friulana di Udine", centerX, HEIGHT - 90f, footerPaint)

        return bitmap
    }

    suspend fun makeVideo(
        context: Context,
        reference: String,
        body: String,
        audioFile: File,
        background: VideoBackground,
        outputFile: File
    ): File? {
        val durationSeconds = AudioCombiner.durationSeconds(audioFile)
        if (durationSeconds <= 0.0) return null

        val bitmap = renderCardBitmap(context, reference, body, background)
        val imageFile = File(context.cacheDir, "video-card-${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

        val imageMediaItem = MediaItem.Builder()
            .setUri(imageFile.toURI().toString())
            .setMimeType(MimeTypes.IMAGE_PNG)
            .build()
        val editedImageItem = EditedMediaItem.Builder(imageMediaItem)
            .setDurationUs((durationSeconds * 1_000_000).toLong())
            .setFrameRate(2)
            .build()
        val videoSequence = EditedMediaItemSequence(listOf(editedImageItem))

        val audioMediaItem = MediaItem.fromUri(audioFile.toURI().toString())
        val editedAudioItem = EditedMediaItem.Builder(audioMediaItem).build()
        val audioSequence = EditedMediaItemSequence(listOf(editedAudioItem))

        val composition = Composition.Builder(videoSequence, audioSequence).build()

        if (outputFile.exists()) outputFile.delete()

        return withContext(Dispatchers.Main) {
            val result = suspendCancellableCoroutine<Boolean> { continuation ->
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    })
                    .build()

                transformer.start(composition, outputFile.absolutePath)
            }
            imageFile.delete()
            if (result) outputFile else null
        }
    }
}
