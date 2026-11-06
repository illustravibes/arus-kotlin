package com.illustravibes.arus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText as Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.illustravibes.arus.data.ContactEntity
import com.illustravibes.arus.data.DebtEntity
import com.illustravibes.arus.data.DebtDao
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusButtonKind
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.OutlinedArusButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DebtManagement(database: ArusDatabase) {
    val scope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf(emptyList<ContactEntity>()) }
    var debts by remember { mutableStateOf(emptyList<DebtEntity>()) }
    var remainingByDebt by remember { mutableStateOf(emptyMap<Long, Long>()) }
    var wallets by remember { mutableStateOf(emptyList<WalletEntity>()) }
    var categories by remember { mutableStateOf(emptyList<com.illustravibes.arus.data.CategoryEntity>()) }
    var addDebtOpen by remember { mutableStateOf(false) }
    var selectedContactId by remember { mutableStateOf<Long?>(null) }
    var settlementDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        val values = withContext(Dispatchers.IO) {
            listOf(database.debtDao().activeContacts(), database.debtDao().activeDebts(),
                database.walletDao().activeWallets(), database.categoryDao().activeCategories(),
                database.debtDao().activeDebts().associate { it.id to database.debtDao().remainingPrincipal(it.id) })
        }
        @Suppress("UNCHECKED_CAST")
        contacts = values[0] as List<ContactEntity>
        @Suppress("UNCHECKED_CAST")
        debts = values[1] as List<DebtEntity>
        @Suppress("UNCHECKED_CAST")
        wallets = values[2] as List<WalletEntity>
        @Suppress("UNCHECKED_CAST")
        categories = values[3] as List<com.illustravibes.arus.data.CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        remainingByDebt = values[4] as Map<Long, Long>
    }

    LaunchedEffect(database) { refresh() }

    Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
        Text("Utang-Piutang", style = ArusTheme.typography.headline)
        ArusButton(onClick = { addDebtOpen = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Tambah catatan utang")
        }
        message?.let { Text(it, style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error)) }
        if (contacts.isEmpty()) {
            ArusCard(modifier = Modifier.fillMaxWidth()) {
                Text("Belum ada catatan utang.", modifier = Modifier.padding(ArusSpacing.Medium))
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
            items(contacts, key = { it.id }) { contact ->
                val contactDebts = debts.filter { it.contactId == contact.id }
                val lent = contactDebts.filter { it.direction == "LENT" && it.status in listOf("OPEN", "PARTIAL") }
                val borrowed = contactDebts.filter { it.direction == "BORROWED" && it.status in listOf("OPEN", "PARTIAL") }
                ArusCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(ArusSpacing.Medium), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                        Text(contact.name, style = ArusTheme.typography.title)
                        OutlinedArusButton(onClick = { selectedContactId = contact.id }, modifier = Modifier.fillMaxWidth()) {
                            Text("Detail kontak")
                        }
                        contactDebts.forEach { debt ->
                            val remaining = remainingByDebt[debt.id] ?: debt.principalRupiah
                            Text("${if (debt.direction == "LENT") "Piutang" else "Utang"} · ${formatRupiah(remaining)} · ${debt.status}")
                            if (debt.status == "OPEN" || debt.status == "PARTIAL") {
                                Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                                    ArusButton(onClick = { settlementDebt = debt }, kind = ArusButtonKind.Outline) {
                                        Text(if (debt.direction == "LENT") "Catat pelunasan" else "Bayar cicilan")
                                    }
                                    OutlinedArusButton(onClick = {
                                        scope.launch {
                                            runCatching { withContext(Dispatchers.IO) {
                                                database.debtDao().writeOff(debt.id, System.currentTimeMillis(), "Sisa utang diikhlaskan")
                                            } }.onSuccess { message = "Sisa utang diselesaikan."; refresh() }
                                                .onFailure { message = it.message ?: "Utang gagal diselesaikan." }
                                        }
                                    }) { Text("Ikhlaskan") }
                                }
                            }
                        }
                        if (lent.isNotEmpty() && borrowed.isNotEmpty()) {
                            ArusButton(onClick = {
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) {
                                        database.debtDao().netDebts(lent.first().id, borrowed.first().id, System.currentTimeMillis())
                                    } }.onSuccess { message = "Netting berhasil disimpan."; refresh() }
                                        .onFailure { message = it.message ?: "Netting gagal." }
                                }
                            }, kind = ArusButtonKind.Outline) { Text("Saling hapus") }
                        }
                    }
                }
            }
        }
    }

    if (addDebtOpen) AddDebtDialog(
        contacts = contacts,
        wallets = wallets,
        onDismiss = { addDebtOpen = false },
        onSave = { contactName, direction, principal, walletId, note ->
            scope.launch {
                runCatching { withContext(Dispatchers.IO) {
                    val normalizedName = contactName.trim()
                    val contact = database.debtDao().activeContacts().firstOrNull { it.name.equals(normalizedName, true) }
                        ?: ContactEntity(normalizedName, null, null, false, System.currentTimeMillis(), System.currentTimeMillis())
                            .let { newContact -> database.debtDao().insertContact(newContact); database.debtDao().activeContacts().first { it.name.equals(normalizedName, true) } }
                    database.debtDao().createDebt(contact.id, direction, principal, walletId,
                        System.currentTimeMillis(), null, note.ifBlank { null }, System.currentTimeMillis())
                } }.onSuccess { addDebtOpen = false; message = "Catatan utang tersimpan."; refresh() }
                    .onFailure { message = it.message ?: "Catatan utang gagal disimpan." }
            }
        },
    )

    selectedContactId?.let { contactId ->
        val contact = contacts.firstOrNull { it.id == contactId }
        if (contact != null) {
            val contactDebts = debts.filter { it.contactId == contactId }
            val receivable = contactDebts.filter { it.direction == "LENT" && it.status in listOf("OPEN", "PARTIAL") }
                .sumOf { remainingByDebt[it.id] ?: 0L }
            val payable = contactDebts.filter { it.direction == "BORROWED" && it.status in listOf("OPEN", "PARTIAL") }
                .sumOf { remainingByDebt[it.id] ?: 0L }
            ArusDialog(
                onDismissRequest = { selectedContactId = null },
                title = { Text(contact.name) },
                text = {
                    Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                        Text("Total piutang · ${formatRupiah(receivable)}")
                        Text("Total utang · ${formatRupiah(payable)}")
                        Text("RIWAYAT CATATAN", style = ArusTheme.typography.label)
                        if (contactDebts.isEmpty()) Text("Belum ada catatan untuk kontak ini.")
                        contactDebts.forEach { debt ->
                            ArusCard(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(ArusSpacing.Small), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${if (debt.direction == "LENT") "Piutang" else "Utang"} · ${debt.status}")
                                    Text("Pokok · ${formatRupiah(debt.principalRupiah)}")
                                    Text("Sisa · ${formatRupiah(remainingByDebt[debt.id] ?: 0L)}")
                                    debt.note?.takeIf(String::isNotBlank)?.let { Text(it) }
                                }
                            }
                        }
                    }
                },
                confirmButton = { OutlinedArusButton(onClick = { selectedContactId = null }) { Text("Tutup") } },
            )
        }
    }

    settlementDebt?.let { debt ->
        val contact = contacts.firstOrNull { it.id == debt.contactId }
        val settlementDebts = debts.filter {
            it.contactId == debt.contactId && it.direction == debt.direction && it.status in listOf("OPEN", "PARTIAL")
        }
        SettlementDialog(
            contactName = contact?.name.orEmpty(),
            debts = settlementDebts,
            remainingByDebt = remainingByDebt,
            wallets = wallets,
            categories = categories,
            onDismiss = { settlementDebt = null },
            onSave = { amount, walletId, overpaymentResolution, categoryId, allocations ->
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) {
                        database.debtDao().recordSettlement(debt.contactId, debt.direction, walletId, amount,
                            allocations, overpaymentResolution,
                            categoryId, System.currentTimeMillis(), null, System.currentTimeMillis())
                    } }.onSuccess { settlementDebt = null; message = "Pelunasan tersimpan."; refresh() }
                        .onFailure { message = it.message ?: "Pelunasan gagal disimpan." }
                }
            },
        )
    }
}

