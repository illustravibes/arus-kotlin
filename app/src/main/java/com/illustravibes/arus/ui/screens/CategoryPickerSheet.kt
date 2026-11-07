package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.ui.designsystem.ArusBottomSheet
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusSearchField
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTheme

/**
 * S-37 Pilih Kategori: Bottom sheet untuk memilih kategori transaksi.
 */
@Composable
fun CategoryPickerSheet(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit,
    onNewCategory: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberArusHaptics()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    ArusBottomSheet(
        onDismissRequest = onDismiss,
        title = "Pilih kategori",
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ArusSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
        ) {
            ArusSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Cari kategori",
                modifier = Modifier.fillMaxWidth(),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { cat ->
                    val isSelected = cat.id == selectedCategoryId
                    ArusCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            haptics.tick()
                            onCategorySelected(cat)
                            onDismiss()
                        },
                        backgroundColor = if (isSelected) ArusTheme.colors.surfaceVariant else ArusTheme.colors.surface,
                        borderColor = if (isSelected) ArusTheme.colors.primary else ArusTheme.colors.border,
                        contentPadding = 12.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
                        ) {
                            val catColor = if (cat.colorArgb != 0) Color(cat.colorArgb) else ArusTheme.colors.muted

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(catColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                ArusIcon(
                                    key = cat.icon ?: ArusIconRegistry.CATEGORY,
                                    tint = if (cat.colorArgb != 0) catColor else ArusTheme.colors.foreground,
                                    size = 18.dp,
                                    contentDescription = cat.name,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                BasicText(
                                    text = cat.name,
                                    style = ArusTheme.typography.title.copy(color = ArusTheme.colors.foreground),
                                )
                                BasicText(
                                    text = if (cat.kind == "INCOME") "Pemasukan" else "Pengeluaran",
                                    style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                                )
                            }
                            if (isSelected) {
                                ArusIcon(
                                    key = ArusIconRegistry.CHECK,
                                    tint = ArusTheme.colors.primary,
                                    size = 18.dp,
                                )
                            }
                        }
                    }
                }
            }

            if (onNewCategory != null) {
                ArusSecondaryButton(
                    text = "+ Kategori baru",
                    onClick = {
                        onDismiss()
                        onNewCategory()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ArusSpacing.Small),
                )
            } else {
                Spacer(modifier = Modifier.height(ArusSpacing.Small))
            }
        }
    }
}
