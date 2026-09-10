# ADR-0002: Room persistence for v0.2

Status: Accepted for v0.2
Date: 2026-09-10

## Decision

Anisa-Chat v0.2 persists authoritative local state with AndroidX Room 2.8.4 over SQLite.

Persisted MVP tables:

- `messages`
- `memories`
- `emotion_state`
- `relationship_state`
- `runtime_metadata`

Room owns storage and transactions. Kotlin/domain code remains authoritative for time calculation, emotion arithmetic, relationship transitions, lexical memory ranking, and validation.

## Why Room 2.8.4

Room 3.0.3 is stable as of 2026-09-09, but the 3.x line changes package/API boundaries and is extremely new relative to this implementation. Google's Room 2.x -> 3.0 migration guidance recommends preparing and modernizing on the current Room 2.8 line before switching to Room 3.

For this milestone, persistence reliability matters more than adopting a newly released major version.

Selected versions:

```text
Room: 2.8.4
KSP: 2.3.12
AGP: 9.4.0
compileSdk/targetSdk: 36
minSdk: 28
```

Room 3 may be benchmarked or adopted later through a separate migration change after v0.2 persistence is proven on the Android target.

## Storage behavior

- Chat messages receive stable UUIDs in the domain layer.
- SQLite uses a local auto-increment row id for ordering and a unique message id for deduplication.
- Conversation writes persist user input before natural-language generation so process death or inference failure does not erase the user turn.
- Emotion and relationship values are normalized before persistence.
- Memory remains lexical/token-overlap retrieval in Kotlin; no vector database or embedding runtime is introduced in v0.2.
- Database destructive migration fallback is intentionally not enabled because chat/memory state is user data.

## Boundaries

This ADR does not authorize:

- llama.cpp/JNI integration
- GGUF import
- model lifecycle changes
- vector embeddings/vector DB
- cloud sync
- destructive user-data operations

Those remain separate milestones with their own evidence gates.

## Evidence

- AndroidX Room 2.8.4 release notes: https://developer.android.com/jetpack/androidx/releases/room
- Room 2.x to 3.0 migration guide: https://developer.android.com/training/data-storage/room/migration-2-to-3
- KSP release 2.3.12: https://github.com/google/ksp/releases/tag/2.3.12
