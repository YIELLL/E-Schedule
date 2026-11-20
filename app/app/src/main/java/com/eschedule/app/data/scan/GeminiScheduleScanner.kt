package com.eschedule.app.data.scan

import com.eschedule.app.data.ScanStage
import com.eschedule.app.data.ScannedClass
import com.eschedule.app.data.ScheduleScanner
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

    override suspend fun scan(image: ByteArray, rotationDegrees: Int?, onStage: (ScanStage) -> Unit): List<ScannedClass> {
        onStage(ScanStage.Preparing)
        val jpeg = withContext(Dispatchers.Default) { ImageCompressor.compress(image, rotationDegrees) }
        val text = try {
            rotator.withKey { key -> client.transcribe(key, jpeg, onStage) }
        } catch (e: GeminiFailure) {
            throw ScanException(ScanException.Kind.BadResponse, "Couldn't read the schedule. Please try again.", e)
        }
        onStage(ScanStage.Reading)
        return try {
            ScanResultParser.parse(text)
        } catch (e: GeminiFailure) {
            throw ScanException(ScanException.Kind.BadResponse, "Couldn't understand the reply. Please try again.", e)
        }
    }
}
