package io.github.vlydev.cs2inspect

import io.github.vlydev.cs2inspect.models.ItemPreviewData
import io.github.vlydev.cs2inspect.models.Sticker
import io.github.vlydev.cs2inspect.proto.ProtoReader
import io.github.vlydev.cs2inspect.proto.ProtoWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * Encodes and decodes CS2 masked inspect links.
 *
 * Binary format:
 *   [key_byte] [proto_bytes XOR'd with key] [4-byte checksum XOR'd with key]
 *
 * For tool-generated links key_byte = 0x00 (no XOR needed).
 * For native CS2 links key_byte != 0x00 — every byte must be XOR'd before parsing.
 *
 * Checksum:
 *   buffer   = [0x00] + proto_bytes
 *   crc      = crc32(buffer)   (java.util.zip.CRC32, polynomial 0xEDB88320)
 *   xored    = (crc & 0xffff) ^ (proto_bytes.size * crc)   [unsigned 32-bit]
 *   checksum = big-endian uint32 of (xored & 0xFFFFFFFF)
 */
object InspectLink {

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Encodes an [ItemPreviewData] to an uppercase hex inspect-link payload.
     *
     * The returned string can be appended to a `steam://` inspect URL or used
     * standalone. The key_byte is always 0x00 (no XOR applied).
     *
     * @throws IllegalArgumentException if [ItemPreviewData.paintWear] is outside [0.0, 1.0]
     *         or [ItemPreviewData.customName] exceeds 100 characters.
     */
    fun serialize(data: ItemPreviewData): String {
        val paintWear = data.paintWear
        if (paintWear != null && (paintWear < 0.0f || paintWear > 1.0f)) {
            throw IllegalArgumentException("paintWear must be in [0.0, 1.0], got $paintWear")
        }
        if (data.customName.length > 100) {
            throw IllegalArgumentException(
                "customName must not exceed 100 characters, got ${data.customName.length}"
            )
        }

        val protoBytes = encodeItem(data)
        val buffer = ByteArray(1 + protoBytes.size)
        buffer[0] = 0x00
        protoBytes.copyInto(buffer, 1)

        val checksum = computeChecksum(buffer, protoBytes.size)
        val result = buffer + checksum

        return result.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    }

    /**
     * Decodes an inspect-link hex payload (or full URL) into an [ItemPreviewData].
     *
     * Accepts:
     *   - A raw uppercase or lowercase hex string
     *   - A full `steam://rungame/...` inspect URL
     *   - A `csgo://rungame/...` URL
     *   - A hybrid `S\d+A\d+D<hex>` URL
     *
     * Handles the XOR obfuscation used in native CS2 links.
     *
     * @throws IllegalArgumentException on invalid or too-short/too-long payloads.
     */
    fun deserialize(input: String): ItemPreviewData {
        val hex = extractHex(input)
        val preview = if (input.length > 120) input.take(100) + "..." else input

        if (hex.length > 4096) {
            throw MalformedInspectLinkException(
                "Malformed inspect URL: payload too long (max 4096 hex chars). Input: \"$preview\""
            )
        }

        // Reject malformed hex up-front so callers always get one consistent
        // MalformedInspectLinkException instead of an internal NumberFormatException
        // / IllegalArgumentException leaking the implementation.
        if (hex.isEmpty() || hex.length % 2 != 0) {
            throw MalformedInspectLinkException(
                "Malformed inspect URL: hex payload has invalid length (${hex.length} chars, must be even and non-empty). The source likely truncated the URL. Input: \"$preview\""
            )
        }
        if (!HEX_ONLY_RE.matches(hex)) {
            throw MalformedInspectLinkException(
                "Malformed inspect URL: payload contains non-hex characters. Input: \"$preview\""
            )
        }

        val raw = try {
            hexToBytes(hex)
        } catch (e: IllegalArgumentException) {
            throw MalformedInspectLinkException(
                "Malformed inspect URL: hex decode failed (${e.message}). Input: \"$preview\"",
                e
            )
        }

        if (raw.size < 6) {
            throw MalformedInspectLinkException(
                "Malformed inspect URL: payload too short (${raw.size} bytes, need >=6). Input: \"$preview\""
            )
        }

        val key = raw[0].toInt() and 0xFF

        val decrypted: ByteArray = if (key == 0) {
            raw
        } else {
            ByteArray(raw.size) { i -> (raw[i].toInt() xor key).toByte() }
        }

        // Layout: [key_byte] [proto_bytes] [4-byte checksum]
        val protoBytes = decrypted.copyOfRange(1, decrypted.size - 4)

        return try {
            decodeItem(protoBytes)
        } catch (e: Throwable) {
            throw MalformedInspectLinkException(
                "Malformed inspect URL: protobuf decode failed (${e.message}). Payload likely corrupted or truncated. Input: \"$preview\"",
                e
            )
        }
    }

