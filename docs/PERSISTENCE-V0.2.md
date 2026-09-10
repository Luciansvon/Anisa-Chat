# Persistence v0.2 verification

## Scope

This milestone proves that authoritative chat continuity survives Android process recreation without introducing model runtime work.

## Automated checks

- Room entities and DAO compile through KSP.
- Conversation state mapping normalizes emotion and relationship values.
- Stable message ids prevent duplicate message insertion across repeated state writes.
- Memory remains searchable after repository recreation.
- A user turn is persisted before generation, so inference failure does not erase the user message.
- Existing deterministic time query still does not load the model.

## Manual device checks before merge

1. Install debug APK.
2. Send at least one deterministic message and one free-form message.
3. Force-stop the app.
4. Reopen the app and verify recent messages are restored.
5. Verify absence/time behavior uses the persisted previous user timestamp.
6. Verify emotion/relationship state remains continuous after restart.
7. Confirm no model/JNI/GGUF dependency was introduced.

## Deferred

- database migration tests beyond schema version 1
- full-history pagination UI
- FTS/vector retrieval
- backup/export
- cloud sync
- llama.cpp runtime
