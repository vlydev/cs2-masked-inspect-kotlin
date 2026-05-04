package io.github.vlydev.cs2inspect

/**
 * Thrown by [InspectLink.deserialize] when the input cannot be a valid inspect-link payload —
 * odd-length hex, non-hex characters, payload shorter than the minimum, or proto bytes that
 * fail to parse cleanly.
 *
 * Extends [IllegalArgumentException] for backwards compatibility with callers that catch
 * the parent class.
 */
class MalformedInspectLinkException : IllegalArgumentException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)
}