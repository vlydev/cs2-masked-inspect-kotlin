package io.github.vlydev.cs2inspect.models

/**
 * Represents a sticker (or keychain) applied to a CS2 item.
 *
 * Float fields (wear, scale, rotation, offsetX, offsetY, offsetZ) use wire type 5
 * (fixed 32-bit little-endian). All other fields use varint encoding.
 * Null values are omitted from the proto encoding (proto3 semantics).
 */
data class Sticker(
    val slot: Int = 0,
    val stickerId: Int = 0,
    val wear: Float? = null,
    val scale: Float? = null,
    val rotation: Float? = null,
    val tintId: Int = 0,
    val offsetX: Float? = null,
    val offsetY: Float? = null,
    val offsetZ: Float? = null,
    val pattern: Int = 0,
    val highlightReel: Int? = null,
    /** Proto field 12 (varint): actual variant ID for sticker slabs; null if not present. */
    val paintKit: Int? = null,
)
