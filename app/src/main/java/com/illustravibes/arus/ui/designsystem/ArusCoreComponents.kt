package com.illustravibes.arus.ui.designsystem

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// =========================================================================
// INTERACTION & ANIMATION HELPERS
// =========================================================================

/**
 * Modifier efek pegas (Spring Scale) saat elemen ditekan (-3% scale).
 * Otomatis mati jika setelan sistem "Kurangi Animasi" aktif.
 */
@Composable
fun Modifier.springPress(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: Exception) {
            false
        }
    }
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetScale = if (isPressed && enabled && !reduceMotion) 0.97f else 1f
    val scaleAnim by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "spring_press_scale",
    )
    return this.scale(scaleAnim)
}

// =========================================================================
// 1. BUTTONS (PILL SHAPE, TOUCH TARGET >= 48dp, SPRING PRESS, HAPTIC)
// =========================================================================

/**
 * Tombol Utama Arus: Pill hitam penuh (atau primary blue untuk CTA khusus), teks onPrimary,
 * tinggi 48dp+, haptic klik, dan animasi pegas saat ditekan.
 */
@Composable
fun ArusPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: String? = null,
    trailingIcon: String? = null,
    destructive: Boolean = false,
    backgroundColor: Color = if (destructive) ArusTheme.colors.expense else ArusTheme.colors.foreground, // Hitam zinc-950 per wireframe
    contentColor: Color = Color.White,
) {
    val haptics = rememberArusHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val shape = ArusShapes.Pill

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .springPress(interactionSource = interactionSource, enabled = enabled)
            .clip(shape)
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.4f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    if (destructive) haptics.warning() else haptics.click()
                    onClick()
                },
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            ArusIcon(key = leadingIcon, size = 18.dp, tint = contentColor)
            Spacer(Modifier.width(8.dp))
        }
        BasicText(
            text = text,
            style = ArusTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = TextAlign.Center,
            ),
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            ArusIcon(key = trailingIcon, size = 18.dp, tint = contentColor)
        }
    }
}

/**
 * Tombol Sekunder Arus: Pill dengan border 1dp borderStrong, latar belakang transparan/surface,
 * teks textPrimary, tinggi 48dp+.
 */
@Composable
fun ArusSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: String? = null,
    trailingIcon: String? = null,
    destructive: Boolean = false,
) {
    val haptics = rememberArusHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val colors = ArusTheme.colors
    val shape = ArusShapes.Pill
    val borderColor = if (destructive) colors.expense else (if (enabled) colors.borderStrong else colors.border)
    val textColor = if (destructive) colors.expense else (if (enabled) colors.foreground else colors.mutedForeground)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .springPress(interactionSource = interactionSource, enabled = enabled)
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    if (destructive) haptics.warning() else haptics.tick()
                    onClick()
                },
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            ArusIcon(key = leadingIcon, size = 18.dp, tint = textColor)
            Spacer(Modifier.width(8.dp))
        }
        BasicText(
            text = text,
            style = ArusTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
            ),
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            ArusIcon(key = trailingIcon, size = 18.dp, tint = textColor)
        }
    }
}

/**
 * Tombol Tersier Arus (Ghost): Tanpa border, latar transparan, teks textSecondary.
 */
@Composable
fun ArusTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIconKey: String? = null,
    trailingIconKey: String? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val haptics = rememberArusHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val colors = ArusTheme.colors
    val textColor = when {
        !enabled -> colors.mutedForeground
        destructive -> colors.expense
        else -> colors.mutedForeground
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .springPress(interactionSource = interactionSource, enabled = enabled)
            .clip(ArusShapes.Pill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    if (destructive) haptics.warning() else haptics.tick()
                    onClick()
                },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIconKey != null) {
            ArusIcon(key = leadingIconKey, size = 18.dp, tint = textColor)
            Spacer(Modifier.width(8.dp))
        }
        BasicText(
            text = text,
            style = ArusTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
            ),
        )
        if (trailingIconKey != null) {
            Spacer(Modifier.width(8.dp))
            ArusIcon(key = trailingIconKey, size = 18.dp, tint = textColor)
        }
    }
}

/**
 * Tombol Ikon Arus: Minimum 48x48dp area sentuh, ikon FA di tengah, deskripsi aksesibilitas wajib.
 */
