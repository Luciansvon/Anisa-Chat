# AGENTS.md

## Purpose

Anisa-Chat is a project repository consuming B.I.M.A-DEV-INFRA conventions. Keep Anisa-specific application architecture, persona, datasets, incidents, and tests here. Keep reusable cross-project verification infrastructure in B.I.M.A-DEV-INFRA.

## Required reading before changes

1. `README.md`
2. `docs/ARCHITECTURE.md`
3. `docs/MODEL-EVAL.md`
4. Relevant project ADRs when added
5. Relevant B.I.M.A-DEV-INFRA contracts when a shared workflow is consumed

## Core rules

- Research -> baseline -> benchmark -> decision -> implementation.
- Do not select a model from size or leaderboard claims alone.
- Preserve raw benchmark evidence and exact model/config identity.
- Training weights and deployment weights are separate artifacts.
- Safetensors is the training/master checkpoint format; Android deployment may use GGUF Q4.
- Do not commit large model weights or private conversation data.
- Persona identity, emotional state, and memory are separate responsibilities.
- The model generates language; it does not own authoritative emotional state or persistent memory.
- Changes to persona state rules require regression scenarios.
- Changes to model/runtime require device benchmark evidence.
- Hardware claims for Android must identify the real tested device.
- Infinix Hot 30 is the initial low-end performance reference, not a universal performance claim.
- Prefer deterministic logic for state transitions, time, storage, retrieval, formatting, and simple tool execution before invoking a model.
- The model must not remain loaded merely to serve deterministic tools or background bookkeeping.
- Simple actions must use Kotlin/native code or bounded scripts; do not route them through the LLM for convenience.
- Load the model only when natural-language generation, ambiguity resolution, or model-specific reasoning is required.
- After an idle grace period, unload the model and release inference context; the grace period is configurable and must be benchmarked on target hardware.
- Background jobs must not keep the model resident. They may persist deterministic events/state that are consumed on the next conversational wake.
- Keep JNI narrow. Kotlin/domain code must not depend directly on llama.cpp internals.
- Destructive user-data operations are out of scope for the chatbot core.

## Dataset rules

- Maintain source/license provenance for every imported dataset.
- Non-commercial datasets must not silently enter a dataset intended for a potentially commercial build.
- Never train on private user chats without explicit, documented consent and sanitization.
- Split train/validation/test by scenario or conversation family, not by random individual turns, to reduce leakage.
- Keep held-out benchmark conversations out of training data.
- Synthetic samples require review rules and generator provenance.

## Evidence required for model decisions

At minimum record:

- model repository + revision
- base format
- quantization
- prompt template
- context size
- sampling settings
- dataset revision
- persona/state configuration
- device/runner
- TTFT
- generation speed
- peak memory
- model load time
- unload/reload time
- idle grace period
- failure/crash state
- persona/emotion/memory benchmark scores

## Shared-infra boundary

Promote only reusable benchmark/reporting contracts to B.I.M.A-DEV-INFRA. Character-specific prompts, emotion rules, datasets, LoRA adapters, and Android UI remain in Anisa-Chat.
