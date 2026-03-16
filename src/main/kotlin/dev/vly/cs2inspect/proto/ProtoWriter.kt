package dev.vly.cs2inspect.proto

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin protobuf binary writer.
 *
 * Writes to an in-memory buffer; call [toBytes] to retrieve the result.
 * Fields with zero/default values are omitted (proto3 semantics).
 *
 * Wire types:
 *   0 = varint
 *   2 = length-delimited (string, bytes, nested messages)
 *   5 = 32-bit fixed (float32 LE)
 */
class ProtoWriter {

    private val buf = ByteArrayOutputStream()

    fun toBytes(): ByteArray = buf.toByteArray()

    // ------------------------------------------------------------------
    // Low-level primitives
    // ------------------------------------------------------------------

    private fun writeVarint(value: Long) {
        var v = value
        do {
            var b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v != 0L) b = b or 0x80
            buf.write(b)
        } while (v != 0L)
    }

    private fun writeVarint(value: Int) {
        // Treat as unsigned 32-bit; for negative int32 proto3 encodes as 64-bit
        if (value >= 0) {
            writeVarint(value.toLong())
        } else {
            // Sign-extend to 64-bit (two's complement)
            writeVarint(value.toLong() and 0xFFFFFFFFFFFFFFFFL.toLong())
        }
    }

    private fun writeTag(fieldNum: Int, wireType: Int) {
        writeVarint((fieldNum shl 3) or wireType)
    }

    // ------------------------------------------------------------------
    // Public field writers
    // ------------------------------------------------------------------

    /** Writes a uint32 varint field; skipped when value == 0. */
    fun writeUint32(fieldNum: Int, value: Int) {
        if (value == 0) return
        writeTag(fieldNum, WIRE_VARINT)
        writeVarint(value.toLong() and 0xFFFFFFFFL)
    }

    /** Writes a uint64 varint field; skipped when value == 0. */
    fun writeUint64(fieldNum: Int, value: Long) {
        if (value == 0L) return
        writeTag(fieldNum, WIRE_VARINT)
        writeVarint(value)
    }

    /** Writes a signed int32 varint field; skipped when value == 0. */
    fun writeInt32(fieldNum: Int, value: Int) {
        if (value == 0) return
        writeTag(fieldNum, WIRE_VARINT)
        writeVarint(value)
    }

    /** Writes a length-delimited string field; skipped when empty. */
    fun writeString(fieldNum: Int, value: String) {
        if (value.isEmpty()) return
        val encoded = value.toByteArray(Charsets.UTF_8)
        writeTag(fieldNum, WIRE_LEN)
        writeVarint(encoded.size.toLong())
        buf.write(encoded)
    }

    /**
     * Writes a float32 as wire type 5 (fixed 32-bit little-endian).
     * Used for sticker float fields (wear, scale, rotation, etc.).
     */
    fun writeFloat32Fixed(fieldNum: Int, value: Float) {
        writeTag(fieldNum, WIRE_FIXED32)
        val bits = java.lang.Float.floatToRawIntBits(value)
        val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(bits)
        buf.write(bb.array())
    }

    /**
     * Writes raw bytes as a length-delimited field (wire type 2).
     * Used for nested messages (stickers, keychains).
     */
    fun writeRawBytes(fieldNum: Int, data: ByteArray) {
        if (data.isEmpty()) return
        writeTag(fieldNum, WIRE_LEN)
        writeVarint(data.size.toLong())
        buf.write(data)
    }

    companion object {
        const val WIRE_VARINT = 0
        const val WIRE_LEN = 2
        const val WIRE_FIXED32 = 5
    }
}
