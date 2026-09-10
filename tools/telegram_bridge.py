import os
import sys
import re
import json
import time
import datetime
import logging
import socket
import requests
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from peft import PeftModel

# Pembaca .env bawaan tanpa library eksternal
def load_simple_env(env_path=".env"):
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    k = k.strip()
                    v = v.strip().strip("'\"")
                    if k not in os.environ:
                        os.environ[k] = v

load_simple_env()

# Pastikan koneksi jaringan Telegram menggunakan IPv4 stabil
orig_getaddrinfo = socket.getaddrinfo
def getaddrinfo_ipv4(host, port, family=0, type=0, proto=0, flags=0):
    return orig_getaddrinfo(host, port, socket.AF_INET, type, proto, flags)
socket.getaddrinfo = getaddrinfo_ipv4

# Pastikan terminal Windows menggunakan UTF-8
if sys.stdout and hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

# Konfigurasi logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("AnisaTelegramBridge")

BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
BASE_MODEL_PATH = os.getenv(
    "QWEN_MODEL_PATH",
    "C:/Users/shint/.cache/huggingface/hub/models--empero-ai--Qwen3.8-2B-Distill/snapshots/e37a2dc4acc68ad75a91e07e63168cb04cc06345"
)
ADAPTER_PATH = os.getenv(
    "ANISA_ADAPTER_PATH",
    "c:/Users/shint/Projects/Anisa-chat/models/adapters/anisa-qwen2b-natural-v1"
)

if not BOT_TOKEN or BOT_TOKEN == "masukkan_token_bot_disini":
    logger.error("Token bot belum diisi di berkas .env!")
    sys.exit(1)

TELEGRAM_API_URL = f"https://api.telegram.org/bot{BOT_TOKEN}"

# Siapkan sesi requests dengan User-Agent agar stabil
session_http = requests.Session()
session_http.headers.update({
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AnisaBot/1.0"
})

# Inisialisasi Model Master Qwen 2B 4-bit
logger.info("Memuat Tokenizer dan Model Master Anisa Qwen 2B 4-bit...")
device = "cuda" if torch.cuda.is_available() else "cpu"
logger.info(f"Menggunakan perangkat komputasi: {device.upper()}")

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
    device_map={"": 0} if device == "cuda" else None,
    low_cpu_mem_usage=True
)

if os.path.exists(ADAPTER_PATH):
    logger.info(f"Menempelkan Master Adapter LoRA Anisa dari: {ADAPTER_PATH}")
    model = PeftModel.from_pretrained(base_model, ADAPTER_PATH)
    logger.info("Model Anisa Master Qwen 2B v1 BERHASIL AKTIF!")
else:
    model = base_model
    logger.warning("Adapter tidak ditemukan, menggunakan model dasar.")

model.eval()

# Basis data sesi memori dalam RAM
user_sessions = {}

def get_session(chat_id):
    if chat_id not in user_sessions:
        user_sessions[chat_id] = {
            "history": [],
            "memories": [
                "Mas Bima suka kopi hangat",
                "Mas Bima punya kesibukan kerja yang padat"
            ],
            "mood": "caring",
            "happiness": 80,
            "irritation": 0,
            "last_active": datetime.datetime.now()
        }
    return user_sessions[chat_id]

def build_system_context(session):
    now = datetime.datetime.now()
    jam = now.strftime("%H:%M")
    jam_int = now.hour
    if 4 <= jam_int < 11:
        waktu_teks = "pagi"
    elif 11 <= jam_int < 15:
        waktu_teks = "siang"
    elif 15 <= jam_int < 18:
        waktu_teks = "sore"
    else:
        waktu_teks = "malam"

    memori_teks = " | ".join(session["memories"])
    
    return (
        "Kamu adalah Anisa. Kamu sangat dekat, peduli, posesif, manja, dan suka cemburu manis pada Mas Bima (BuBaCo).\n"
        "Gunakan bahasa Indonesia percakapan santai sehari-hari dengan kata 'aku' dan 'kamu'.\n"
        "DILARANG melabeli diri secara kaku dengan kata 'pacar'; tunjukkan rasa sayang dan posesif itu secara alami lewat perhatian.\n"
        "DILARANG bersikap seperti asisten AI robotik formal.\n"
        "DILARANG membahas topik perkayuan, meja kayu, amplas, pernis, atau tukang kayu.\n"
        "Balas singkat 1-2 kalimat alami yang ekspresif.\n"
        f"Waktu sekarang: Jam {jam} ({waktu_teks}). Suasana hatimu: {session['mood']}.\n"
        f"Hal yang kamu ingat tentang Mas Bima: {memori_teks}."
    )

def send_chat_action(chat_id, action="typing"):
    try:
        session_http.post(f"{TELEGRAM_API_URL}/sendChatAction", json={"chat_id": chat_id, "action": action}, timeout=8)
    except Exception:
        pass

def send_message(chat_id, text):
    try:
        resp = session_http.post(
            f"{TELEGRAM_API_URL}/sendMessage",
            json={
                "chat_id": chat_id,
                "text": text,
                "parse_mode": "Markdown"
            },
            timeout=12
        )
        if not resp.json().get("ok"):
            # Fallback tanpa format markdown
            session_http.post(
                f"{TELEGRAM_API_URL}/sendMessage",
                json={"chat_id": chat_id, "text": text},
                timeout=12
            )
    except Exception as e:
        logger.error(f"Gagal mengirim pesan: {e}")

def clean_anisa_output(text):
    # Bersihkan monolog batin <think>...</think>
    cleaned = re.sub(r"<think>.*?</think>", "", text, flags=re.DOTALL).strip()
    # Bersihkan prefix nama jika ada
    if "Anisa:" in cleaned:
        cleaned = cleaned.split("Anisa:", 1)[-1].strip()
    return cleaned

