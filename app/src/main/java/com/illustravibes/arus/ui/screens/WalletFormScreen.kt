package com.illustravibes.arus.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.ui.designsystem.ArusAmountInput
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusFilterChip
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class FormWalletPreset(val id: String, val name: String, val icon: String, val color: Int)

private fun loadWalletPresets(context: Context, type: String): List<FormWalletPreset> {
    return try {
        val json = context.assets.open("wallet-presets.json").bufferedReader().use { it.readText() }
        val presets = JSONObject(json).getJSONArray("presets")
        (0 until presets.length()).map { presets.getJSONObject(it) }
            .filter { it.optString("type") == type }
            .map {
                FormWalletPreset(
                    it.getString("id"),
                    it.getString("name"),
                    it.getString("icon"),
                    it.optString("colorArgb", "FF607D8B").toLong(16).toInt(),
                )
            }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * S-21 Form Dompet: Layar tambah atau edit dompet dengan pemilihan tipe,
 * preset populer, input nama, saldo awal, dan limit kredit jika kartu kredit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletFormScreen(
    database: ArusDatabase,
    wallet: WalletEntity?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = rememberArusHaptics()

    var type by remember(wallet?.id) { mutableStateOf(wallet?.type ?: "BANK") }
    var name by remember(wallet?.id) { mutableStateOf(wallet?.name.orEmpty()) }
    var balance by remember(wallet?.id) { mutableStateOf(wallet?.initialBalanceRupiah?.toString() ?: "0") }
    var limit by remember(wallet?.id) { mutableStateOf(wallet?.creditLimitRupiah?.toString().orEmpty()) }
    var billingDay by remember(wallet?.id) { mutableStateOf(wallet?.billingDay?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    val presets = remember(type) { loadWalletPresets(context, type) }

    fun submit() {
        val start = balance.toLongOrNull()
        val creditLimit = if (type == "CREDIT") limit.toLongOrNull() else null
        val day = if (type == "CREDIT" && billingDay.isNotBlank()) billingDay.toIntOrNull() else null

        if (name.isBlank() || start == null || start < 0 ||
            (type == "CREDIT" && (creditLimit == null || creditLimit <= 0)) ||
            (day != null && day !in 1..31)
        ) {
            error = "Periksa nama, saldo, batas kredit, dan hari tagihan."
            haptics.reject()
            return
        }

        scope.launch {
            withContext(Dispatchers.IO) {
                val target = wallet ?: WalletEntity(
                    name.trim(), type, "account_balance_wallet",
                    0xFF607D8B.toInt(), start, type != "CREDIT", false, null,
                )
                target.name = name.trim()
                target.type = type
                target.initialBalanceRupiah = start
                target.creditLimitRupiah = creditLimit
                target.billingDay = day
                target.includeInTotal = type != "CREDIT"

                if (wallet == null) {
                    target.sortOrder = database.walletDao().nextSortOrder()
                    database.walletDao().insert(target)
                } else {
                    database.walletDao().update(target)
                }
            }
            haptics.confirm()
            onSaved()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background)
            .imePadding(),
    ) {
        ArusTopBar(
            title = if (wallet == null) "Dompet baru" else "Edit dompet",
            onBackClick = onDismiss,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            // Type Selector
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                BasicText(
                    text = "Tipe Dompet",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("BANK" to "Bank", "EWALLET" to "E-Wallet", "CASH" to "Tunai", "CREDIT" to "Kredit").forEach { (valType, label) ->
                        ArusFilterChip(
                            label = label,
                            selected = type == valType,
                            onClick = {
                                haptics.tick()
                                type = valType
                            },
                        )
                    }
                }
            }

            // Presets if available
            if (presets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    BasicText(
                        text = "Preset Populer",
                        style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        presets.take(8).forEach { preset ->
                            ArusFilterChip(
                                label = preset.name,
                                selected = name == preset.name,
                                onClick = {
                                    haptics.tick()
                                    name = preset.name
                                },
                            )
                        }
                    }
                }
            }

            // Name Input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "Nama Dompet",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                ArusTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    placeholder = "Misal: BCA, GoPay, Tunai Dompet",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Initial Balance Input
            ArusAmountInput(
                value = balance,
                onValueChange = {
                    balance = it
                    error = null
                },
                showSign = false,
                label = "SALDO AWAL",
                modifier = Modifier.fillMaxWidth(),
            )

            // Credit specific fields
            if (type == "CREDIT") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(
                        text = "Batas Kredit (Limit)",
                        style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                    )
                    ArusTextField(
                        value = limit,
                        onValueChange = { limit = it.filter(Char::isDigit) },
                        placeholder = "Batas kredit (Rp)",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(
                        text = "Tanggal Tagihan Bulanan (1–31)",
                        style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                    )
                    ArusTextField(
                        value = billingDay,
                        onValueChange = { billingDay = it.filter(Char::isDigit).take(2) },
                        placeholder = "Misal: 25",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
}

