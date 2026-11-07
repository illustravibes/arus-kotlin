package com.illustravibes.arus.ui.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illustravibes.arus.R

val FontAwesomeSolid = FontFamily(Font(R.font.font_awesome_solid, FontWeight.Normal))

/**
 * Registry pemetaan kunci string Font Awesome ke Unicode Codepoint.
 * Menggunakan Font Awesome 6 Free Solid.
 */
object ArusIconRegistry {
    const val HOUSE = "house"
    const val CLOCK = "clock"
    const val WALLET = "wallet"
    const val PIGGY_BANK = "piggy-bank"
    const val PLUS = "plus"
    const val MINUS = "minus"
    const val CHECK = "check"
    const val XMARK = "xmark"
    const val ARROW_LEFT = "arrow-left"
    const val ARROW_RIGHT = "arrow-right"
    const val ARROW_UP = "arrow-up"
    const val ARROW_DOWN = "arrow-down"
    const val TRANSFER = "arrow-right-arrow-left"
    const val ROTATE = "arrows-rotate"
    const val ELLIPSIS = "ellipsis"
    const val GEAR = "gear"
    const val LOCK = "lock"
    const val CAMERA = "camera"
    const val RECEIPT = "receipt"
    const val GROUP_ORDER = "users"
    const val RECONCILIATION = "scale-balanced"
    const val EXPENSE = "minus"
    const val INCOME = "plus"
    const val CATEGORY = "tag"
    const val BANK = "building-columns"
    const val CASH = "money-bill-wave"
    const val CREDIT_CARD = "credit-card"
    const val CALENDAR = "calendar"
    const val CHEVRON_RIGHT = "chevron-right"
    const val PEN = "pen"
    const val STORE = "store"
    const val SPLIT = "divide"
    const val SEARCH = "magnifying-glass"
    const val TRASH = "trash"
    const val SLIDERS = "sliders"

    private val keyMap = mapOf(
        // Navigasi & Umum
        "house" to 0xF015,
        "clock" to 0xF017,
        "wallet" to 0xF555,
        "piggy-bank" to 0xF4D3,
        "plus" to 0x2B,
        "minus" to 0xF068,
        "check" to 0xF00C,
        "xmark" to 0xF00D,
        "arrow-left" to 0xF060,
        "arrow-right" to 0xF061,
        "arrow-up" to 0xF062,
        "arrow-down" to 0xF063,
        "arrow-right-arrow-left" to 0xF0EC,
        "arrows-rotate" to 0xF021,
        "ellipsis" to 0xF141,
        "gear" to 0xF013,
        "lock" to 0xF023,
        "unlock" to 0xF09C,
        "magnifying-glass" to 0xF002,
        "pen" to 0xF304,
        "trash" to 0xF1F8,
        "archive" to 0xF187,
        "circle-info" to 0xF05A,
        "circle-check" to 0xF058,
        "circle-exclamation" to 0xF06A,
        "triangle-exclamation" to 0xF071,
        "circle-question" to 0xF059,
        "fingerprint" to 0xF577,
        "camera" to 0xF030,
        "image" to 0xF03E,
        "crop" to 0xF125,
        "share-nodes" to 0xF1E0,
        "bell" to 0xF0F3,
        "calendar" to 0xF133,
        "chart-pie" to 0xF200,
        "file-export" to 0xF56E,
        "cloud-arrow-up" to 0xF0EE,
        "cloud-arrow-down" to 0xF0ED,
        "user" to 0xF007,
        "users" to 0xF0C0,

        // Keuangan & Pembayaran
        "money-bill" to 0xF0D6,
        "money-bill-wave" to 0xF53A,
        "credit-card" to 0xF09D,
        "building-columns" to 0xF19C,
        "bank" to 0xF19C,
        "receipt" to 0xF543,
        "tags" to 0xF02C,
        "tag" to 0xF02B,
        "coins" to 0xF51E,
        "hand-holding-dollar" to 0xF4C0,
        "hand-holding-heart" to 0xF4BE,

        // Kategori Pengeluaran & Pemasukan
        "utensils" to 0xF2E7,
        "burger" to 0xF805,
        "mug-hot" to 0xF0F4,
        "cart-shopping" to 0xF07A,
        "bag-shopping" to 0xF290,
        "basket-shopping" to 0xF291,
        "shirt" to 0xF553,
        "house-chimney" to 0xF015,
        "bolt" to 0xF0E7,
        "droplet" to 0xF043,
        "wifi" to 0xF1EB,
        "mobile" to 0xF3CD,
        "mobile-screen" to 0xF3CF,
        "car" to 0xF1B9,
        "bus" to 0xF207,
        "motorcycle" to 0xF21C,
        "gas-pump" to 0xF52F,
        "plane" to 0xF072,
        "film" to 0xF008,
        "gamepad" to 0xF11B,
        "music" to 0xF001,
        "heart-pulse" to 0xF21E,
        "heart" to 0xF004,
        "kit-medical" to 0xF479,
        "graduation-cap" to 0xF19D,
        "book" to 0xF02D,
        "briefcase" to 0xF0B1,
        "laptop" to 0xF109,
        "gift" to 0xF06B,
        "cake-candles" to 0xF1FD,
        "paw" to 0xF1B0,
        "baby" to 0xF77C,
        "wrench" to 0xF0AD,
        "shield" to 0xF132,
    )

