package com.illustravibes.arus.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true, widthDp = 390, heightDp = 1800)
@Composable
fun TokenGalleryPreview() {
    ArusTheme {
        val colors = ArusTheme.colors
        val typography = ArusTheme.typography
        val shapes = ArusTheme.shapes
        val haptics = rememberArusHaptics()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(ArusSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.XLarge),
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "Arus Design Tokens",
                    style = typography.headlineLarge.copy(color = colors.foreground),
                )
                BasicText(
                    text = "Galeri token warna, tipografi, bentuk, nominal, dan haptic.",
                    style = typography.bodyMedium.copy(color = colors.mutedForeground),
                )
            }

            // 1. Brand & Semantic Colors
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText(
                    text = "1. WARNA & PALET",
                    style = typography.label.copy(color = colors.mutedForeground),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                    verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                ) {
                    ColorSwatch("Primary", colors.primary, colors.onPrimary)
                    ColorSwatch("Zinc 950", colors.foreground, Color.White)
                    ColorSwatch("Zinc 900", colors.inverseSurface, colors.inverseForeground)
                    ColorSwatch("Zinc 500", colors.mutedForeground, Color.White)
                    ColorSwatch("Zinc 100", colors.surfaceSecondary, colors.foreground)
                    ColorSwatch("Income", colors.income, Color.White)
                    ColorSwatch("IncomeText", colors.incomeText, Color.White)
                    ColorSwatch("Expense", colors.expense, Color.White)
                    ColorSwatch("Warning", colors.warning, Color.White)
                }
            }

            // 2. Tipografi Specimen
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText(
                    text = "2. TIPOGRAFI",
                    style = typography.label.copy(color = colors.mutedForeground),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.surfaceSecondary)
                        .padding(ArusSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                ) {
                    BasicText("Display 32sp Bold", style = typography.display.copy(color = colors.foreground))
                    BasicText("Heading Large 24sp Bold", style = typography.headlineLarge.copy(color = colors.foreground))
                    BasicText("Heading Medium 20sp SemiBold", style = typography.headlineMedium.copy(color = colors.foreground))
                    BasicText("Heading Small 18sp SemiBold", style = typography.headlineSmall.copy(color = colors.foreground))
                    BasicText("Body Large 16sp Normal", style = typography.bodyLarge.copy(color = colors.foreground))
                    BasicText("Body Medium 14sp Normal", style = typography.bodyMedium.copy(color = colors.foreground))
                    BasicText("Body Small 12sp Normal", style = typography.bodySmall.copy(color = colors.mutedForeground))
                    BasicText("LABEL 11SP SEMIBOLD", style = typography.label.copy(color = colors.primary))
                    BasicText("Lora Editorial 24sp SemiBold", style = typography.editorial.copy(color = colors.foreground))
                    BasicText("“Lora Editorial Quote 16sp Italic”", style = typography.editorialQuote.copy(color = colors.mutedForeground))
                    BasicText("Mono 16sp Medium (1.250.000)", style = typography.amountMono.copy(color = colors.foreground))
                    BasicText("Code 14sp Normal (BCA-08123456)", style = typography.code.copy(color = colors.mutedForeground))
                }
            }

            // 3. Bentuk (Shapes) & Sudut
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText(
                    text = "3. BENTUK (SHAPES)",
                    style = typography.label.copy(color = colors.mutedForeground),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                ) {
                    ShapeBox("Pill", shapes.pill, Modifier.weight(1f))
                    ShapeBox("Input 18dp", shapes.input, Modifier.weight(1f))
                    ShapeBox("Card 24dp", shapes.card, Modifier.weight(1f))
                    ShapeBox("Sheet 28dp", shapes.sheet, Modifier.weight(1f))
                }
            }

            // 4. Nominal Keuangan (AmountText)
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText(
                    text = "4. ATURAN NOMINAL (AmountText)",
                    style = typography.label.copy(color = colors.mutedForeground),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.surface)
                        .border(1.dp, colors.border, shapes.card)
                        .padding(ArusSpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText("Pengeluaran (Standard)", style = typography.bodyMedium)
                        AmountText(amount = 65000, kind = AmountKind.EXPENSE)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText("Pemasukan (Standard)", style = typography.bodyMedium)
                        AmountText(amount = 2500000, kind = AmountKind.INCOME)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText("Transfer / Netral", style = typography.bodyMedium)
                        AmountText(amount = 500000, kind = AmountKind.TRANSFER)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText("Format Ringkas (Compact)", style = typography.bodyMedium)
                        AmountText(amount = 12500000, style = AmountStyle.COMPACT, kind = AmountKind.INCOME)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText("Selisih Negatif (Alert)", style = typography.bodyMedium)
                        AmountText(amount = -145000, kind = AmountKind.DIFFERENCE)
                    }
                }
            }

            // 5. Ikon Font Awesome
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText(
                    text = "5. IKON (Font Awesome Registry)",
                    style = typography.label.copy(color = colors.mutedForeground),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
                ) {
                    listOf("house", "wallet", "clock", "piggy-bank", "utensils", "bolt", "wifi", "gas-pump", "cart-shopping", "gift", "plus", "gear").forEach { key ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(shapes.pill)
                                    .background(colors.surfaceSecondary),
                                contentAlignment = Alignment.Center,
                            ) {
                                ArusIcon(key = key, size = 20.dp, tint = colors.foreground)
                            }
                            Spacer(Modifier.height(4.dp))
                            BasicText(key, style = typography.bodySmall.copy(color = colors.mutedForeground))
                        }
                    }
                }
            }

            // 6. Penguji Haptic
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText(
                    text = "6. PENGUJI HAPTIC INTERAKTIF",
                    style = typography.label.copy(color = colors.mutedForeground),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                    verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                ) {
                    HapticButton("Klik", onClick = { haptics.click() })
                    HapticButton("Tick", onClick = { haptics.tick() })
                    HapticButton("Switch On", onClick = { haptics.switchOn() })
                    HapticButton("Switch Off", onClick = { haptics.switchOff() })
                    HapticButton("Konfirmasi", onClick = { haptics.confirm() })
                    HapticButton("Penolakan", onClick = { haptics.reject() })
                    HapticButton("Peringatan", onClick = { haptics.warning() })
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, bg: Color, text: Color) {
    Box(
        modifier = Modifier
            .width(105.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, ArusTheme.colors.border, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        BasicText(name, style = ArusTheme.typography.bodySmall.copy(color = text))
    }
}

@Composable
private fun ShapeBox(label: String, shape: androidx.compose.ui.graphics.Shape, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(shape)
            .background(ArusTheme.colors.surfaceSecondary)
            .border(1.dp, ArusTheme.colors.borderStrong, shape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.foreground))
    }
}

@Composable
private fun HapticButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(ArusShapes.Pill)
            .background(ArusColorsPalette.Zinc900)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        BasicText(label, style = ArusTheme.typography.bodySmall.copy(color = Color.White))
    }
}
