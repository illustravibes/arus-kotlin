# Spec Delta

## Purpose

Memberikan ringkasan keuangan yang bisa ditelusuri, termasuk aset, kewajiban, dompet, dan aktivitas terbaru.

## ADDED Requirements

### Requirement: Ringkasan beranda
Aplikasi SHALL menampilkan total aset dompet yang disertakan, dompet utama, transaksi terbaru, dan akses ke tugas atau rincian yang relevan.

#### Scenario: Beranda memiliki dompet
- **WHEN** pengguna membuka beranda setelah menyiapkan dompet
- **THEN** aplikasi menampilkan ringkasan saldo dan dompet yang dapat dibuka ke detail

### Requirement: Perhitungan net worth MVP
Aplikasi SHALL menghitung net worth MVP dari aset dompet yang disertakan dikurangi saldo terpakai dompet kredit.

#### Scenario: Rincian net worth
- **WHEN** pengguna membuka rincian net worth
- **THEN** aplikasi menampilkan komponen aset dan kewajiban kredit yang membentuk nilai tersebut