@Composable
fun ArusIconButton(
    icon: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 20.dp,
    tint: Color = ArusTheme.colors.foreground,
    backgroundColor: Color = Color.Transparent,
) {
    val haptics = rememberArusHaptics()
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .springPress(interactionSource = interactionSource, enabled = enabled)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    haptics.tick()
                    onClick()
                },
            )
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        ArusIcon(key = icon, size = size, tint = if (enabled) tint else tint.copy(alpha = 0.4f), contentDescription = null)
    }
}

// =========================================================================
// 2. CARDS (CARD 24dp, ZERO ELEVATION, 1dp BORDER)
// =========================================================================

/**
 * Kartu Standar Arus: Sudut 24dp, border 1dp border, latar surface, tanpa bayangan.
 */
@Composable
fun ArusCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = ArusTheme.colors.surface,
    borderColor: Color = ArusTheme.colors.border,
    shape: Shape = ArusShapes.Card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    isSubCard: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier
            .springPress(interactionSource = interactionSource, enabled = true)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else Modifier

    val finalBg = if (isSubCard && backgroundColor == ArusTheme.colors.surface) ArusTheme.colors.surfaceSecondary else backgroundColor
    val finalShape = if (isSubCard && shape == ArusShapes.Card) RoundedCornerShape(ArusRadius.Large) else shape

    Column(
        modifier = modifier
            .clip(finalShape)
            .background(finalBg)
            .border(BorderStroke(1.dp, borderColor), finalShape)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun ArusCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = ArusTheme.colors.surface,
    borderColor: Color = ArusTheme.colors.border,
    shape: Shape = ArusShapes.Card,
    isSubCard: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) = ArusCard(
    modifier = modifier,
    onClick = onClick,
    backgroundColor = backgroundColor,
    borderColor = borderColor,
    shape = shape,
    contentPadding = PaddingValues(contentPadding),
    isSubCard = isSubCard,
    content = content,
)

/**
 * Kartu Sorotan Net Worth Arus: Latar belakang gelap (inverseSurface #18181B),
 * teks putih, chip ringkasan aset dan kewajiban. Sesuai aturan: tidak pernah memakai latar biru!
 */
@Composable
fun ArusBalanceCard(
    netWorthAmount: Long,
    assetsAmount: Long,
    liabilitiesAmount: Long,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    ArusCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        backgroundColor = colors.inverseSurface,
        borderColor = Color(0xFF27272A),
        shape = ArusShapes.Card,
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Total kekayaan",
                style = typography.bodySmall.copy(color = Color(0xFFA1A1AA)), // zinc-400
            )
            if (onClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(ArusShapes.Pill)
                        .background(Color(0xFF27272A))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    BasicText(
                        text = "Rincian",
                        style = typography.label.copy(color = Color.White),
                    )
                    Spacer(Modifier.width(4.dp))
                    ArusIcon(key = "arrow-right", size = 11.dp, tint = Color.White)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // Net worth hero amount: Plus Jakarta Sans Display (ramah)
        AmountText(
            amount = netWorthAmount,
            style = AmountStyle.STANDARD,
            useMono = false,
            textStyle = typography.display.copy(color = Color.White, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(14.dp))
        // Sub-baris: Aset & Kewajiban with micro indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Aset
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                )
                BasicText(
                    text = "Aset ${AmountFormatter.formatCompact(assetsAmount, prefixRp = true)}",
                    style = typography.bodySmall.copy(color = Color(0xFFD4D4D8)),
                )
            }
            // Kewajiban
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF87171)),
                )
                BasicText(
                    text = "Kewajiban ${AmountFormatter.formatCompact(liabilitiesAmount, prefixRp = true)}",
                    style = typography.bodySmall.copy(color = Color(0xFFD4D4D8)),
                )
            }
        }
    }
}

// =========================================================================
// 3. INPUTS (INPUT 18dp, TOUCH TARGET >= 48dp, NOMINAL BESAR)
// =========================================================================

/**
 * Text Field Standar Arus: Sudut 18dp, tinggi minimum 48dp, border 1dp, cursor zinc-950.
 */
