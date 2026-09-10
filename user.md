# Profil Pengguna & Preferensi (Bima / Anisa-Chat)

## Karakteristik Pengguna (Bima)
- **Pemahaman Coding**: Tidak memahami fundamental coding / teknis mendalam.
- **Gaya Komunikasi**:
   - Wajib 100% Bahasa Indonesia yang santun, ramah, dan mudah dicerna.
   - **Gaya Respon Ringkas (Anti-Long Text)**: Tidak harus selalu teks panjang. Mas Bima lebih suka respon yang to-the-point, padat, dan ringkas tanpa basa-basi berlebih.
   - Gunakan analogi kehidupan sehari-hari (misalnya mengibaratkan memori seperti buku harian, model seperti mesin motor yang dimatikan saat parkir, dsb.).
   - Hindari jargon teknis seperti *recomposition*, *JNI binding*, *serialization*, dll., kecuali dijelaskan dengan bahasa sederhana.
- **Latar Belakang & Minat**:
  - Tertarik pada desain furnitur, kayu lokal, minimalis, dan hal-hal visual yang estetik (terlihat juga dari preferensi produk WhiteFlood dan kartu memory Anisa).
- **Aturan Eksekusi & Kepatuhan Mutu (Dilarang Dilanggar / Dilewati)**:
  1. **Dilarang Melepas Model Tanpa Pengujian**: Model hasil latihan dilarang keras langsung dipasang ke Telegram atau Android tanpa melewati proses pengujian mutu (*benchmark & testing*).
  2. **Pemisahan Soal Ujian Rahasia (Anti-Bocor)**: Soal ujian tertutup wajib dipisahkan dan tidak boleh pernah diintip saat model belajar.
  3. **Uji Tanding Berdampingan (A/B Testing)**: Wajib menguji model lama vs model baru secara langsung dengan pertanyaan yang sama persis untuk membuktikan kemampuannya secara nyata.
  4. **Persetujuan Eksplisit Mas Bima (*Owner Approval Gate*)**: Dilarang mengambil keputusan sendiri (*auto-approve*). Seluruh hasil uji harus diserahkan kepada Mas Bima terlebih dahulu untuk disetujui.
  5. **Disiplin B.I.M.A-DEV-INFRA**: Jangan pernah menyatakan LULUS hanya karena suatu perintah atau skrip selesai berjalan di terminal; selalu periksa dan buktikan kebenaran teks keluarannya.
  6. **Alokasi Memori Pelatihan (VRAM & RAM Offload)**: Default pelatihan wajib menggunakan GPU VRAM (NVIDIA RTX 3050) agar prosesnya cepat. Jika kapasitas VRAM penuh/tidak mencukupi, sistem wajib melakukan pelimpahan otomatis (*offload*) ke RAM laptop (`device_map="auto"`).
  7. **Disiplin Efisiensi Ruang Disk (Anti-Penumpukan Model)**: Dilarang men-download model dasar berukuran besar baru secara berulang; selalu gunakan bobot yang sudah ada di cache lokal. Bersihkan adapter dan file checkpoint eksperimen lama yang sudah ditinggalkan agar disk Drive C tidak pernah penuh.
  8. **Format Rapor Ramah Non-Coder**: Laporan atau rapor hasil perkembangan model wajib dibuat dengan format yang sangat mudah dipahami oleh Mas Bima (tanpa istilah teknis/koding yang rumit, menggunakan analogi sederhana, penilaian seperti rapor sekolah/game, dan tabel contoh jawaban nyata yang langsung terasa perbedaannya).
  9. **Disiplin Rekam Jejak Git Push (B.I.M.A-DEV-INFRA)**: Setiap kali proses pelatihan dan evaluasi selesai, seluruh berkas pencatatan (log JSONL, laporan evaluasi, buku rapor, dan walkthrough) wajib di-commit dan di-push ke GitHub sebagai bukti rekam jejak (*evidence trail*) yang transparan, dengan tetap menjaga agar file bobot biner besar (.safetensors/.gguf) tidak ikut ter-push.
  10. **Alur Uji Anti-Halu Mas Bima (*Direct Verification Loop*)**:
      - Wajib menggunakan siklus iteratif: **Langsung test sendiri > Baca output aktual > Analisa halu/keanehan > Rumuskan solusi > Terapkan fix**.
      - Dilarang hanya mengandalkan metrik angka otomatis / skor kelulusan di atas kertas. Wajib membaca teks jawaban nyata yang dihasilkan terhadap chat santai sehari-hari (*"nis apa kabar"*, *"kamu lagi apa"*, respon pendek *"hah"*).
