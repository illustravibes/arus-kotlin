# Spec Delta

## Purpose

Memungkinkan pengguna menyiapkan dan mengelola rekening bank, e-wallet, uang tunai, dan kredit sebagai sumber saldo dalam Arus.

## ADDED Requirements

### Requirement: Onboarding dompet awal
Aplikasi SHALL memandu pengguna membuat setidaknya satu dompet dan menetapkan saldo awal aktual sebelum menyelesaikan onboarding.

#### Scenario: Dompet pertama dibuat
- **WHEN** pengguna memilih tipe, nama atau preset, dan saldo awal
- **THEN** dompet tersimpan dengan saldo awal tersebut dan pengguna dapat melanjutkan onboarding

### Requirement: Pengelolaan dompet
Aplikasi SHALL memungkinkan pengguna membuat, melihat, mengedit, mengurutkan, dan mengarsipkan dompet.

#### Scenario: Mengarsipkan dompet bertransaksi
- **WHEN** pengguna mengarsipkan dompet yang memiliki riwayat transaksi
- **THEN** dompet tidak tersedia untuk transaksi baru tetapi tetap terlihat pada riwayat

### Requirement: Saldo dan rincian dompet
Aplikasi SHALL menampilkan saldo dompet yang dapat ditelusuri ke transaksi pembentuknya.

#### Scenario: Pengguna membuka detail saldo
- **WHEN** pengguna memilih dompet dari daftar atau beranda
- **THEN** aplikasi menampilkan saldo hasil hitung dan riwayat transaksi dompet tersebut

### Requirement: Pengaturan dompet kredit
Aplikasi SHALL meminta batas kredit dan hari tagihan opsional hanya untuk dompet bertipe kredit.

#### Scenario: Membuat dompet kredit
- **WHEN** pengguna memilih tipe Kredit
- **THEN** form menampilkan pengaturan kredit dan detail dompet membedakan pemakaian dari sisa limit
