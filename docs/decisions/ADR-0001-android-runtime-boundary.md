# ADR-0001: Android local runtime boundary

Status: Accepted for v0.1
Date: 2026-09-10

## Decision

Anisa-Chat uses a native Android application with Kotlin + Jetpack Compose. Domain behavior is deterministic-first and local. Language-model inference sits behind a narrow `InferenceRuntime` boundary.

Initial deployment path:

```text
safetensors master/training checkpoint
  -> LoRA/SFT when justified by baseline evidence
  -> merge/export
  -> GGUF
  -> Q4 candidate
  -> llama.cpp Android adapter
```

The app must not make domain modules depend on llama.cpp classes, JNI handles, model-specific chat templates, or GGUF internals.

## Runtime layers

```text
Compose UI
  |
ChatViewModel
  |
ChatOrchestrator
  +-- TimeContextEngine
  +-- EmotionEngine
  +-- Relationship state
  +-- Memory/state repository
  |
SystemContextBuilder
  |
ModelSessionManager
  |
InferenceRuntime
  +-- Development runtime
  +-- llama.cpp adapter (next)
  +-- optional measured backend later
```

## Model lifecycle

The model is not a background service and is not authoritative for simple tools.

Native Kotlin handles:

- wall clock and elapsed-time calculation
- daily rhythm / daypart
- emotion-state arithmetic and decay
- relationship flags
- exact local lookups
- persistence and bookkeeping
- deterministic validation

The model loads only when natural-language generation is required. After generation, an idle grace timer unloads it. The initial default is 60 seconds and must be benchmarked against 0/30/60/120 seconds on the Infinix Hot 30.

## Why llama.cpp first

- Direct GGUF support fits the training -> export -> Q4 workflow.
- Official `ggml-org/llama.cpp` contains an Android binding/example.
- CPU execution works on generic arm64 Android without assuming a specific mobile NPU.
- Backend remains replaceable because the app depends on `InferenceRuntime`, not llama.cpp.

## Why not LiteRT-first

LiteRT remains a valid benchmark candidate, especially for supported accelerators/models, but it is not the v0.1 default because Anisa needs rapid comparison of multiple tiny models and quantizations produced from the training pipeline. Adopting it requires measured benefit on a target device.

## Constraints

- arm64-v8a is the first native target.
- Infinix Hot 30 / Helio G88 is the low-end reference device.
- No model weights in Git.
- No private chat data in CI artifacts.
- No backend may silently change persona, emotion, memory, or time semantics.
- Backend changes require the same character benchmark plus device evidence.
