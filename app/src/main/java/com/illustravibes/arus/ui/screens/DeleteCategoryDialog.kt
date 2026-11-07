package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.ui.designsystem.ArusBottomSheet
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusFilterChip
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * S-42 Hapus Kategori: Bottom sheet untuk memindahkan transaksi ke kategori lain
 * atau menghapus kategori sebelum penghapusan dikonfirmasi.
 */
@Composable
fun DeleteCategoryDialog(
    database: ArusDatabase,
    sourceCategory: CategoryEntity,
    allCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberArusHaptics()

    var moveTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteWithoutMove by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val candidates = allCategories.filter {
        it.kind == sourceCategory.kind && it.id != sourceCategory.id && !it.isArchived
    }

    LaunchedEffect(sourceCategory.id) {
        val hasReferences = withContext(Dispatchers.IO) {
            database.categoryDao().activeTransactionCount(sourceCategory.id) > 0 ||
                database.categoryDao().splitReferenceCount(sourceCategory.id) > 0 ||
                database.categoryDao().walletReferenceCount(sourceCategory.id) > 0
        }
        if (!hasReferences) {
            deleteWithoutMove = true
        }
    }

    fun confirmDelete() {
        if (!deleteWithoutMove && moveTarget == null) {
            error = "Pilih kategori tujuan atau 'Tanpa kategori'."
            haptics.reject()
            return
        }

        scope.launch {
            val failure = withContext(Dispatchers.IO) {
                var message: String? = null
                database.runInTransaction {
                    val dao = database.categoryDao()
                    val sourceNow = dao.findById(sourceCategory.id)
                    if (sourceNow == null || sourceNow.isSystem) {
                        message = "Kategori sistem tidak dapat dihapus."
                    } else {
                        moveTarget?.let { dao.moveTransactions(sourceCategory.id, it.id) }
                        if (deleteWithoutMove) dao.clearAllTransactionCategories(sourceCategory.id)
                        dao.clearWalletDefaults(sourceCategory.id)
                        if (dao.walletReferenceCount(sourceCategory.id) > 0) {
                            message = "Kategori masih dipakai sebagai default dompet."
                        } else if (dao.deleteCustom(sourceCategory.id) != 1) {
                            message = "Kategori gagal dihapus."
                        }
                    }
                }
                message
            }

            if (failure != null) {
                error = failure
                haptics.reject()
            } else {
                haptics.confirm()
                onDeleted()
            }
        }
    }

    ArusBottomSheet(
        onDismissRequest = onDismiss,
        title = "Hapus ${sourceCategory.name}?",
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            BasicText(
                text = "Pindahkan transaksi yang ada ke kategori lain, atau biarkan tanpa kategori.",
                style = ArusTheme.typography.body.copy(color = ArusTheme.colors.mutedForeground),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                candidates.forEach { candidate ->
                    ArusCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            haptics.tick()
                            moveTarget = candidate
                            deleteWithoutMove = false
                        },
                        backgroundColor = if (moveTarget?.id == candidate.id) ArusTheme.colors.surfaceVariant else ArusTheme.colors.surface,
                        borderColor = if (moveTarget?.id == candidate.id) ArusTheme.colors.primary else ArusTheme.colors.border,
                        contentPadding = 12.dp,
                    ) {
                        BasicText(
                            text = candidate.name,
                            style = ArusTheme.typography.title,
                        )
                    }
                }

                ArusCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        haptics.tick()
                        moveTarget = null
                        deleteWithoutMove = true
                    },
                    backgroundColor = if (deleteWithoutMove) ArusTheme.colors.surfaceVariant else ArusTheme.colors.surface,
                    borderColor = if (deleteWithoutMove) ArusTheme.colors.primary else ArusTheme.colors.border,
                    contentPadding = 12.dp,
                ) {
                    BasicText(
                        text = "Tanpa kategori",
                        style = ArusTheme.typography.title,
                    )
                }
            }

            error?.let {
                BasicText(
                    text = it,
                    style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ArusSecondaryButton(
                    text = "Batal",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                ArusPrimaryButton(
                    text = "Konfirmasi hapus",
                    onClick = { confirmDelete() },
                    destructive = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
