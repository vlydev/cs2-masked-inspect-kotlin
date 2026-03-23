package io.github.vlydev.cs2inspect

import io.github.vlydev.cs2inspect.models.ItemPreviewData
import io.github.vlydev.cs2inspect.models.Sticker
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GenCodeTest {

    @Test
    fun testToGenCodeBasic() {
        val item = ItemPreviewData(defIndex = 7, paintIndex = 474, paintSeed = 306, paintWear = 0.22540508f)
        assertEquals("!gen 7 474 306 0.22540508", GenCode.toGenCode(item))
    }

    @Test
    fun testToGenCodeCustomPrefix() {
        val item = ItemPreviewData(defIndex = 7, paintIndex = 474, paintSeed = 306, paintWear = 0.22540508f)
        assertEquals("!g 7 474 306 0.22540508", GenCode.toGenCode(item, "!g"))
    }

    @Test
    fun testToGenCodeWithStickerAndKeychain() {
        val item = ItemPreviewData(
            defIndex = 7, paintIndex = 941, paintSeed = 2, paintWear = 0.22540508f,
            stickers = listOf(Sticker(slot = 2, stickerId = 7203, wear = 0f)),
            keychains = listOf(Sticker(slot = 0, stickerId = 36, wear = 0f)),
        )
        assertEquals("!g 7 941 2 0.22540508 0 0 0 0 7203 0 0 0 0 0 36 0", GenCode.toGenCode(item, "!g"))
    }

    @Test
    fun testToGenCodeZeroWear() {
        val item = ItemPreviewData(defIndex = 7, paintIndex = 474, paintSeed = 306, paintWear = 0f)
        assertEquals("!gen 7 474 306 0", GenCode.toGenCode(item))
    }

    @Test
    fun testToGenCodeKeychainWithPaintKitAppendsPaintKit() {
        val item = ItemPreviewData(
            defIndex = 1355, paintIndex = 0, paintSeed = 0, paintWear = 0f,
            keychains = listOf(Sticker(slot = 0, stickerId = 37, wear = 0f, paintKit = 929)),
        )
        val code = GenCode.toGenCode(item, "")
        val tokens = code.split(" ")
        val n = tokens.size
        assertEquals("37",  tokens[n - 3])
        assertEquals("0",   tokens[n - 2])
        assertEquals("929", tokens[n - 1])
    }

    @Test
    fun testToGenCodeKeychainWithoutPaintKitNoExtraToken() {
        val item = ItemPreviewData(
            defIndex = 7, paintIndex = 0, paintSeed = 0, paintWear = 0f,
            keychains = listOf(Sticker(slot = 0, stickerId = 36, wear = 0f)),
        )
        val code = GenCode.toGenCode(item, "")
        val tokens = code.split(" ")
        val n = tokens.size
        assertEquals("36", tokens[n - 2])
        assertEquals("0",  tokens[n - 1])
    }

    @Test
    fun testGenCodeFromLinkSlabUrlEndsWithPaintKit() {
        val slabUrl = "steam://run/730//+csgo_econ_action_preview%20819181994A8BA181A982B189E981F181238086898191A4E1208698F309C9"
        val code = GenCode.genCodeFromLink(slabUrl, "")
        val tokens = code.split(" ")
        val n = tokens.size
        assertEquals("37",  tokens[n - 3])
        assertEquals("0",   tokens[n - 2])
        assertEquals("929", tokens[n - 1])
    }

    @Test
    fun testParseGenCodeBasic() {
        val item = GenCode.parseGenCode("!gen 7 474 306 0.22540508")
        assertEquals(7, item.defIndex)
        assertEquals(474, item.paintIndex)
        assertEquals(306, item.paintSeed)
        assertNotNull(item.paintWear)
        assertTrue(abs(item.paintWear!! - 0.22540508f) < 1e-5f)
    }

    @Test
    fun testParseGenCodeWithStickerAndKeychain() {
        val item = GenCode.parseGenCode("!g 7 941 2 0.22540508 0 0 0 0 7203 0 0 0 0 0 36 0")
        assertEquals(1, item.stickers.size)
        assertEquals(7203, item.stickers[0].stickerId)
        assertEquals(1, item.keychains.size)
        assertEquals(36, item.keychains[0].stickerId)
    }

    @Test
    fun testGenCodeFromLinkFromHex() {
        val url = GenCode.generate(7, 474, 306, 0.22540508f)
        val hex = url.removePrefix(GenCode.INSPECT_BASE)
        val code = GenCode.genCodeFromLink(hex)
        assertTrue(code.startsWith("!gen 7 474 306"), "got: $code")
    }

    @Test
    fun testGenCodeFromLinkFromFullUrl() {
        val url = GenCode.generate(7, 474, 306, 0.22540508f)
        val code = GenCode.genCodeFromLink(url)
        assertTrue(code.startsWith("!gen 7 474 306"), "got: $code")
    }

    @Test
    fun testGenerateRoundtrip() {
        val url = GenCode.generate(7, 474, 306, 0.22540508f)
        assertTrue(url.startsWith(GenCode.INSPECT_BASE))
        val hex = url.removePrefix(GenCode.INSPECT_BASE)
        val item = InspectLink.deserialize(hex)
        assertEquals(7, item.defIndex)
        assertEquals(474, item.paintIndex)
        assertEquals(306, item.paintSeed)
    }
}
