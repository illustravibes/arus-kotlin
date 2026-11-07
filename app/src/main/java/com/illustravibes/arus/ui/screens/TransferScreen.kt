package com.illustravibes.arus.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.BalanceRepository
import com.illustravibes.arus.data.TransactionDao
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.ui.ArusFormatting
import com.illustravibes.arus.ui.designsystem.AmountText
import com.illustravibes.arus.ui.designsystem.ArusAmountInput
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusFilterChip
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTertiaryButton
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * S-34 Transfer: Layar transfer antar dompet dengan kartu dompet asal, tombol tukar,
 * kartu dompet tujuan, input nominal, biaya admin, tanggal, dan tombol Simpan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransferScreen(
    database: ArusDatabase,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptics = rememberArusHaptics()

    val wallets = remember { mutableStateListOf<WalletEntity>() }
    val walletBalances = remember { mutableStateMapOf<Long, Long>() }

    var sourceId by rememberSaveable { mutableStateOf<Long?>(null) }
    var destinationId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amount by rememberSaveable { mutableStateOf("") }
    var fee by rememberSaveable { mutableStateOf("0") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var allowNegative by rememberSaveable { mutableStateOf(false) }
    var template by rememberSaveable { mutableStateOf("Tarik tunai") }

    var sourcePickerOpen by rememberSaveable { mutableStateOf(false) }
    var destinationPickerOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(database) {
        val values = withContext(Dispatchers.IO) {
            val w = database.walletDao().activeWallets()
            val wb = w.associate { wallet ->
                wallet.id to (BalanceRepository(database).balance(wallet.id, System.currentTimeMillis()) ?: 0L)
            }
            w to wb
        }
        wallets.clear()
        wallets.addAll(values.first)
        walletBalances.clear()
        walletBalances.putAll(values.second)

        val lastId = context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE)
            .getLong("last_wallet_id", -1L)
        sourceId = wallets.firstOrNull { it.id == lastId }?.id ?: wallets.firstOrNull()?.id
        destinationId = wallets.firstOrNull { it.id != sourceId }?.id
    }

    val sourceWallet = wallets.firstOrNull { it.id == sourceId }
    val destinationWallet = wallets.firstOrNull { it.id == destinationId }
    val occurred = runCatching {
        LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()

    fun submit() {
        val source = sourceId
        val destination = destinationId
        val value = amount.toLongOrNull()
        val feeValue = fee.toLongOrNull()

        if (source == destination) {
            error = "Dompet asal dan tujuan harus berbeda."
            return
        }
        if (source == null || destination == null || value == null || feeValue == null || occurred == null || value <= 0) {
            error = "Pilih dua dompet berbeda, nominal transfer positif, dan tanggal valid."
            return
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    database.transactionDao().insertTransfer(
                        source, destination, value, feeValue, occurred,
                        note.trim().ifEmpty { null }, allowNegative,
                    )
                }
            }
            result.fold(
                onSuccess = {
                    haptics.confirm()
                    context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE)
                        .edit().putLong("last_wallet_id", source).apply()
                    onDismiss()
                },
                onFailure = { failure ->
                    haptics.reject()
                    if (failure is TransactionDao.InsufficientBalanceException && !allowNegative) {
                        allowNegative = true
                        error = "Saldo asal tidak cukup, transfer akan membuat saldo minus. Tekan Simpan lagi untuk melanjutkan."
                    } else {
                        error = failure.message ?: "Transfer tidak dapat disimpan."
                    }
                },
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background)
            .imePadding(),
    ) {
        ArusTopBar(
            title = "Transfer",
            onBackClick = onDismiss,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            // Template quick options
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                BasicText(
                    text = "Template",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("Tarik tunai", "Bayar kredit", "Top up").forEach { choice ->
                        ArusFilterChip(
                            label = choice,
                            selected = template == choice,
                            onClick = {
                                template = choice
                                if (choice == "Bayar kredit") {
                                    destinationId = wallets.firstOrNull { it.type == "CREDIT" }?.id
                                }
                            },
                        )
                    }
                }
            }

            // Dari Wallet Card
            ArusCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { role = Role.Button },
                onClick = { sourcePickerOpen = true },
                contentPadding = 14.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        BasicText(
                            text = "Dari",
                            style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                        )
                        BasicText(
                            text = sourceWallet?.name ?: "Pilih dompet asal",
                            style = ArusTheme.typography.title,
                        )
                    }
                    sourceWallet?.let {
                        val bal = walletBalances[it.id] ?: 0L
                        AmountText(amount = bal, style = ArusTheme.typography.title)
                    }
                }
            }

            // Swap Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ArusSecondaryButton(
                    text = "⇅ Tukar dompet",
                    onClick = {
                        haptics.tick()
                        val prevSource = sourceId
                        sourceId = destinationId
                        destinationId = prevSource
                    },
                )
            }

            // Ke Wallet Card
            ArusCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { role = Role.Button },
                onClick = { destinationPickerOpen = true },
                contentPadding = 14.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        BasicText(
                            text = if (destinationWallet?.type == "CREDIT") "Bayar tagihan · Ke kartu kredit" else "Ke",
                            style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                        )
                        BasicText(
                            text = destinationWallet?.name ?: "Pilih dompet tujuan",
                            style = ArusTheme.typography.title,
                        )
                    }
                    destinationWallet?.let {
                        val bal = walletBalances[it.id] ?: 0L
                        AmountText(amount = bal, style = ArusTheme.typography.title)
                    }
                }
            }

            if (destinationWallet?.type == "CREDIT") {
                ArusTertiaryButton(
                    text = "Bayar penuh (terpakai saat ini)",
                    onClick = {
                        val destId = destinationId ?: return@ArusTertiaryButton
                        scope.launch {
                            val used = withContext(Dispatchers.IO) {
                                BalanceRepository(database).creditUsed(destId, System.currentTimeMillis()) ?: 0L
                            }
                            amount = used.coerceAtLeast(0L).toString()
                        }
                    },
                )
            }

            // Nominal Input
            ArusAmountInput(
                value = amount,
                onValueChange = {
                    amount = it
                    error = null
                },
                showSign = false,
                label = "NOMINAL TRANSFER",
                testTag = "transfer_amount_input",
                modifier = Modifier.fillMaxWidth(),
            )

            // Biaya Admin Input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "Biaya admin",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                ArusTextField(
                    value = fee,
                    onValueChange = {
                        fee = it.filter(Char::isDigit)
                        error = null
                    },
                    placeholder = "Rp 0",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Tanggal Selector Row
            ArusCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { role = Role.Button },
                onClick = {
                    val initial = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() },
                        initial.year,
                        initial.monthValue - 1,
                        initial.dayOfMonth,
                    ).show()
                },
                contentPadding = 14.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        BasicText(
                            text = "Tanggal",
                            style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                        )
                        BasicText(
                            text = if (date == LocalDate.now().toString()) "Hari ini ($date)" else date,
                            style = ArusTheme.typography.title,
                        )
                    }
                    ArusIcon(
                        key = ArusIconRegistry.CALENDAR,
                        tint = ArusTheme.colors.foreground,
                        size = 18.dp,
                    )
                }
            }

            // Catatan
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "Catatan",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                ArusTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Catatan transfer (opsional)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            error?.let {
                BasicText(
                    text = it,
                    style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error),
                )
            }
        }

        // Bottom Simpan Button
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

    if (sourcePickerOpen) {
        WalletPickerSheet(
            wallets = wallets,
            walletBalances = walletBalances,
            selectedWalletId = sourceId,
            onWalletSelected = {
                sourceId = it.id
                error = null
            },
            onDismiss = { sourcePickerOpen = false },
            title = "Pilih dompet asal",
        )
    }

    if (destinationPickerOpen) {
        WalletPickerSheet(
            wallets = wallets,
            walletBalances = walletBalances,
            selectedWalletId = destinationId,
            onWalletSelected = {
                destinationId = it.id
                error = null
            },
            onDismiss = { destinationPickerOpen = false },
            title = "Pilih dompet tujuan",
        )
    }
}