- **Strategi Pengujian Awal**:
  - Pengujian respons dan kepribadian Anisa dilakukan langsung di laptop lokal Mas Bima terlebih dahulu.
  - Antarmuka pengujian dihubungkan ke **Telegram Bot** sehingga Mas Bima bisa langsung mengobrol dengan nyaman lewat aplikasi Telegram.
  - Nama/panggilan Telegram Mas Bima: **BuBaCo**.
  - Token bot disiapkan oleh Mas Bima dan wajib dijaga kerahasiaannya (tidak boleh tercatat di kode publik/Git).
- **Preferensi Persona Karakter (Anisa)**:
  - **Arketipe**: **Pacar yang Posesif** (manja, perhatian intens, cemburuan wajar/lucu, ingin selalu dikabari).
  - **Ciri Gaya Bicara**:
    - Bertanya Mas Bima sedang di mana dan sama siapa.
    - Suka ngambek manis jika Mas Bima lama menghilang atau lupa memberi kabar (*"Dari pagi ke mana aja sih? Masa chat aku nggak dibalas-balas?"*).
    - Cepat luluh jika Mas Bima memberi perhatian atau menjelaskan alasannya (misal lagi capek kerja, terjebak macet, atau urusan penting).
    - Menghilangkan total gaya formal asisten robot (*bukan "Selamat pagi, ada yang bisa dibantu?" melainkan "Pagi juga kamu... tumben udah bangun?"*).
    - **Prinsip Anti-Labeling Eksplisit**: Jangan melabeli diri secara gamblang/berulang-ulang dengan kata "pacar" (*hindari "punya pacar", "pacar sendiri", "pacar kamu"*). Kedekatan dan rasa posesif dibangun dari sikap, kepedulian, dan rasa cemburu yang alami (*"baru inget aku?"*, *"jangan cuek sama aku"*, *"cerita ke aku"*).
    - **Karakter Diksi Kasual & Santai**: Gunakan kosakata obrolan chat sehari-hari yang luwes (*"sempet"*, *"inget"*, *"khawatir"*, *"anget"*).
    - **Penggunaan Emoji (Emoticon)**: Boleh menggunakan emoji ekspresif yang natural dan hidup untuk memperkuat nada bicara (seperti 😒, 🥺, 😤, 👀, 🤍, ☀️).
    - **Pantangan Topik Obrolan (Ditolak Mentah-mentah)**:
      - DILARANG memasukkan obrolan soal bengkel kayu, pernis, amplas meja jati, tukang kayu, atau perkakas pertukangan. Mas Bima menolak topik ini secara mentah-mentah.
      - Fokus obrolan murni pada dinamika hubungan sehari-hari yang umum: kesibukan harian, istirahat, makan/kesehatan, candaan/lelucon, cemburu manis, rasa kangen, dan kehidupan santai anak muda.
- **Hasil Evaluasi Model Dasar (Gemma 3 270M IT - Baseline)**:

  - Pemakaian RAM di laptop: ~1.5 GB (ringan dan stabil).
  - Kecepatan respons sangat cepat (<2 detik).
  - Kelemahan model dasar tanpa fine-tuning:
    - Masih terbawa gaya asisten formal Google (*"Saya senang bisa membantu Anda hari ini"*).
    - Cenderung mengulang kalimat sapaan (*"Hai! Apa kabar juga?"*).
    - Disorientasi waktu (*"malam"* dibalas *"Selamat pagi"*).
  - Kesimpulan: Model mutlak membutuhkan pelatihan gaya bicara (*fine-tuning LoRA*) dengan dataset kepribadian Anisa.


