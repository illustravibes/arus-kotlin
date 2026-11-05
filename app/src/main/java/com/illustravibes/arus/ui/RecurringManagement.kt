package com.illustravibes.arus.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.RecurringOccurrenceEntity
import com.illustravibes.arus.data.RecurringRuleEntity
import com.illustravibes.arus.domain.BudgetPeriod
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusButtonKind
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun RecurringManagement(database: ArusDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scope.launch {
            val count = withContext(Dispatchers.IO) { database.recurringDao().pendingOccurrences().size }
            if (count > 0) notifyPending(context, count)
        }
    }
    val rules = remember { mutableStateListOf<RecurringRuleEntity>() }
    val pausedRules = remember { mutableStateListOf<RecurringRuleEntity>() }
    val pending = remember { mutableStateListOf<RecurringOccurrenceEntity>() }
    val wallets = remember { mutableStateListOf<com.illustravibes.arus.data.WalletEntity>() }
    val categories = remember { mutableStateListOf<com.illustravibes.arus.data.CategoryEntity>() }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("EXPENSE") }
    var walletId by remember { mutableStateOf<Long?>(null) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var interval by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var confirmation by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingOccurrence by rememberSaveable { mutableStateOf<Long?>(null) }
    var editedAmount by rememberSaveable { mutableStateOf("") }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    suspend fun refresh() {
        val data = withContext(Dispatchers.IO) {
            database.recurringDao().runDue(LocalDate.now().toEpochDay())
            RefreshData(database.recurringDao().allRules(), database.recurringDao().pausedRules(),
                database.recurringDao().pendingOccurrences(), database.walletDao().activeWallets(),
                database.categoryDao().activeCategories("EXPENSE") + database.categoryDao().activeCategories("INCOME"))
        }
        rules.clear(); rules.addAll(data.rules)
        pausedRules.clear(); pausedRules.addAll(data.pausedRules)
        pending.clear(); pending.addAll(data.pending)
        wallets.clear(); wallets.addAll(data.wallets)
        categories.clear(); categories.addAll(data.categories)
        if (walletId == null) walletId = wallets.firstOrNull()?.id
        if (categoryId == null) categoryId = categories.firstOrNull { it.kind == transactionType }?.id
        if (pending.isNotEmpty()) notifyPending(context, pending.size)
    }

    LaunchedEffect(database) { refresh() }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, database) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { refresh() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Transaksi rutin", style = com.illustravibes.arus.ui.designsystem.ArusTheme.typography.headline)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArusButton(onClick = { transactionType = "EXPENSE"; categoryId = categories.firstOrNull { it.kind == "EXPENSE" }?.id }, selected = transactionType == "EXPENSE") { Text("Pengeluaran") }
                    ArusButton(onClick = { transactionType = "INCOME"; categoryId = categories.firstOrNull { it.kind == "INCOME" }?.id }, selected = transactionType == "INCOME") { Text("Pemasukan") }
                }
                ArusTextField(name, { name = it }, placeholder = "Nama aturan", singleLine = true)
                ArusTextField(amount, { amount = it.filter(Char::isDigit) }, placeholder = "Nominal (Rp)", singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArusButton(onClick = { walletId = wallets.firstOrNull()?.id }) { Text("Dompet: ${wallets.firstOrNull { it.id == walletId }?.name ?: "Pilih"}") }
                    ArusButton(onClick = { categoryId = categories.firstOrNull { it.kind == transactionType }?.id }) { Text("Kategori: ${categories.firstOrNull { it.id == categoryId }?.name ?: "Pilih"}") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(BudgetPeriod.WEEKLY to "Mingguan", BudgetPeriod.MONTHLY to "Bulanan", BudgetPeriod.YEARLY to "Tahunan").forEach { (type, label) ->
                        ArusButton(onClick = { interval = type }, selected = interval == type) { Text(label) }
                    }
                }
                ArusButton(onClick = { confirmation = !confirmation }, kind = ArusButtonKind.Outline) { Text(if (confirmation) "Mode: konfirmasi" else "Mode: otomatis") }
                error?.let { Text(it, color = com.illustravibes.arus.ui.designsystem.ArusTheme.colors.error) }
                ArusButton(onClick = {
                    val value = amount.toLongOrNull()
                    if (name.isBlank() || value == null || value <= 0 || walletId == null || categoryId == null) {
                        error = "Lengkapi nama, nominal, dompet, dan kategori."
                    } else {
                        val rule = RecurringRuleEntity(name.trim(), transactionType, walletId!!, categoryId!!, value,
                            name.trim(), interval, LocalDate.now().toEpochDay(), null, confirmation, true)
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { database.recurringDao().createRule(rule); database.recurringDao().runDue(LocalDate.now().toEpochDay()) }
                                name = ""; amount = ""; error = null; refresh()
                            } catch (failure: Exception) { error = failure.message ?: "Aturan gagal disimpan." }
                        }
                    }
                }) { Text("Simpan aturan") }
                if (pending.isNotEmpty()) {
                    Text("Perlu konfirmasi (${pending.size})", style = com.illustravibes.arus.ui.designsystem.ArusTheme.typography.title)
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ArusButton(onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }, kind = ArusButtonKind.Outline) { Text("Aktifkan izin notifikasi") }
                    }
                }
            }
        }
        items(pending, key = { it.id }) { occurrence ->
            val rule = rules.firstOrNull { it.id == occurrence.ruleId }
            ArusCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${rule?.name ?: "Aturan"} · ${LocalDate.ofEpochDay(occurrence.occurrenceEpochDay)} · ${formatRupiah(rule?.amountRupiah ?: 0)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArusButton(onClick = { resolve( database, occurrence, "POST", null, scope, ::refresh, { error = it }) }) { Text("Catat") }
                        ArusButton(onClick = { editedAmount = rule?.amountRupiah?.toString() ?: ""; editingOccurrence = occurrence.id }, kind = ArusButtonKind.Outline) { Text("Ubah nominal") }
                        ArusButton(onClick = { resolve(database, occurrence, "SKIP", null, scope, ::refresh, { error = it }) }, kind = ArusButtonKind.Outline) { Text("Lewati") }
                    }
                }
            }
        }
        items(rules.filter { it.isActive }, key = { it.id }) { rule ->
            ArusCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(rule.name)
                    Text("${rule.transactionType} · ${formatRupiah(rule.amountRupiah)} · ${intervalLabel(rule.intervalType)} · berikutnya ${LocalDate.ofEpochDay(rule.nextOccurrenceEpochDay)}")
                    ArusButton(onClick = {
                        scope.launch { withContext(Dispatchers.IO) { database.recurringDao().deactivate(rule.id) }; refresh() }
                    }, kind = ArusButtonKind.Outline) { Text("Jeda aturan") }
                }
            }
        }
        items(pausedRules, key = { it.id }) { rule ->
            ArusCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${rule.name} · dijeda")
                    ArusButton(onClick = {
                        scope.launch { withContext(Dispatchers.IO) { database.recurringDao().activate(rule.id) }; refresh() }
                    }) { Text("Lanjutkan aturan") }
                }
            }
        }
    }
    editingOccurrence?.let { occurrenceId ->
        Dialog(onDismissRequest = { editingOccurrence = null }) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ubah nominal", style = com.illustravibes.arus.ui.designsystem.ArusTheme.typography.title)
                ArusTextField(editedAmount, { editedAmount = it.filter(Char::isDigit) }, placeholder = "Nominal (Rp)", singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArusButton(onClick = { editingOccurrence = null }, kind = ArusButtonKind.Outline) { Text("Batal") }
                    ArusButton(onClick = {
                        val value = editedAmount.toLongOrNull()
                        if (value == null || value <= 0) error = "Nominal harus lebih dari nol."
                        else {
                            resolve(database, pending.firstOrNull { it.id == occurrenceId } ?: return@ArusButton,
                                "POST", value, scope, ::refresh, { error = it })
                            editingOccurrence = null
                        }
                    }) { Text("Catat nominal ini") }
                }
            }
        }
    }
}

