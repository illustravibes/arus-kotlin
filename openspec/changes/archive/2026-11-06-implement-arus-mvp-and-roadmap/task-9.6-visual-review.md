# Catatan Verifikasi Task 9.6

## Tes integritas

- AVD `arus_api26`, Android 8.0.
- `:app:testDebugUnitTest` dan `:app:connectedDebugAndroidTest` lulus; 80 tes instrumentasi lulus.
- Skenario group order mencakup finalisasi split Rp 400, pelunasan sebagian Rp 100, sisa utang Rp 100, lalu pelunasan akhir dan status `SETTLED`.
- Tes UI juga memastikan detail hanya menampilkan satu judul order.

## Review visual

- Wireframe acuan: `wireframes/GroupOrder.dc.html`.
- Screenshot bersih tersimpan sementara di `/private/var/folders/ph/8b8cxcks2nj3fr08slv9ldwc0000gn/T/opencode/arus-group-order-review-final.png` dan `/private/var/folders/ph/8b8cxcks2nj3fr08slv9ldwc0000gn/T/opencode/arus-group-order-detail-partial-final.png`.
- Wizard cocok pada struktur lima langkah, rincian item/biaya per peserta, total, dan aksi finalisasi.
- Perbedaan disengaja: aplikasi memakai Plus Jakarta Sans dan token desain Arus; wireframe memakai Shantell Sans, garis hitam tebal, bayangan, dan kartu abu-abu.
- Detail peserta memperluas wireframe dengan status utang, sisa tagihan, pelunasan, berbagi, dan pembatalan.
- Review menemukan warna teks tombol utama keliru, pesan sukses memakai warna error, dan judul detail tampil ganda. Warna dan pesan diperbaiki lalu ditinjau ulang; tes UI memastikan judul detail tampil satu kali.

## Status

Review visual dan tes integritas end-to-end selesai. Task 9.6 siap ditutup.
