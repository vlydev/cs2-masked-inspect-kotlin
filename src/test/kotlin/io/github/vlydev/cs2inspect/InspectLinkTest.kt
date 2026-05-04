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

        /** Sticker slab A — defIndex=1355, rarity=5, quality=8, keychains[0].stickerId=37, paintKit=7256 */
        const val SLAB_A = "918191895A9BB191B994A199F991E191339096999181B4F149A98D5C0889"

        /** Sticker slab B — defIndex=1355, rarity=3, quality=8, keychains[0].stickerId=37, paintKit=275 */
        const val SLAB_B = "CBDBCBD300C1EBCBE3C8FBC3A3CBBBCB69CACCC3CBDBEEAB58C9B8B67C83"

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

    // -----------------------------------------------------------------------
    // Sticker slab test vectors (paintKit field)
    // -----------------------------------------------------------------------

    @Test
    fun testSlabADefIndex() {
        assertEquals(1355, InspectLink.deserialize(SLAB_A).defIndex)
    }

    @Test
    fun testSlabARarity() {
        assertEquals(5, InspectLink.deserialize(SLAB_A).rarity)
    }

    @Test
    fun testSlabAQuality() {
        assertEquals(8, InspectLink.deserialize(SLAB_A).quality)
    }

    @Test
    fun testSlabAKeychainStickerId() {
        val keychains = InspectLink.deserialize(SLAB_A).keychains
        assertEquals(1, keychains.size)
        assertEquals(37, keychains[0].stickerId)
    }

    @Test
    fun testSlabAKeychainPaintKit() {
        val keychains = InspectLink.deserialize(SLAB_A).keychains
        assertEquals(7256, keychains[0].paintKit)
    }

    @Test
    fun testSlabBDefIndex() {
        assertEquals(1355, InspectLink.deserialize(SLAB_B).defIndex)
    }

    @Test
    fun testSlabBRarity() {
        assertEquals(3, InspectLink.deserialize(SLAB_B).rarity)
    }

    @Test
    fun testSlabBQuality() {
        assertEquals(8, InspectLink.deserialize(SLAB_B).quality)
    }

    @Test
    fun testSlabBKeychainStickerId() {
        val keychains = InspectLink.deserialize(SLAB_B).keychains
        assertEquals(1, keychains.size)
        assertEquals(37, keychains[0].stickerId)
    }

    @Test
    fun testSlabBKeychainPaintKit() {
        val keychains = InspectLink.deserialize(SLAB_B).keychains
        assertEquals(275, keychains[0].paintKit)
    }

    @Test
    fun testSlabRoundtripPaintKit() {
        val data = ItemPreviewData(
            defIndex = 1355,
            rarity = 5,
            quality = 8,
            keychains = listOf(Sticker(slot = 0, stickerId = 37, paintKit = 7256))
        )
        val result = roundtrip(data)
        assertEquals(1, result.keychains.size)
        assertEquals(7256, result.keychains[0].paintKit)
    }

    // -----------------------------------------------------------------------
    // Malformed URLs (regression: must reject cleanly with MalformedInspectLinkException)
    // -----------------------------------------------------------------------

    private val malformedUrls = listOf(
        "steam://run/730//+csgo_econ_action_preview%20ADBD1050393912ACB5AC8D45AC85A99DA9956A116D5FAEED21ACCFB4A5AFBD348EB0ADAD2D9280ADADDD6F90EDA37510E84D8BEE11CFB4A5ACBD348EB0ADAD2D9280ADAD5D6F906F2B4C13E84D93D591CFB9A5ADBD419EB0ADAD2D9290ADF22010E8B72FB213CFB4A5ADBD549EB0ADAD2D9280ADADED6C90CFD43F10E892DFE513CFB4A5ADBD549EB0ADAD2D9280ADAD85EE902F952210E82EB8A613C52E2D2D2DA1DDA90FACBBA5ADBD89902923AAECE83",
        "steam://run/730//+csgo_econ_action_preview%20EEFE3144332550EFF6E7CE28E8C6EADEEAD642323218EDAE4DEA8CFAE6ECFE35A7F302BFD6D1D3004FCB50ABAE5CF8528CF7E6EEFE0DA7F3394DDED1C3EEEE7EAFD39E25B3D3AB9EAE70D28CFAE6ECFE32A7F3EEEE6ED1D3595B17D3AB9E8E65538CFAE6ECFE64ADF3EEEE6ED1D3E597F3D3AB2EEA1AD58CF7E6EDFE0BCAF302BFD6D1C3EEEEEF2DD3AEF5F552ABEE31A855866D6E6E6EE29EE64CEFF8E6EEFED8D3B6CBCBACABFD70EED1A31F96E7AFBE5",
        "steam://run/730//+csgo_econ_action_preview%204A5A8EFCB1B9F44B524B6AA24B624E7A4E72BACFF6B8490AD449285E42485AAB75576316457577CA2422760F4A413E712853424A5AA679574A4ACA75674A4A8A8A770A7F85760FD04246F4285342495AB279574A4ACA75674A4A8A0A7799714A750F0A5140F7285342495AAD7277EB4547F40F0A00EB7122C9CACACA463A4EE84B5D424A5A4C776C02A10A0F34A5C17407F0145C0A1AA",
        "steam://run/730//+csgo_econ_action_preview%20FAEA5766387F45FBE2FDDA71F2D2FECAF3C2142C0C0EF9BA7CFFB2FAAAFA98EEF2F8EA3BFBD7FAFA3ABBC7EA2FB7C7BF9ACC47C698E3F2F9EA03C9E7FAFA7AC5D7FAFABA3AC780CA89C4BFFAAAD9C198E3F2F9EA03C9E7FAFA7AC5D7FAFACEB9C7B60177C4BFAAB82AC698EEF2F9EA03C9E7FAFA7AC5C7C11558C4BFFA43DBC198F5F2FBEA13DEC7759A1147BF9A16F64692797A7A7AF68AF258FBEFF2FAEADEC7DEAC32BBBF0BF9BAC4B760382CC5A2D24",
        "steam://run/730//+csgo_econ_action_preview%209F8F4F504C7C219E87B7BF629EB79CAF9BA73F1D53419CDF0699FD8B979F8F5CCF82050686A0A2F4038821DA17F3FD22FD8B979F8F49D4825C6AB7A0A25F50B224DA7F0CF222FD8B979D8F5DCF82F9F9B9A0A2B7B25422DA6F6F1422FD8B979D8F5DCF822781DAA0A247B731A2DA7F92EF23FD86979F8F5DCF827EE5CBA0B29F9FDFDFA285AD4322DA9FFD8AA3F71C1F1F1F93EF873D9E88979F8FDDA243C05EDFDA4CFB44A0D202B77EDFCF3F339C3DF89",
        "steam://run/730//+csgo_econ_action_preview%20FAEA5B24060844FBE2C6DA10F2D2FECAF3C2631A3308F9BA47F8B2FAAAFA98EEF2FEEA29B2E781EED4C5C7776B78C4BFFAFA21CD98EEF2FAEA09BCE7F02DD9C5C7FE6C57C7BF7A58ED4698EEF2FEEA6DBDE79C9CDCC5C7929F2F47BFFA461BC398E3F2FBEA15BDE7E57FD1C5D7FAFA8AB8C7C2CEDC46BF12973EC798EEF2FEEA24B8E781EED4C5C75696FCC5BF2AEBF2C792797A7A7AF68AF258FBEDF2FAEAD2C7B3683FBBBF065F8CC5B763A382BAAA0C5",
        "steam://run/730//+csgo_econ_action_preview%204D5D9DF8C7D2F34C556E6DDC4C654E7D4975B2D2AEBB4E0DAB4B2F59454E5D9A70604D4DBD8C7002A356F308CD4603F62F5445495DEB745011C20F72604D4D798F70797F9FF3089D49ABF12F5945495DEB74502B2BAB737045B3FAF308FD4BE8F12F5945495DF868508081417270BFB9D2F308ADD283F12F5945495DF86850AC375972702FA3CBF3083D2EBFF125CECDCDCD413D5AEF4C5A454D5D567084E4F80C08254C547200CE3E1D0D1DF9D24C63938",
        "steam://run/730//+csgo_econ_action_preview%20CFDF6258412F71CED7C8EF5CC6E7C9FFCBF7465B3B38CC8F7ACEADD6C7CEDF3BF2D2F2C5D8F0E2CFCFE40CF27F72E1728AF786F5F2ADC0C7CCDF3BF2F241D88EF18A0F03F6F2ADDBC7CCDF3CF2E2CFCF0F8DF27B3FB7F18A6F5ACDF2ADD6C7CCDF3CF2D2C518ECF0E2CFCF8F0EF2D5C29AF18ACFCFD8F5ADDBC7CFDF3CF2E2CFCF6D8DF24DB0DA718A2FA6DBF3A74C4F4F4FC3BFC76DCED8C7CFDF87F27B950C8E8A37D0A3F182A2B8A48F9F4332CD2C2B6",
        "steam://run/730//+csgo_econ_action_preview%20CEDE51082D1C70CFD6CFEE54C6E6CAFEC7F631274538CD8E2DC886CE9ECEACDAC6CDDE0C8DD3CECE4EF1F382996B738B4E5E8D75ACD7C6CEDE0C8DD3CECE4EF1E3CECE8E0FF3A56603708BECDBE170ACD7C6CEDE0C8DD3CECE4EF1E3CECEDE0FF37682A5708B0650FB70ACD7C6CEDE0C8DD3CECE4EF1E3CECEDE0FF34E3CEC738BDAC6F870ACD7C6CDDE798AD3CECE4EF1E3CECE0E0EF3333BC5F18BAE8E9FF2A64D4E4E4EC2BECA6CCFD9C6CEDECFF37C6",
        "ABC",
        "",
        "ZZZZZZZZZZZZ"
    )

    @Test
    fun testMalformedUrlsAllRejected() {
        for ((idx, url) in malformedUrls.withIndex()) {
            assertFailsWith<MalformedInspectLinkException>("URL #$idx should be rejected: ${url.take(80)}") {
                InspectLink.deserialize(url)
            }
        }
    }

    @Test
    fun testMalformedInspectLinkExceptionExtendsIllegalArgumentException() {
        assertTrue(IllegalArgumentException::class.java.isAssignableFrom(MalformedInspectLinkException::class.java))
    }

    @Test
    fun testMalformedOddHexMessageMentionsLength() {
        val ex = assertFailsWith<MalformedInspectLinkException> { InspectLink.deserialize("ABC") }
        assertTrue(ex.message!!.contains("Malformed"))
        assertTrue(Regex("(?i)length|even|hex").containsMatchIn(ex.message!!))
    }
}
