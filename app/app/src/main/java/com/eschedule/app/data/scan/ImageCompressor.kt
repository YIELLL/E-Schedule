package com.eschedule.app.data.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * Shrinks a photo before it is uploaded: decode, straighten, scale the long edge down to
 * [MAX_EDGE] pixels and re-encode as JPEG. A 12 MP phone photo (3–6 MB) comes out around
 * 300–800 KB, which is plenty to read printed text and keeps uploads quick on mobile data.
 *
 * The input can be a JPEG (from the camera) or a HEIC/HEIF picture (gallery, API 28+); the output
 * is always JPEG, so whatever the source, the upload size is decided here.
 */
object ImageCompressor {
    const val MAX_EDGE = 2000
    const val QUALITY = 85

    /** Long edge of the on-screen preview of the captured photo. */
    const val PREVIEW_EDGE = 1200

    /**
     * @param rotationDegrees clockwise turn needed to make the picture upright when the caller knows
     *   it (the camera does); null reads the file's own EXIF orientation instead (gallery files).
     * @throws ScanException when the bytes aren't a picture we can read.
     */
    fun compress(source: ByteArray, rotationDegrees: Int? = null): ByteArray {
        val decoded = decode(source) ?: throw ScanException(
            ScanException.Kind.BadImage,
            "That picture couldn't be read. Try taking the photo again.",
        )
        val turn = rotationDegrees ?: exifRotation(source)
        var bitmap = decoded
        try {
            bitmap = scaleAndRotate(decoded, turn)
            val out = ByteArrayOutputStream(256 * 1024)
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)) {
                throw ScanException(ScanException.Kind.BadImage, "That picture couldn't be prepared for upload.")
            }
            return out.toByteArray()
        } finally {
            if (bitmap !== decoded) bitmap.recycle()
            decoded.recycle()
        }
    }

    /**
     * A small, upright copy of the photo for showing on screen (the frozen frame while it is being
     * read). Returns null when the bytes can't be decoded; the caller just skips the preview then.
     */
    fun preview(source: ByteArray, rotationDegrees: Int? = null, maxEdge: Int = PREVIEW_EDGE): Bitmap? {
        val decoded = decode(source, maxEdge) ?: return null
        val turn = rotationDegrees ?: exifRotation(source)
        return try {
            val upright = scaleAndRotate(decoded, turn, maxEdge)
            if (upright !== decoded) decoded.recycle()
            upright
        } catch (e: OutOfMemoryError) {
            decoded.recycle()
            null
        }
    }

    private fun decode(source: ByteArray, maxEdge: Int = MAX_EDGE): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)

        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            // Decode at 1/2, 1/4, … size while the result still has at least maxEdge on the long side.
            var sample = 1
            val longEdge = max(bounds.outWidth, bounds.outHeight)
            while (longEdge / (sample * 2) >= maxEdge) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(source, 0, source.size, options)?.let { return it }
        }

        // BitmapFactory can't read HEIC before API 28; ImageDecoder is the fallback on newer phones.
        if (Build.VERSION.SDK_INT >= 28) {
            return runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(source))) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val edge = max(info.size.width, info.size.height)
                    if (edge > maxEdge) {
                        val scale = maxEdge.toFloat() / edge
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1),
                        )
                    }
                }
            }.getOrNull()
        }
        return null
    }

    private fun scaleAndRotate(bitmap: Bitmap, rotationDegrees: Int, maxEdge: Int = MAX_EDGE): Bitmap {
        val edge = max(bitmap.width, bitmap.height)
        val scale = if (edge > maxEdge) maxEdge.toFloat() / edge else 1f
        val turn = ((rotationDegrees % 360) + 360) % 360
        if (scale == 1f && turn == 0) return bitmap
        val matrix = Matrix().apply {
            if (scale != 1f) postScale(scale, scale)
            if (turn != 0) postRotate(turn.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun exifRotation(source: ByteArray): Int = runCatching {
        val orientation = ExifInterface(ByteArrayInputStream(source))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)
}
