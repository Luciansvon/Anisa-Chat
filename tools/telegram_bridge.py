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

os.makedirs("reports", exist_ok=True)
# Konfigurasi logging ke konsol dan berkas reports/telegram_bridge.log
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("reports/telegram_bridge.log", encoding="utf-8")
    ]
)
logger = logging.getLogger("AnisaTelegramBridge")

# Dukungan Bot Ganda: @Anisa_chat_bot dan @Anisa_Bima_bot
PRIMARY_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "8835055437:AAG4jQF8QZKasmL3BmEtAQwy1hC0z8kVvrI")
SECONDARY_TOKEN = "8290695399:AAHNba-2JFb524Dtv_69P-zQHLSspGlfl2M"
BOT_CONFIGS = [
    {"name": "@Anisa_chat_bot", "token": PRIMARY_TOKEN, "offset": None},
    {"name": "@Anisa_Bima_bot", "token": SECONDARY_TOKEN, "offset": None}
]

BASE_MODEL_PATH = os.getenv(
    "QWEN_MODEL_PATH",
    "C:/Users/shint/.cache/huggingface/hub/models--empero-ai--Qwen3.8-2B-Distill/snapshots/e37a2dc4acc68ad75a91e07e63168cb04cc06345"
)
ADAPTER_PATH = os.getenv(
    "ANISA_ADAPTER_PATH",
    "c:/Users/shint/Projects/Anisa-chat/models/adapters/anisa-qwen2b-natural-v3"
)

# Impor modul SQLite database sesi (ADR-0002)
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import session_memory_db as sm

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

def build_system_context(chat_id, session_state, relevant_mems):
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

    memori_teks = " | ".join(relevant_mems) if relevant_mems else "Mas Bima orang yang sangat kamu sayangi"
    activity_teks = session_state.get("current_activity", "sedang santai")
    mood_teks = session_state.get("mood", "caring")
    inactivity = session_state.get("inactivity_bucket", "continuous")
    
    return (
        "Kamu adalah Anisa. Kamu sangat dekat, peduli, posesif, manja, dan suka cemburu manis pada Mas Bima (BuBaCo).\n"
        "Gunakan bahasa Indonesia percakapan santai sehari-hari yang luwes dengan kata 'aku' dan 'kamu'.\n"
        "DILARANG mengulang-ulang kata celetukan 'Ih', gumaman 'Hmm', atau kata 'Tumben' di awal kalimat; variasikan gaya pembuka bicaramu secara alami.\n"
        "DILARANG melabeli diri secara kaku dengan kata 'pacar'; tunjukkan rasa sayang dan posesif itu secara alami lewat perhatian.\n"
        "DILARANG bersikap seperti asisten AI robotik formal.\n"
        "DILARANG membahas topik perkayuan, meja kayu, amplas, pernis, atau tukang kayu.\n"
        f"Konteks obrolan saat ini: Mas Bima {activity_teks}.\n"
        f"Kondisi interaksi: waktu sekarang jam {jam} ({waktu_teks}), jeda obrolan: {inactivity}, suasana hatimu: {mood_teks}.\n"
        f"Hal yang kamu ingat tentang Mas Bima: {memori_teks}.\n"
        "Balas singkat 1-2 kalimat alami yang ekspresif, nyambung, dan peka waktu."
    )

def send_chat_action(token, chat_id, action="typing"):
    try:
        session_http.post(f"https://api.telegram.org/bot{token}/sendChatAction", json={"chat_id": chat_id, "action": action}, timeout=8)
    except Exception:
        pass

