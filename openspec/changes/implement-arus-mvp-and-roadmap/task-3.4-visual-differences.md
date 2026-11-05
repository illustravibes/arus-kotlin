# Catatan Pemeriksaan Visual Task 3.4

## Referensi

- `wireframes/Sambutan.dc.html`
- `wireframes/SetupDompet.dc.html`
- `wireframes/PilihKategori.dc.html`
- `wireframes/Dompet.dc.html`
- `wireframes/Kategori.dc.html`
- `wireframes/FormDompet.dc.html`
- `wireframes/FormKategori.dc.html`

## Selisih disengaja

- Font wireframe memakai Shantell Sans. Aplikasi memakai Plus Jakarta Sans sesuai keputusan design system produk.
- Wireframe onboarding mencakup sambutan, penyiapan dompet, dan pilihan kategori. UI mempertahankan alur tiga langkah onboarding saat ini; keamanan tetap dapat dilewati sebagaimana spesifikasi.
- Wireframe dompet menampilkan total aset, kelompok tipe dompet, deskripsi, dan saldo terhitung. Total aset dan saldo per dompet ditunda sampai agregasi saldo dikerjakan pada Task 5.1; daftar tetap menunjukkan saldo awal agar tidak memberi angka saldo yang keliru.
- Wireframe kategori menekankan daftar ringkas, tab pemasukan/pengeluaran, tambah kategori, dan menu tindakan. Aplikasi mempertahankan pencarian, pengaturan kategori default dompet, edit/arsip/urut/hapus untuk memenuhi perilaku Task 3.3; tindakan kategori dipindah ke menu overflow.
- Detail warna, ketebalan garis, radius, dan tombol memakai token custom Arus yang mengikuti struktur visual wireframe.

## Verifikasi

- Build debug: `./gradlew :app:assembleDebug`.
- Instrumentasi emulator API 26: `./gradlew :app:connectedDebugAndroidTest`.
- Pemeriksaan screenshot emulator dilakukan untuk onboarding sambutan. Screenshot awal dibandingkan dengan `wireframes/Sambutan.dc.html`; hasil setelah penyesuaian menunjukkan ilustrasi placeholder, latar abu muda, indikator langkah, dan tombol sesuai komposisi wireframe.
- Daftar dompet dan kategori ditinjau terhadap wireframe HTML dan disesuaikan pada struktur layar, kartu, seleksi tipe, ikon, dan menu tindakan.
- Task 3.3 menampilkan kategori sistem teknis seperti `ADMIN_FEE`, `WRITE_OFF`, `INTEREST_PENALTY`, dan `BALANCE_ADJUST` saat onboarding karena seed MVP menyimpan semua kategori. Onboarding hanya menampilkan kategori bawaan yang dapat dipilih; kategori teknis tetap tersedia saat dibutuhkan alur domain terkait.