    /**
     * Returns true if the link contains a decodable protobuf payload that can be decoded offline.
     */
    fun isMasked(link: String): Boolean {
        val s = link.trim()
        // Pure hex blob after csgo_econ_action_preview separator
        if (MASKED_URL_RE.containsMatchIn(s)) return true
        // Hybrid: S\d+A\d+D<hexproto>
        val m = HYBRID_URL_RE.find(s)
        return m != null && HEX_LETTER_RE.containsMatchIn(m.groupValues[1])
    }

    /**
     * Returns true if the link is a classic S/A/D inspect URL with decimal values.
     */
    fun isClassic(link: String): Boolean {
        return CLASSIC_URL_RE.containsMatchIn(link.trim())
    }

    // ------------------------------------------------------------------
    // Private helpers: URL extraction
    // ------------------------------------------------------------------

    private val HYBRID_URL_RE = Regex("""S\d+A\d+D([0-9A-Fa-f]+)$""")
    private val CLASSIC_URL_RE = Regex("""csgo_econ_action_preview(?:%20|\s)[SM]\d+A\d+D\d+$""", RegexOption.IGNORE_CASE)
    private val MASKED_URL_RE = Regex("""csgo_econ_action_preview(?:%20|\s)%?[0-9A-Fa-f]{10,}$""", RegexOption.IGNORE_CASE)
    private val INSPECT_URL_RE = Regex("""(?:%20|\s|\+)A([0-9A-Fa-f]+)""", RegexOption.IGNORE_CASE)
    private val PURE_MASKED_RE = Regex("""csgo_econ_action_preview(?:%20|\s|\+)%?([0-9A-Fa-f]{10,})$""", RegexOption.IGNORE_CASE)
    private val HEX_LETTER_RE = Regex("""[A-Fa-f]""")
    private val HEX_ONLY_RE = Regex("""^[0-9A-Fa-f]+$""")

    private fun extractHex(input: String): String {
        val stripped = input.trim()

        // Hybrid format: S\d+A\d+D<hexproto> — extract the hex part after D
        val mh = HYBRID_URL_RE.find(stripped)
        if (mh != null && HEX_LETTER_RE.containsMatchIn(mh.groupValues[1])) {
            return mh.groupValues[1]
        }

        // Classic/market URL: A<hex> preceded by %20, space, or +
        // If stripping A yields odd-length hex, fall through to pure-masked check
        val m = INSPECT_URL_RE.find(stripped)
        if (m != null && m.groupValues[1].length % 2 == 0) {
            return m.groupValues[1]
        }

        // Pure masked format: csgo_econ_action_preview%20<hexblob>
        // Also handles payloads whose first hex character happens to be 'A'
        val mm = PURE_MASKED_RE.find(stripped)
        if (mm != null) {
            return mm.groupValues[1]
        }

        // Bare hex — strip whitespace
        return stripped.replace(Regex("""\s+"""), "")
    }

    // ------------------------------------------------------------------
    // Private helpers: checksum
    // ------------------------------------------------------------------

    private fun computeChecksum(buffer: ByteArray, protoLen: Int): ByteArray {
        val crc32 = CRC32()
        crc32.update(buffer)
        val crc = crc32.value // unsigned 32-bit

        val xored = ((crc and 0xFFFFL) xor (protoLen.toLong() * crc)) and 0xFFFFFFFFL

        val bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        bb.putInt(xored.toInt())
        return bb.array()
    }

