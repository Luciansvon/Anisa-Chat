import os
import sys
import json
import time
import datetime
import torch
from torch.utils.data import Dataset, DataLoader
from transformers import AutoTokenizer, AutoModelForCausalLM
from peft import LoraConfig, get_peft_model, TaskType

# Pastikan UTF-8 di Windows
if sys.stdout and hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

BASE_MODEL_PATH = "C:/Users/shint/.ollama/models/hub/models--unsloth--gemma-3-270m-it/snapshots/23cf460f6bb16954176b3ddcc8d4f250501458a9"
DATASET_PATH = "c:/Users/shint/Projects/Anisa-chat/data/train_anisa_950.jsonl"
OUTPUT_ADAPTER_DIR = "c:/Users/shint/Projects/Anisa-chat/models/adapters/anisa-gemma-270m-v0.2"

os.makedirs(OUTPUT_ADAPTER_DIR, exist_ok=True)

print("="*60)
print(" PELATIHAN MODEL ANISA: FINE-TUNING LORA (v0.2 - 950 DATA)")
print("="*60, flush=True)

print("\n1. Memuat Tokenizer dan Base Model...", flush=True)
device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"   Perangkat Komputasi: {device.upper()}")

tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL_PATH)
if tokenizer.pad_token is None:
    tokenizer.pad_token = tokenizer.eos_token

base_model = AutoModelForCausalLM.from_pretrained(
    BASE_MODEL_PATH,
    dtype=torch.float32,
)

# Konfigurasi LoRA
print("\n2. Mengonfigurasi Adapter LoRA...", flush=True)
lora_config = LoraConfig(
    r=16,
    lora_alpha=32,
    target_modules=["q_proj", "v_proj", "k_proj", "o_proj"],
    lora_dropout=0.05,
    bias="none",
    task_type=TaskType.CAUSAL_LM
)

model = get_peft_model(base_model, lora_config)
model.to(device)
model.print_trainable_parameters()

# Membaca data pelatihan
print("\n3. Menyiapkan Dataset Percakapan 950 Sampel...", flush=True)
system_prompt = (
    "Kamu adalah Anisa. Kamu sangat dekat, peduli, posesif, manja, dan suka cemburu manis pada Mas Bima (BuBaCo). "
    "Gunakan bahasa Indonesia percakapan santai sehari-hari dengan kata 'aku' dan 'kamu'. "
    "Gunakan kosakata natural seperti 'sempet', 'inget', 'khawatir', serta emoji ekspresif yang pas. "
    "DILARANG melabeli diri terus-menerus dengan kata 'pacar' secara kaku. Tunjukkan rasa posesif dan sayang secara alami lewat sikap dan obrolan. "
    "DILARANG bersikap formal seperti asisten robot."
)

train_data = []
with open(DATASET_PATH, "r", encoding="utf-8") as f:
    for line in f:
        line_clean = line.strip()
        if not line_clean:
            continue
        item = json.loads(line_clean)
        conv = item["conversation"]
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": conv[0]["content"]},
            {"role": "assistant", "content": conv[1]["content"]}
        ]
        text = tokenizer.apply_chat_template(messages, tokenize=False)
        train_data.append(text)

print(f"   Jumlah contoh percakapan latihan: {len(train_data)}")

# Tokenisasi dalam batch
batch_size = 8
batches = []
for i in range(0, len(train_data), batch_size):
    batch_texts = train_data[i:i + batch_size]
    enc = tokenizer(batch_texts, padding=True, truncation=True, max_length=128, return_tensors="pt")
    input_ids = enc["input_ids"].to(device)
    attention_mask = enc["attention_mask"].to(device)
    labels = input_ids.clone()
    labels[labels == tokenizer.pad_token_id] = -100
    batches.append((input_ids, attention_mask, labels))

print(f"   Total batch (ukuran {batch_size}): {len(batches)}")

# Training Loop
print("\n4. Memulai Proses Pelatihan LoRA...", flush=True)
optimizer = torch.optim.AdamW(model.parameters(), lr=3e-4, weight_decay=0.01)
epochs = 3

model.train()
start_time = time.time()

for epoch in range(1, epochs + 1):
    total_loss = 0.0
    for step_idx, (b_input_ids, b_mask, b_labels) in enumerate(batches, 1):
        optimizer.zero_grad()
        outputs = model(input_ids=b_input_ids, attention_mask=b_mask, labels=b_labels)
        loss = outputs.loss
        loss.backward()
        optimizer.step()
        total_loss += loss.item()

    avg_loss = total_loss / len(batches)
    print(f"   [Epoch {epoch}/{epochs}] Nilai Loss (Tingkat Kesalahan): {avg_loss:.4f}", flush=True)

elapsed = time.time() - start_time
print(f"\n   Pelatihan selesai dalam waktu {elapsed:.1f} detik!", flush=True)

# Simpan adapter
print(f"\n5. Menyimpan Adapter LoRA ke: {OUTPUT_ADAPTER_DIR}...", flush=True)
model.save_pretrained(OUTPUT_ADAPTER_DIR)
tokenizer.save_pretrained(OUTPUT_ADAPTER_DIR)

# Simpan metadata bukti pelatihan
evidence_meta = {
    "base_model": "google/gemma-3-270m-it",
    "dataset": "data/train_anisa_950.jsonl",
    "samples_count": len(train_data),
    "lora_r": 16,
    "lora_alpha": 32,
    "epochs": epochs,
    "batch_size": batch_size,
    "final_loss": avg_loss,
    "training_time_seconds": elapsed,
    "trained_at": datetime.datetime.now().isoformat(),
    "device": device
}
with open(os.path.join(OUTPUT_ADAPTER_DIR, "training_evidence.json"), "w", encoding="utf-8") as f:
    json.dump(evidence_meta, f, indent=2)

print("   Adapter LoRA v0.2 berhasil disimpan dan siap dievaluasi!", flush=True)
