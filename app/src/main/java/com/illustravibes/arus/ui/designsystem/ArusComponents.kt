package com.illustravibes.arus.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicText
import com.illustravibes.arus.ui.designsystem.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

enum class ArusButtonKind { Primary, Secondary, Outline, Ghost, Destructive }

@Composable
fun ArusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    kind: ArusButtonKind = ArusButtonKind.Primary,
    selected: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = ArusTheme.colors
    val shape = RoundedCornerShape(if (kind == ArusButtonKind.Primary) ArusRadius.Large else ArusRadius.Medium)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState().value
    val background = when (kind) {
        ArusButtonKind.Primary -> colors.primary
        ArusButtonKind.Secondary -> if (selected) colors.muted else colors.surface
        ArusButtonKind.Outline, ArusButtonKind.Ghost -> colors.background
        ArusButtonKind.Destructive -> colors.error
    }
    val foreground = when (kind) {
        ArusButtonKind.Primary -> colors.onPrimary
        ArusButtonKind.Secondary, ArusButtonKind.Outline, ArusButtonKind.Ghost -> colors.foreground
        ArusButtonKind.Destructive -> Color.White
    }
    Row(
        modifier = modifier.defaultMinSize(minHeight = 44.dp)
            .clip(shape)
            .background((if (pressed && enabled) colors.muted else background).copy(alpha = if (enabled) 1f else 0.5f))
            .then(if (kind != ArusButtonKind.Primary && kind != ArusButtonKind.Ghost && kind != ArusButtonKind.Destructive) Modifier.border(BorderStroke(1.dp, colors.border), shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProvideArusTextStyle(ArusTheme.typography.body, foreground) { content() }
    }
}

@Composable
fun ArusDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(ArusRadius.ExtraLarge))
                .background(ArusTheme.colors.surface)
                .border(1.dp, ArusTheme.colors.border, RoundedCornerShape(ArusRadius.ExtraLarge))
                .padding(ArusSpacing.Large),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            title?.invoke()
            text?.invoke()
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(ArusSpacing.Small)) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}

@Composable
fun OutlinedArusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = ArusButton(onClick = onClick, modifier = modifier, enabled = enabled, kind = ArusButtonKind.Outline, content = content)

@Composable
fun ArusSegmentedButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ArusButton(
        onClick = onClick,
        modifier = modifier,
        enabled = !selected,
        kind = if (selected) ArusButtonKind.Secondary else ArusButtonKind.Ghost,
        selected = selected,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
fun ArusNavigationBar(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(ArusTheme.colors.background)
            .border(BorderStroke(1.dp, ArusTheme.colors.border)),
        content = content,
    )
}

@Composable
fun RowScope.ArusNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        icon()
        ProvideArusTextStyle(
            ArusTheme.typography.label,
            if (selected) ArusTheme.colors.foreground else ArusTheme.colors.mutedForeground,
        ) { label() }
    }
}

@Composable
fun ArusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = false,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    testTag: String = "arus_text_input",
) {
    val colors = ArusTheme.colors
    val shape = RoundedCornerShape(ArusRadius.Medium)
    Row(
        modifier = modifier.defaultMinSize(minHeight = 42.dp)
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.border), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            if (value.isEmpty() && placeholder != null) {
                BasicText(placeholder, style = ArusTheme.typography.body.copy(color = colors.mutedForeground))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.testTag(testTag),
                enabled = !readOnly,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
                textStyle = ArusTheme.typography.body.copy(color = colors.foreground),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.foreground),
            )
        }
    }
}

// ArusCard is defined in ArusCoreComponents.kt

@Composable
fun ArusCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = ArusTheme.colors
    Box(
        modifier = modifier.defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
            .clip(RoundedCornerShape(ArusRadius.Small))
            .background(if (checked) colors.primary else colors.background)
            .border(2.dp, if (checked) colors.primary else colors.border, RoundedCornerShape(ArusRadius.Small))
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) BasicText("✓", style = ArusTheme.typography.label.copy(color = colors.onPrimary))
    }
}

@Composable
fun ArusDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.ui.window.Popup(onDismissRequest = onDismissRequest) {
        if (expanded) {
            Column(
                modifier = Modifier.clip(RoundedCornerShape(ArusRadius.Medium))
                    .background(ArusTheme.colors.surface)
                    .border(1.dp, ArusTheme.colors.border, RoundedCornerShape(ArusRadius.Medium))
                    .padding(vertical = 4.dp),
                content = content,
            )
        }
    }
}

@Composable
fun ArusDropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { ProvideArusTextStyle(ArusTheme.typography.body, if (enabled) ArusTheme.colors.foreground else ArusTheme.colors.mutedForeground) { text() } }
}
