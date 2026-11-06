# Verifikasi Task 10.5

## Review visual Fase 4

- Acuan layar: `wireframes/OCR.dc.html`, `wireframes/Backup.dc.html`, dan `wireframes/Export.dc.html`.
- Layar OCR mempertahankan hierarki foto, hasil total/tanggal/merchant, penanda `Cek ulang`, teks OCR, dan aksi transaksi atau Group Order. Input transaksi lanjutan menampilkan saran kategori merchant sebelum konfirmasi simpan.
- Layar backup memakai dua bagian, buat dan pulihkan, dengan input kata sandi penuh lebar serta konfirmasi overwrite. Berbeda dari wireframe Google Drive, produk mengikuti keputusan SAF: pengguna memilih file/lokasi, tanpa OAuth atau backup otomatis.
- Layar ekspor menampilkan pilihan format, tanggal mulai/akhir, simpan file, dan berbagi melalui chooser. Filter dompet/kategori dan opsi lampiran/selisih dari mockup tidak diimplementasikan: spec ekspor menetapkan rentang tanggal, dan task 10.4 tidak menetapkan filter tersebut.
- Gaya mengikuti token Arus, bukan font Shantell Sans, garis tebal, dan bayangan mockup. Screenshot debug: `/private/var/folders/ph/8b8cxcks2nj3fr08slv9ldwc0000gn/T/opencode/arus-task10-5-onboarding-final.png` dan `/private/var/folders/ph/8b8cxcks2nj3fr08slv9ldwc0000gn/T/opencode/arus-task10-5-backup-final.png`.
- Review menemukan tombol Pulihkan backup onboarding terpotong pada layar API 26. Kartu ilustrasi dipadatkan; screenshot terbaru menunjukkan tombol Pulihkan backup terlihat utuh.
- Field kata sandi backup sebelumnya sempit. Field, tombol buat, dan pemilih restore kini memenuhi lebar konten. Pilihan format ekspor memakai lebar setara agar tidak meluber pada layar ponsel.

## Keamanan dan restore end-to-end

- Tes AVD membuat backup berisi database SQLCipher dan foto lampiran, lalu menghapus foto lokal sebelum restore. Restore harus mengambil foto dari arsip backup, bukan folder perangkat lama.
- Password salah dan versi skema lebih baru ditolak; dua wallet lokal tetap utuh sebelum restore yang sah.
- Restore sah memulihkan wallet, transaksi, teks OCR, dan foto. Tes menutup lalu membuka kembali database untuk memverifikasi passphrase hasil restore tetap dapat membuka data.
- Validasi lampiran staged kini memeriksa berkas hasil ekstraksi backup. Ini memperbaiki restore pada perangkat baru yang belum memiliki folder foto.
- Tes khusus restore lulus. Suite unit dan instrumentasi penuh AVD API 26 lulus **97/97**; `git diff --check` bersih dan logcat tidak menunjukkan crash.
