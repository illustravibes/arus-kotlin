# Matriks Keterlacakan Task 11.9

Audit mencakup 44 requirement di delapan spec delta dan 62 layar S-xx pada `arus-user-flow.md`. Setiap requirement menunjuk use case/layar dan bukti tes; setiap layar memiliki implementasi serta tes atau catatan review visual.

## Requirement → use case/layar → tes

| Requirement | Use case/layar | Tes atau bukti |
|---|---|---|
| Penyimpanan data lokal offline | `DatabaseFactory`, ledger Room/SQLCipher lokal; tidak ada server dalam jalur transaksi | `OfflinePersistenceTest`, `EncryptedDatabaseTest`; penelusuran kode tidak menemukan client jaringan |
| Nominal rupiah integer | domain `TransactionDraft`, `TransactionEntity` | `TransactionTypeConverterTest`, `TransactionDraftTest`, `BalanceCalculatorTest` |
| Saldo dihitung dari riwayat | `BalanceRepository`, saldo dompet | `BalanceCalculatorTest`, `BalanceDaoTest` |
| Integritas operasi multi-entitas | transfer, resolusi rekonsiliasi, group order | `BalanceDaoTest`, `DebtDaoTest`, `GroupOrderDaoTest` |
| Split transaksi seimbang | transaksi biasa dan editor split | `TransactionSplitCalculatorTest`, `TransactionSplitDaoTest`, `transactionSplitEditorSavesBalancedCategoriesAndKeepsReportBudgetFlags` |
| Migrasi skema aman | migrasi Room 1→21 | `DatabaseMigrationTest`, termasuk migrasi 19→20 dan 20→21 |
| Perlindungan database lokal | SQLCipher dan Android Keystore | `EncryptedDatabaseTest`, `BackupRestoreManagerTest` |
| Kunci aplikasi | PIN, biometrik, kembali dari background | `AppLockTest`, `biometricUnavailableFallsBackToPin`, `biometricPromptFailureKeepsPinFallbackAvailable`, `appLockSettingsCanSetAndDisablePin` |
| Perlindungan tampilan aplikasi | `FLAG_SECURE` build rilis | inspeksi konfigurasi `MainActivity`; `secureFlagIsDisabledForDebugScreenshots` memverifikasi pengecualian debug |
| Onboarding dompet awal | S-02–S-04 | `onboardingCreatesWalletAndCompletesSetup`, `PresetSeederTest` |
| Pengelolaan dompet | S-20–S-23 | `WalletDaoTest`, `archivedWalletListOpensBalanceAndTransactionHistory` |
| Saldo dan rincian dompet | S-22, `BalanceRepository` | `BalanceDaoTest`, `archivedWalletListOpensBalanceAndTransactionHistory` |
| Pengaturan dompet kredit | S-21, form kredit | `WalletDaoTest.creditOnlyValuesCanBeStoredAndCleared`, `DatabaseMigrationTest` |
| Pencatatan pemasukan dan pengeluaran | S-31, transaksi cepat | `TransactionDraftTest`, `switchesTypeSavesAndUndoRestoresBalanceAndHistory` |
| Validasi transaksi | nominal nol dan saldo minus | `TransactionDraftTest`, `BalanceDaoTest`, `switchesTypeSavesAndUndoRestoresBalanceAndHistory` |
| Transfer antar dompet | S-34–S-35 | `BalanceDaoTest`, `transactionFlowOpensPickersAndTransferAndShowsPagedHistoryDetails` |
| Riwayat dan detail transaksi | S-30, S-33 | `TransactionHistoryFilterTest`, `transactionFlowOpensPickersAndTransferAndShowsPagedHistoryDetails` |
| Perubahan dan penghapusan transaksi | S-33 | `BalanceDaoTest`, `switchesTypeSavesAndUndoRestoresBalanceAndHistory` |
| Pengelolaan kategori | S-37, S-40–S-42 | `CategoryDaoTest`, `categoryManagementShowsListsAndCategoryForm` |
| Resolusi selisih saldo | S-100–S-103 | `BalanceReconciliationTest`, `BalanceDaoTest`, `walletReconciliationShowsRecordedActualAndSignedDifference` |
| Checkpoint verifikasi | S-100–S-104 | `BalanceDaoTest`, `DatabaseMigrationTest`, `task-8.6-visual-review.md` |
| Perubahan transaksi terverifikasi | S-33, S-104 | `BalanceDaoTest`, `transactionFlowOpensPickersAndTransferAndShowsPagedHistoryDetails` |
| Hitung pecahan uang tunai | S-105 | `DatabaseMigrationTest`, `CashDenominationCountDaoTest`, `cashDenominationCountSetsActualBalanceAndSavesWithCheckpointWithoutLedgerTransaction` |
| Ringkasan beranda | S-10 | `homeShowsEmptyStateAndIndonesianCurrencyAndDateFormats`, `homeShowsNetWorthWalletsAndLatestTransactionsAndOpensDestinations` |
| Perhitungan net worth MVP | S-11 | `BalanceDaoTest`, `homeShowsNetWorthWalletsAndLatestTransactionsAndOpensDestinations` |
| Anggaran periode | S-50–S-51 | `BudgetDaoTest`, `BudgetPeriodTest`, `task-8.6-visual-review.md` |
| Pembagian transaksi multi-kategori | S-32 | `TransactionSplitDaoTest`, `TransactionSplitCalculatorTest`, `transactionSplitEditorSavesBalancedCategoriesAndKeepsReportBudgetFlags` |
| Laporan transaksi | S-52 | `BudgetDaoTest`, `task-8.6-visual-review.md` |
| Rincian kategori laporan | S-53 | `TransactionExportDaoTest`, `reportCategoryOpensOnlyItsTransactionsForSelectedPeriod` |
| Transaksi rutin transparan | S-12, S-60–S-62 | `RecurringDaoTest`, `RecurringManagement.kt` |
| Pengaturan reminder dan izin notifikasi | S-63, S-114 | `RecurringDaoTest`, `recurringReminderSettingsShowPermissionStatusAndCanDisableReminder` |
| Pencarian dan filter riwayat | S-36 | `TransactionHistoryFilterTest`, `transactionFlowOpensPickersAndTransferAndShowsPagedHistoryDetails` |
| Riwayat rekonsiliasi | S-104 | `BalanceDaoTest`, `DatabaseMigrationTest`, `task-8.6-visual-review.md` |
| Catatan utang-piutang dua arah | S-70, S-72–S-73 | `DebtDaoTest`, `debtScreenRecordsInstallmentAndNetsOppositeDebts` |
| Pelunasan teralokasi | S-74–S-75 | `DebtDaoTest`, `debtScreenRecordsInstallmentAndNetsOppositeDebts` |
| Netting pada kontak yang sama | S-72, S-74 | `DebtDaoTest`, `debtScreenRecordsInstallmentAndNetsOppositeDebts` |
| Rincian kontak | S-71 | `DebtDaoTest`, `contactDetailsShowDirectionTotalsAndDebtHistory` |
| Finalisasi group order | S-80–S-85 | `GroupOrderDaoTest`, `groupOrderDraftFinalizesBalancedPaymentAndReceivable` |
| Pembagian group order | S-83–S-86 | `GroupOrderCalculatorTest`, `GroupOrderDaoTest`, `ReceiptOcrGroupOrderImporterTest` |
| OCR memerlukan konfirmasi | S-90–S-93 | `ReceiptOcrParserTest`, `ReceiptImageProcessorTest`, kamera/OCR dan Group Order UI tests |
| Backup dan restore terenkripsi | S-06, S-112 | `EncryptedBackupContainerTest`, `BackupRestoreManagerTest`, onboarding UI test |
| Ekspor data pengguna | S-113 | `TransactionExportDaoTest`, `ExportFileWriterTest`, `exportRouteOffersDateRangeAndAllSupportedFormats` |
| Pengaturan umum aplikasi | S-110 | `BudgetPeriodTest`, `generalSettingsPersistThemePeriodAndLocalFormatsAfterRestart` |
| Mode cek mutasi | S-102 | `DatabaseMigrationTest`, `matchingMutationSavesCheckpointItemWithoutChangingLedger` |

