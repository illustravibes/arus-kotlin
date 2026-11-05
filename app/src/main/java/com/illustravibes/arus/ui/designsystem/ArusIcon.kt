package com.illustravibes.arus.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.illustravibes.arus.R
import com.illustravibes.arus.ui.designsystem.Text

private val FontAwesomeSolid = FontFamily(Font(R.font.font_awesome_solid, FontWeight.Normal))

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
    ArusText(
        text = String(Character.toChars(icon.codePoint)),
        modifier = modifier,
        color = ArusTheme.colors.foreground,
        fontFamily = FontAwesomeSolid,
        fontSize = fontSize,
        fontWeight = FontWeight.Normal,
    )
}