@Composable
private fun AddDebtDialog(
    contacts: List<ContactEntity>,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long?, String) -> Unit,
) {
    var contactName by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("LENT") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var walletId by remember { mutableStateOf(wallets.firstOrNull()?.id) }
    var withWallet by remember { mutableStateOf(wallets.isNotEmpty()) }
    ArusDialog(onDismissRequest = onDismiss, title = { Text("Tambah catatan utang") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ArusTextField(contactName, { contactName = it }, placeholder = "Nama kontak", singleLine = true)
            contacts.take(4).forEach { contact ->
                OutlinedArusButton(onClick = { contactName = contact.name }) { Text(contact.name) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArusButton(onClick = { direction = "LENT" }, kind = if (direction == "LENT") ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text("Piutang") }
                ArusButton(onClick = { direction = "BORROWED" }, kind = if (direction == "BORROWED") ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text("Utang") }
            }
            ArusTextField(amount, { amount = it.filter(Char::isDigit) }, placeholder = "Nominal pokok (Rp)", singleLine = true)
            ArusTextField(note, { note = it }, placeholder = "Catatan (opsional)", singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArusButton(onClick = { withWallet = true }, kind = if (withWallet) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text("Dengan dompet") }
                ArusButton(onClick = { withWallet = false }, kind = if (!withWallet) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text("Catat saja") }
            }
            if (withWallet) wallets.forEach { wallet ->
                ArusButton(onClick = { walletId = wallet.id }, kind = if (walletId == wallet.id) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(wallet.name) }
            }
        }
    }, confirmButton = {
        ArusButton(onClick = {
            val parsedAmount = amount.toLongOrNull() ?: return@ArusButton
            onSave(contactName, direction, parsedAmount, if (withWallet) walletId else null, note)
        }) { Text("Simpan") }
    }, dismissButton = { OutlinedArusButton(onClick = onDismiss) { Text("Batal") } })
}

