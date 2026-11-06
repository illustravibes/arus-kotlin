package com.illustravibes.arus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.data.ContactEntity
import com.illustravibes.arus.data.DebtEntity
import com.illustravibes.arus.data.GroupOrderEntity
import com.illustravibes.arus.data.GroupOrderFeeEntity
import com.illustravibes.arus.data.GroupOrderFeeShareEntity
import com.illustravibes.arus.data.GroupOrderItemEntity
import com.illustravibes.arus.data.GroupOrderOwnerEntity
import com.illustravibes.arus.data.GroupOrderShareEntity
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.domain.GroupFeeSplitMode
import com.illustravibes.arus.domain.GroupOrderCalculator
import com.illustravibes.arus.domain.GroupOrderFeeInput
import com.illustravibes.arus.domain.GroupOrderItemInput
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusButtonKind
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.Text as Text
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.OutlinedArusButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class GroupOrderDraftContent(
    val draft: GroupOrderEntity,
    val items: List<GroupOrderItemEntity>,
    val ownersByItem: Map<Long, List<GroupOrderOwnerEntity>>,
    val fees: List<GroupOrderFeeEntity>,
    val feeSharesById: Map<Long, List<GroupOrderFeeShareEntity>>,
)

private data class GroupOrderListContent(
    val drafts: List<GroupOrderEntity>,
    val orders: List<GroupOrderEntity>,
    val contacts: List<ContactEntity>,
    val wallets: List<WalletEntity>,
)