    // ------------------------------------------------------------------
    // Private helpers: hex conversion
    // ------------------------------------------------------------------

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Odd-length hex string: $hex" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    // ------------------------------------------------------------------
    // Private helpers: float32 <-> uint32 reinterpretation
    // ------------------------------------------------------------------

    private fun float32ToUint32(f: Float): Int {
        return java.lang.Float.floatToRawIntBits(f)
    }

    private fun uint32ToFloat32(u: Long): Float {
        return java.lang.Float.intBitsToFloat((u and 0xFFFFFFFFL).toInt())
    }

    // ------------------------------------------------------------------
    // Private helpers: Sticker encode/decode
    // ------------------------------------------------------------------

    private fun encodeSticker(s: Sticker): ByteArray {
        val w = ProtoWriter()
        w.writeUint32(1, s.slot)
        w.writeUint32(2, s.stickerId)
        s.wear?.let { w.writeFloat32Fixed(3, it) }
        s.scale?.let { w.writeFloat32Fixed(4, it) }
        s.rotation?.let { w.writeFloat32Fixed(5, it) }
        w.writeUint32(6, s.tintId)
        s.offsetX?.let { w.writeFloat32Fixed(7, it) }
        s.offsetY?.let { w.writeFloat32Fixed(8, it) }
        s.offsetZ?.let { w.writeFloat32Fixed(9, it) }
        w.writeUint32(10, s.pattern)
        s.highlightReel?.let { w.writeUint32(11, it) }
        s.paintKit?.let { w.writeUint32(12, it) }
        return w.toBytes()
    }

    private fun decodeSticker(data: ByteArray): Sticker {
        val reader = ProtoReader(data)
        var slot = 0
        var stickerId = 0
        var wear: Float? = null
        var scale: Float? = null
        var rotation: Float? = null
        var tintId = 0
        var offsetX: Float? = null
        var offsetY: Float? = null
        var offsetZ: Float? = null
        var pattern = 0
        var highlightReel: Int? = null
        var paintKit: Int? = null

        for (field in reader.readAllFields()) {
            when (field) {
                is ProtoReader.Field.Varint -> when (field.num) {
                    1 -> slot = field.value.toInt()
                    2 -> stickerId = field.value.toInt()
                    6 -> tintId = field.value.toInt()
                    10 -> pattern = field.value.toInt()
                    11 -> highlightReel = field.value.toInt()
                    12 -> paintKit = field.value.toInt()
                    else -> { /* unknown field, skip */ }
                }
                is ProtoReader.Field.Fixed32 -> {
                    val bb = ByteBuffer.wrap(field.value).order(ByteOrder.LITTLE_ENDIAN)
                    val f = bb.float
                    when (field.num) {
                        3 -> wear = f
                        4 -> scale = f
                        5 -> rotation = f
                        7 -> offsetX = f
                        8 -> offsetY = f
                        9 -> offsetZ = f
                        else -> { /* unknown field, skip */ }
                    }
                }
                is ProtoReader.Field.LenDelim -> { /* unknown field, skip */ }
            }
        }

        return Sticker(
            slot = slot,
            stickerId = stickerId,
            wear = wear,
            scale = scale,
            rotation = rotation,
            tintId = tintId,
            offsetX = offsetX,
            offsetY = offsetY,
            offsetZ = offsetZ,
            pattern = pattern,
            highlightReel = highlightReel,
            paintKit = paintKit,
        )
    }

    // ------------------------------------------------------------------
    // Private helpers: ItemPreviewData encode/decode
    // ------------------------------------------------------------------

