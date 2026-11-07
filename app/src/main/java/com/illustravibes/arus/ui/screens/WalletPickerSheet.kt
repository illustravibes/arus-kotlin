package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.ui.designsystem.AmountText
import com.illustravibes.arus.ui.designsystem.ArusBottomSheet
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTheme

/**
 * S-38 Pilih Dompet: Bottom sheet untuk memilih dompet transaksi / transfer.
 */
@Composable
fun WalletPickerSheet(
    wallets: List<WalletEntity>,
    walletBalances: Map<Long, Long>,
    selectedWalletId: Long?,
    onWalletSelected: (WalletEntity) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Pilih dompet",
    modifier: Modifier = Modifier,
) {
    val haptics = rememberArusHaptics()
    ArusBottomSheet(
        onDismissRequest = onDismiss,
        title = title,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ArusSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(wallets, key = { it.id }) { wallet ->
                    val isSelected = wallet.id == selectedWalletId
                    val balance = walletBalances[wallet.id] ?: 0L
                    val iconKey = when (wallet.type.uppercase()) {
                        "BANK" -> ArusIconRegistry.BANK
                        "E-WALLET", "EWALLET" -> ArusIconRegistry.WALLET
                        "CASH", "TUNAI" -> ArusIconRegistry.CASH
                        "CREDIT", "KREDIT" -> ArusIconRegistry.CREDIT_CARD
                        else -> ArusIconRegistry.WALLET
                    }

                    ArusCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            haptics.tick()
                            onWalletSelected(wallet)
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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ArusTheme.colors.muted),
                                contentAlignment = Alignment.Center,
                            ) {
                                ArusIcon(
                                    key = iconKey,
                                    tint = ArusTheme.colors.foreground,
                                    size = 18.dp,
                                    contentDescription = wallet.name,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                BasicText(
                                    text = wallet.name,
                                    style = ArusTheme.typography.title.copy(color = ArusTheme.colors.foreground),
                                )
                                BasicText(
                                    text = wallet.type,
                                    style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                                )
                            }
                            AmountText(
                                amount = balance,
                                style = ArusTheme.typography.title,
                            )
                        }
                    }
                }
            }

            BasicText(
                text = "Dompet terarsip tidak ditampilkan",
                style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ArusSpacing.Small),
            )
        }
    }
}