@Composable
internal fun GroupOrderManagement(
    database: ArusDatabase,
    initialDraftId: Long? = null,
    onInitialDraftConsumed: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var drafts by remember { mutableStateOf(emptyList<GroupOrderEntity>()) }
    var orders by remember { mutableStateOf(emptyList<GroupOrderEntity>()) }
    var contacts by remember { mutableStateOf(emptyList<ContactEntity>()) }
    var wallets by remember { mutableStateOf(emptyList<WalletEntity>()) }
    var categories by remember { mutableStateOf(emptyList<CategoryEntity>()) }
    var selectedDraft by remember { mutableStateOf<GroupOrderDraftContent?>(null) }
    var selectedOrder by remember { mutableStateOf<GroupOrderEntity?>(null) }
    var step by remember { mutableStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var payerWalletId by remember { mutableStateOf<Long?>(null) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var roundingBearer by remember { mutableStateOf<Long?>(null) }
    val participantIds = remember { mutableStateListOf<Long>() }
    var participantName by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    var itemQuantity by remember { mutableStateOf("1") }
    val itemOwners = remember { mutableStateListOf<Long?>() }
    val manualOwnerAmounts = remember { mutableStateMapOf<Long?, String>() }
    var itemUsesCustomShares by remember { mutableStateOf(false) }
    var feeLabel by remember { mutableStateOf("") }
    var feeAmount by remember { mutableStateOf("") }
    var feeIsDiscount by remember { mutableStateOf(false) }
    var feeMode by remember { mutableStateOf(GroupFeeSplitMode.EQUAL) }
    val manualFeeAmounts = remember { mutableStateMapOf<Long?, String>() }
    var completed by remember { mutableStateOf(false) }

    suspend fun refreshDrafts() {
        val values = withContext(Dispatchers.IO) {
            GroupOrderListContent(database.groupOrderDao().drafts(), database.groupOrderDao().allOrders(),
                database.debtDao().activeContacts(), database.walletDao().activeWallets())
        }
        drafts = values.drafts
        orders = values.orders
        contacts = values.contacts
        wallets = values.wallets
        if (payerWalletId == null) payerWalletId = wallets.firstOrNull()?.id
        if (categoryId == null) categoryId = categories.firstOrNull { it.kind == "EXPENSE" }?.id
    }

    suspend fun loadDraft(id: Long, resetStep: Boolean = true, initialStep: Int = 1) {
        val content = withContext(Dispatchers.IO) {
            val draft = database.groupOrderDao().find(id) ?: return@withContext null
            val items = database.groupOrderDao().items(id)
            val owners = items.associate { it.id to database.groupOrderDao().owners(it.id) }
            val fees = database.groupOrderDao().fees(id)
            val feeShares = fees.associate { it.id to database.groupOrderDao().feeShares(it.id) }
            GroupOrderDraftContent(draft, items, owners, fees, feeShares)
        } ?: return
        selectedDraft = content
        title = content.draft.title
        merchant = content.draft.merchant.orEmpty()
        payerWalletId = content.draft.payerWalletId
        categoryId = content.draft.categoryId
        roundingBearer = content.draft.roundingBearer.toLongOrNull()
        participantIds.clear()
        content.ownersByItem.values.flatten().mapNotNull { it.contactId }.distinct().forEach(participantIds::add)
        if (resetStep) step = initialStep
        completed = false
        error = null
    }

    LaunchedEffect(database, initialDraftId) {
        categories = withContext(Dispatchers.IO) { database.categoryDao().activeCategories() }
        refreshDrafts()
        initialDraftId?.let { id ->
            loadDraft(id, initialStep = 3)
            onInitialDraftConsumed()
        }
    }

    val current = selectedDraft
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
        if (selectedOrder == null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (current == null) "Group Order" else current.draft.title, style = ArusTheme.typography.headline)
                if (current == null) ArusButton(onClick = {
                    val wallet = payerWalletId
                    val category = categoryId
                    if (wallet == null || category == null) { error = "Tambahkan dompet dan kategori pengeluaran terlebih dahulu."; return@ArusButton }
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) {
                            val now = System.currentTimeMillis()
                            database.groupOrderDao().createDraft(GroupOrderEntity("Group Order", null, now,
                                wallet, category, 0, 0, 0, 0, null, "ME", "DRAFT", now, now, null))
                        } }.onSuccess { id -> refreshDrafts(); loadDraft(id) }
                            .onFailure { error = it.message ?: "Draft gagal dibuat." }
                    }
                }) { Text("Buat draft") }
            }
        }
        error?.let { Text(it, style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error)) }

        if (selectedOrder != null) {
            GroupOrderDetail(
                database = database,
                order = selectedOrder!!,
                contacts = contacts,
                wallets = wallets,
                categories = categories,
                onBack = { selectedOrder = null; scope.launch { refreshDrafts() } },
                onChanged = { scope.launch { refreshDrafts() } },
            )
        } else if (current == null) {
            if (drafts.isEmpty()) Text("Belum ada draft group order.")
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                drafts.forEach { draft ->
                    ArusCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(ArusSpacing.Medium), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(draft.title)
                            ArusButton(onClick = { scope.launch { loadDraft(draft.id) } }, kind = ArusButtonKind.Outline) { Text("Lanjutkan") }
                        }
                    }
                }
            }
            if (orders.isNotEmpty()) Text("Order final", style = ArusTheme.typography.title)
            orders.forEach { order ->
                ArusCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(ArusSpacing.Medium), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(order.title)
                            Text("${order.status} · ${formatRupiah(order.grandTotalRupiah)}", style = ArusTheme.typography.label)
                        }
                        ArusButton(onClick = { selectedOrder = order }, kind = ArusButtonKind.Outline) { Text("Detail") }
                    }
                }
            }
        } else if (completed) {
            Text("Group order berhasil difinalisasi.")
            ArusButton(onClick = { selectedDraft = null; scope.launch { refreshDrafts() } }) { Text("Kembali") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                (1..5).forEach { index ->
                    ArusButton(onClick = { if (index <= step) step = index }, kind = if (step == index) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(index.toString()) }
                }
            }
            when (step) {
                1 -> Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Text("Info order", style = ArusTheme.typography.title)
                    ArusTextField(title, { title = it }, placeholder = "Judul", singleLine = true)
                    ArusTextField(merchant, { merchant = it }, placeholder = "Merchant", singleLine = true)
                    wallets.forEach { wallet ->
                        ArusButton(onClick = { payerWalletId = wallet.id }, kind = if (payerWalletId == wallet.id) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(wallet.name) }
                    }
                    categories.filter { it.kind == "EXPENSE" }.forEach { category ->
                        ArusButton(onClick = { categoryId = category.id }, kind = if (categoryId == category.id) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(category.name) }
                    }
                    ArusButton(onClick = {
                        val id = current.draft.id
                        val wallet = payerWalletId ?: current.draft.payerWalletId
                        val category = categoryId ?: current.draft.categoryId
                        if (title.isBlank()) { error = "Isi judul group order."; return@ArusButton }
                        error = null
                        step = 2
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) {
                                database.groupOrderDao().updateDraftInfo(id, title, merchant.ifBlank { null },
                                    current.draft.occurredAtEpochMillis, wallet, category,
                                    roundingBearer?.toString() ?: "ME", System.currentTimeMillis())
                            } }.onSuccess { updated ->
                                if (updated == 1) refreshDrafts()
                                else { step = 1; error = "Draft tidak dapat diperbarui." }
                            }.onFailure { step = 1; error = it.message ?: "Draft tidak dapat diperbarui." }
                        }
                    }) { Text("Lanjut · Peserta") }
                }
                2 -> Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Text("Peserta · Aku selalu disertakan", style = ArusTheme.typography.title)
                    ArusTextField(participantName, { participantName = it }, placeholder = "Nama kontak baru", singleLine = true)
                    ArusButton(onClick = {
                        if (participantName.isBlank()) { error = "Isi nama kontak."; return@ArusButton }
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) {
                                val name = participantName.trim()
                                val found = database.debtDao().activeContacts().firstOrNull { it.name.equals(name, true) }
                                found?.id ?: database.debtDao().insertContact(ContactEntity(name, null, null, false,
                                    System.currentTimeMillis(), System.currentTimeMillis()))
                            } }.onSuccess { id -> if (id !in participantIds) participantIds.add(id); participantName = ""; refreshDrafts() }
                                .onFailure { error = it.message ?: "Kontak gagal ditambahkan." }
                        }
                    }, kind = ArusButtonKind.Outline) { Text("Tambah peserta") }
                    contacts.forEach { contact ->
                        ArusButton(onClick = {
                            if (contact.id in participantIds) participantIds.remove(contact.id) else participantIds.add(contact.id)
                        }, kind = if (contact.id in participantIds) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(contact.name) }
                    }
                    ArusButton(onClick = { step = 3 }) { Text("Lanjut · Item") }
                }
                3 -> Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Text("Item dan pemilik", style = ArusTheme.typography.title)
                    ArusTextField(itemName, { itemName = it }, placeholder = "Nama item", singleLine = true)
                    ArusTextField(itemPrice, { itemPrice = it.filter(Char::isDigit) }, placeholder = "Harga satuan (Rp)", singleLine = true)
                    ArusTextField(itemQuantity, { itemQuantity = it.filter(Char::isDigit) }, placeholder = "Jumlah", singleLine = true)
                    ArusButton(onClick = {
                        if (null in itemOwners) { itemOwners.remove(null); manualOwnerAmounts.remove(null) }
                        else { itemOwners.add(null); manualOwnerAmounts[null] = "" }
                    }, kind = if (null in itemOwners) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text("Aku") }
                    contacts.filter { it.id in participantIds }.forEach { contact ->
                        ArusButton(onClick = {
                            if (contact.id in itemOwners) { itemOwners.remove(contact.id); manualOwnerAmounts.remove(contact.id) }
                            else { itemOwners.add(contact.id); manualOwnerAmounts[contact.id] = "" }
                        },
                            kind = if (contact.id in itemOwners) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(contact.name) }
                    }
                    if (itemOwners.size > 1) {
                        ArusButton(onClick = { itemUsesCustomShares = !itemUsesCustomShares },
                            kind = if (itemUsesCustomShares) ArusButtonKind.Secondary else ArusButtonKind.Outline) {
                            Text(if (itemUsesCustomShares) "Nominal patungan manual" else "Bagi rata")
                        }
                    }
                    if (itemUsesCustomShares) itemOwners.forEach { ownerId ->
                        val ownerName = if (ownerId == null) "Aku" else contacts.firstOrNull { it.id == ownerId }?.name ?: "Kontak"
                        ArusTextField(manualOwnerAmounts[ownerId].orEmpty(),
                            { manualOwnerAmounts[ownerId] = it.filter(Char::isDigit) },
                            placeholder = "$ownerName (Rp)", singleLine = true)
                    }
                    ArusButton(onClick = {
                        val price = itemPrice.toLongOrNull()
                        val quantity = itemQuantity.toIntOrNull()
                        if (itemName.isBlank() || price == null || quantity == null || quantity <= 0) { error = "Isi nama item, harga, dan jumlah valid."; return@ArusButton }
                        val itemTotal = runCatching { Math.multiplyExact(price, quantity.toLong()) }.getOrElse { error = "Total item terlalu besar."; return@ArusButton }
                        val owners = if (itemUsesCustomShares) {
                            val shares = itemOwners.map { id -> id to (manualOwnerAmounts[id]?.toLongOrNull() ?: 0L) }
                            if (shares.any { it.second <= 0 } || shares.sumOf { it.second } != itemTotal) {
                                error = "Nominal patungan manual harus positif dan sama dengan total item."; return@ArusButton
                            }
                            shares.map { (id, amount) -> GroupOrderOwnerEntity(0, id, 1, amount) }
                        } else itemOwners.map { GroupOrderOwnerEntity(0, it, 1, null) }
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) {
                                database.groupOrderDao().addItem(current.draft.id, itemName, price, quantity, "MANUAL", owners)
                            } }.onSuccess {
                                itemName = ""; itemPrice = ""; itemQuantity = "1"; itemOwners.clear(); error = null
                                manualOwnerAmounts.clear(); itemUsesCustomShares = false
                                loadDraft(current.draft.id, resetStep = false); refreshDrafts()
                            }
                                .onFailure { error = it.message ?: "Item gagal ditambahkan." }
                        }
                    }, kind = ArusButtonKind.Outline) { Text("Tambah item") }
                    current.items.forEach { item ->
                        val isAssigned = current.ownersByItem[item.id].orEmpty().isNotEmpty()
                        Text("${item.name} · ${formatRupiah(item.unitPriceRupiah * item.quantity)}${if (isAssigned) "" else " · Belum ditetapkan"}")
                    }
                    ArusButton(onClick = { step = 4 }) { Text("Lanjut · Biaya") }
                }
                4 -> Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Text("Biaya bersama", style = ArusTheme.typography.title)
                    ArusTextField(feeLabel, { feeLabel = it }, placeholder = "Ongkir, pajak, diskon", singleLine = true)
                    ArusTextField(feeAmount, { feeAmount = it.filter(Char::isDigit) }, placeholder = "Nominal (Rp)", singleLine = true)
                    ArusButton(onClick = { feeIsDiscount = !feeIsDiscount }, kind = if (feeIsDiscount) ArusButtonKind.Secondary else ArusButtonKind.Outline) {
                        Text(if (feeIsDiscount) "Diskon" else "Biaya")
                    }
                    listOf(GroupFeeSplitMode.EQUAL, GroupFeeSplitMode.PROPORTIONAL, GroupFeeSplitMode.MANUAL).forEach { mode ->
                        ArusButton(onClick = { feeMode = mode }, kind = if (feeMode == mode) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(mode.name) }
                    }
                    if (feeMode == GroupFeeSplitMode.MANUAL) {
                        Text("Masukkan alokasi untuk setiap peserta.")
                        (listOf<Long?>(null) + participantIds).forEach { participantId ->
                            val name = if (participantId == null) "Aku" else contacts.firstOrNull { it.id == participantId }?.name ?: "Kontak"
                            ArusTextField(manualFeeAmounts[participantId].orEmpty(),
                                { manualFeeAmounts[participantId] = it.filter(Char::isDigit) },
                                placeholder = "$name (Rp)", singleLine = true)
                        }
                    }
                    ArusButton(onClick = {
                        val amount = feeAmount.toLongOrNull()
                        if (feeLabel.isBlank() || amount == null || amount <= 0) { error = "Isi label dan nominal biaya positif."; return@ArusButton }
                        val signedAmount = if (feeIsDiscount) -amount else amount
                        val manual = if (feeMode == GroupFeeSplitMode.MANUAL) {
                            val values = mutableMapOf<Long?, Long>()
                            for (id in listOf<Long?>(null) + participantIds) {
                                val share = manualFeeAmounts[id]?.toLongOrNull()
                                if (share == null) { error = "Isi nominal manual untuk setiap peserta."; return@ArusButton }
                                values[id] = if (feeIsDiscount) -share else share
                            }
                            if (values.values.fold(0L) { total, value -> Math.addExact(total, value) } != signedAmount) {
                                error = "Total alokasi manual harus sama dengan nominal biaya."; return@ArusButton
                            }
                            values
                        } else null
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) {
                                database.groupOrderDao().addFee(current.draft.id, feeLabel, signedAmount, feeMode.name, manual)
                            } }.onSuccess {
                                feeLabel = ""; feeAmount = ""; manualFeeAmounts.clear(); error = null
                                loadDraft(current.draft.id, resetStep = false); refreshDrafts()
                            }
                                .onFailure { error = it.message ?: "Biaya gagal ditambahkan." }
                        }
                    }, kind = ArusButtonKind.Outline) { Text("Tambah biaya") }
                    current.fees.forEach { fee -> Text("${fee.label} · ${formatRupiah(fee.amountRupiah)} · ${fee.splitMode}") }
                    ArusButton(onClick = { step = 5 }) { Text("Lanjut · Tinjau") }
                }
                else -> GroupOrderReview(
                    draft = current.draft,
                    items = current.items,
                    ownersByItem = current.ownersByItem,
                    fees = current.fees,
                    feeSharesById = current.feeSharesById,
                    participantIds = participantIds.toList(),
                    contacts = contacts,
                    roundingBearer = roundingBearer,
                    onRoundingBearer = { roundingBearer = it },
                    onFinalize = {
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) {
                                val updated = database.groupOrderDao().updateDraftInfo(current.draft.id, title, merchant.ifBlank { null },
                                    current.draft.occurredAtEpochMillis, payerWalletId ?: current.draft.payerWalletId,
                                    categoryId ?: current.draft.categoryId, roundingBearer?.toString() ?: "ME", System.currentTimeMillis())
                                if (updated != 1) throw IllegalStateException("Draft group order tidak dapat diperbarui.")
                                database.groupOrderDao().finalizeDraft(current.draft.id, System.currentTimeMillis())
                            } }.onSuccess {
                                completed = true
                                refreshDrafts()
                                selectedOrder = withContext(Dispatchers.IO) { database.groupOrderDao().find(current.draft.id) }
                            }
                                .onFailure { error = it.message ?: "Finalisasi group order gagal." }
                        }
                    },
                )
            }
            if (step > 1 && step <= 5) OutlinedArusButton(onClick = { step -= 1 }) { Text("‹ Kembali") }
            OutlinedArusButton(onClick = { selectedDraft = null; scope.launch { refreshDrafts() } }) { Text("Simpan draft dan keluar") }
        }
    }
}

