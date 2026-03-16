package io.github.vlydev.cs2inspect.models

/**
 * Decoded representation of a CEconItemPreviewDataBlock protobuf message.
 *
 * Field numbering follows the CS2 protobuf schema exactly.
 * Zero/default values are omitted when encoding (proto3 semantics).
 * Use [Long] for [itemId] since it can exceed Int range.
 * [paintWear] is nullable — absent from the proto when not set (e.g., keychains).
 */
data class ItemPreviewData(
    /** Field 1: accountId (UInt varint) */
    val accountId: Int = 0,
    /** Field 2: itemId (ULong varint — may exceed 32-bit range) */
    val itemId: Long = 0L,
    /** Field 3: defIndex — item definition index */
    val defIndex: Int = 0,
    /** Field 4: paintIndex — paint kit index */
    val paintIndex: Int = 0,
    /** Field 5: rarity */
    val rarity: Int = 0,
    /** Field 6: quality */
    val quality: Int = 0,
    /** Field 7: paintWear stored as float32 bits in a varint; null means field absent */
    val paintWear: Float? = null,
    /** Field 8: paintSeed (0–1000) */
    val paintSeed: Int = 0,
    /** Field 9: killEaterScoreType */
    val killEaterScoreType: Int = 0,
    /** Field 10: killEaterValue */
    val killEaterValue: Int = 0,
    /** Field 11: customName (length-delimited string) */
    val customName: String = "",
    /** Field 12: stickers (repeated, length-delimited) */
    val stickers: List<Sticker> = emptyList(),
    /** Field 13: inventory */
    val inventory: Int = 0,
    /** Field 14: origin */
    val origin: Int = 0,
    /** Field 15: questId */
    val questId: Int = 0,
    /** Field 16: dropReason */
    val dropReason: Int = 0,
    /** Field 17: musicIndex */
    val musicIndex: Int = 0,
    /** Field 18: entIndex (signed Int) */
    val entIndex: Int = 0,
    /** Field 19: petIndex */
    val petIndex: Int = 0,
    /** Field 20: keychains (repeated, length-delimited) */
    val keychains: List<Sticker> = emptyList(),
)