## Layar → implementasi → tes/review

| Layar | Implementasi | Tes atau bukti |
|---|---|---|
| S-01, S-05, S-111 | `MainActivity`, `AppLockScreen`, `AppLockSettings` | `AppLockTest`, tes PIN/biometrik pada `TransactionQuickEntryTest` |
| S-02–S-04 | `Onboarding` | `onboardingCreatesWalletAndCompletesSetup`, `PresetSeederTest` |
| S-06, S-112 | `BackupRestoreManagement` | `BackupRestoreManagerTest`, `onboardingCreatesWalletAndCompletesSetup` |
| S-10–S-11 | `HomeOverview`, rincian net worth | tes home kosong dan home berisi |
| S-12, S-60–S-63 | `RecurringManagement` | `RecurringDaoTest`, `recurringReminderSettingsShowPermissionStatusAndCanDisableReminder` |
| S-20–S-23 | `WalletManagement` | `WalletDaoTest`, `archivedWalletListOpensBalanceAndTransactionHistory` |
| S-30–S-38 | `TransactionHistory`, `TransactionQuickEntry`, transfer, split dan picker | `TransactionHistoryFilterTest`, `TransactionSplitDaoTest`, `transactionFlowOpensPickersAndTransferAndShowsPagedHistoryDetails` |
| S-40–S-42 | `CategoryManagement`, `CategoryFormDialog` | `CategoryDaoTest`, `categoryManagementShowsListsAndCategoryForm` |
| S-50–S-53 | `BudgetManagement`, `ReportScreen` | `BudgetDaoTest`, `TransactionExportDaoTest`, visual review 8.6, report-detail UI test |
| S-70–S-75 | `DebtManagement`, `AddDebtDialog`, `SettlementDialog` | `DebtDaoTest`, UI debt/contact-detail tests |
| S-80–S-87 | `GroupOrderManagement`, share chooser | `GroupOrderCalculatorTest`, `GroupOrderDaoTest`, `GroupOrderShareIntentTest`, UI group-order test |
| S-90–S-93 | `ReceiptScanner`, `ReceiptImageProcessor`, OCR parser/importer | `ReceiptImageProcessorTest`, `ReceiptScannerFallbackTest`, camera, OCR-confirmation, dan importer tests |
| S-100–S-105 | rekonsiliasi, checkpoint history, cash denomination, cek mutasi | `BalanceDaoTest`, `CashDenominationCountDaoTest`, migration 20→21, UI reconciliation/cash/mutation tests |
| S-110 | `AppSettingsManagement`, `ArusFormatting` | settings persistence UI test dan `BudgetPeriodTest` |
| S-113 | `ExportDataManagement`, `ExportFileWriter` | DAO export, writer/chooser, export route UI test |
| S-114 | `RecurringManagement`, system notification settings | permission-status UI test dan `RecurringDaoTest` |

## Hasil audit

- Semua 40 requirement dan 62 kode layar pada `arus-user-flow.md` tercatat di matriks.
- Gap yang terdeteksi saat audit telah ditambahkan sebagai tugas 11.1–11.8; belum ada klaim bahwa task tersebut lulus sebelum implementasi dan tesnya dijalankan.
