package com.illustravibes.arus.data;

public class ExportTransactionRow {
    public long id;
    public long occurredAtEpochMillis;
    public String type;
    public long amountRupiah;
    public String walletName;
    public String categoryName;
    public String merchant;
    public String note;
    public boolean verified;
    public boolean hasAttachment;
    public String splitSummary;
}
