package com.caccavo.chiesaudinerispostaweb.video

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/** Concatena più file audio in un unico file, usando Media3 Transformer (stesso motore usato
 * per generare il video): serve perché i clip vocali del versetto del giorno / di più versetti
 * sono registrati come file separati (titolo, versetto, commento). */
object AudioCombiner {

    fun durationSeconds(file: File): Double {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            ms / 1000.0
        } catch (e: Exception) {
            0.0
        } finally {
            retriever.release()
        }
    }

    suspend fun combine(context: Context, files: List<File>, outputFile: File): File? {
        if (files.isEmpty()) return null
        if (files.size == 1) {
            files[0].copyTo(outputFile, overwrite = true)
            return outputFile
        }

        if (outputFile.exists()) outputFile.delete()

        val editedItems = files.map { file ->
            EditedMediaItem.Builder(MediaItem.fromUri(file.toURI().toString())).build()
        }
        val sequence = EditedMediaItemSequence(editedItems)
        val composition = Composition.Builder(sequence).build()

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (continuation.isActive) continuation.resume(outputFile)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    })
                    .build()

                transformer.start(composition, outputFile.absolutePath)
            }
        }
    }
}
