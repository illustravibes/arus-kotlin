package com.illustravibes.arus.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSegmentedControl
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CATEGORY_COLOR_PALETTE = listOf(
    0xFF0009FF.toInt(), // Brand Primary
    0xFF16A34A.toInt(), // Income Green
    0xFFDC2626.toInt(), // Expense Red
    0xFFD97706.toInt(), // Warning Amber
    0xFF2563EB.toInt(), // Blue
    0xFF7C3AED.toInt(), // Purple
    0xFFDB2777.toInt(), // Pink
    0xFF0D9488.toInt(), // Teal
    0xFFEA580C.toInt(), // Orange
    0xFF71717A.toInt(), // Zinc Neutral
    0xFF18181B.toInt(), // Dark Zinc
)

/**
 * S-41 Form Kategori: Layar tambah atau edit kategori dengan input nama, jenis,
 * grid visual pemilih warna, dan grid visual pemilih ikon Font Awesome.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryFormScreen(
    database: ArusDatabase,
    category: CategoryEntity?,
    initialKind: String = "EXPENSE",
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberArusHaptics()

    var name by remember(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var kind by remember(category?.id) { mutableStateOf(category?.kind ?: initialKind) }
    var icon by remember(category?.id) { mutableStateOf(category?.icon ?: "tag") }
    var colorArgb by remember(category?.id) {
        mutableStateOf(category?.colorArgb ?: CATEGORY_COLOR_PALETTE.first())
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (name.isBlank()) {
            error = "Nama kategori tidak boleh kosong."
            haptics.reject()
            return
        }

        scope.launch {
            val success = withContext(Dispatchers.IO) {
                val dao = database.categoryDao()
                if (category != null) {
                    if (dao.countName(kind, name.trim(), category.id) > 0) false
                    else dao.editCustom(category.id, name.trim(), icon, colorArgb) > 0
                } else {
                    if (dao.countName(kind, name.trim()) > 0) false
                    else {
                        val count = dao.allCategories().size
                        dao.insert(CategoryEntity(name.trim(), kind, icon, colorArgb, false, false, count)) > 0
                    }
                }
            }

            if (success) {
                haptics.confirm()
                onSaved()
            } else {
                haptics.reject()
                error = "Kategori gagal disimpan. Nama mungkin sudah dipakai."
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background)
            .imePadding(),
    ) {
        ArusTopBar(
            title = if (category == null) "Kategori baru" else "Edit kategori",
            onBackClick = onDismiss,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Large),
        ) {
            // Live Preview Card
            ArusCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val previewColor = Color(colorArgb)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(previewColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        ArusIcon(key = icon, size = 22.dp, tint = previewColor)
                    }
                    Column {
                        BasicText(
                            text = name.ifBlank { "Nama Kategori" },
                            style = ArusTheme.typography.title,
                        )
                        BasicText(
                            text = if (kind == "INCOME") "Pemasukan" else "Pengeluaran",
                            style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                        )
                    }
                }
            }

            // Segmented Kind Selector
            if (category == null) {
                Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    BasicText(
                        text = "Jenis Kategori",
                        style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                    )
                    ArusSegmentedControl(
                        options = listOf("Pengeluaran", "Pemasukan"),
                        selectedIndex = if (kind == "EXPENSE") 0 else 1,
                        onOptionSelected = { idx ->
                            kind = if (idx == 0) "EXPENSE" else "INCOME"
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Name Input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "Nama Kategori",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                ArusTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    placeholder = "Misal: Makan Siang, Ojek, Netflix",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Visual Color Picker Grid
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                BasicText(
                    text = "Pilih Warna",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CATEGORY_COLOR_PALETTE.forEach { paletteArgb ->
                        val isSelected = paletteArgb == colorArgb
                        val c = Color(paletteArgb)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, ArusTheme.colors.foreground, CircleShape)
                                    else Modifier
                                )
                                .clickable {
                                    haptics.tick()
                                    colorArgb = paletteArgb
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                ArusIcon(key = ArusIconRegistry.CHECK, size = 16.dp, tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Visual Icon Picker Grid
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                BasicText(
                    text = "Pilih Ikon",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ArusIconRegistry.categoryIconCandidates.forEach { candidateKey ->
                        val isSelected = candidateKey == icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) ArusTheme.colors.foreground else ArusTheme.colors.surfaceSecondary)
                                .clickable {
                                    haptics.tick()
                                    icon = candidateKey
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            ArusIcon(
                                key = candidateKey,
                                size = 20.dp,
                                tint = if (isSelected) ArusTheme.colors.background else ArusTheme.colors.foreground,
                            )
                        }
                    }
                }
            }

            error?.let {
                BasicText(
                    text = it,
                    style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error),
                )
            }
        }

        // Bottom Action Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArusTheme.colors.surface)
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Medium),
        ) {
            ArusPrimaryButton(
                text = "Simpan",
                onClick = ::submit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