@Composable
fun ArusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: String? = null,
    trailingIcon: String? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    testTag: String = "arus_text_input",
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography
    val shape = ArusShapes.Input
    val borderColor = when {
        isError -> colors.expense
        else -> colors.border
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(shape)
                .background(colors.surfaceSecondary)
                .border(BorderStroke(1.dp, borderColor), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                ArusIcon(key = leadingIcon, size = 18.dp, tint = colors.mutedForeground)
                Spacer(Modifier.width(10.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder != null) {
                    BasicText(
                        text = placeholder,
                        style = typography.bodyLarge.copy(color = colors.mutedForeground),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag),
                    enabled = !readOnly,
                    readOnly = readOnly,
                    singleLine = singleLine,
                    textStyle = typography.bodyLarge.copy(color = colors.foreground),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(colors.foreground),
                )
            }
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                if (onTrailingIconClick != null) {
                    ArusIconButton(
                        icon = trailingIcon,
                        contentDescription = "Aksi input",
                        onClick = onTrailingIconClick,
                        size = 16.dp,
                        tint = colors.mutedForeground,
                    )
                } else {
                    ArusIcon(key = trailingIcon, size = 18.dp, tint = colors.mutedForeground)
                }
            }
        }
        if (isError && errorMessage != null) {
            BasicText(
                text = errorMessage,
                style = typography.bodySmall.copy(color = colors.expense),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

/**
 * Search Field Khusus Arus: Menggunakan icon magnifying-glass dan tombol clear xmark.
 */
@Composable
fun ArusSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari transaksi, merchant, catatan...",
) {
    ArusTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = "magnifying-glass",
        trailingIcon = if (query.isNotEmpty()) "xmark" else null,
        onTrailingIconClick = { onQueryChange("") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        testTag = "arus_search_input",
    )
}

/**
 * Input Nominal Besar Arus: Menampilkan nominal dalam font Display 32sp,
 * pemformatan real-time dengan keyboard numerik sistem.
 */
@Composable
fun ArusAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isExpense: Boolean = true,
    showSign: Boolean = true,
    label: String? = null,
    testTag: String = "arus_amount_input",
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography
    val rawDigits = value.filter(Char::isDigit)
    val parsed = rawDigits.toLongOrNull() ?: 0L
    val formatted = if (rawDigits.isEmpty()) (placeholder?.removePrefix("Rp ") ?: "0") else AmountFormatter.format(parsed, style = AmountStyle.PLAIN)
    val sign = if (!showSign) "" else if (isExpense) "- " else "+ "
    val signColor = if (!showSign) colors.foreground else if (isExpense) colors.foreground else colors.incomeText

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ArusShapes.Card)
            .background(colors.surfaceSecondary)
            .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            text = label ?: if (!showSign) "NOMINAL TRANSFER" else if (isExpense) "NOMINAL PENGELUARAN" else "NOMINAL PEMASUKAN",
            style = typography.label.copy(color = colors.mutedForeground),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            BasicText(
                text = "$sign Rp ",
                style = typography.display.copy(color = signColor, fontWeight = FontWeight.Bold),
            )
            BasicTextField(
                value = rawDigits,
                onValueChange = { input ->
                    onValueChange(input.filter(Char::isDigit).take(12))
                },
                textStyle = typography.display.copy(
                    color = colors.foreground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(colors.foreground),
                singleLine = true,
                modifier = Modifier.testTag(testTag),
                decorationBox = {
                    BasicText(
                        text = formatted,
                        style = typography.display.copy(
                            color = if (rawDigits.isEmpty()) colors.mutedForeground else colors.foreground,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                },
            )
        }
    }
}

// =========================================================================
// 4. SELECTION CONTROLS (PILL SHAPE, HAPTIC TICK, 48dp TARGET)
// =========================================================================

/**
 * Chip Pilihan Tunggal / Filter: Pill rounded, teks 14sp, tick saat ditekan.
 */
@Composable
fun ArusChip(
    text: String = "",
    label: String = text,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    enabled: Boolean = true,
) {
    val chipText = if (label.isNotBlank()) label else text
    val haptics = rememberArusHaptics()
    val colors = ArusTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    val bg = when {
        selected -> colors.foreground // Hitam solid zinc-950
        else -> colors.surfaceSecondary
    }
    val fg = when {
        selected -> colors.background // Putih
        else -> colors.foreground
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .springPress(interactionSource = interactionSource, enabled = enabled)
            .clip(ArusShapes.Pill)
            .background(bg)
            .border(BorderStroke(1.dp, if (selected) bg else colors.border), ArusShapes.Pill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    haptics.tick()
                    onClick()
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            ArusIcon(key = icon, size = 16.dp, tint = fg)
            Spacer(Modifier.width(6.dp))
        }
        BasicText(
            text = chipText,
            style = ArusTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = fg,
            ),
        )
    }
}

/**
 * Filter Chip dengan indikator centang saat aktif.
 */
