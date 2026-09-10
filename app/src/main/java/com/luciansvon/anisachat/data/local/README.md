# Local persistence

Room v0.2 stores conversation continuity only. It must not own persona rules, emotion transitions, relationship arithmetic, lexical ranking, or model runtime behavior.

Current tables:

- `messages`
- `memories`
- `emotion_state`
- `relationship_state`
- `runtime_metadata`

Do not enable destructive migrations for user chat/memory data.
