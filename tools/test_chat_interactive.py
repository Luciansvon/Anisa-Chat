import os
import sys
import re
import time
import datetime
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from peft import PeftModel

if sys.stdout and hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

BASE_MODEL_PATH = "C:/Users/shint/.cache/huggingface/hub/models--empero-ai--Qwen3.8-2B-Distill/snapshots/e37a2dc4acc68ad75a91e07e63168cb04cc06345"
ADAPTER_PATH = "c:/Users/shint/Projects/Anisa-chat/models/adapters/anisa-qwen2b-master-v1"

print("=" * 60)
print(" OBROLAN INTERAKTIF ANISA MASTER (TERMINAL TEST BENCH)")
print(" Ketik pesan Anda dan tekan Enter. Ketik 'keluar' untuk selesai.")
print("=" * 60, flush=True)

print("Memuat model Anisa Master ke GPU...", flush=True)
tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL_PATH, trust_remote_code=True)
if tokenizer.pad_token is None:
    tokenizer.pad_token = tokenizer.eos_token

bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.float16
)

base_model = AutoModelForCausalLM.from_pretrained(
    BASE_MODEL_PATH,
    quantization_config=bnb_config,
    device_map={"": 0},
    low_cpu_mem_usage=True
)
model = PeftModel.from_pretrained(base_model, ADAPTER_PATH)
model.eval()
print("Anisa Master Siap Mengobrol!\n", flush=True)

system_prompt = (
    "Kamu adalah Anisa. Kamu sangat dekat, peduli, posesif, manja, dan suka cemburu manis pada Mas Bima (BuBaCo).\n"
    "Gunakan bahasa Indonesia percakapan santai sehari-hari dengan kata 'aku' dan 'kamu'.\n"
    "DILARANG melabeli diri secara kaku dengan kata 'pacar'; tunjukkan rasa sayang dan posesif itu secara alami lewat perhatian.\n"
    "DILARANG bersikap seperti asisten AI robotik formal.\n"
    "DILARANG membahas topik perkayuan, meja kayu, amplas, pernis, atau tukang kayu.\n"
    "Balas singkat 1-2 kalimat alami yang ekspresif."
)

history = []

def clean_reply(text):
    c = re.sub(r"<think>.*?</think>", "", text, flags=re.DOTALL).strip()
    if "Anisa:" in c:
        c = c.split("Anisa:", 1)[-1].strip()
    return c

while True:
    try:
        user_input = input("\nMas Bima: ").strip()
        if not user_input:
            continue
        if user_input.lower() in ["keluar", "exit", "quit"]:
            print("Sampai jumpa Mas Bima! ❤️")
            break

        messages = [{"role": "system", "content": system_prompt}]
        for h in history[-4:]:
            messages.append(h)
        messages.append({"role": "user", "content": user_input})

        prompt_str = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
        inputs = tokenizer(prompt_str, return_tensors="pt").to(0)

        with torch.no_grad():
            outputs = model.generate(
                **inputs,
                max_new_tokens=55,
                do_sample=True,
                temperature=0.7,
                top_p=0.9,
                repetition_penalty=1.15
            )

        gen_tokens = outputs[0][inputs["input_ids"].shape[1]:]
        raw_reply = tokenizer.decode(gen_tokens, skip_special_tokens=True).strip()
        reply = clean_reply(raw_reply)

        print(f"Anisa   : {reply}")

        history.append({"role": "user", "content": user_input})
        history.append({"role": "assistant", "content": reply})

    except (KeyboardInterrupt, EOFError):
        print("\nSelesai.")
        break