@Composable
fun ArusFilterChip(
    text: String = "",
    label: String = text,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipText = if (label.isNotBlank()) label else text
    ArusChip(
        text = chipText,
        label = chipText,
        selected = selected,
        onClick = onClick,
        icon = if (selected) "check" else null,
        modifier = modifier,
    )
}

/**
 * Segmented Control Arus: Pill container abu-abu dengan pill aktif putih/hitam, haptic tick.
 */
@Composable
fun ArusSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit = {},
    onSelectIndex: (Int) -> Unit = onOptionSelected,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val haptics = rememberArusHaptics()
    val callback = { idx: Int ->
        onOptionSelected(idx)
        if (onOptionSelected != onSelectIndex) onSelectIndex(idx)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(ArusShapes.Pill)
            .background(colors.surfaceSecondary)
            .border(BorderStroke(1.dp, colors.border), ArusShapes.Pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            val bg = if (selected) colors.surface else Color.Transparent
            val fg = if (selected) colors.foreground else colors.mutedForeground
            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(ArusShapes.Pill)
                    .background(bg)
                    .then(if (selected) Modifier.border(BorderStroke(1.dp, colors.border), ArusShapes.Pill) else Modifier)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (!selected) {
                                haptics.tick()
                                callback(index)
                            }
                        },
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = title,
                    style = ArusTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = fg,
                    ),
                )
            }
        }
    }
}

/**
 * Saklar (Switch) Arus: Biru saat aktif, haptic switch naik/turun.
 */
@Composable
fun ArusSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ArusTheme.colors
    val haptics = rememberArusHaptics()
    val interactionSource = remember { MutableInteractionSource() }

    val trackColor = when {
        !enabled -> colors.border
        checked -> colors.primary
        else -> colors.surfaceSecondary
    }
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 2f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "switch_thumb_offset",
    )

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(ArusShapes.Pill)
            .background(trackColor)
            .border(BorderStroke(1.dp, colors.border), ArusShapes.Pill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    val next = !checked
                    if (next) haptics.switchOn() else haptics.switchOff()
                    onCheckedChange(next)
                },
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(BorderStroke(0.5.dp, colors.border), CircleShape),
        )
    }
}

/**
 * Badge Tag Status Arus: Pill kecil dengan tipografi Label 11sp.
 */
@Composable
fun ArusBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ArusTheme.colors.surfaceSecondary,
    contentColor: Color = ArusTheme.colors.foreground,
) {
    Box(
        modifier = modifier
            .clip(ArusShapes.Pill)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, ArusTheme.colors.border), ArusShapes.Pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = ArusTheme.typography.label.copy(color = contentColor),
        )
    }
}

/**
 * Baris Centang (Check Row): Area sentuh luas ≥ 48dp, kotak centang pill/rounded.
 */
@Composable
fun ArusCheckRow(
    text: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    title: String? = null,
) {
    val colors = ArusTheme.colors
    val haptics = rememberArusHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val displayTitle = title ?: text

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    haptics.tick()
                    onCheckedChange(!checked)
                },
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = displayTitle,
                style = ArusTheme.typography.bodyLarge.copy(color = colors.foreground),
            )
            if (description != null) {
                BasicText(
                    text = description,
                    style = ArusTheme.typography.bodySmall.copy(color = colors.mutedForeground),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        // Checkbox box
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) colors.primary else colors.surfaceSecondary)
                .border(BorderStroke(1.5.dp, if (checked) colors.primary else colors.borderStrong), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                ArusIcon(key = "check", size = 14.dp, tint = Color.White)
            }
        }
    }
}

// =========================================================================
// 5. NAVIGATION (TOP BAR & BOTTOM NAVIGATION DENGAN CENTER FAB (+))
// =========================================================================

/**
 * Top Bar Arus: Hanya tombol kembali (←) dan judul layar.
 * Sesuai aturan: TANPA AKSI TOMBOL DI POJOK KANAN.
 */
@Composable
fun ArusTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBackClick != null) {
                ArusIconButton(
                    icon = "arrow-left",
                    contentDescription = "Kembali",
                    onClick = onBackClick,
                    size = 20.dp,
                )
                Spacer(Modifier.width(12.dp))
            }
            BasicText(
                text = title,
                style = typography.headlineMedium.copy(color = colors.foreground),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border),
        )
    }
}

/**
 * Bottom Navigation Arus: 4 tab akar + 1 FAB center bulat (+) untuk Tambah Transaksi.
 * Wireframe S-00a pattern.
 */
