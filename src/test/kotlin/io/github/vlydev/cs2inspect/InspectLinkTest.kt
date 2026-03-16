package io.github.vlydev.cs2inspect

import io.github.vlydev.cs2inspect.models.ItemPreviewData
import io.github.vlydev.cs2inspect.models.Sticker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InspectLinkTest {

    // -----------------------------------------------------------------------
    // Known test vectors
    // -----------------------------------------------------------------------

    companion object {
        /** A real CS2 item encoded with XOR key 0xE3. */
        const val NATIVE_HEX =
            "E3F3367440334DE2FBE4C345E0CBE0D3E7DB6943400AE0A379E481ECEBE2F36F" +
            "D9DE2BDB515EA6E30D74D981ECEBE3F37BCBDE640D475DA6E35EFCD881ECEBE3" +
            "F359D5DE37E9D75DA6436DD3DD81ECEBE3F366DCDE3F8F9BDDA69B43B6DE81EC" +
            "EBE3F33BC8DEBB1CA3DFA623F7DDDF8B71E293EBFD43382B"
        // itemId=46876117973, defIndex=7, paintIndex=422, paintSeed=922, paintWear≈0.04121
        // rarity=3, quality=4, stickers: [7436, 5144, 6970, 8069, 5592]

        /** A tool-generated link with key 0x00. */
        const val TOOL_HEX = "00183C20B803280538E9A3C5DD0340E102C246A0D1"

        /** CSFloat vector A — no stickers, paintWear ≈ 0.6337 */
        const val CSFLOAT_A = "00180720DA03280638FBEE88F90340B2026BC03C96"

        /** CSFloat vector B — 4 stickers id=76 each, paintWear ≈ 0.99 */
        const val CSFLOAT_B =
            "00180720C80A280638A4E1F5FB03409A0562040800104C62040801104C62040802104C62040803104C6D4F5E30"

        /** CSFloat vector C — keychain item (defIndex=1355), highlightReel=345 */
        const val CSFLOAT_C = "A2B2A2BA69A882A28AA192AECAA2D2B700A3A5AAA2B286FA7BA0D684BE72"

        private const val HYBRID_URL =
            "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%20" +
            "S76561199323320483A50075495125D1101C4C4FCD4AB10092D31B8143914211829A1FAE3FD125119591141117308191301EA550C1111912E3C111151D12C413E6BAC54D1D29BAD731E191501B92C2C9B6BF92F5411C25B2A731E191501B92C2CEA2B182E5411F7212A731E191501B92C2C4F89C12F549164592A799713611956F4339F"

        private const val CLASSIC_URL =
            "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%20" +
            "S76561199842063946A49749521570D2751293026650298712"

        // Payloads starting with hex 'A' (key byte = 0xA4 or 0xA6)
        const val A_START_HEX_1 = "A6B6710C51510DA7BE848628A18EA396A29E1C181D56A5E682CEE8D6AEE7BC380F"
        const val A_START_HEX_2 = "A4B4725C7B1EE6BC608084A48CA294A0CC9CD4AD3EDF347E"
        const val A_START_HEX_3 = "A6B617190F659DBE47AC86A68EA096A2CEB4D6AFBFFA9FD2"
    }

    // -----------------------------------------------------------------------
    // Deserialize tests — NATIVE_HEX (XOR key 0xE3)
    // -----------------------------------------------------------------------

    @Test
    fun testNativeXorKeyItemId() {
        assertEquals(46876117973L, InspectLink.deserialize(NATIVE_HEX).itemId)
    }

    @Test
    fun testNativeXorKeyDefIndex() {
        assertEquals(7, InspectLink.deserialize(NATIVE_HEX).defIndex) // AK-47
    }

    @Test
    fun testNativeXorKeyPaintIndex() {
        assertEquals(422, InspectLink.deserialize(NATIVE_HEX).paintIndex)
    }

    @Test
    fun testNativeXorKeyPaintSeed() {
        assertEquals(922, InspectLink.deserialize(NATIVE_HEX).paintSeed)
    }

    @Test
    fun testNativeXorKeyPaintWear() {
        val item = InspectLink.deserialize(NATIVE_HEX)
        assertNotNull(item.paintWear)
        assertTrue(
            Math.abs(item.paintWear - 0.04121f) < 0.0001f,
            "Expected paintWear ≈ 0.04121, got ${item.paintWear}"
        )
    }

    @Test
    fun testNativeXorKeyRarity() {
        assertEquals(3, InspectLink.deserialize(NATIVE_HEX).rarity)
    }

    @Test
    fun testNativeXorKeyQuality() {
        assertEquals(4, InspectLink.deserialize(NATIVE_HEX).quality)
    }

    @Test
    fun testNativeStickerCount() {
        assertEquals(5, InspectLink.deserialize(NATIVE_HEX).stickers.size)
    }

    @Test
    fun testNativeStickerIds() {
        val stickers = InspectLink.deserialize(NATIVE_HEX).stickers
        assertEquals(listOf(7436, 5144, 6970, 8069, 5592), stickers.map { it.stickerId })
    }

    // -----------------------------------------------------------------------
    // Deserialize tests — TOOL_HEX (key 0x00)
    // -----------------------------------------------------------------------

    @Test
    fun testToolHexKeyZeroDefIndex() {
        assertEquals(60, InspectLink.deserialize(TOOL_HEX).defIndex)
    }

    @Test
    fun testToolHexKeyZeroPaintIndex() {
        assertEquals(440, InspectLink.deserialize(TOOL_HEX).paintIndex)
    }

    @Test
    fun testToolHexKeyZeroPaintSeed() {
        assertEquals(353, InspectLink.deserialize(TOOL_HEX).paintSeed)
    }

    @Test
    fun testToolHexKeyZeroPaintWear() {
        val item = InspectLink.deserialize(TOOL_HEX)
        assertNotNull(item.paintWear)
        assertTrue(
            Math.abs(item.paintWear - 0.005411375779658556f) < 1e-7f,
            "Expected paintWear ≈ 0.005411, got ${item.paintWear}"
        )
    }

    @Test
    fun testToolHexKeyZeroRarity() {
        assertEquals(5, InspectLink.deserialize(TOOL_HEX).rarity)
    }

    @Test
    fun testLowercaseHex() {
        assertEquals(60, InspectLink.deserialize(TOOL_HEX.lowercase()).defIndex)
    }

    @Test
    fun testAcceptsSteamUrl() {
        val url = "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%20A$TOOL_HEX"
        assertEquals(60, InspectLink.deserialize(url).defIndex)
    }

    @Test
    fun testAcceptsCsgoStyleUrl() {
        val url = "csgo://rungame/730/76561202255233023/+csgo_econ_action_preview A$TOOL_HEX"
        assertEquals(60, InspectLink.deserialize(url).defIndex)
    }

    @Test
    fun testPayloadTooShortThrows() {
        assertFailsWith<IllegalArgumentException> {
            InspectLink.deserialize("0000")
        }
    }

    // -----------------------------------------------------------------------
    // Serialize tests
    // -----------------------------------------------------------------------

    @Test
    fun testKnownHexOutput() {
        val data = ItemPreviewData(
            defIndex = 60,
            paintIndex = 440,
            paintSeed = 353,
            paintWear = 0.005411375779658556f,
            rarity = 5,
        )
        assertEquals(TOOL_HEX, InspectLink.serialize(data))
    }

    @Test
    fun testReturnsUppercase() {
        val data = ItemPreviewData(defIndex = 1)
        val result = InspectLink.serialize(data)
        assertEquals(result.uppercase(), result)
    }

    @Test
    fun testStartsWithDoubleZero() {
        val data = ItemPreviewData(defIndex = 1)
        assertTrue(InspectLink.serialize(data).startsWith("00"))
    }

    // -----------------------------------------------------------------------
    // Round-trip tests
    // -----------------------------------------------------------------------

    private fun roundtrip(data: ItemPreviewData): ItemPreviewData =
        InspectLink.deserialize(InspectLink.serialize(data))

    @Test
    fun testRoundtripDefIndex() {
        assertEquals(7, roundtrip(ItemPreviewData(defIndex = 7)).defIndex)
    }

    @Test
    fun testRoundtripPaintIndex() {
        assertEquals(422, roundtrip(ItemPreviewData(paintIndex = 422)).paintIndex)
    }

    @Test
    fun testRoundtripPaintSeed() {
        assertEquals(999, roundtrip(ItemPreviewData(paintSeed = 999)).paintSeed)
    }

    @Test
    fun testRoundtripPaintWear() {
        // float32 round-trip precision
        val original = 0.123456789f
        val result = roundtrip(ItemPreviewData(paintWear = original))
        assertNotNull(result.paintWear)
        assertTrue(
            Math.abs(result.paintWear - original) < 1e-7f,
            "Expected ≈ $original, got ${result.paintWear}"
        )
    }

    @Test
    fun testRoundtripItemIdLarge() {
        assertEquals(46876117973L, roundtrip(ItemPreviewData(itemId = 46876117973L)).itemId)
    }

    @Test
    fun testRoundtripStickers() {
        val data = ItemPreviewData(
            defIndex = 7,
            stickers = listOf(
                Sticker(slot = 0, stickerId = 7436),
                Sticker(slot = 1, stickerId = 5144),
            )
        )
        val result = roundtrip(data)
        assertEquals(2, result.stickers.size)
        assertEquals(7436, result.stickers[0].stickerId)
        assertEquals(5144, result.stickers[1].stickerId)
    }

    @Test
    fun testRoundtripStickerSlots() {
        val data = ItemPreviewData(stickers = listOf(Sticker(slot = 3, stickerId = 123)))
        assertEquals(3, roundtrip(data).stickers[0].slot)
    }

    @Test
    fun testRoundtripStickerWear() {
        val data = ItemPreviewData(stickers = listOf(Sticker(stickerId = 1, wear = 0.5f)))
        val result = roundtrip(data)
        assertNotNull(result.stickers[0].wear)
        assertTrue(Math.abs(result.stickers[0].wear!! - 0.5f) < 1e-6f)
    }

    @Test
    fun testRoundtripKeychains() {
        val data = ItemPreviewData(keychains = listOf(Sticker(slot = 0, stickerId = 999, pattern = 42)))
        val result = roundtrip(data)
        assertEquals(1, result.keychains.size)
        assertEquals(999, result.keychains[0].stickerId)
        assertEquals(42, result.keychains[0].pattern)
    }

    @Test
    fun testRoundtripCustomName() {
        val data = ItemPreviewData(defIndex = 7, customName = "My Knife")
        assertEquals("My Knife", roundtrip(data).customName)
    }

    @Test
    fun testRoundtripRarityQuality() {
        val data = ItemPreviewData(rarity = 6, quality = 9)
        val result = roundtrip(data)
        assertEquals(6, result.rarity)
        assertEquals(9, result.quality)
    }

    @Test
    fun testRoundtripFullItem() {
        val data = ItemPreviewData(
            itemId = 46876117973L,
            defIndex = 7,
            paintIndex = 422,
            rarity = 3,
            quality = 4,
            paintWear = 0.04121f,
            paintSeed = 922,
            stickers = listOf(
                Sticker(slot = 0, stickerId = 7436),
                Sticker(slot = 1, stickerId = 5144),
                Sticker(slot = 2, stickerId = 6970),
                Sticker(slot = 3, stickerId = 8069),
                Sticker(slot = 4, stickerId = 5592),
            )
        )
        val result = roundtrip(data)
        assertEquals(7, result.defIndex)
        assertEquals(422, result.paintIndex)
        assertEquals(922, result.paintSeed)
        assertEquals(5, result.stickers.size)
        assertEquals(listOf(7436, 5144, 6970, 8069, 5592), result.stickers.map { it.stickerId })
    }

    // -----------------------------------------------------------------------
    // Hybrid URL format tests
    // -----------------------------------------------------------------------

    @Test
    fun testIsMaskedReturnsTrueForPureHexPayload() {
        val url = "steam://run/730//+csgo_econ_action_preview%20$TOOL_HEX"
        assertTrue(InspectLink.isMasked(url))
    }

    @Test
    fun testIsMaskedReturnsTrueForFullMaskedUrl() {
        val url = "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%20$NATIVE_HEX"
        assertTrue(InspectLink.isMasked(url))
    }

    @Test
    fun testIsMaskedReturnsTrueForHybridUrl() {
        assertTrue(InspectLink.isMasked(HYBRID_URL))
    }

    @Test
    fun testIsMaskedReturnsFalseForClassicUrl() {
        assertTrue(!InspectLink.isMasked(CLASSIC_URL))
    }

    @Test
    fun testIsClassicReturnsTrueForClassicUrl() {
        assertTrue(InspectLink.isClassic(CLASSIC_URL))
    }

    @Test
    fun testIsClassicReturnsFalseForMaskedUrl() {
        val url = "steam://run/730//+csgo_econ_action_preview%20$TOOL_HEX"
        assertTrue(!InspectLink.isClassic(url))
    }

    @Test
    fun testIsClassicReturnsFalseForHybridUrl() {
        assertTrue(!InspectLink.isClassic(HYBRID_URL))
    }

    @Test
    fun testDeserializeHybridUrlReturnsCorrectItemId() {
        val item = InspectLink.deserialize(HYBRID_URL)
        assertEquals(50075495125L, item.itemId)
    }

    // -----------------------------------------------------------------------
    // Regression: hex payload starting with 'A' (key byte = 0xA4 or 0xA6)
    // -----------------------------------------------------------------------

    @Test
    fun testIsMaskedReturnsTrueForAStartHex1() {
        val url = "steam://run/730//+csgo_econ_action_preview%20$A_START_HEX_1"
        assertTrue(InspectLink.isMasked(url))
    }

    @Test
    fun testIsMaskedReturnsTrueForAStartHex2() {
        val url = "steam://run/730//+csgo_econ_action_preview%20$A_START_HEX_2"
        assertTrue(InspectLink.isMasked(url))
    }

    @Test
    fun testIsMaskedReturnsTrueForAStartHex3() {
        val url = "steam://run/730//+csgo_econ_action_preview%20$A_START_HEX_3"
        assertTrue(InspectLink.isMasked(url))
    }

    @Test
    fun testDeserializeAStartBareHex1() {
        assertEquals(34, InspectLink.deserialize(A_START_HEX_1).defIndex)
    }

    @Test
    fun testDeserializeAStartBareHex2() {
        assertEquals(4676, InspectLink.deserialize(A_START_HEX_2).defIndex)
    }

    @Test
    fun testDeserializeAStartBareHex3() {
        assertEquals(1377, InspectLink.deserialize(A_START_HEX_3).defIndex)
    }

    @Test
    fun testDeserializeAStartSteamRunUrl1() {
        val url = "steam://run/730//+csgo_econ_action_preview%20$A_START_HEX_1"
        assertEquals(34, InspectLink.deserialize(url).defIndex)
    }

    @Test
    fun testDeserializeAStartSteamRunUrl2() {
        val url = "steam://run/730//+csgo_econ_action_preview%20$A_START_HEX_2"
        assertEquals(4676, InspectLink.deserialize(url).defIndex)
    }

    @Test
    fun testDeserializeAStartSteamRunUrl3() {
        val url = "steam://run/730//+csgo_econ_action_preview%20$A_START_HEX_3"
        assertEquals(1377, InspectLink.deserialize(url).defIndex)
    }

    @Test
    fun testDeserializeAStartSteamRungameUrl1() {
        val url = "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%20$A_START_HEX_1"
        assertEquals(34, InspectLink.deserialize(url).defIndex)
    }

    @Test
    fun testDeserializeAStartSteamRungameUrl2() {
        val url = "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%20$A_START_HEX_2"
        assertEquals(4676, InspectLink.deserialize(url).defIndex)
    }

    @Test
    fun testDeserializeAStartSteamRungameUrl3() {
        val url = "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%20$A_START_HEX_3"
        assertEquals(1377, InspectLink.deserialize(url).defIndex)
    }

    // -----------------------------------------------------------------------
    // Validation: payload length, value ranges
    // -----------------------------------------------------------------------

    @Test
    fun testDeserializePayloadTooLong() {
        assertFailsWith<IllegalArgumentException> {
            InspectLink.deserialize("AB".repeat(2049)) // 4098 chars > 4096
        }
    }

    @Test
    fun testSerializePaintWearAboveOne() {
        assertFailsWith<IllegalArgumentException> {
            InspectLink.serialize(ItemPreviewData(paintWear = 1.5f))
        }
    }

    @Test
    fun testSerializePaintWearBelowZero() {
        assertFailsWith<IllegalArgumentException> {
            InspectLink.serialize(ItemPreviewData(paintWear = -0.1f))
        }
    }

    @Test
    fun testSerializePaintWearBoundaryValid() {
        // 0.0 and 1.0 are valid wear values
        assertTrue(InspectLink.serialize(ItemPreviewData(paintWear = 0.0f)).startsWith("00"))
        assertTrue(InspectLink.serialize(ItemPreviewData(paintWear = 1.0f)).startsWith("00"))
    }

    @Test
    fun testSerializeCustomNameTooLong() {
        assertFailsWith<IllegalArgumentException> {
            InspectLink.serialize(ItemPreviewData(customName = "A".repeat(101)))
        }
    }

    @Test
    fun testSerializeCustomNameMaxLength() {
        // exactly 100 chars is allowed
        val result = InspectLink.serialize(ItemPreviewData(customName = "A".repeat(100)))
        assertTrue(result.startsWith("00"))
    }

    // -----------------------------------------------------------------------
    // CSFloat vectors
    // -----------------------------------------------------------------------

    @Test
    fun testCsfloatADefIndex() {
        assertEquals(7, InspectLink.deserialize(CSFLOAT_A).defIndex)
    }

    @Test
    fun testCsfloatAPaintIndex() {
        assertEquals(474, InspectLink.deserialize(CSFLOAT_A).paintIndex)
    }

    @Test
    fun testCsfloatAPaintSeed() {
        assertEquals(306, InspectLink.deserialize(CSFLOAT_A).paintSeed)
    }

    @Test
    fun testCsfloatARarity() {
        assertEquals(6, InspectLink.deserialize(CSFLOAT_A).rarity)
    }

    @Test
    fun testCsfloatAPaintWear() {
        val item = InspectLink.deserialize(CSFLOAT_A)
        assertNotNull(item.paintWear)
        assertTrue(
            Math.abs(item.paintWear - 0.6337f) < 0.001f,
            "Expected paintWear ≈ 0.6337, got ${item.paintWear}"
        )
    }

    @Test
    fun testCsfloatBStickerCount() {
        assertEquals(4, InspectLink.deserialize(CSFLOAT_B).stickers.size)
    }

    @Test
    fun testCsfloatBStickerIds() {
        val stickers = InspectLink.deserialize(CSFLOAT_B).stickers
        for (s in stickers) {
            assertEquals(76, s.stickerId)
        }
    }

    @Test
    fun testCsfloatBPaintIndex() {
        assertEquals(1352, InspectLink.deserialize(CSFLOAT_B).paintIndex)
    }

    @Test
    fun testCsfloatBPaintWear() {
        val item = InspectLink.deserialize(CSFLOAT_B)
        assertNotNull(item.paintWear)
        assertTrue(
            Math.abs(item.paintWear - 0.99f) < 0.001f,
            "Expected paintWear ≈ 0.99, got ${item.paintWear}"
        )
    }

    @Test
    fun testCsfloatCDefIndex() {
        assertEquals(1355, InspectLink.deserialize(CSFLOAT_C).defIndex)
    }

    @Test
    fun testCsfloatCQuality() {
        assertEquals(12, InspectLink.deserialize(CSFLOAT_C).quality)
    }

    @Test
    fun testCsfloatCKeychainHighlightReel() {
        val keychains = InspectLink.deserialize(CSFLOAT_C).keychains
        assertEquals(1, keychains.size)
        assertEquals(345, keychains[0].highlightReel)
    }

    @Test
    fun testCsfloatCNoPaintWear() {
        // keychain items have no wear — field 7 not present in proto
        assertNull(InspectLink.deserialize(CSFLOAT_C).paintWear)
    }

    // -----------------------------------------------------------------------
    // Round-trip: highlight_reel and nullable paintWear
    // -----------------------------------------------------------------------

    @Test
    fun testRoundtripHighlightReel() {
        val data = ItemPreviewData(
            defIndex = 7,
            keychains = listOf(Sticker(slot = 0, stickerId = 36, highlightReel = 345))
        )
        val result = roundtrip(data)
        assertEquals(1, result.keychains.size)
        assertEquals(345, result.keychains[0].highlightReel)
    }

    @Test
    fun testRoundtripNullPaintWear() {
        val data = ItemPreviewData(defIndex = 7, paintWear = null)
        val result = roundtrip(data)
        assertNull(result.paintWear)
    }

    @Test
    fun testSerializeNullPaintWearProducesFewerBytes() {
        // An item with null paintWear omits field 7 entirely; a non-zero float writes it.
        val withNull = InspectLink.serialize(ItemPreviewData(defIndex = 7, paintWear = null))
        val withFloat = InspectLink.serialize(ItemPreviewData(defIndex = 7, paintWear = 0.5f))
        assertTrue(
            withNull.length < withFloat.length,
            "Expected null paintWear (${ withNull.length }) to produce fewer bytes than 0.5f (${withFloat.length})"
        )
    }
}
