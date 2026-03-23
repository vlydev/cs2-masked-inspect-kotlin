package io.github.vlydev.cs2inspect

import io.github.vlydev.cs2inspect.models.ItemPreviewData
import io.github.vlydev.cs2inspect.models.Sticker
import java.util.Locale

/**
 * Gen code utilities for CS2 inspect links.
 *
 * Gen codes are space-separated command strings used on community servers:
 * ```
 * !gen {defindex} {paintindex} {paintseed} {paintwear}
 * !gen ... {s0_id} {s0_wear} {s1_id} {s1_wear} ... {s4_id} {s4_wear} [{kc_id} {kc_wear} ...]
 * ```
 *
 * Stickers are always padded to 5 slot pairs. Keychains follow without padding.
 */
object GenCode {

    const val INSPECT_BASE = "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%20"

    /** Format a Float, stripping trailing zeros (max 8 decimal places). */
    private fun formatFloat(value: Float): String {
        val s = String.format(Locale.US, "%.8f", value).trimEnd('0').trimEnd('.')
        return if (s.isEmpty()) "0" else s
    }

    private fun serializeStickerPairs(stickers: List<Sticker>, padTo: Int?): List<String> {
        val result = mutableListOf<String>()
        val filtered = stickers.filter { it.stickerId != 0 }

        if (padTo != null) {
            val slotMap = filtered.associateBy { it.slot }
            for (slot in 0 until padTo) {
                val s = slotMap[slot]
                if (s != null) {
                    result += s.stickerId.toString()
                    result += formatFloat(s.wear ?: 0f)
                } else {
                    result += "0"
                    result += "0"
                }
            }
        } else {
            for (s in filtered.sortedBy { it.slot }) {
                result += s.stickerId.toString()
                result += formatFloat(s.wear ?: 0f)
                if (s.paintKit != null) {
                    result += s.paintKit.toString()
                }
            }
        }

        return result
    }

    /**
     * Convert an [ItemPreviewData] to a gen code string.
     *
     * @param item The item to convert.
     * @param prefix The command prefix, e.g. `"!gen"` or `"!g"`.
     * @return A gen code like `"!gen 7 474 306 0.22540508"`.
     */
    fun toGenCode(item: ItemPreviewData, prefix: String = "!gen"): String {
        val wearStr = item.paintWear?.let { formatFloat(it) } ?: "0"
        val parts = mutableListOf(
            item.defIndex.toString(),
            item.paintIndex.toString(),
            item.paintSeed.toString(),
            wearStr,
        )

        val hasStickers = item.stickers.any { it.stickerId != 0 }
        val hasKeychains = item.keychains.any { it.stickerId != 0 }
        if (hasStickers || hasKeychains) {
            parts += serializeStickerPairs(item.stickers, 5)
            parts += serializeStickerPairs(item.keychains, null)
        }

        val payload = parts.joinToString(" ")
        return if (prefix.isEmpty()) payload else "$prefix $payload"
    }

    /**
     * Generate a full Steam inspect URL from item parameters.
     *
     * @param defIndex Weapon definition ID (e.g. 7 = AK-47).
     * @param paintIndex Skin/paint ID.
     * @param paintSeed Pattern index (0-1000).
     * @param paintWear Float value (0.0-1.0).
     * @param rarity Item rarity tier (default 0).
     * @param quality Item quality (default 0).
     * @param stickers Stickers applied to the item.
     * @param keychains Keychains applied to the item.
     * @return Full `steam://rungame/...` inspect URL.
     * @throws IllegalArgumentException if paintWear is outside [0.0, 1.0].
     */
    fun generate(
        defIndex: Int,
        paintIndex: Int,
        paintSeed: Int,
        paintWear: Float,
        rarity: Int = 0,
        quality: Int = 0,
        stickers: List<Sticker> = emptyList(),
        keychains: List<Sticker> = emptyList(),
    ): String {
        val data = ItemPreviewData(
            defIndex = defIndex,
            paintIndex = paintIndex,
            paintSeed = paintSeed,
            paintWear = paintWear,
            rarity = rarity,
            quality = quality,
            stickers = stickers,
            keychains = keychains,
        )
        val hex = InspectLink.serialize(data)
        return "$INSPECT_BASE$hex"
    }

    /**
     * Generate a gen code string from an existing CS2 inspect link.
     *
     * Deserializes the inspect link and converts the item data to gen code format.
     *
     * @param hexOrUrl A hex payload or full steam:// inspect URL.
     * @param prefix The command prefix (default "!gen").
     * @return Gen code string like "!gen 7 474 306 0.22540508".
     */
    fun genCodeFromLink(hexOrUrl: String, prefix: String = "!gen"): String {
        val item = InspectLink.deserialize(hexOrUrl)
        return toGenCode(item, prefix)
    }

    /**
     * Parse a gen code string into an [ItemPreviewData].
     *
     * @param genCode A gen code like `"!gen 7 474 306 0.22540508"`.
     * @return Parsed [ItemPreviewData].
     * @throws IllegalArgumentException if the code has fewer than 4 tokens.
     */
    fun parseGenCode(genCode: String): ItemPreviewData {
        var tokens = genCode.trim().split(Regex("\\s+"))
        if (tokens.firstOrNull()?.startsWith("!") == true) {
            tokens = tokens.drop(1)
        }

        require(tokens.size >= 4) {
            "Gen code must have at least 4 tokens, got: \"$genCode\""
        }

        val defIndex   = tokens[0].toInt()
        val paintIndex = tokens[1].toInt()
        val paintSeed  = tokens[2].toInt()
        val paintWear  = tokens[3].toFloat()
        var rest = tokens.drop(4)

        val stickers  = mutableListOf<Sticker>()
        val keychains = mutableListOf<Sticker>()

        if (rest.size >= 10) {
            val stickerTokens = rest.take(10)
            for (slot in 0 until 5) {
                val sid  = stickerTokens[slot * 2].toInt()
                val wear = stickerTokens[slot * 2 + 1].toFloat()
                if (sid != 0) stickers += Sticker(slot = slot, stickerId = sid, wear = wear)
            }
            rest = rest.drop(10)
        }

        var i = 0
        while (i + 1 < rest.size) {
            val sid  = rest[i].toInt()
            val wear = rest[i + 1].toFloat()
            if (sid != 0) keychains += Sticker(slot = i / 2, stickerId = sid, wear = wear)
            i += 2
        }

        return ItemPreviewData(
            defIndex   = defIndex,
            paintIndex = paintIndex,
            paintSeed  = paintSeed,
            paintWear  = paintWear,
            stickers   = stickers,
            keychains  = keychains,
        )
    }
}
