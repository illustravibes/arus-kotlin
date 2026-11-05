# Spec Delta

## Purpose

Menyediakan kontrol pengeluaran berbasis periode, laporan yang berasal dari split transaksi, dan pencatatan transaksi rutin dengan eksekusi yang transparan.

## ADDED Requirements

### Requirement: Anggaran periode
Aplikasi SHALL menghitung pemakaian anggaran dari split yang ditandai masuk budget dan menampilkan peringatan sesuai ambang batas.

Fondasi split transaksi (`transaction_split`, `counts_in_report`, dan `counts_in_budget`) tersedia sejak MVP; pembuatan split multi-kategori dan fitur anggaran tetap bagian Fase 2.

Anggaran dapat berlaku untuk semua kategori atau satu kategori, memakai periode mingguan, bulanan, atau tahunan. Periode memakai tanggal lokal dengan batas akhir eksklusif. Penggunaan berasal dari split pengeluaran aktif yang `counts_in_budget`-nya aktif. Sistem menampilkan progres, rincian transaksi, dan peringatan pada ambang yang dikonfigurasi serta saat pemakaian mencapai atau melewati 100%. Anggaran aktif kategori yang sama tidak boleh tumpang tindih dalam periode.

Rollover membawa sisa periode sebelumnya, dibatasi nol minimum dan nominal anggaran dasar maksimum; rollover tidak menambah saldo ledger.

#### Scenario: Anggaran melewati ambang
- **WHEN** transaksi menyebabkan pemakaian kategori mencapai ambang peringatan atau batas anggaran
- **THEN** aplikasi memperbarui progress periode dan memberi peringatan kepada pengguna

### Requirement: Laporan transaksi
Aplikasi SHALL menghitung laporan pemasukan dan pengeluaran dari transaksi serta split yang ditandai masuk laporan, tanpa menghitung transfer dan pelunasan sebagai pendapatan atau pengeluaran.

#### Scenario: Transfer tidak dihitung sebagai pengeluaran
- **WHEN** laporan periode memuat transfer antar dompet
- **THEN** transfer tidak menambah total pemasukan atau pengeluaran laporan

### Requirement: Transaksi rutin transparan
Aplikasi SHALL mendukung aturan transaksi berulang dengan mode otomatis atau konfirmasi, serta mencegah occurrence ganda untuk aturan dan tanggal yang sama.

#### Scenario: Transaksi rutin perlu konfirmasi
- **WHEN** occurrence mode konfirmasi jatuh tempo
- **THEN** transaksi belum dicatat diam-diam dan pengguna dapat mencatat, mengubah nominal, atau melewati occurrence

#### Scenario: Occurrence tertunda memberi pengingat
- **WHEN** occurrence mode konfirmasi menunggu keputusan
- **THEN** aplikasi menampilkannya di inbox rutin dan menjadwalkan pengingat lokal; izin notifikasi dapat ditolak
