package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.ui.designsystem.ArusBottomSheet
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTheme

/**
 * S-00a Menu Tambah (+): Quick action sheet triggered from center FAB.
 */
data class QuickActionItem(
    val title: String,
    val description: String,
    val iconKey: String,
    val onClick: () -> Unit,
)

@Composable
fun QuickAddActionSheet(
    onDismiss: () -> Unit,
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onTransfer: () -> Unit,
    onScanReceipt: () -> Unit,
    onGroupOrder: () -> Unit,
    onReconciliation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberArusHaptics()
    val items = listOf(
        QuickActionItem("Pengeluaran", "Catat belanja", ArusIconRegistry.EXPENSE, onExpense),
        QuickActionItem("Pemasukan", "Gaji, bonus", ArusIconRegistry.INCOME, onIncome),
        QuickActionItem("Transfer", "Antar dompet", ArusIconRegistry.TRANSFER, onTransfer),
        QuickActionItem("Scan struk", "Kamera atau galeri", ArusIconRegistry.CAMERA, onScanReceipt),
        QuickActionItem("Group order", "Talangan teman", ArusIconRegistry.GROUP_ORDER, onGroupOrder),
        QuickActionItem("Cocokkan saldo", "Cek selisih", ArusIconRegistry.RECONCILIATION, onReconciliation),
    )

    ArusBottomSheet(
        onDismissRequest = onDismiss,
        title = "Tambah apa?",
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
        ) {
            // Render as 2-column grid in rows of 2
            for (chunk in items.chunked(2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                ) {
                    for (item in chunk) {
                        ArusCard(
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.Button },
                            onClick = {
                                haptics.tick()
                                onDismiss()
                                item.onClick()
                            },
                            contentPadding = 14.dp,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ArusTheme.colors.muted),
                                contentAlignment = Alignment.Center,
                            ) {
                                ArusIcon(
                                    iconKey = item.iconKey,
                                    tint = ArusTheme.colors.foreground,
                                    size = 18.dp,
                                    contentDescription = item.title,
                                )
                            }
                            BasicText(
                                text = item.title,
                                style = ArusTheme.typography.title.copy(color = ArusTheme.colors.foreground),
                                modifier = Modifier.padding(top = ArusSpacing.Small),
                            )
                            BasicText(
                                text = item.description,
                                style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    if (chunk.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