    private fun encodeItem(item: ItemPreviewData): ByteArray {
        val w = ProtoWriter()
        w.writeUint32(1, item.accountId)
        w.writeUint64(2, item.itemId)
        w.writeUint32(3, item.defIndex)
        w.writeUint32(4, item.paintIndex)
        w.writeUint32(5, item.rarity)
        w.writeUint32(6, item.quality)

        // paintWear: float32 reinterpreted as uint32 varint (only written if non-null)
        item.paintWear?.let { pw ->
            val pwUint32 = float32ToUint32(pw)
            // Use writeUint32 but must handle the uint32 value correctly (could be large)
            if (pwUint32 != 0) {
                val fieldTag = (7 shl 3) or ProtoWriter.WIRE_VARINT
                // Write tag + varint manually via writeUint32 which handles unsigned correctly
                w.writeUint32(7, pwUint32)
            }
            // If pwUint32 == 0 (float 0.0), proto3 skips it — but paintWear=0.0 is a valid wear
            // value. However, since proto3 skips zeros by default and float32 bits of 0.0f are 0,
            // this matches behavior across all other language implementations.
        }

        w.writeUint32(8, item.paintSeed)
        w.writeUint32(9, item.killEaterScoreType)
        w.writeUint32(10, item.killEaterValue)
        w.writeString(11, item.customName)

        for (sticker in item.stickers) {
            w.writeRawBytes(12, encodeSticker(sticker))
        }

        w.writeUint32(13, item.inventory)
        w.writeUint32(14, item.origin)
        w.writeUint32(15, item.questId)
        w.writeUint32(16, item.dropReason)
        w.writeUint32(17, item.musicIndex)
        w.writeInt32(18, item.entIndex)
        w.writeUint32(19, item.petIndex)

        for (kc in item.keychains) {
            w.writeRawBytes(20, encodeSticker(kc))
        }

        return w.toBytes()
    }

    private fun decodeItem(data: ByteArray): ItemPreviewData {
        val reader = ProtoReader(data)
        var accountId = 0
        var itemId = 0L
        var defIndex = 0
        var paintIndex = 0
        var rarity = 0
        var quality = 0
        var paintWear: Float? = null
        var paintSeed = 0
        var killEaterScoreType = 0
        var killEaterValue = 0
        var customName = ""
        val stickers = mutableListOf<Sticker>()
        var inventory = 0
        var origin = 0
        var questId = 0
        var dropReason = 0
        var musicIndex = 0
        var entIndex = 0
        var petIndex = 0
        val keychains = mutableListOf<Sticker>()

        for (field in reader.readAllFields()) {
            when (field) {
                is ProtoReader.Field.Varint -> when (field.num) {
                    1 -> accountId = field.value.toInt()
                    2 -> itemId = field.value
                    3 -> defIndex = field.value.toInt()
                    4 -> paintIndex = field.value.toInt()
                    5 -> rarity = field.value.toInt()
                    6 -> quality = field.value.toInt()
                    7 -> paintWear = uint32ToFloat32(field.value)
                    8 -> paintSeed = field.value.toInt()
                    9 -> killEaterScoreType = field.value.toInt()
                    10 -> killEaterValue = field.value.toInt()
                    13 -> inventory = field.value.toInt()
                    14 -> origin = field.value.toInt()
                    15 -> questId = field.value.toInt()
                    16 -> dropReason = field.value.toInt()
                    17 -> musicIndex = field.value.toInt()
                    18 -> entIndex = field.value.toInt()
                    19 -> petIndex = field.value.toInt()
                    else -> { /* unknown varint field, skip */ }
                }
                is ProtoReader.Field.LenDelim -> when (field.num) {
                    11 -> customName = field.value.toString(Charsets.UTF_8)
                    12 -> stickers += decodeSticker(field.value)
                    20 -> keychains += decodeSticker(field.value)
                    else -> { /* unknown len-delim field, skip */ }
                }
                is ProtoReader.Field.Fixed32 -> { /* unexpected at top level, skip */ }
            }
        }

        return ItemPreviewData(
            accountId = accountId,
            itemId = itemId,
            defIndex = defIndex,
            paintIndex = paintIndex,
            rarity = rarity,
            quality = quality,
            paintWear = paintWear,
            paintSeed = paintSeed,
            killEaterScoreType = killEaterScoreType,
            killEaterValue = killEaterValue,
            customName = customName,
            stickers = stickers,
            inventory = inventory,
            origin = origin,
            questId = questId,
            dropReason = dropReason,
            musicIndex = musicIndex,
            entIndex = entIndex,
            petIndex = petIndex,
            keychains = keychains,
        )
    }
}
