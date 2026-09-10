# Draft PR notes

## Summary

Add Room-backed local persistence for chat continuity while preserving deterministic-first and model-runtime boundaries.

## Added

- Room 2.8.4 + KSP 2.3.12
- persistent messages, memories, emotion, relationship, runtime metadata
- stable message UUIDs with deduplicated SQLite ordering
- Room conversation state store
- Room lexical memory repository
- startup history restore in `ChatViewModel`
- persist-user-before-generation durability behavior
- persistence contract regression tests
- ADR-0002 and verification checklist

## Explicitly not included

- llama.cpp/JNI
- GGUF import
- vector DB/embeddings
- cloud sync
- destructive migrations
