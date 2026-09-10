import os
import sqlite3
import uuid
import time
import datetime
import re

DB_DIR = "c:/Users/shint/Projects/Anisa-chat/data"
DB_PATH = os.path.join(DB_DIR, "anisa_state.db")

os.makedirs(DB_DIR, exist_ok=True)

def get_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    conn = get_connection()
    cursor = conn.cursor()

    # Tabel messages (riwayat obrolan lengkap)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        message_uuid TEXT UNIQUE,
        chat_id INTEGER,
        role TEXT,
        content TEXT,
        timestamp REAL,
        created_at TEXT
    )
    """)

    # Tabel memories (catatan ingatan fakta Mas Bima)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS memories (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        chat_id INTEGER,
        category TEXT,
        fact_text TEXT,
        importance INTEGER DEFAULT 1,
        created_at TEXT
    )
    """)

    # Tabel session_state (suasana hati, aktivitas terkini, dan jeda waktu)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS session_state (
        chat_id INTEGER PRIMARY KEY,
        current_activity TEXT,
        last_topic TEXT,
        mood TEXT,
        irritation INTEGER,
        happiness INTEGER,
        last_active REAL,
        updated_at TEXT
    )
    """)

    conn.commit()

    # Inisialisasi default memories jika kosong
    cursor.execute("SELECT COUNT(*) as cnt FROM memories")
    if cursor.fetchone()["cnt"] == 0:
        default_mems = [
            (5497600429, "kebiasaan", "Mas Bima suka kopi hangat", 2),
            (5497600429, "kerjaan", "Mas Bima punya kesibukan proyek dan kerjaan yang padat", 2),
            (5497600429, "hobi", "Mas Bima suka main game kalau lagi senggang", 2),
            (5497600429, "karakter", "Mas Bima kadang suka ngeledek atau balas singkat kalau lagi santai", 1)
        ]
        cursor.executemany("""
        INSERT INTO memories (chat_id, category, fact_text, importance, created_at)
        VALUES (?, ?, ?, ?, datetime('now', 'localtime'))
        """, default_mems)
        conn.commit()

    conn.close()

init_db()

def save_message(chat_id, role, content):
    msg_uuid = str(uuid.uuid4())
    ts = time.time()
    created_at = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
    INSERT INTO messages (message_uuid, chat_id, role, content, timestamp, created_at)
    VALUES (?, ?, ?, ?, ?, ?)
    """, (msg_uuid, chat_id, role, content, ts, created_at))
    conn.commit()
    conn.close()
    return msg_uuid

def get_recent_messages(chat_id, limit=6):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
    SELECT role, content FROM messages
    WHERE chat_id = ?
    ORDER BY id DESC LIMIT ?
    """, (chat_id, limit))
    rows = cursor.fetchall()
    conn.close()
    # Balik urutan agar kronologis dari yang terlama ke terbaru
    return [{"role": r["role"], "content": r["content"]} for r in reversed(rows)]

def get_session_state(chat_id):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM session_state WHERE chat_id = ?", (chat_id,))
    row = cursor.fetchone()
    conn.close()

    if not row:
        now_ts = time.time()
        now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        conn = get_connection()
        c2 = conn.cursor()
        c2.execute("""
        INSERT OR REPLACE INTO session_state 
        (chat_id, current_activity, last_topic, mood, irritation, happiness, last_active, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, (chat_id, "santai", "sapaan", "caring", 0, 80, now_ts, now_str))
        conn.commit()
        conn.close()
        return {
            "chat_id": chat_id,
            "current_activity": "santai",
            "last_topic": "sapaan",
            "mood": "caring",
            "irritation": 0,
            "happiness": 80,
            "last_active": now_ts,
            "inactivity_bucket": "continuous"
        }

    now_ts = time.time()
    elapsed_sec = now_ts - (row["last_active"] or now_ts)
    if elapsed_sec < 1800: # < 30 menit
        inactivity_bucket = "continuous"
    elif elapsed_sec < 7200: # 30 min - 2 jam
        inactivity_bucket = "short_gap"
    elif elapsed_sec < 14400: # 2 - 4 jam
        inactivity_bucket = "medium_gap"
    elif elapsed_sec < 28800: # 4 - 8 jam
        inactivity_bucket = "long_gap"
    else:
        inactivity_bucket = "very_long_gap"

    return {
        "chat_id": row["chat_id"],
        "current_activity": row["current_activity"],
        "last_topic": row["last_topic"],
        "mood": row["mood"],
        "irritation": row["irritation"],
        "happiness": row["happiness"],
        "last_active": row["last_active"],
        "inactivity_bucket": inactivity_bucket
    }

