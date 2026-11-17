package com.example.eschedule.data.scan

import android.content.Context
import com.example.eschedule.data.ApiKeyEntry
import com.example.eschedule.data.AppLog
import com.example.eschedule.data.SettingsRepository
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

/**
 * Why a key is sitting out, and until when. A key that is "bad until edited" has [untilMillis] =
 * [Long.MAX_VALUE]; [valueHash] ties the verdict to the key text, so pasting in a new value clears it.
 */
internal data class KeyHealth(val kind: FailureKind, val untilMillis: Long, val valueHash: Int, val reason: String)

/** Remembers which keys are cooling down or dead across app restarts. */
class KeyHealthStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("api_key_health", Context.MODE_PRIVATE)

    internal fun get(key: ApiKeyEntry, now: Long): KeyHealth? {
        val raw = prefs.getString(key.id, null) ?: return null
        val health = runCatching {
            val o = JSONObject(raw)
            KeyHealth(FailureKind.valueOf(o.getString("kind")), o.getLong("until"), o.getInt("hash"), o.optString("reason"))
        }.getOrNull()
        // Expired cooldowns and verdicts about an older key text no longer apply.
        if (health == null || health.untilMillis <= now || health.valueHash != key.value.hashCode()) {
            prefs.edit().remove(key.id).apply()
            return null
        }
        return health
    }

    internal fun bench(key: ApiKeyEntry, kind: FailureKind, untilMillis: Long, reason: String) {
        val o = JSONObject()
            .put("kind", kind.name).put("until", untilMillis)
            .put("hash", key.value.hashCode()).put("reason", reason)
        prefs.edit().putString(key.id, o.toString()).apply()
    }
}

/**
 * Runs a request with the saved API keys, moving on to the next key when one can't do the job.
 *
 * Quota belongs to the Google *project*, so this only helps when the keys come from different
 * projects/accounts (which is the plan). Per failure:
 *  - temporary server trouble / timeout → retry the same key a few times with growing pauses, then try the next key
 *  - per-minute rate limit             → key cools down ~1 min, next key
 *  - daily quota used up               → key rests until the quota resets (midnight Pacific), next key
 *  - credits gone / invalid / not allowed → key sits out until it is edited, next key
 *  - bad request, blocked picture, no internet, unusable reply → stop; another key would fail the same way
 */