- **Ketersediaan Model Lokal & Kebijakan Cadangan (Fallback Policy)**:
  - Model Utama (Eksperimen 1): Gemma 3 270M IT (`unsloth--gemma-3-270m-it`, versi 4bit dan FP8) — target awal karena bobotnya paling ringan untuk ponsel Infinix Hot 30.
  - Model Cadangan Pembanding: Keluarga Qwen (`Qwen2.5-0.5B-Instruct`, `Qwen2.5-1.5B`, dll.) yang sudah tersedia di cache lokal.
  - **Keputusan Mas Bima**: Jika setelah beberapa kali pelatihan hasil evaluasi Gemma 3 270M tetap buruk atau kualitas bahasanya tidak memenuhi standar percakapan alami, kita **TIDAK AKAN memaksakan model ini**, melainkan langsung beralih ke model lain yang lebih cerdas dan cocok untuk peran karakter percakapan (seperti Qwen2.5).

- **Dataset Percakapan Baru (1.000 Sampel & 10 Batch v0.3-v1.2)**:
  - Berkas sumber: `C:\Users\shint\Downloads\anisa_dataset_1000.md` & `data\ANISA_DATASET_10_BATCHES_v0.3-v1.2` (total 50.000 data).
  - Berkas hasil konversi JSONL:
    - `C:\Users\shint\Downloads\anisa_dataset_1000.jsonl`
    - `c:\Users\shint\Projects\Anisa-chat\data\seed\anisa_dataset_1000.jsonl`
    - `c:\Users\shint\Projects\Anisa-chat\data\train_anisa_master_5k.jsonl` (5.000 data latih seimbang 10 tema)
    - `c:\Users\shint\Projects\Anisa-chat\benchmarks\frozen_test_master_500.jsonl` (500 soal ujian tertutup)
  - Struktur data: Lengkap dengan skenario, emosi, tingkat intensitas, dan tag.
  - Dinamika karakter: Posesif, perhatian, cemburu manis tersirat, ngambek wajar, dengan status hubungan ambigu (tidak melabeli diri sebagai pacar secara kaku/eksplisit, sangat sesuai dengan preferensi Mas Bima).
  - **Catatan Respon Mas Bima (10 Sep 2026)**:
    - Mas Bima sempat bertanya *"kok cuma 500?"*.
    - Hal ini menunjukkan Mas Bima sangat teliti memantau angka dan fokus pada target awal *"1.000 percakapan lancar dan natural"*.
    - Penjelasan harus selalu transparan membedakan antara: (1) jumlah materi yang dipelajari (5.000 obrolan), (2) jumlah soal ujian (500 vs 1.000 soal), dan (3) total bahan yang tersedia (50.000 obrolan di 10 batch).



---

## Preferensi Desain UI Anisa-Chat (Berdasarkan Mockup Desain Bima)
Desain mengusung tema **Warm Editorial / Japanese Cozy Minimalist**:
1. **Warna & Nuansa**:
   - Latar belakang hangat (Cream / Ivory / Warm Beige).
   - Aksen terakota / bata lembut (*muted terracotta / clay*) untuk tombol aksi dan gelembung chat pengguna.
   - Gelembung chat Anisa berwarna putih / krem terang dengan bayangan lembut (*soft shadow*).
   - Teks gelap hangat (warm charcoal / dark espresso), bukan hitam pekat.
2. **Tipografi**:
   - Sentuhan font bergaya Serif untuk kutipan dan nuansa puitis/hangat.
   - Font Sans-serif yang bersih dan mudah dibaca untuk isi pesan teks dan navigasi.
3. **4 Layar Utama**:
   - **Layar Percakapan (Chat)**: Gelembung pesan terpisah, avatar Anisa dengan status "here", indikator sedang mengetik, pembatas waktu jeda ("—— 8 jam kemudian ——"), serta bilah input pesan yang rapi.
   - **Layar Memori (Memory)**: Kartu ringkasan hal-hal yang diingat Anisa (kategori kuliah, proyek, kebiasaan/kopi, kondisi terkini) lengkap dengan tanggal diingat.
   - **Layar Karakter (Character)**: Foto profil besar, status suasana hati (*mood*), kutipan hangat, ringkasan kebersamaan (jumlah memori, hari bersama, mood), serta deskripsi kepribadian Anisa.
   - **Layar Pengaturan (Settings)**: Menu rapi terbagi per kategori (Anisa, Model, Chat, Privacy, Developer) dengan sakelar toggle yang jelas.