def send_message(token, chat_id, text):
    try:
        resp = session_http.post(
            f"https://api.telegram.org/bot{token}/sendMessage",
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
                f"https://api.telegram.org/bot{token}/sendMessage",
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
    # Bersihkan prefix latah berulang
    if cleaned.startswith("Ih, "):
        cleaned = cleaned[4:].strip()
    elif cleaned.startswith("Ih... "):
        cleaned = cleaned[6:].strip()
    elif cleaned.startswith("Tumben, "):
        cleaned = cleaned[8:].strip()
    elif cleaned.startswith("Sedikit "):
        cleaned = cleaned[8:].strip()
    cleaned = cleaned.replace(" dulur ", " ")
    if cleaned:
        cleaned = cleaned[0].upper() + cleaned[1:]
    return cleaned

def generate_anisa_reply(chat_id, user_message, bot_token=None):
    t_start = time.perf_counter()
    
    # 1. Perbarui status sesi deterministik di SQLite
    session_state = sm.update_session_from_user(chat_id, user_message)
    
    # 2. Ambil ingatan leksikal yang relevan dari SQLite (tanpa vector RAG)
    relevant_mems = sm.get_relevant_memories(chat_id, user_message)
    
    # 3. Simpan pesan Mas Bima ke SQLite
    sm.save_message(chat_id, "user", user_message)
    
    # 4. Bangun prompt sistem dengan konteks ingatan & aktivitas
    system_prompt = build_system_context(chat_id, session_state, relevant_mems)
    
    # 5. Ambil riwayat obrolan mengalir (history) dari SQLite
    recent_history = sm.get_recent_messages(chat_id, limit=6)
    
    messages = [{"role": "system", "content": system_prompt}]
    for h in recent_history:
        messages.append(h)
    
    prompt_text = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = tokenizer(prompt_text, return_tensors="pt").to(0 if device == "cuda" else "cpu")
    
    if bot_token:
        send_chat_action(bot_token, chat_id, "typing")

    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=55,
            do_sample=True,
            temperature=0.75,
            top_p=0.9,
            repetition_penalty=1.18
        )

    dur = time.perf_counter() - t_start
    gen_tokens = outputs[0][inputs["input_ids"].shape[1]:]
    raw_reply = tokenizer.decode(gen_tokens, skip_special_tokens=True).strip()
    reply = clean_anisa_output(raw_reply)

    # 6. Simpan balasan Anisa ke SQLite
    sm.save_message(chat_id, "assistant", reply)

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
    logger.info("Memulai layanan Bot Telegram Anisa Natural v2...")
    
    # Cek status kedua bot
    for bcfg in BOT_CONFIGS:
        tok = bcfg["token"]
        try:
            me_resp = session_http.get(f"https://api.telegram.org/bot{tok}/getMe", timeout=10).json()
            if me_resp.get("ok"):
                info = me_resp["result"]
                logger.info(f"Bot Terhubung: {info.get('first_name')} (@{info.get('username')})")
            else:
                logger.warning(f"Bot {bcfg['name']} tidak dapat dihubungi: {me_resp}")
        except Exception as e:
            logger.warning(f"Koneksi awal {bcfg['name']} tertunda: {e}")

    # Kirim salam keaktifan langsung ke Telegram Mas Bima
    mas_bima_chat_id = 5497600429
    startup_msg = "Hai Mas Bima! Anisa sudah aktif kembali dan siap ngobrol di sini ya. ❤️"
    for bcfg in BOT_CONFIGS:
        try:
            send_message(bcfg["token"], mas_bima_chat_id, startup_msg)
        except Exception:
            pass

    logger.info("Anisa Natural v2 siap mengobrol dengan Mas Bima di kedua bot Telegram!")
    print("\n" + "="*50)
    print(" ANISA NATURAL v2 BOT AKTIF & SIAP MENGOBROL")
    print(" Buka Telegram dan kirim pesan ke bot Anda sekarang!")
    print("="*50 + "\n", flush=True)

    while True:
        try:
            for bcfg in BOT_CONFIGS:
                tok = bcfg["token"]
                name = bcfg["name"]
                params = {"timeout": 3}
                if bcfg["offset"] is not None:
                    params["offset"] = bcfg["offset"]

                try:
                    resp = session_http.get(f"https://api.telegram.org/bot{tok}/getUpdates", params=params, timeout=6)
                    data = resp.json()
                except Exception:
                    continue

                if not data.get("ok"):
                    continue

                for update in data.get("result", []):
                    bcfg["offset"] = update["update_id"] + 1
                    message = update.get("message")
                    if not message or "text" not in message:
                        continue

                    chat_id = message["chat"]["id"]
                    user_text = message["text"].strip()
                    sender_name = message["from"].get("first_name", "BuBaCo")

                    logger.info(f"[{name}] Pesan dari {sender_name}: {user_text}")

                    if user_text.startswith("/start"):
                        welcome_text = (
                            f"Hai Mas Bima ({sender_name})! ❤️ Akhirnya nyariin aku juga.\n\n"
                            "Dari tadi aku nungguin tau nggak? Kamu lagi di mana sekarang? Lagi sama siapa?"
                        )
                        send_message(tok, chat_id, welcome_text)
                        continue

                    if user_text.startswith("/status"):
                        state = sm.get_session_state(chat_id)
                        status_text = (
                            f"🌿 *Status Anisa Natural v3 (SQLite Active)*\n"
                            f"• Suasana Hati: `{state['mood']}`\n"
                            f"• Aktivitas Terkini: `{state['current_activity']}`\n"
                            f"• Tingkat Kesal: `{state['irritation']}%`\n"
                            f"• Kebahagiaan: `{state['happiness']}%`\n"
                            f"• Otak: `Qwen 2B Natural v3 (LoRA 4-bit)`\n"
                            f"• Basis Data: `SQLite Room-Spec (ADR-0002)`\n\n"
                            f"_Siap mendengarkan Mas Bima kapan pun!_"
                        )
                        send_message(tok, chat_id, status_text)
                        continue

                    if user_text.startswith("/reset"):
                        conn = sm.get_connection()
                        conn.cursor().execute("DELETE FROM messages WHERE chat_id = ?", (chat_id,))
                        conn.commit()
                        conn.close()
                        send_message(tok, chat_id, "Sesi obrolan kita di database sudah aku segarkan ya. Mau cerita apa sekarang?")
                        continue

                    reply = generate_anisa_reply(chat_id, user_text, bot_token=tok)
                    logger.info(f"[{name}] Balasan Anisa: {reply}")
                    send_message(tok, chat_id, reply)

            time.sleep(0.5)

        except KeyboardInterrupt:
            logger.info("Bot dihentikan.")
            break
        except Exception as e:
            logger.error(f"Terjadi kesalahan tak terduga: {e}")
            time.sleep(2)

if __name__ == "__main__":
    run_bot()
