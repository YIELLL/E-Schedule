package com.eschedule.app.data.scan

import android.util.Base64
import com.eschedule.app.data.ScanStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.Locale

/**
 * One call to Gemini's `generateContent`: a picture of the schedule in, the transcribed class rows
 * (as JSON text) out. Knows nothing about which key to use or what to do on failure — it just
 * reports the failure precisely as a [GeminiFailure] so [KeyRotator] can decide.
 *
 * The API key travels in the `x-goog-api-key` header, never in the URL, so it can't end up in logs.
 */
class GeminiClient(private val model: String = DEFAULT_MODEL) {

    /**
     * Sends [jpeg] with [apiKey] and returns the model's JSON text (see [SCHEMA]).
     * [onStage] hears [ScanStage.Uploading] when the photo starts going out and [ScanStage.Recognizing]
     * once it has all been sent (what follows is the wait for the reply).
     */
    suspend fun transcribe(apiKey: String, jpeg: ByteArray, onStage: (ScanStage) -> Unit = {}): String =
        withContext(Dispatchers.IO) {
        val body = buildRequest(jpeg)
        val connection = (URL(ENDPOINT.format(model)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            // Stream the body as it is written instead of buffering it until the reply is asked for,
            // so "uploading" really is the upload and "recognizing" really is the wait.
            setFixedLengthStreamingMode(body.size)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        try {
            onStage(ScanStage.Uploading)
            connection.outputStream.use { it.write(body) }
            onStage(ScanStage.Recognizing)
            val status = connection.responseCode
            if (status in 200..299) {
                parseSuccess(connection.inputStream.bufferedReader().use { it.readText() })
            } else {
                val raw = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw classify(status, raw)
            }
        } catch (e: GeminiFailure) {
            throw e
        } catch (e: UnknownHostException) {
            throw GeminiFailure(FailureKind.Offline, 0, "No connection to the service", cause = e)
        } catch (e: ConnectException) {
            throw GeminiFailure(FailureKind.Offline, 0, "No connection to the service", cause = e)
        } catch (e: SocketTimeoutException) {
            throw GeminiFailure(FailureKind.Transient, 0, "The service took too long to answer", cause = e)
        } catch (e: IOException) {
            throw GeminiFailure(FailureKind.Transient, 0, "Network error: ${e.message}", cause = e)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildRequest(jpeg: ByteArray): ByteArray {
        val parts = JSONArray()
            .put(JSONObject().put("text", PROMPT))                                   // text first…
            .put(
                JSONObject().put(                                                     // …then the picture
                    "inlineData",
                    JSONObject()
                        .put("mimeType", "image/jpeg")
                        .put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP)),
                ),
            )
        val request = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("responseJsonSchema", JSONObject(SCHEMA))
                    .put("mediaResolution", "MEDIA_RESOLUTION_HIGH"),
            )
        return request.toString().toByteArray(Charsets.UTF_8)
    }

    /** Pulls the model's text out of a 200 reply, or fails if it was blocked / cut off / empty. */
    private fun parseSuccess(raw: String): String {
        val root = runCatching { JSONObject(raw) }.getOrNull()
            ?: throw GeminiFailure(FailureKind.BadResponse, 200, "The reply wasn't valid JSON")

        root.optJSONObject("promptFeedback")?.optString("blockReason")?.takeIf { it.isNotBlank() }?.let {
            throw GeminiFailure(FailureKind.Blocked, 200, "The picture was blocked ($it)")
        }

        val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
            ?: throw GeminiFailure(FailureKind.BadResponse, 200, "The reply had no result")

        val finish = candidate.optString("finishReason")
        when {
            finish.isBlank() || finish == "STOP" -> Unit
            finish == "MAX_TOKENS" -> throw GeminiFailure(FailureKind.BadResponse, 200, "The reply was cut off")
            else -> throw GeminiFailure(FailureKind.Blocked, 200, "The reply was stopped ($finish)")
        }

        // Reply parts can also carry thought signatures; only the ones with text are the answer.
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        val text = buildString {
            if (parts != null) for (i in 0 until parts.length()) append(parts.optJSONObject(i)?.optString("text").orEmpty())
        }
        if (text.isBlank()) throw GeminiFailure(FailureKind.BadResponse, 200, "The reply was empty")
        return text
    }

    /** Maps an error reply (HTTP status + Google's error body) to a [FailureKind]. */
    private fun classify(http: Int, raw: String): GeminiFailure {
        val error = runCatching { JSONObject(raw).optJSONObject("error") }.getOrNull()
        val status = error?.optString("status").orEmpty()
        val message = error?.optString("message").orEmpty().ifBlank { "HTTP $http" }
        val lower = "$message $raw".lowercase(Locale.US)
        val creditsGone = "credits" in lower && ("deplet" in lower || "insufficient" in lower) || "prepayment" in lower

        val kind = when {
            creditsGone || http == 402 -> FailureKind.CreditsDepleted
            http == 429 -> when {
                "perday" in lower || "per day" in lower -> FailureKind.DailyQuota
                "perminute" in lower || "per minute" in lower -> FailureKind.RateLimited
                "daily" in lower -> FailureKind.DailyQuota
                else -> FailureKind.RateLimited
            }
            http == 401 -> FailureKind.InvalidKey
            http == 403 -> FailureKind.Forbidden
            http == 404 -> FailureKind.Rejected                       // unknown model / endpoint
            http == 400 -> when {
                "api_key_invalid" in lower || "api key not valid" in lower || "api key expired" in lower ->
                    FailureKind.InvalidKey
                status == "FAILED_PRECONDITION" -> FailureKind.Forbidden   // billing / region not available
                else -> FailureKind.Rejected
            }
            http in 500..599 -> FailureKind.Transient
            else -> FailureKind.Rejected
        }
        return GeminiFailure(kind, http, "HTTP $http $status: ${message.take(200)}", retryAfterMs(error))
    }

    /** `error.details[]` may hold `{"@type": ".../RetryInfo", "retryDelay": "34s"}`. */
    private fun retryAfterMs(error: JSONObject?): Long? {
        val details = error?.optJSONArray("details") ?: return null
        for (i in 0 until details.length()) {
            val delay = details.optJSONObject(i)?.optString("retryDelay").orEmpty()
            val seconds = delay.removeSuffix("s").toDoubleOrNull()
            if (seconds != null) return (seconds * 1000).toLong()
        }
        return null
    }

    companion object {
        /** One place to change the model. Alternatives: gemini-3.1-flash-lite, gemini-2.5-flash-lite. */
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000

        /** Transcribe only: our own code does the day expansion and time conversion (see [ScanResultParser]). */
        private const val PROMPT =
            "This is a photo of a student's certificate of registration. Transcribe ONLY the rows of the class table " +
                "(columns like SubjectCode, Description, From, To, Day, Room), exactly as printed. " +
                "One entry per printed line of times: a subject can have several lines, each with its own times, day and room. " +
                "Repeat the subject code and description on every line of that subject. " +
                "Do NOT convert anything: copy the day text (e.g. 'TueThu', 'MonWed', 'Fri') and the times (e.g. '01:00 PM') verbatim. " +
                "Ignore names, student numbers, fees, signatures and every other part of the page. " +
                "Do not invent rows; use null for unreadable values."

        private const val SCHEMA = """
            {"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{
              "subject_code":{"type":"string"},
              "description":{"type":["string","null"]},
              "from":{"type":"string","description":"Start time exactly as printed"},
              "to":{"type":"string","description":"End time exactly as printed"},
              "day":{"type":"string","description":"Day text exactly as printed"},
              "room":{"type":["string","null"]}},
              "required":["subject_code","from","to","day"]}}},"required":["rows"]}
        """
    }
}
