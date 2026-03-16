package io.github.vlydev.cs2inspect.proto

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin protobuf binary reader.
 *
 * Implements the subset of wire types needed for CEconItemPreviewDataBlock:
 *   - Wire type 0: varint (uint32, uint64, int32)
 *   - Wire type 2: length-delimited (string, bytes, nested messages)
 *   - Wire type 5: 32-bit fixed (float32 LE)
 */
class ProtoReader(private val data: ByteArray) {

    private var pos = 0

    val remaining: Int get() = data.size - pos

    // ------------------------------------------------------------------
    // Low-level primitives
    // ------------------------------------------------------------------

    private fun readByte(): Int {
        if (pos >= data.size) throw IllegalArgumentException("Unexpected end of protobuf data at position $pos")
        return data[pos++].toInt() and 0xFF
    }

    private fun readBytes(n: Int): ByteArray {
        if (pos + n > data.size) {
            throw IllegalArgumentException("Need $n bytes but only ${data.size - pos} remain at position $pos")
        }
        val chunk = data.copyOfRange(pos, pos + n)
        pos += n
        return chunk
    }

    /**
     * Reads a base-128 varint; returns as Long to handle uint64 fields safely.
     * Handles up to 64 bits (10 bytes of varint).
     */
    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        do {
            val b = readByte()
            result = result or ((b.toLong() and 0x7F) shl shift)
            shift += 7
            if (shift > 70) throw IllegalArgumentException("Varint too long (> 10 bytes)")
            if (b and 0x80 == 0) break
        } while (true)
        return result
    }

    /**
     * Reads a proto tag; returns Pair(fieldNumber, wireType).
     */
    fun readTag(): Pair<Int, Int> {
        val tag = readVarint()
        return Pair((tag ushr 3).toInt(), (tag and 0x07).toInt())
    }

    /**
     * Reads a 4-byte little-endian float32 (wire type 5).
     */
    fun readFloat32Fixed(): Float {
        val raw = readBytes(4)
        val bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        return bb.float
    }

    fun readLengthDelimited(): ByteArray {
        val length = readVarint().toInt()
        return readBytes(length)
    }

    // ------------------------------------------------------------------
    // Field iterator
    // ------------------------------------------------------------------

    sealed class Field {
        data class Varint(val num: Int, val value: Long) : Field()
        data class LenDelim(val num: Int, val value: ByteArray) : Field()
        data class Fixed32(val num: Int, val value: ByteArray) : Field()
    }

    /**
     * Reads and returns all fields until EOF.
     * Enforces a max of 100 fields to prevent degenerate inputs.
     */
    fun readAllFields(): List<Field> {
        val fields = mutableListOf<Field>()
        var fieldCount = 0

        while (remaining > 0) {
            if (++fieldCount > 100) {
                throw IllegalArgumentException("Protobuf field count exceeds limit of 100")
            }
            val (fieldNum, wireType) = readTag()
            val field: Field = when (wireType) {
                ProtoWriter.WIRE_VARINT -> Field.Varint(fieldNum, readVarint())
                ProtoWriter.WIRE_LEN -> Field.LenDelim(fieldNum, readLengthDelimited())
                ProtoWriter.WIRE_FIXED32 -> Field.Fixed32(fieldNum, readBytes(4))
                1 -> { readBytes(8); continue } // wire type 1 = 64-bit fixed, skip
                3, 4 -> continue // start/end group, skip
                else -> throw IllegalArgumentException("Unknown wire type $wireType for field $fieldNum")
            }
            fields += field
        }

        return fields
    }
}
