# Registri Eksperimen Model Anisa-Chat (Experiment Registry)
**Standar Protokol**: B.I.M.A-DEV-INFRA & ClawFile (Evidence-First)
**Hardware Standar**: Laptop Mas Bima (NVIDIA GeForce RTX 3050 Laptop GPU 4 GB VRAM)

---

## Tabel Silsilah Model & Hasil Pengujian

| Run ID | Model Dasar | Metode & Konfigurasi | Dataset | Loss Akhir | VRAM Puncak | TTFT (ms) | Status & Kesimpulan |
| :--- | :--- | :--- | :--- | :---: | :---: | :---: | :--- |
| **RUN-01** | `unsloth/gemma-3-270m-it` | Baseline (Tanpa LoRA) | - | - | 1.5 GB | <200 ms | **GAGAL**: Bahasa robotik ("senang membantu Anda"), salam klise. |
| **RUN-02** | `unsloth/gemma-3-270m-it` | LoRA v0.1 (Tanpa Loss Masking) | 30 Percakapan | 0.85 | 1.9 GB | ~250 ms | **GAGAL**: Hafalan instruksi sistem (*system prompt regurgitation*). |
| **RUN-03** | `empero-ai/Qwen3.8-2B-Distill` | QLoRA 4-bit (Loss Masking Aktif) | 500 Percakapan v0.2 | 1.48 | 2.9 GB | ~450 ms | **LULUS TAHAP AWAL**: Bebas regurgitasi, persona posesif terbentuk. |
| **RUN-04** | `empero-ai/Qwen3.8-2B-Distill` | QLoRA 4-bit Master (150 Langkah) | 5.000 Sampel (10 Batch v0.3-v1.2) | 0.59 | 2.98 GB | ~638 ms | **LULUS SEMPURNA (MASTER)**: 0% robot, 0% label pacar, 0% topik kayu pada 1.000 soal ujian. |

---

## Detail Konfigurasi Master Model (RUN-04)
* **Model ID**: `anisa-qwen2b-master-v1`
* **Lokasi Adapter**: `models/adapters/anisa-qwen2b-master-v1`
* **Format Bobot**: LoRA Safetensors (Rank 16, Alpha 32, Target: q, k, v, o, gate, up, down proj)
* **Kuantisasi Inferensi**: BitsAndBytes 4-bit NF4 (`float16` compute)
* **Waktu Latih**: 19.7 menit (150 langkah x batch 1 x grad accum 4)
* **Bank Soal Evaluasi**: `benchmarks/frozen_test_master_1000.jsonl` (1.000 Soal Zero Leakage)
* **Laporan Evaluasi**: `reports/EVAL_REPORT_MASTER_V1.md`
* **Buku Rapor Pengguna**: `reports/RAPOR_HASIL_BELAJAR_ANISA.md`