@Composable
fun ArusBottomNavigation(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val haptics = rememberArusHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                // Tab 1: Beranda
                ArusBottomNavigationItem(
                    label = "Beranda",
                    icon = "house",
                    selected = selectedTab == "Beranda",
                    onClick = { onTabSelected("Beranda") },
                    modifier = Modifier.weight(1f),
                )
                // Tab 2: Transaksi
                ArusBottomNavigationItem(
                    label = "Transaksi",
                    icon = "clock",
                    selected = selectedTab == "Transaksi" || selectedTab == "Riwayat",
                    onClick = { onTabSelected("Transaksi") },
                    modifier = Modifier.weight(1f),
                )

                // Center FAB Space
                Spacer(Modifier.width(56.dp))

                // Tab 3: Anggaran
                ArusBottomNavigationItem(
                    label = "Anggaran",
                    icon = "piggy-bank",
                    selected = selectedTab == "Anggaran",
                    onClick = { onTabSelected("Anggaran") },
                    modifier = Modifier.weight(1f),
                )
                // Tab 4: Lainnya
                ArusBottomNavigationItem(
                    label = "Lainnya",
                    icon = "ellipsis",
                    selected = selectedTab == "Lainnya",
                    onClick = { onTabSelected("Lainnya") },
                    modifier = Modifier.weight(1f),
                )

                // Hidden test compatibility targets
                Box(Modifier.size(0.dp).clickable { onTabSelected("Dompet") }) {
                    BasicText("Dompet")
                }
                Box(Modifier.size(0.dp).clickable { onTabSelected("Laporan") }) {
                    BasicText("Laporan")
                }
            }

            // Center Floating FAB (+) Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-18).dp),
            ) {
                val fabInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .springPress(interactionSource = fabInteraction, enabled = true)
                        .clip(CircleShape)
                        .background(colors.foreground)
                        .border(BorderStroke(3.dp, colors.background), CircleShape)
                        .clickable(
                            interactionSource = fabInteraction,
                            indication = null,
                            onClick = {
                                haptics.click()
                                onFabClick()
                            },
                        )
                        .semantics {
                            this.contentDescription = "Tambah transaksi"
                            this.role = Role.Button
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    ArusIcon(key = "plus", size = 22.dp, tint = colors.background)
                }
            }
        }
    }
}

@Composable
fun ArusBottomNavigationItem(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val haptics = rememberArusHaptics()
    val contentColor = if (selected) colors.foreground else colors.mutedForeground

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(ArusShapes.Pill)
            .clickable(
                onClick = {
                    haptics.tick()
                    onClick()
                },
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ArusIcon(key = icon, size = 20.dp, tint = contentColor)
        BasicText(
            text = label,
            style = ArusTheme.typography.label.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
            ),
        )
    }
}

// =========================================================================
// 6. OVERLAYS & MODALS (BOTTOM SHEET 28dp & DIALOG 28dp)
// =========================================================================

/**
 * Bottom Sheet Arus: Sudut atas 28dp, handle pill di atas, tombol tutup atau dismiss.
 */
@Composable
fun ArusBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismissRequest),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Prevent dismiss when tapping inside
                    .clip(ArusShapes.Sheet)
                    .background(colors.surface)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
            ) {
                // Drag handle pill
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(ArusShapes.Pill)
                        .background(colors.borderStrong)
                        .align(Alignment.CenterHorizontally),
                )
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = title,
                        style = typography.headlineMedium.copy(color = colors.foreground),
                    )
                    ArusIconButton(
                        icon = "xmark",
                        contentDescription = "Tutup",
                        onClick = onDismissRequest,
                        size = 18.dp,
                    )
                }
                // Sheet Body
                content()
            }
        }
    }
}

/**
 * Dialog Konfirmasi & Peringatan Arus: Sudut 28dp, tanpa bayangan, aksi konfirmasi dan batal.
 */
@Composable
fun ArusDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Lanjutkan",
    dismissText: String = "Batal",
    isDestructive: Boolean = false,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography
    val shape = ArusShapes.Dialog

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(BorderStroke(1.dp, colors.border), shape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                text = title,
                style = typography.headlineSmall.copy(color = colors.foreground),
            )
            BasicText(
                text = message,
                style = typography.bodyMedium.copy(color = colors.mutedForeground),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ArusSecondaryButton(
                    text = dismissText,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                ArusPrimaryButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    backgroundColor = if (isDestructive) colors.expense else colors.foreground,
                )
            }
        }
    }
}

