# cs2-masked-inspect

Pure Kotlin library for encoding and decoding CS2 masked inspect links — no dependencies, targets JVM 8+.

Offline encoder/decoder for the binary protobuf format used in CS2 `csgo_econ_action_preview` inspect URLs. Handles both tool-generated links (key byte `0x00`) and native CS2 links (XOR-obfuscated).

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("dev.vly:cs2-masked-inspect:0.1.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'dev.vly:cs2-masked-inspect:0.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>dev.vly</groupId>
    <artifactId>cs2-masked-inspect</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Usage

### Deserialize (decode) an inspect link

```kotlin
import dev.vly.cs2inspect.InspectLink

// From a full Steam URL
val item = InspectLink.deserialize(
    "steam://rungame/730/76561202255233023/+csgo_econ_action_preview%20A00183C20B803280538E9A3C5DD0340E102C246A0D1"
)
println(item.defIndex)   // 60
println(item.paintIndex) // 440
println(item.paintSeed)  // 353
println(item.paintWear)  // 0.005411376

// From a bare hex string (tool-generated, key=0x00)
val item2 = InspectLink.deserialize("00183C20B803280538E9A3C5DD0340E102C246A0D1")

// From a native CS2 hex string (XOR-obfuscated, key!=0x00)
val item3 = InspectLink.deserialize("E3F3367440334DE2FBE4C345E0CBE0D3...")

// Access stickers
for (sticker in item.stickers) {
    println("Slot ${sticker.slot}: ID=${sticker.stickerId}, wear=${sticker.wear}")
}

// Access keychains
for (keychain in item.keychains) {
    println("Keychain ID=${keychain.stickerId}, highlightReel=${keychain.highlightReel}")
}
```

### Serialize (encode) an item

```kotlin
import dev.vly.cs2inspect.InspectLink
import dev.vly.cs2inspect.models.ItemPreviewData
import dev.vly.cs2inspect.models.Sticker

val data = ItemPreviewData(
    defIndex = 7,          // AK-47
    paintIndex = 422,
    paintSeed = 922,
    paintWear = 0.04121f,
    rarity = 3,
    quality = 4,
    stickers = listOf(
        Sticker(slot = 0, stickerId = 7436),
        Sticker(slot = 1, stickerId = 5144),
    )
)

val hex = InspectLink.serialize(data)
// Returns uppercase hex string starting with "00" (key byte = 0x00)
println(hex)
```

### Check link type

```kotlin
val maskedUrl = "steam://rungame/730/.../+csgo_econ_action_preview%2000183C..."
val classicUrl = "steam://rungame/730/.../+csgo_econ_action_preview%20S123A456D789"

InspectLink.isMasked(maskedUrl)  // true  — contains decodable protobuf payload
InspectLink.isClassic(classicUrl) // true  — classic S/A/D decimal format
```

## Supported URL formats

The deserializer accepts all of:

- **Bare hex**: `00183C20B803...`
- **Steam URL with A-prefix**: `steam://rungame/730/.../+csgo_econ_action_preview%20A<hex>`
- **Pure masked URL**: `steam://run/730//+csgo_econ_action_preview%20<hex>`
- **Hybrid URL**: `steam://rungame/730/.../+csgo_econ_action_preview%20S<steam64>A<assetId>D<hexproto>`
- **Classic URL** (read-only check via `isClassic`): `...S<id>A<id>D<decimal>`

## Validation rules

| Rule | Details |
|------|---------|
| `paintWear` range | Must be in `[0.0, 1.0]` or `null` |
| `customName` length | Max 100 characters |
| Payload max length | 4096 hex characters |
| Payload min length | 6 bytes (12 hex chars) |
| Proto field count | Max 100 fields per message |

## Binary format

```
[key_byte] [proto_bytes XOR'd with key] [4-byte checksum XOR'd with key]
```

- `key_byte = 0x00`: tool-generated link, no XOR applied
- `key_byte != 0x00`: native CS2 link, every byte (including key_byte itself) is XOR'd

**Checksum algorithm:**
```
buffer = [0x00] + proto_bytes
crc = CRC32(buffer)                             // java.util.zip.CRC32
xored = (crc & 0xFFFF) ^ (proto_bytes.size * crc)
checksum = big-endian uint32(xored & 0xFFFFFFFF)
```

## Proto field reference (CEconItemPreviewDataBlock)

| Field | Name | Type | Wire |
|-------|------|------|------|
| 1 | accountId | UInt | varint |
| 2 | itemId | ULong | varint |
| 3 | defIndex | UInt | varint |
| 4 | paintIndex | UInt | varint |
| 5 | rarity | UInt | varint |
| 6 | quality | UInt | varint |
| 7 | paintWear | Float? | varint (float32 bits) |
| 8 | paintSeed | UInt | varint |
| 9 | killEaterScoreType | UInt | varint |
| 10 | killEaterValue | UInt | varint |
| 11 | customName | String | len-delimited |
| 12 | stickers | List\<Sticker\> | len-delimited (repeated) |
| 13 | inventory | UInt | varint |
| 14 | origin | UInt | varint |
| 15 | questId | UInt | varint |
| 16 | dropReason | UInt | varint |
| 17 | musicIndex | UInt | varint |
| 18 | entIndex | Int | varint |
| 19 | petIndex | UInt | varint |
| 20 | keychains | List\<Sticker\> | len-delimited (repeated) |

### Sticker proto fields

| Field | Name | Type | Wire |
|-------|------|------|------|
| 1 | slot | Int | varint |
| 2 | stickerId | Int | varint |
| 3 | wear | Float? | fixed32 LE |
| 4 | scale | Float? | fixed32 LE |
| 5 | rotation | Float? | fixed32 LE |
| 6 | tintId | Int | varint |
| 7 | offsetX | Float? | fixed32 LE |
| 8 | offsetY | Float? | fixed32 LE |
| 9 | offsetZ | Float? | fixed32 LE |
| 10 | pattern | Int | varint |
| 11 | highlightReel | Int? | varint |

## Test vectors

```
NATIVE_HEX = "E3F3367440334DE2..."  // XOR key=0xE3, itemId=46876117973, defIndex=7, paintIndex=422
TOOL_HEX   = "00183C20B803..."      // key=0x00, defIndex=60, paintIndex=440, paintSeed=353
CSFLOAT_A  = "00180720DA03..."      // defIndex=7, paintIndex=474, paintWear≈0.6337
CSFLOAT_B  = "00180720C80A..."      // 4 stickers id=76, paintIndex=1352, paintWear≈0.99
CSFLOAT_C  = "A2B2A2BA69..."        // keychain defIndex=1355, highlightReel=345
```

## Running tests

```bash
./gradlew test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for your changes
4. Run `./gradlew test` to verify
5. Submit a pull request

## License

MIT License — see [LICENSE](LICENSE) for details.