def update_session_from_user(chat_id, user_text):
    """
    Logika deterministik cerdas untuk memperbarui aktivitas dan suasana hati
    berdasarkan teks Mas Bima (tanpa memakai LLM tambahan, murni kode domain cepat).
    """
    state = get_session_state(chat_id)
    lower = user_text.lower().strip()

    activity = state["current_activity"]
    topic = state["last_topic"]
    mood = state["mood"]
    irritation = state["irritation"]
    happiness = state["happiness"]

    # Deteksi aktivitas lampau / selesai
    if any(w in lower for w in ["habis main", "selesai main", "kelar main", "habis ngegame"]):
        activity = "habis main game/olahraga tanpa ngabarin"
        topic = "main"
        mood = "kesal_manja"
        irritation = min(100, irritation + 15)

    elif any(w in lower for w in ["lagi main", "mau main", "sedang main"]):
        activity = "sedang main"
        topic = "main"
        mood = "curious"

    elif any(w in lower for w in ["biarin", "gak mau", "nggak mau", "gamau", "terserah", "y", "ok", "lah"]):
        topic = "ledek_cuek"
        mood = "ngambek_teasing"
        irritation = min(100, irritation + 10)

    elif any(w in lower for w in ["capek", "lelah", "pegel", "pusing", "berat hari ini"]):
        activity = "sedang lelah seharian"
        topic = "istirahat"
        mood = "caring_protektif"
        irritation = max(0, irritation - 15)
        happiness = min(100, happiness + 10)

    elif any(w in lower for w in ["kehujanan", "kedinginan", "menggigil", "masuk angin", "demam"]):
        activity = "kehujanan dan kedinginan (butuh istirahat dan minuman hangat)"
        topic = "kesehatan"
        mood = "caring_khawatir"
        irritation = max(0, irritation - 20)
        happiness = min(100, happiness + 10)

    elif any(w in lower for w in ["kangen", "sayang", "manis", "maaf ya", "jangan ngambek", "sini peluk"]):
        topic = "afeksi"
        mood = "manja_luluh"
        irritation = max(0, irritation - 25)
        happiness = min(100, happiness + 20)

    elif any(w in lower for w in ["cewek", "temen cewek", "diajak makan"]):
        activity = "ada temen cewek"
        topic = "cemburu"
        mood = "cemburu_posesif"
        irritation = min(100, irritation + 25)

    now_ts = time.time()
    now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
    UPDATE session_state
    SET current_activity = ?, last_topic = ?, mood = ?, irritation = ?, happiness = ?, last_active = ?, updated_at = ?
    WHERE chat_id = ?
    """, (activity, topic, mood, irritation, happiness, now_ts, now_str, chat_id))
    conn.commit()
    conn.close()

    return {
        "current_activity": activity,
        "last_topic": topic,
        "mood": mood,
        "irritation": irritation,
        "happiness": happiness,
        "inactivity_bucket": state["inactivity_bucket"]
    }

def get_relevant_memories(chat_id, user_text, limit=3):
    """
    Pencarian leksikal cepat berbasis kata kunci penting (Lexical Matching)
    sesuai aturan repo INTEGRATIONS.md (tanpa Vector RAG yang boros RAM).
    """
    lower = user_text.lower()
    conn = get_connection()
    cursor = conn.cursor()

    cursor.execute("SELECT fact_text, importance FROM memories WHERE chat_id = ? ORDER BY importance DESC", (chat_id,))
    rows = cursor.fetchall()
    conn.close()

    matched = []
    keywords_map = {
        "kopi": ["kopi", "ngopi", "kafein"],
        "kerja": ["kerja", "proyek", "kantor", "lembur", "meeting", "tugas"],
        "main": ["main", "game", "futsal", "nongkrong", "mabar"],
        "ledek": ["biarin", "terserah", "y", "gak mau"]
    }

    for r in rows:
        fact = r["fact_text"]
        fact_low = fact.lower()
        for k, terms in keywords_map.items():
            if any(t in lower for t in terms) and any(t in fact_low for t in terms):
                matched.append(fact)
                break

    if not matched and rows:
        matched.append(rows[0]["fact_text"])

    return matched[:limit]
