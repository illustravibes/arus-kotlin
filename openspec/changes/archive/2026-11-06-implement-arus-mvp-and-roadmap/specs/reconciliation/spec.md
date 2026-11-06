# Spec Delta

## Purpose

Membantu pengguna mencocokkan saldo dompet aktual dengan saldo hasil pencatatan dan menandai riwayat yang telah diverifikasi.

## ADDED Requirements

### Requirement: Resolusi selisih saldo
Aplikasi SHALL membandingkan saldo aktual dengan saldo hasil hitung dan menyediakan resolusi berupa pencatatan transaksi tertinggal, pemecahan selisih, atau pencatatan selisih tak terlacak.

#### Scenario: Selisih ditemukan
- **WHEN** pengguna memasukkan saldo aktual yang berbeda dari saldo aplikasi
- **THEN** aplikasi menampilkan kedua saldo, nominal selisih, dan pilihan untuk menyelesaikan selisih

### Requirement: Checkpoint verifikasi
Aplikasi SHALL menyimpan checkpoint rekonsiliasi dan menandai transaksi dompet sampai waktu checkpoint sebagai terverifikasi.

#### Scenario: Checkpoint disimpan
- **WHEN** pengguna menyelesaikan rekonsiliasi pada waktu efektif tertentu
- **THEN** checkpoint menyimpan saldo aplikasi, saldo aktual, selisih dan resolusi, serta transaksi terkait ditandai terverifikasi

### Requirement: Perubahan transaksi terverifikasi
Aplikasi SHALL memperingatkan pengguna ketika transaksi yang telah diverifikasi diubah atau dihapus dan menandai checkpoint terkait untuk diperiksa ulang.

#### Scenario: Mengedit transaksi terverifikasi
- **WHEN** pengguna mencoba mengubah transaksi yang terkait checkpoint
- **THEN** aplikasi memperingatkan dampak perubahan dan checkpoint ditandai perlu rekonsiliasi ulang bila perubahan dilanjutkan

### Requirement: Hitung pecahan uang tunai
Aplikasi SHALL memungkinkan pengguna menghitung jumlah uang tunai dari jumlah lembar/keping setiap pecahan dan menampilkan total sebelum menyimpan hasil hitung.

#### Scenario: Total pecahan ditambahkan ke saldo aktual
- **WHEN** pengguna memasukkan jumlah untuk satu atau lebih pecahan
- **THEN** aplikasi menghitung total rupiah secara integer dan mengisi saldo aktual untuk rekonsiliasi tanpa membuat transaksi otomatis

### Requirement: Mode cek mutasi
Aplikasi SHALL memungkinkan pengguna mencocokkan transaksi dompet satu per satu dan menyimpan status cocok tanpa mengubah nominal transaksi.

#### Scenario: Mencocokkan transaksi dengan mutasi
- **WHEN** pengguna menandai transaksi sebagai cocok dalam rentang pemeriksaan
- **THEN** aplikasi menyimpan relasi pemeriksaan dan tidak mengubah saldo atau nominal ledger
