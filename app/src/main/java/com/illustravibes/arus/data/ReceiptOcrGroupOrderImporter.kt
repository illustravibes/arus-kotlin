package com.illustravibes.arus.data

import com.illustravibes.arus.domain.ReceiptOcrReview
import java.util.concurrent.Callable

internal object ReceiptOcrGroupOrderImporter {
    fun createDraft(
        database: ArusDatabase,
        attachmentId: Long,
        review: ReceiptOcrReview,
        payerWalletId: Long,
        categoryId: Long,
        nowEpochMillis: Long,
    ): Long = database.runInTransaction(Callable {
        val title = review.merchant?.takeIf(String::isNotBlank) ?: "Group order dari struk"
        val occurredAt = review.date?.atStartOfDay(java.time.ZoneId.systemDefault())
            ?.toInstant()?.toEpochMilli() ?: nowEpochMillis
        val orderId = database.groupOrderDao().createDraft(GroupOrderEntity(
            title, review.merchant, occurredAt, payerWalletId, categoryId,
            0, 0, 0, 0, null, "ME", "DRAFT", nowEpochMillis, nowEpochMillis, null,
        ))
        review.items.forEach { item ->
            database.groupOrderDao().addItem(orderId, item.name, item.amountRupiah, 1, "OCR", emptyList())
        }
        check(database.attachmentDao().attach(attachmentId, "GROUP_ORDER", orderId) == 1) {
            "Lampiran struk tidak dapat ditautkan ke group order."
        }
        orderId
    })
}