@Composable
private fun GroupOrderReview(
    draft: GroupOrderEntity,
    items: List<GroupOrderItemEntity>,
    ownersByItem: Map<Long, List<GroupOrderOwnerEntity>>,
    fees: List<GroupOrderFeeEntity>,
    feeSharesById: Map<Long, List<GroupOrderFeeShareEntity>>,
    participantIds: List<Long>,
    contacts: List<ContactEntity>,
    roundingBearer: Long?,
    onRoundingBearer: (Long?) -> Unit,
    onFinalize: () -> Unit,
) {
    val participantKeys: List<Long?> = listOf(null) + participantIds
    val calculation = remember(draft.id, items, ownersByItem, fees, feeSharesById, participantIds, roundingBearer) {
        runCatching {
            val itemInputs = items.map { item ->
                val assigned = ownersByItem[item.id].orEmpty()
                GroupOrderItemInput(Math.multiplyExact(item.unitPriceRupiah, item.quantity.toLong()),
                    assigned.associate { it.contactId to (it.shareAmountRupiah ?: it.shareWeight) })
            }
            val feeInputs = fees.map { fee ->
                val manual = if (fee.splitMode == GroupFeeSplitMode.MANUAL.name) {
                    val allocated = feeSharesById[fee.id].orEmpty().associate { it.contactId to it.amountRupiah }
                    participantKeys.associateWith { allocated[it] ?: 0L }
                } else emptyMap()
                GroupOrderFeeInput(fee.amountRupiah, GroupFeeSplitMode.valueOf(fee.splitMode), manual)
            }
            GroupOrderCalculator.calculate(participantKeys, itemInputs, feeInputs, roundingBearer)
        }.getOrNull()
    }
    Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
        Text("Tinjau tagihan", style = ArusTheme.typography.title)
        Text("Pembulatan ditanggung")
        ArusButton(onClick = { onRoundingBearer(null) }, kind = if (roundingBearer == null) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text("Aku") }
        contacts.filter { it.id in participantIds }.forEach { contact ->
            ArusButton(onClick = { onRoundingBearer(contact.id) }, kind = if (roundingBearer == contact.id) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(contact.name) }
        }
        if (calculation == null) {
            Text(if (items.any { ownersByItem[it.id].isNullOrEmpty() }) "Item tanpa pemilik memblokir finalisasi." else "Periksa pembagian biaya dan total.",
                style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error))
        } else {
            calculation.participants.forEach { (contactId, share) ->
                val name = if (contactId == null) "Aku" else contacts.firstOrNull { it.id == contactId }?.name ?: "Kontak"
                Text("$name · ${formatRupiah(share.totalAmountRupiah)} (item ${formatRupiah(share.itemsAmountRupiah)}, biaya ${formatRupiah(share.feeAmountRupiah)})")
            }
            Text("Total · ${formatRupiah(calculation.grandTotalRupiah)}")
        }
        ArusButton(onClick = onFinalize, enabled = calculation != null && calculation.grandTotalRupiah > 0) { Text("Finalisasi") }
    }
}