@Composable
internal fun SettlementDialog(
    contactName: String,
    debts: List<DebtEntity>,
    remainingByDebt: Map<Long, Long>,
    wallets: List<WalletEntity>,
    categories: List<com.illustravibes.arus.data.CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, Long, String?, Long?, List<DebtDao.AllocationRequest>) -> Unit,
) {
    val direction = debts.firstOrNull()?.direction ?: "LENT"
    var amount by remember(contactName, direction) { mutableStateOf("") }
    var walletId by remember(contactName, direction) { mutableStateOf(wallets.firstOrNull()?.id) }
    var resolution by remember(contactName, direction) { mutableStateOf<String?>(null) }
    val paidAmount = amount.toLongOrNull() ?: 0L
    var unallocated = paidAmount
    val allocations = debts.sortedBy { it.createdAtEpochMillis }.mapNotNull { debt ->
        val allocation = minOf(unallocated, remainingByDebt[debt.id] ?: debt.principalRupiah)
        if (allocation <= 0) null else DebtDao.AllocationRequest(debt.id, allocation).also { unallocated -= allocation }
    }
    val excess = unallocated.coerceAtLeast(0L)
    val remaining = debts.sumOf { remainingByDebt[it.id] ?: it.principalRupiah }
    val kind = if (direction == "LENT") "INCOME" else "EXPENSE"
    val categoryId = if (direction == "LENT") {
        categories.firstOrNull { it.kind == kind && !it.isSystem }?.id
    } else {
        categories.firstOrNull { it.kind == kind && it.isSystem && it.name == "Ikhlas" }?.id
    }
    ArusDialog(onDismissRequest = onDismiss, title = { Text("Pelunasan · $contactName") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sisa catatan terpilih · ${formatRupiah(remaining)}")
            val allocatedByDebt = allocations.associate { it.debtId to it.amountRupiah }
            debts.sortedBy { it.createdAtEpochMillis }.forEach { debt ->
                Text("${debt.note?.takeIf(String::isNotBlank) ?: "Catatan utang"} · ${formatRupiah(allocatedByDebt[debt.id] ?: 0L)} dari ${formatRupiah(remainingByDebt[debt.id] ?: debt.principalRupiah)}")
            }
            ArusTextField(amount, { amount = it.filter(Char::isDigit); resolution = null }, placeholder = "Nominal dibayar (Rp)", singleLine = true)
            wallets.forEach { wallet ->
                ArusButton(onClick = { walletId = wallet.id }, kind = if (walletId == wallet.id) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(wallet.name) }
            }
            if (paidAmount in 1 until remaining) Text("Sisa tetap tercatat sebagai utang.")
            if (excess > 0) {
                Text("Kelebihan · ${formatRupiah(excess)}")
                val options = if (direction == "LENT") listOf("REFUND" to "Kembalikan", "FORGIVE" to "Ikhlaskan sebagai pemasukan")
                    else listOf("FORGIVE" to "Catat sebagai pengeluaran", "CREATE_RECEIVABLE" to "Jadikan piutang baru")
                options.forEach { (value, label) ->
                    ArusButton(onClick = { resolution = value }, kind = if (resolution == value) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(label) }
                }
            }
        }
    }, confirmButton = {
        ArusButton(onClick = {
            val parsed = amount.toLongOrNull() ?: return@ArusButton
            if (parsed <= 0 || walletId == null || (excess > 0 && (resolution == null || (resolution == "FORGIVE" && categoryId == null)))) return@ArusButton
            onSave(parsed, walletId!!, resolution, if (resolution == "FORGIVE") categoryId else null, allocations)
        }) { Text("Simpan") }
    }, dismissButton = { OutlinedArusButton(onClick = onDismiss) { Text("Batal") } })
}
