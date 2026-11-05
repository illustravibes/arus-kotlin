package com.illustravibes.arus.ui.designsystem

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ArusText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = ArusTheme.typography.body,
    color: Color = ArusTheme.colors.foreground,
    textAlign: TextAlign? = null,
) {
    BasicText(text, modifier, style.copy(color = color, textAlign = textAlign ?: style.textAlign))
}

@Composable
fun ArusText(
    text: String,
    modifier: Modifier,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: androidx.compose.ui.text.font.FontWeight,
    color: Color,
) {
    BasicText(text, modifier, TextStyle(fontFamily = fontFamily, fontSize = fontSize, fontWeight = fontWeight, color = color))
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalArusTextStyle.current,
    color: Color = LocalArusContentColor.current,
    textAlign: TextAlign? = null,
) = ArusText(text, modifier, style, color, textAlign)

@Composable
fun ProvideArusTextStyle(style: TextStyle, color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalArusTextStyle provides style, LocalArusContentColor provides color, content = content)
}
