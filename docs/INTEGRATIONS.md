# Integration Map

## Adopt directly

### ggml-org/llama.cpp

Role: primary GGUF inference backend candidate.

Integration rule:

- Pin an exact upstream revision.
- Prefer the official Android binding/example as the reference implementation.
- Expose only Anisa's narrow `InferenceRuntime` API to the rest of the app.
- Do not copy the sample application's UI or application architecture.
- Keep upstream license/notice requirements.

Why: official Android support, GGUF compatibility, generic arm64 CPU path, and direct fit with Anisa's training/export workflow.

## Reference patterns, not direct dependencies

### tk85457/LocalMind

Useful patterns:

- Kotlin + Jetpack Compose + Material 3 local-LLM application structure.
- llama.cpp submodule/JNI integration.
- model file import and lifecycle patterns.

Decision: inspect and reuse ideas only where they beat our simpler implementation. Do not depend on the application itself.

### a-ghorbani/pocketpal-ai

Useful patterns:

- model download/import UX
- model lifecycle and memory-pressure handling
- device-oriented local inference UX

Decision: reference only. It is React Native based, while Anisa is native Kotlin/Compose.

### aelcode/ChatterUI

Useful patterns:

- character/chat UX concepts
- local-model frontend behavior
- prompt/profile interoperability ideas

Decision: reference only. Do not inherit its React Native application stack.

## Optional backend experiment

### Google LiteRT / LiteRT-LM

Potential value:

- hardware-accelerated on-device inference where model/device support is strong
- Android-focused runtime and tooling

Adoption gate:

- same model or meaningfully equivalent candidate
- same character benchmark
- same context budget
- real target-device measurement
- material improvement in TTFT, tokens/s, peak memory, battery, or thermal behavior

If adopted, implement `LiteRtInferenceRuntime`; no domain rewrite is allowed.

## Android platform libraries

Use platform/Jetpack before external libraries when sufficient:

- Kotlin coroutines for asynchronous orchestration
- Java time APIs for authoritative local time calculations
- SQLite/Room boundary for persistence
- WorkManager only for bounded deferred work such as optional check-ins
- Android Storage Access Framework for user-controlled model import
- AndroidX Benchmark/Perfetto later for measured device performance

## Rejected for v0.1

- agent frameworks
- vector databases
- cloud memory services
- autonomous background services
- second LLM for emotion classification
- full RAG frameworks
- cross-platform UI frameworks

Add an integration only when it removes real project work or improves measured behavior. A dependency is not a feature.