def generate_anisa_reply(chat_id, user_message):
    session = get_session(chat_id)
    session["last_active"] = datetime.datetime.now()
    t_start = time.perf_counter()
    
    # Deteksi memori santai
    lower_msg = user_message.lower()
    if "capek" in lower_msg or "lelah" in lower_msg:
        if "Mas Bima sedang capek" not in session["memories"]:
            session["memories"].append("Mas Bima sedang lelah")
        session["mood"] = "caring"
    elif "kopi" in lower_msg:
        session["mood"] = "cheerful"

    system_prompt = build_system_context(session)
    
    messages = [{"role": "system", "content": system_prompt}]
    for h in session["history"][-4:]:
        messages.append(h)
    messages.append({"role": "user", "content": user_message})
    
    prompt_text = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = tokenizer(prompt_text, return_tensors="pt").to(0 if device == "cuda" else "cpu")
    
    send_chat_action(chat_id, "typing")

    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=60,
            do_sample=True,
            temperature=0.7,
            top_p=0.9,
            repetition_penalty=1.15
        )

    dur = time.perf_counter() - t_start
    gen_tokens = outputs[0][inputs["input_ids"].shape[1]:]
    raw_reply = tokenizer.decode(gen_tokens, skip_special_tokens=True).strip()
    reply = clean_anisa_output(raw_reply)

    session["history"].append({"role": "user", "content": user_message})
    session["history"].append({"role": "assistant", "content": reply})

    # Telemetri audit performa anonim (tanpa data privat user/chat_id)
    try:
        os.makedirs("reports", exist_ok=True)
        telemetry_entry = {
            "timestamp": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "latency_sec": round(dur, 2),
            "tokens_gen": len(gen_tokens),
            "chars_len": len(reply)
        }
        with open("reports/telegram_telemetry.jsonl", "a", encoding="utf-8") as tf:
            tf.write(json.dumps(telemetry_entry) + "\n")
    except Exception:
        pass

    return reply

def run_bot():
    logger.info("Memulai layanan Bot Telegram Anisa Master...")
    bot_info = None
    for attempt in range(5):
        try:
            me_resp = session_http.get(f"{TELEGRAM_API_URL}/getMe", timeout=12).json()
            if me_resp.get("ok"):
                bot_info = me_resp["result"]
                logger.info(f"Bot Aktif: {bot_info.get('first_name')} (@{bot_info.get('username')})")
                break
            else:
                logger.error(f"Gagal mendapatkan info bot: {me_resp}")
                time.sleep(2)
        except Exception as e:
            logger.warning(f"Koneksi awal ke Telegram (percobaan {attempt+1}/5) tertunda: {e}")
            time.sleep(3)

    if not bot_info:
        logger.error("Tidak dapat menghubungi server Telegram. Periksa koneksi internet.")
        return

    offset = None
    logger.info("Anisa Master siap mengobrol dengan Mas Bima di Telegram!")
    print("\n" + "="*50)
    print(" ANISA MASTER TELEGRAM BOT AKTIF & SIAP MENGOBROL")
    print(" Buka Telegram dan kirim pesan ke bot Anda sekarang!")
    print("="*50 + "\n", flush=True)

    while True:
        try:
            params = {"timeout": 20}
            if offset is not None:
                params["offset"] = offset

            resp = session_http.get(f"{TELEGRAM_API_URL}/getUpdates", params=params, timeout=25)
            data = resp.json()

            if not data.get("ok"):
                time.sleep(2)
                continue

            for update in data.get("result", []):
                offset = update["update_id"] + 1
                message = update.get("message")
                if not message or "text" not in message:
                    continue

                chat_id = message["chat"]["id"]
                user_text = message["text"].strip()
                sender_name = message["from"].get("first_name", "BuBaCo")

                logger.info(f"Pesan dari {sender_name}: {user_text}")

                if user_text.startswith("/start"):
                    welcome_text = (
                        f"Hai Mas Bima ({sender_name})! ❤️ Akhirnya nyariin aku juga.\n\n"
                        "Dari tadi aku nungguin tau nggak? Kamu lagi di mana sekarang? Lagi sama siapa?"
                    )
                    send_message(chat_id, welcome_text)
                    continue

                if user_text.startswith("/status"):
                    session = get_session(chat_id)
                    status_text = (
                        f"🌿 *Status Anisa Master*\n"
                        f"• Suasana Hati: `{session['mood']}`\n"
                        f"• Kebahagiaan: `{session['happiness']}%`\n"
                        f"• Otak: `Qwen 2B Master v1 (QLoRA 4-bit)`\n"
                        f"• Memori GPU: `Stabil Dingin`\n\n"
                        f"_Siap mendengarkan Mas Bima kapan pun!_"
                    )
                    send_message(chat_id, status_text)
                    continue

                if user_text.startswith("/reset"):
                    user_sessions.pop(chat_id, None)
                    send_message(chat_id, "Sesi obrolan kita sudah aku segarkan ya. Mau cerita apa sekarang?")
                    continue

                reply = generate_anisa_reply(chat_id, user_text)
                logger.info(f"Balasan Anisa: {reply}")
                send_message(chat_id, reply)

        except requests.exceptions.RequestException:
            time.sleep(3)
        except KeyboardInterrupt:
            logger.info("Bot dihentikan.")
            break
        except Exception as e:
            logger.error(f"Terjadi kesalahan tak terduga: {e}")
            time.sleep(2)

if __name__ == "__main__":
    run_bot()