class KeyRotator(
    private val settings: SettingsRepository,
    private val health: KeyHealthStore,
    private val pause: suspend (Long) -> Unit = { delay(it) },
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** Saved keys, the chosen (active) one first. */
    private fun orderedKeys(): List<ApiKeyEntry> {
        val keys = settings.apiKeys.value.filter { it.value.isNotBlank() }
        val active = settings.activeApiKeyId.value
        return keys.sortedByDescending { it.id == active }
    }

    suspend fun <T> withKey(block: suspend (apiKey: String) -> T): T {
        val keys = orderedKeys()
        if (keys.isEmpty()) {
            throw ScanException(
                ScanException.Kind.NoKey,
                "Add a Gemini API key first (Settings → Developer → API keys).",
            )
        }

        var usable = 0
        var lastTransient: GeminiFailure? = null
        var coolDownOnly = true            // every skipped/failed key was just resting for a short while

        for (key in keys) {
            val now = clock()
            val resting = health.get(key, now)
            if (resting != null) {
                if (resting.kind != FailureKind.RateLimited) coolDownOnly = false
                continue
            }
            usable++

            var attempt = 0
            while (true) {
                attempt++
                try {
                    return block(key.value)
                } catch (f: GeminiFailure) {
                    when (f.kind) {
                        FailureKind.Transient -> {
                            if (attempt < MAX_TRIES) {
                                pause(backoffMs(attempt))
                                continue
                            }
                            AppLog.e("Scan", "Key \"${key.label}\": service unavailable after $attempt tries (${f.message})")
                            lastTransient = f
                        }
                        FailureKind.RateLimited -> {
                            val rest = (f.retryAfterMs ?: COOLDOWN_MS).coerceIn(5_000, 5 * 60_000)
                            health.bench(key, f.kind, clock() + rest, "rate limited")
                            AppLog.e("Scan", "Key \"${key.label}\": rate limited, resting ${rest / 1000}s")
                        }
                        FailureKind.DailyQuota -> {
                            health.bench(key, f.kind, nextQuotaReset(), "daily quota used")
                            coolDownOnly = false
                            AppLog.e("Scan", "Key \"${key.label}\": daily quota used up, resting until reset")
                        }
                        FailureKind.CreditsDepleted, FailureKind.InvalidKey, FailureKind.Forbidden -> {
                            health.bench(key, f.kind, Long.MAX_VALUE, f.kind.name)
                            coolDownOnly = false
                            AppLog.e("Scan", "Key \"${key.label}\": ${f.kind} (${f.message}) — skipped until edited")
                        }
                        FailureKind.Offline -> throw ScanException(
                            ScanException.Kind.Offline,
                            "No internet connection. Connect and try again.",
                            f,
                        )
                        FailureKind.Blocked -> {
                            AppLog.e("Scan", "Picture refused: ${f.message}")
                            throw ScanException(
                                ScanException.Kind.Blocked,
                                "The service wouldn't read that picture. Try a clearer photo of the schedule.",
                                f,
                            )
                        }
                        FailureKind.BadResponse -> {
                            AppLog.e("Scan", "Unusable reply: ${f.message}")
                            throw ScanException(
                                ScanException.Kind.BadResponse,
                                "Couldn't understand the reply. Please try again.",
                                f,
                            )
                        }
                        FailureKind.Rejected -> {
                            AppLog.e("Scan", "Request rejected: ${f.message}")
                            throw ScanException(
                                ScanException.Kind.Rejected,
                                "The recognition service rejected the request. Check the log under Settings → Developer.",
                                f,
                            )
                        }
                    }
                    break                    // this key is done for now → next key
                }
            }
        }

        throw when {
            usable == 0 && coolDownOnly -> ScanException(
                ScanException.Kind.AllKeysUnavailable,
                "All your API keys are busy right now. Try again in a minute.",
            )
            usable == 0 -> ScanException(
                ScanException.Kind.AllKeysUnavailable,
                "None of your API keys can be used: " + restingSummary(keys) + " Add another key in Settings.",
            )
            lastTransient != null -> ScanException(
                ScanException.Kind.Busy,
                "The recognition service is busy. Please try again in a moment.",
                lastTransient,
            )
            else -> ScanException(
                ScanException.Kind.AllKeysUnavailable,
                "All your API keys hit a limit. Try again in a minute, or add another key in Settings.",
            )
        }
    }

    /** "Key A (daily limit used, back after 3:00 PM)…" for the all-keys-out message. */
    private fun restingSummary(keys: List<ApiKeyEntry>): String {
        val now = clock()
        val parts = keys.mapNotNull { key ->
            val h = health.get(key, now) ?: return@mapNotNull null
            val why = when (h.kind) {
                FailureKind.DailyQuota -> "daily limit used" + backAt(h.untilMillis)
                FailureKind.RateLimited -> "busy" + backAt(h.untilMillis)
                FailureKind.CreditsDepleted -> "out of credits"
                FailureKind.InvalidKey -> "invalid key"
                else -> "not allowed"
            }
            "${key.label.ifBlank { "Key" }} ($why)"
        }
        return parts.joinToString("; ") + "."
    }

    private fun backAt(untilMillis: Long): String {
        if (untilMillis == Long.MAX_VALUE) return ""
        val local = Instant.ofEpochMilli(untilMillis).atZone(ZoneId.systemDefault())
        return ", back " + local.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }

    private fun backoffMs(attempt: Int): Long {
        val base = 1_000L shl (attempt - 1)               // 1s, 2s, 4s …
        return base + Random.nextLong(0, base / 2 + 1)    // plus jitter so retries don't line up
    }

    /** Google resets daily quotas at midnight Pacific time. */
    private fun nextQuotaReset(): Long {
        val pacific = ZoneId.of("America/Los_Angeles")
        return ZonedDateTime.now(pacific).toLocalDate().plusDays(1).atStartOfDay(pacific).toInstant().toEpochMilli()
    }

    private companion object {
        const val MAX_TRIES = 3
        const val COOLDOWN_MS = 60_000L
    }
}
