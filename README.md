# Anisa-Chat

Local-first Android character chatbot focused on strong persona consistency, adaptive emotional state, persistent memory, and efficient on-device inference.

## Product target

- Android-first, offline-first.
- Primary target device for performance validation: Infinix Hot 30 / Helio G88 / 8 GB physical RAM.
- Strong Indonesian conversational persona.
- Emotion affects response style, but emotional state is managed outside model weights.
- User-visible chat stays natural; internal persona, memory, and emotion state stay structured.
- Model training happens from original Hugging Face/safetensors checkpoints.
- Android deployment uses quantized GGUF, initially Q4.
- Model files are not committed to Git.

## Initial architecture

```text
User
  |
  v
Compose chat UI
  |
  v
Conversation orchestrator
  +--> Persona core
  +--> Emotion state engine
  +--> Memory retrieval
  |
  v
Prompt builder
  |
  v
llama.cpp JNI / GGUF Q4
  |
  v
Response
  |
  +--> memory candidate extraction
  +--> deterministic emotion update
```

## Android stack

- Kotlin
- Jetpack Compose + Material 3
- Gradle Kotlin DSL
- Android NDK + CMake
- llama.cpp through a narrow JNI boundary
- Room/SQLite for local memory and conversation metadata
- arm64-v8a first
- model imported/downloaded separately into app-specific storage

See `docs/ARCHITECTURE.md` and `docs/MODEL-EVAL.md` before implementation decisions.

## Model evaluation sequence

Do not select a model from generic benchmark tables alone.

Initial candidates:

1. Gemma 3 270M IT
2. LFM2.5 350M
3. Qwen-family ~0.5-0.8B candidate

Each candidate must be evaluated as:

- raw instruct model
- persona prompt only
- persona + external emotion/memory state
- LoRA/SFT only after baseline evidence exists

## Repository ownership

Anisa-Chat owns application architecture, persona definitions, datasets created for Anisa, domain-specific tests, model evaluation records, and Android implementation.

B.I.M.A-DEV-INFRA owns reusable verification/evidence/workflow contracts. Anisa-Chat may consume those contracts but must not hard-code Anisa-specific behavior into shared infrastructure.

## Large files

Do not commit:

- safetensors model weights
- GGUF model weights
- model caches
- generated APK/AAB binaries
- private chat history

Store only manifests, hashes, configuration, reports, and reproducible scripts.
