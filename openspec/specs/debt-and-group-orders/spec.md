# debt-and-group-orders Specification

## Purpose

Mencatat utang-piutang dua arah dan pembagian biaya bersama secara akurat, dengan alokasi pelunasan yang dapat ditelusuri.

## Requirements

### Requirement: Catatan utang-piutang dua arah
Aplikasi SHALL mendukung catatan piutang dan utang yang dapat dibuat dengan pergerakan dompet atau sebagai catatan lama tanpa perubahan saldo.

#### Scenario: Catat piutang lama
- **WHEN** pengguna membuat piutang dengan pilihan catat saja
- **THEN** piutang tersimpan tanpa transaksi asal dan saldo dompet tidak berubah

### Requirement: Pelunasan teralokasi
Aplikasi SHALL mencatat penerimaan pelunasan piutang atau pembayaran utang serta mengalokasikannya ke satu atau lebih catatan tanpa menghitungnya sebagai pemasukan atau pengeluaran biasa.

#### Scenario: Pembayaran cicilan utang
- **WHEN** pengguna mencatat pembayaran sebagian untuk utang yang dipinjam
- **THEN** saldo dompet berkurang, sisa utang diperbarui, dan pembayaran tidak masuk laporan pengeluaran

### Requirement: Netting pada kontak yang sama
Aplikasi SHALL memungkinkan saling hapus hanya untuk catatan berlawanan arah milik kontak yang sama, hingga nilai yang lebih kecil dari saldo masing-masing.

#### Scenario: Netting piutang dan utang
- **WHEN** pengguna memilih piutang dan utang terbuka milik kontak yang sama untuk netting
- **THEN** sistem mengalokasikan nominal sebesar sisa yang lebih kecil tanpa mengubah saldo dompet

### Requirement: Rincian kontak
Aplikasi SHALL menampilkan total piutang, total utang, sisa berjalan, dan riwayat catatan utang-piutang untuk kontak yang dipilih.

#### Scenario: Membuka detail kontak
- **WHEN** pengguna memilih kontak dari daftar utang-piutang
- **THEN** aplikasi menampilkan ringkasan saldo dua arah dan daftar catatan terkait

### Requirement: Finalisasi group order
Aplikasi SHALL memfinalisasi group order hanya jika seluruh item memiliki pemilik dan total tagihan peserta sama dengan total yang dibayar.

#### Scenario: Item belum ditetapkan
- **WHEN** pengguna mencoba melanjutkan atau memfinalisasi order dengan item tanpa pemilik
- **THEN** aplikasi menolak finalisasi dan menunjukkan item yang belum ditetapkan

### Requirement: Pembagian group order
Aplikasi SHALL membuat satu transaksi pembayaran dan split untuk bagian pribadi serta piutang peserta, dengan total split sama dengan nilai transaksi.

#### Scenario: Order final dan seimbang
- **WHEN** pengguna memfinalisasi order dengan alokasi peserta yang jumlahnya sama dengan grand total
- **THEN** sistem membuat transaksi, split, dan catatan piutang secara atomik
