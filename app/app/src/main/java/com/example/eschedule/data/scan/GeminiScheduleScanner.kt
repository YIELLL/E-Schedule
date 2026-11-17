package com.example.eschedule.data.scan

import com.example.eschedule.data.ScannedClass
import com.example.eschedule.data.ScheduleScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The real [ScheduleScanner]: shrink the photo, send it to Gemini with whichever saved key works,
 * and turn the transcription into class rows.
 *
 * Failures surface as [ScanException] with a message that is safe to show. An empty list means the
 * photo was read fine but held no classes.
 */
class GeminiScheduleScanner(
    private val rotator: KeyRotator,
    private val client: GeminiClient,
) : ScheduleScanner {

    override suspend fun scan(image: ByteArray, rotationDegrees: Int?): List<ScannedClass> {
        val jpeg = withContext(Dispatchers.Default) { ImageCompressor.compress(image, rotationDegrees) }
        val text = try {
            rotator.withKey { key -> client.transcribe(key, jpeg) }
        } catch (e: GeminiFailure) {
            throw ScanException(ScanException.Kind.BadResponse, "Couldn't read the schedule. Please try again.", e)
        }
        return try {
            ScanResultParser.parse(text)
        } catch (e: GeminiFailure) {
            throw ScanException(ScanException.Kind.BadResponse, "Couldn't understand the reply. Please try again.", e)
        }
    }
}