private data class RefreshData(
    val rules: List<RecurringRuleEntity>,
    val pausedRules: List<RecurringRuleEntity>,
    val pending: List<RecurringOccurrenceEntity>,
    val wallets: List<com.illustravibes.arus.data.WalletEntity>,
    val categories: List<com.illustravibes.arus.data.CategoryEntity>,
)

private fun resolve(database: ArusDatabase, occurrence: RecurringOccurrenceEntity, action: String, amount: Long?,
                    scope: kotlinx.coroutines.CoroutineScope, refresh: suspend () -> Unit, onError: (String) -> Unit) {
    scope.launch {
        try { withContext(Dispatchers.IO) { database.recurringDao().resolvePending(occurrence.id, action, amount) }; refresh() }
        catch (failure: Exception) { onError(failure.message ?: "Occurrence gagal diproses.") }
    }
}

private fun intervalLabel(type: Int) = when (type) {
    BudgetPeriod.WEEKLY -> "Mingguan"
    BudgetPeriod.MONTHLY -> "Bulanan"
    else -> "Tahunan"
}

private fun notifyPending(context: Context, count: Int) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(NotificationChannel("recurring", "Transaksi rutin", NotificationManager.IMPORTANCE_DEFAULT))
    }
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    try {
        NotificationManagerCompat.from(context).notify(8401, NotificationCompat.Builder(context, "recurring")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Transaksi rutin menunggu konfirmasi")
            .setContentText("Ada $count occurrence yang perlu ditinjau di Arus.")
            .setAutoCancel(true).build())
    } catch (_: SecurityException) { }
}
