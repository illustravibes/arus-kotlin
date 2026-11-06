# Spec Delta

## Purpose

Menyediakan kontrol pengeluaran berbasis periode, laporan yang berasal dari split transaksi, dan pencatatan transaksi rutin dengan eksekusi yang transparan.

## ADDED Requirements

### Requirement: Anggaran periode
Aplikasi SHALL mendukung anggaran total atau kategori dengan periode mingguan, bulanan, dan tahunan. Pemakaian dihitung dari split pengeluaran aktif yang masuk budget; aplikasi menampilkan progres, rincian, serta peringatan. Anggaran aktif kategori yang sama SHALL NOT tumpang tindih.

Fondasi split transaksi (`transaction_split`, `counts_in_report`, dan `counts_in_budget`) tersedia sejak MVP; pembuatan split multi-kategori dan fitur anggaran tetap bagian Fase 2.

#### Scenario: Periode dan progres anggaran
- **WHEN** pengguna melihat anggaran untuk periode lokal yang dipilih
- **THEN** tanggal awal masuk dan batas akhir tidak masuk, dan aplikasi menampilkan pemakaian dari split yang ditandai `counts_in_budget`

#### Scenario: Peringatan dan rollover anggaran
- **WHEN** pemakaian mencapai ambang peringatan atau 100%, atau periode baru dimulai dengan rollover aktif
- **THEN** aplikasi memberi peringatan; rollover membawa sisa periode lama dalam batas nol sampai nominal dasar dan tidak mengubah ledger

### Requirement: Pembagian transaksi multi-kategori
Aplikasi SHALL memungkinkan pengguna membagi nominal transaksi pemasukan atau pengeluaran ke beberapa kategori, dengan jumlah split sama dengan nominal transaksi sebelum disimpan.

#### Scenario: Split transaksi tidak seimbang
- **WHEN** jumlah split berbeda dari nominal transaksi
- **THEN** aplikasi menolak penyimpanan dan menunjukkan selisih yang harus diperbaiki

### Requirement: Pengaturan reminder dan izin notifikasi
Aplikasi SHALL memungkinkan pengguna mengatur waktu serta jenis pengingat lokal, menampilkan status izin notifikasi, dan memberi akses ke pengaturan izin sistem.

#### Scenario: Izin notifikasi ditolak
- **WHEN** pengguna menolak izin notifikasi atau mematikannya di pengaturan sistem
- **THEN** occurrence rutin tertunda tetap tersedia di inbox, dan aplikasi menampilkan status izin serta langkah untuk mengubahnya

### Requirement: Laporan transaksi
Aplikasi SHALL menghitung laporan pemasukan dan pengeluaran dari transaksi serta split yang ditandai masuk laporan, tanpa menghitung transfer dan pelunasan sebagai pendapatan atau pengeluaran.

#### Scenario: Transfer tidak dihitung sebagai pengeluaran
- **WHEN** laporan periode memuat transfer antar dompet
- **THEN** transfer tidak menambah total pemasukan atau pengeluaran laporan

### Requirement: Rincian kategori laporan
Aplikasi SHALL memungkinkan pengguna membuka kategori laporan dan melihat transaksi pembentuk total kategori pada periode terpilih.

#### Scenario: Membuka rincian kategori laporan
- **WHEN** pengguna memilih kategori pada laporan periode
- **THEN** aplikasi menampilkan transaksi aktif kategori tersebut dalam rentang periode yang sama

### Requirement: Transaksi rutin transparan
Aplikasi SHALL mendukung aturan transaksi berulang dengan mode otomatis atau konfirmasi, serta mencegah occurrence ganda untuk aturan dan tanggal yang sama.

#### Scenario: Transaksi rutin perlu konfirmasi
- **WHEN** occurrence mode konfirmasi jatuh tempo
- **THEN** transaksi belum dicatat diam-diam dan pengguna dapat mencatat, mengubah nominal, atau melewati occurrence

#### Scenario: Occurrence tertunda memberi pengingat
- **WHEN** occurrence mode konfirmasi menunggu keputusan
- **THEN** aplikasi menampilkannya di inbox rutin dan menjadwalkan pengingat lokal; izin notifikasi dapat ditolak

### Requirement: Pencarian dan filter riwayat
Aplikasi SHALL memungkinkan pengguna mencari transaksi berdasarkan catatan atau merchant dan memfilter berdasarkan dompet, jenis, kategori, nominal, serta rentang tanggal lokal dengan batas akhir eksklusif.

#### Scenario: Filter riwayat digabung
- **WHEN** pengguna mengisi pencarian dan memilih dompet, jenis, atau rentang tanggal
- **THEN** riwayat hanya menampilkan transaksi aktif yang memenuhi semua filter

#### Scenario: Filter riwayat mencakup kategori dan nominal
- **WHEN** pengguna memilih kategori serta batas nominal minimum atau maksimum
- **THEN** riwayat hanya menampilkan transaksi aktif dalam batas yang dipilih

### Requirement: Riwayat rekonsiliasi
Aplikasi SHALL menampilkan checkpoint rekonsiliasi per dompet, termasuk saldo tercatat, saldo aktual, selisih, resolusi, dan status pemeriksaan ulang.

#### Scenario: Checkpoint berubah setelah transaksi terverifikasi diedit
- **WHEN** pengguna membuka riwayat rekonsiliasi setelah transaksi terverifikasi berubah
- **THEN** checkpoint terkait ditandai perlu rekonsiliasi ulang

#### Scenario: Izin notifikasi ditolak
- **WHEN** izin notifikasi tidak tersedia atau ditolak
- **THEN** occurrence konfirmasi tetap terlihat di inbox dan tidak diubah statusnya oleh kegagalan notifikasi
