package com.eschedule.app.data.scan

/** What went wrong in one call to the recognition service, as far as key handling is concerned. */
enum class FailureKind {
    /** Too many requests per minute: the key is fine again shortly. */
    RateLimited,

    /** Daily quota used up: the key is fine again after the quota resets. */
    DailyQuota,

    /** Prepaid credits ran out (or billing is required): the key stays out until it is edited. */
    CreditsDepleted,

    /** The key is wrong, expired or revoked. */
    InvalidKey,

    /** The key is valid but not allowed to do this (restricted project, region, not enabled yet). */
    Forbidden,

    /** Server hiccup or timeout: worth another try, on this key and then the next. */
    Transient,

    /** No connection to the service at all. */
    Offline,

    /** The request itself was rejected (bad payload, unknown model): another key won't help. */
    Rejected,

    /** The service declined to read the picture (safety / policy). */
    Blocked,

    /** A reply came back, but not one we can use (cut off, empty, malformed). */
    BadResponse,
}

/** Thrown by [GeminiClient]; carries just enough for [KeyRotator] to decide what happens next. */
class GeminiFailure(
    val kind: FailureKind,
    val httpStatus: Int,
    message: String,
    /** The service's own "try again in" hint, when it gave one. */
    val retryAfterMs: Long? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/** A scan failure the UI can show as-is: [userMessage] is already plain language. */
class ScanException(
    val kind: Kind,
    val userMessage: String,
    cause: Throwable? = null,
) : Exception(userMessage, cause) {

    enum class Kind { NoKey, AllKeysUnavailable, Offline, Busy, BadImage, Blocked, BadResponse, Rejected }
}