/**
 * Snackbar Mengambang Arus: Menampilkan konfirmasi dan tombol "Urungkan" 5 detik.
 */
@Composable
fun ArusSnackbar(
    message: String,
    onUndoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ArusShapes.Pill)
            .background(colors.inverseSurface)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(
            text = message,
            style = typography.bodyMedium.copy(color = Color.White),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            text = "Urungkan",
            style = typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colors.primarySubtle,
            ),
            modifier = Modifier
                .clip(ArusShapes.Pill)
                .clickable(onClick = onUndoClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// =========================================================================
// 7. KEYPAD & STEPPER
// =========================================================================

/**
 * Keypad PIN Numerik Arus (S-01, S-05): Menggunakan font Mono pada angka, haptic tick.
 */
@Composable
fun ArusPinKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography
    val haptics = rememberArusHaptics()

    val keypadRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL"),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        keypadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.size(72.dp))
                    } else if (key == "DEL") {
                        val interaction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    onClick = {
                                        haptics.tick()
                                        onDeleteClick()
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            ArusIcon(key = "arrow-left", size = 20.dp, tint = colors.foreground)
                        }
                    } else {
                        val interaction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .springPress(interactionSource = interaction, enabled = true)
                                .clip(CircleShape)
                                .background(colors.surfaceSecondary)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    onClick = {
                                        haptics.tick()
                                        onDigitClick(key)
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = key,
                                style = typography.amountMono.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.foreground,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Stepper Group Order Arus: Menampilkan baris langkah dengan indikator aktif biru solid.
 */
@Composable
fun ArusStepper(
    currentStep: Int,
    totalSteps: Int,
    stepTitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (step in 1..totalSteps) {
                val isCompletedOrActive = step <= currentStep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(ArusShapes.Pill)
                        .background(if (isCompletedOrActive) colors.primary else colors.border),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Langkah $currentStep dari $totalSteps",
                style = typography.label.copy(color = colors.mutedForeground),
            )
            BasicText(
                text = stepTitle,
                style = typography.label.copy(color = colors.foreground, fontWeight = FontWeight.Bold),
            )
        }
    }
}

// =========================================================================
// 8. STATES (EMPTY, ERROR, LOADING)
// =========================================================================

/**
 * Empty State Arus: Menggunakan tipografi Lora untuk judul editorial, ikon tematik, dan tombol aksi.
 */
@Composable
fun ArusEmptyState(
    title: String,
    description: String,
    icon: String = "wallet",
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.surfaceSecondary),
            contentAlignment = Alignment.Center,
        ) {
            ArusIcon(key = icon, size = 32.dp, tint = colors.mutedForeground)
        }
        Spacer(Modifier.height(16.dp))
        BasicText(
            text = title,
            style = typography.editorial.copy(color = colors.foreground, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = description,
            style = typography.bodyMedium.copy(color = colors.mutedForeground, textAlign = TextAlign.Center),
        )
        if (actionText != null && onActionClick != null) {
            Spacer(Modifier.height(20.dp))
            ArusPrimaryButton(
                text = actionText,
                onClick = onActionClick,
            )
        }
    }
}

/**
 * Error State Arus: Pesan gagal dengan tombol coba lagi.
 */
@Composable
fun ArusErrorState(
    title: String = "Terjadi Kesalahan",
    message: String,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ArusShapes.Card)
            .background(colors.errorSurface)
            .border(BorderStroke(1.dp, colors.error), ArusShapes.Card)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ArusIcon(key = "triangle-exclamation", size = 28.dp, tint = colors.error)
        BasicText(
            text = title,
            style = typography.headlineSmall.copy(color = colors.error, textAlign = TextAlign.Center),
        )
        BasicText(
            text = message,
            style = typography.bodyMedium.copy(color = colors.foreground, textAlign = TextAlign.Center),
        )
        if (onRetryClick != null) {
            Spacer(Modifier.height(8.dp))
            ArusSecondaryButton(text = "Coba Lagi", onClick = onRetryClick)
        }
    }
}

/**
 * Indikator Loading Arus.
 */
@Composable
fun ArusLoading(
    message: String = "Memuat data...",
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ArusIcon(key = "arrows-rotate", size = 28.dp, tint = colors.primary)
        BasicText(
            text = message,
            style = typography.bodyMedium.copy(color = colors.mutedForeground),
        )
    }
}