    // Kamus alias nama ikon Material lama ke Font Awesome
    private val materialAliases = mapOf(
        "restaurant" to "utensils",
        "home" to "house",
        "directions_bus" to "bus",
        "local_gas_station" to "gas-pump",
        "shopping_bag" to "bag-shopping",
        "smartphone" to "mobile",
        "favorite" to "heart",
        "movie" to "film",
        "school" to "graduation-cap",
        "autorenew" to "arrows-rotate",
        "card_giftcard" to "gift",
        "redeem" to "gift",
        "payments" to "money-bill-wave",
        "work" to "briefcase",
        "savings" to "piggy-bank",
        "emoji_events" to "gift",
        "more_horiz" to "ellipsis",
        "account_balance_wallet" to "wallet",
        "credit_card" to "credit-card",
        "receipt" to "receipt",
    )

    fun getCodePoint(key: String): Int {
        val normalized = key.trim().lowercase().replace('_', '-')
        // Cari langsung di FA keyMap
        keyMap[normalized]?.let { return it }
        // Cari di Material alias
        materialAliases[normalized]?.let { alias ->
            keyMap[alias]?.let { return it }
        }
        // Fallback untuk karakter tunggal (misal inisial "B", "M", "S" dari preset dompet)
        if (key.length == 1 && (key[0].isLetterOrDigit())) {
            return key[0].code
        }
        // Fallback jika tidak dikenal
        return keyMap["circle-question"] ?: 0xF059
    }

    /**
     * Kumpulan ikon kategori yang dapat dipilih pengguna di Form Kategori (S-41)
     */
    val categoryIconCandidates = listOf(
        "utensils", "burger", "mug-hot", "cart-shopping", "bag-shopping",
        "house", "bolt", "droplet", "wifi", "mobile",
        "car", "bus", "motorcycle", "gas-pump", "plane",
        "film", "gamepad", "music", "heart-pulse", "heart",
        "graduation-cap", "book", "briefcase", "laptop", "gift",
        "money-bill-wave", "piggy-bank", "coins", "paw", "wrench",
    )
}

/**
 * Komponen utama ArusIcon berbasis Font Awesome.
 * Mendukung ukuran token (16dp, 20dp, 24dp) dan deskripsi aksesibilitas.
 */