private data class GroupOrderDetailData(
    val items: List<GroupOrderItemEntity>,
    val ownersByItem: Map<Long, List<GroupOrderOwnerEntity>>,
    val shares: List<GroupOrderShareEntity>,
    val debts: List<DebtEntity>,
    val remainingByDebt: Map<Long, Long>,
)

@Composable
private fun GroupOrderDetail(
    database: ArusDatabase,
    order: GroupOrderEntity,
    contacts: List<ContactEntity>,
    wallets: List<WalletEntity>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var currentOrder by remember(order.id) { mutableStateOf(order) }
    var detail by remember(order.id) { mutableStateOf<GroupOrderDetailData?>(null) }
    var settlementDebts by remember { mutableStateOf<List<DebtEntity>?>(null) }
    var cancelPrompt by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }

    suspend fun refresh() {
        val updated = withContext(Dispatchers.IO) {
            val items = database.groupOrderDao().items(currentOrder.id)
            val debts = database.groupOrderDao().debts(currentOrder.id)
            GroupOrderDetailData(
                items,
                items.associate { item -> item.id to database.groupOrderDao().owners(item.id) },
                database.groupOrderDao().shares(currentOrder.id),
                debts,
                debts.associate { debt -> debt.id to database.debtDao().remainingPrincipal(debt.id) },
            )
        }
        detail = updated
        currentOrder = withContext(Dispatchers.IO) { database.groupOrderDao().find(currentOrder.id) } ?: currentOrder
    }

    LaunchedEffect(order.id) { refresh() }

    Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
        Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
            OutlinedArusButton(onClick = onBack) { Text("‹ Kembali") }
            Text(currentOrder.title, style = ArusTheme.typography.headline)
        }
        Text("${currentOrder.status} · ${formatRupiah(currentOrder.grandTotalRupiah)}")
        message?.let {
            Text(it, style = ArusTheme.typography.body.copy(
                color = if (messageIsError) ArusTheme.colors.error else ArusTheme.colors.mutedForeground))
        }
        detail?.let { loaded ->
            Text("ITEM", style = ArusTheme.typography.label)
            loaded.items.forEach { item ->
                val owners = loaded.ownersByItem[item.id].orEmpty().map { owner ->
                    owner.contactId?.let { id -> contacts.firstOrNull { it.id == id }?.name } ?: "Aku"
                }
                val itemTotal = Math.multiplyExact(item.unitPriceRupiah, item.quantity.toLong())
                Text("${item.name} · ${formatRupiah(itemTotal)} · ${owners.joinToString()}")
            }
            Text("PESERTA", style = ArusTheme.typography.label)
            loaded.shares.forEach { share ->
                val name = share.contactId?.let { id -> contacts.firstOrNull { it.id == id }?.name } ?: "Aku"
                val debt = share.contactId?.let { id -> loaded.debts.firstOrNull { it.contactId == id } }
                val remaining = debt?.let { loaded.remainingByDebt[it.id] } ?: 0L
                val status = when {
                    currentOrder.status == "CANCELLED" -> "Dibatalkan"
                    share.contactId == null -> "Bagianmu"
                    debt == null -> "Lunas"
                    else -> "${debt.status} · sisa ${formatRupiah(remaining)}"
                }
                ArusCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(ArusSpacing.Medium), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                        Text("$name · ${formatRupiah(share.totalAmountRupiah)} · $status")
                        if (share.contactId != null && currentOrder.status != "CANCELLED") {
                            Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                                if (debt != null && debt.status in listOf("OPEN", "PARTIAL")) {
                                    ArusButton(onClick = {
                                        settlementDebts = loaded.debts.filter {
                                            it.contactId == share.contactId && it.direction == "LENT" && it.status in listOf("OPEN", "PARTIAL")
                                        }
                                    }) { Text("Catat pelunasan") }
                                }
                                OutlinedArusButton(onClick = {
                                    context.startActivity(createGroupOrderShareIntent(currentOrder.title, name,
                                        share.totalAmountRupiah, status))
                                }) { Text("Bagikan tagihan") }
                            }
                        }
                    }
                }
            }
        }
        if (currentOrder.status in listOf("FINALIZED", "SETTLED")) {
            OutlinedArusButton(onClick = { cancelPrompt = true }) { Text("Batalkan order") }
        }
    }

    if (cancelPrompt) ArusDialog(
        onDismissRequest = { cancelPrompt = false },
        title = { Text("Batalkan group order?") },
        text = { Text("Pembatalan menghapus transaksi dan piutang terkait. Order dengan pelunasan tidak dapat dibatalkan.") },
        confirmButton = {
            ArusButton(onClick = {
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) {
                        database.groupOrderDao().cancelGroupOrder(currentOrder.id, System.currentTimeMillis())
                    } }.onSuccess {
                        cancelPrompt = false
                        message = "Group order dibatalkan."
                        messageIsError = false
                        refresh()
                        onChanged()
                    }.onFailure {
                        message = it.message ?: "Group order tidak dapat dibatalkan."
                        messageIsError = true
                    }
                }
            }) { Text("Batalkan") }
        },
        dismissButton = { OutlinedArusButton(onClick = { cancelPrompt = false }) { Text("Jangan") } },
    )

    settlementDebts?.let { selected ->
        val debt = selected.firstOrNull()
        if (debt != null) {
            val name = contacts.firstOrNull { it.id == debt.contactId }?.name.orEmpty()
            SettlementDialog(
                contactName = name,
                debts = selected,
                remainingByDebt = detail?.remainingByDebt.orEmpty(),
                wallets = wallets,
                categories = categories,
                onDismiss = { settlementDebts = null },
                onSave = { amount, walletId, resolution, categoryId, allocations ->
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) {
                            database.debtDao().recordSettlement(debt.contactId, debt.direction, walletId,
                                amount, allocations, resolution, categoryId, System.currentTimeMillis(),
                                currentOrder.title, System.currentTimeMillis())
                        } }.onSuccess {
                            settlementDebts = null
                            message = "Pelunasan tersimpan."
                            messageIsError = false
                            refresh()
                            onChanged()
                        }.onFailure {
                            message = it.message ?: "Pelunasan gagal disimpan."
                            messageIsError = true
                        }
                    }
                },
            )
        }
    }
}

internal fun createGroupOrderShareIntent(orderTitle: String, participantName: String,
                                         amountRupiah: Long, status: String): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT,
            "Tagihan $orderTitle\n$participantName: ${formatRupiah(amountRupiah)}\n$status")
    }
    return Intent.createChooser(sendIntent, "Bagikan tagihan")
}
