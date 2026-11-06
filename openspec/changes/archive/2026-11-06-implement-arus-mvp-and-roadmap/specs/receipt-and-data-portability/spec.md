# Spec Delta

## Purpose

Mempercepat pencatatan dari struk dan memberi pengguna cara terenkripsi untuk mencadangkan, memulihkan, serta mengekspor data keuangan.

## ADDED Requirements

### Requirement: OCR memerlukan konfirmasi
Aplikasi SHALL menampilkan hasil OCR dalam form yang dapat diperiksa dan tidak membuat transaksi tanpa konfirmasi eksplisit pengguna.

#### Scenario: Total struk terbaca
- **WHEN** OCR menghasilkan nominal dan merchant
- **THEN** aplikasi menampilkan hasil beserta penanda keyakinan agar pengguna meninjau sebelum menyimpan transaksi

### Requirement: Backup dan restore terenkripsi
Aplikasi SHALL mengenkripsi backup dan meminta konfirmasi eksplisit sebelum restore mengganti seluruh data lokal.

#### Scenario: Memulihkan versi skema lebih baru
- **WHEN** pengguna memilih backup dengan skema yang lebih baru daripada versi aplikasi
- **THEN** aplikasi menolak restore dan meminta pengguna memperbarui aplikasi

### Requirement: Ekspor data pengguna
Aplikasi SHALL memungkinkan pengguna mengekspor catatan keuangan untuk rentang tanggal yang dipilih dalam format yang didukung aplikasi.

#### Scenario: Ekspor rentang tanggal
- **WHEN** pengguna memilih rentang tanggal dan format ekspor
- **THEN** aplikasi menghasilkan berkas berisi data dalam rentang tersebut dan menawarkan penyimpanan atau berbagi melalui mekanisme sistem