@Composable
fun ArusIcon(
    key: String = "",
    iconKey: String = key,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = ArusTheme.colors.foreground,
    contentDescription: String? = null,
) {
    val actualKey = if (iconKey.isNotBlank()) iconKey else key
    val codePoint = ArusIconRegistry.getCodePoint(actualKey)
    val density = LocalDensity.current
    val fontSizeSp = with(density) { size.toSp() }

    BasicText(
        text = String(Character.toChars(codePoint)),
        modifier = modifier
            .size(size)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                    this.role = Role.Image
                } else {
                    this.hideFromAccessibility()
                }
            },
        style = TextStyle(
            fontFamily = FontAwesomeSolid,
            fontSize = fontSizeSp,
            fontWeight = FontWeight.Normal,
            color = tint,
            textAlign = TextAlign.Center,
        ),
    )
}

// =========================================================================
// BACKWARDS COMPATIBILITY LAYER (Memastikan kode layar lama tetap compile)
// =========================================================================

enum class ArusIconCode(val codePoint: Int) {
    HOUSE(0xF015), CLOCK(0xF017), WALLET(0xF555), LAYER_GROUP(0xF5FD),
    PLUS(0x2B), MAGNIFYING_GLASS(0xF002), PEN(0xF304), TRASH(0xF1F8),
    ARCHIVE(0xF187), ARROW_UP(0xF062), ARROW_DOWN(0xF063), CHECK(0xF00C),
    XMARK(0xF00D), ELLIPSIS(0xF141), CIRCLE_INFO(0xF05A), GEAR(0xF013),
    CREDIT_CARD(0xF09D), MONEY_BILL(0xF0D6), BANK(0xF19C),
    ARROW_RIGHT_ARROW_LEFT(0xF0EC),
    UTENSILS(0xF2E7), WIFI(0xF1EB), BOLT(0xF0E7),
    BUS(0xF207), GAS_PUMP(0xF52F), BAG_SHOPPING(0xF290), MOBILE(0xF3CD),
    HEART(0xF004), FILM(0xF008), GRADUATION_CAP(0xF19D), ROTATE(0xF2F1),
    GIFT(0xF06B), MONEY_BILL_WAVE(0xF53A), BRIEFCASE(0xF0B1),
    HAND_HOLDING_HEART(0xF4BE), PIGGY_BANK(0xF4D3), CIRCLE_QUESTION(0xF059),
}

fun categoryIcon(name: String): ArusIconCode = when (name) {
    "restaurant" -> ArusIconCode.UTENSILS
    "home" -> ArusIconCode.HOUSE
    "wifi" -> ArusIconCode.WIFI
    "bolt" -> ArusIconCode.BOLT
    "directions_bus" -> ArusIconCode.BUS
    "local_gas_station" -> ArusIconCode.GAS_PUMP
    "shopping_bag" -> ArusIconCode.BAG_SHOPPING
    "smartphone" -> ArusIconCode.MOBILE
    "favorite" -> ArusIconCode.HEART
    "movie" -> ArusIconCode.FILM
    "school" -> ArusIconCode.GRADUATION_CAP
    "autorenew" -> ArusIconCode.ROTATE
    "card_giftcard", "redeem" -> ArusIconCode.GIFT
    "payments" -> ArusIconCode.MONEY_BILL_WAVE
    "work" -> ArusIconCode.BRIEFCASE
    "savings" -> ArusIconCode.PIGGY_BANK
    "emoji_events" -> ArusIconCode.GIFT
    "more_horiz" -> ArusIconCode.ELLIPSIS
    else -> ArusIconCode.CIRCLE_QUESTION
}

@Composable
fun ArusIcon(
    icon: ArusIconCode,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    fontSize: TextUnit = 18.sp,
) {
    BasicText(
        text = String(Character.toChars(icon.codePoint)),
        modifier = modifier.semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
                this.role = Role.Image
            } else {
                this.hideFromAccessibility()
            }
        },
        style = TextStyle(
            fontFamily = FontAwesomeSolid,
            fontSize = fontSize,
            fontWeight = FontWeight.Normal,
            color = ArusTheme.colors.foreground,
            textAlign = TextAlign.Center,
        ),
    )
}
