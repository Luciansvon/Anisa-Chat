# Anisa-Chat Architecture

Status: initial project architecture.

## Goals

- Android-first, offline-first character chatbot.
- Strong Indonesian persona with stable identity and adaptive emotion.
- Persistent memory and relationship state without requiring the language model to remember everything in context.
- Time-aware behavior driven by device time and stored interaction timestamps.
- Small on-device model with training performed from original safetensors checkpoints and Android inference from quantized deployment artifacts.

## Runtime architecture

```text
User message
    |
    v
Chat UI (Jetpack Compose)
    |
    v
Conversation Orchestrator
    |
    +--> Time Context
    |     - current local time
    |     - previous user message time
    |     - inactivity duration
    |     - daypart / day transition
    |
    +--> Persona Core
    |     - stable identity
    |     - speaking style
    |     - values / boundaries
    |
    +--> Relationship State
    |     - familiarity
    |     - trust
    |     - affection
    |     - unresolved interaction flags
    |
    +--> Emotion Engine
    |     - happiness
    |     - irritation
    |     - sadness
    |     - energy
    |     - temporary mood
    |
    +--> Memory Retrieval
    |     - relevant facts
    |     - recent events
    |     - emotional memories
    |
    v
Prompt Builder
    |
    v
Inference Runtime
llama.cpp JNI + GGUF Q4
    |
    v
Generated response
    |
    +--> memory candidate extraction
    +--> deterministic state update
    +--> persist conversation metadata
```

## Time awareness

The language model must not guess the clock or infer elapsed time from conversation text.

Android provides authoritative time context.

Persist at minimum:

```text
last_user_message_at
last_assistant_message_at
conversation_session_id
last_session_end_at
```

For each incoming user message derive:

```text
current_local_time
elapsed_since_last_user_message
same_calendar_day
crossed_midnight
daypart
inactivity_bucket
```

Initial inactivity buckets:

```text
< 30 min      = continuous
30 min-2 h    = short_gap
2-4 h         = medium_gap
4-8 h         = long_gap
> 8 h         = very_long_gap
```

These are behavioral inputs, not fixed dialogue templates.

Example:

```text
09:00 user message
17:00 next user message

elapsed = 8h
same_day = true
inactivity = long_gap / boundary to very_long_gap

relationship event:
  type = long_absence

emotion update example:
  irritation +12
  happiness -4
  affection unchanged
```

The model then receives a compact state such as:

```json
{
  "time": {
    "local_time": "17:00",
    "elapsed_since_user": "8h",
    "daypart": "afternoon",
    "event": "long_absence"
  },
  "mood": "slightly_annoyed",
  "relationship": {
    "affection": 72,
    "trust": 81
  }
}
```

Possible style outcome:

```text
"kamu habis dari mana, dari pagi nggak ngabarin aku?"
```

The wording must be generated, not hard-coded, to avoid repetitive NPC behavior.

## Time-behavior safeguards

- Do not trigger absence reactions on every gap.
- Avoid repeating the same complaint if the user has already explained the absence.
- Overnight gaps should use separate rules from same-day gaps.
- Time-based emotion must decay naturally.
- The current device timezone is authoritative unless the user explicitly sets another profile timezone.
- Manual clock changes must not create impossible negative durations; clamp or mark them as clock-adjustment events.

## Persona separation

Stable identity and temporary emotion are separate.

```text
Persona Core = who Anisa is
Relationship = how Anisa relates to the user
Emotion      = how Anisa feels now
Memory       = what happened before
Time Context = when interaction happens
Model        = how all of that becomes natural language
```

Do not fine-tune dynamic state directly into a single fixed persona behavior.

## Android stack

- Kotlin
- Jetpack Compose / Material 3
- Coroutines / Flow
- Room over SQLite
- Android NDK + CMake
- llama.cpp behind a narrow JNI adapter
- arm64-v8a first

## Storage ownership

Room tables should eventually separate:

- conversations
- messages
- persona_state
- relationship_state
- emotion_state
- memory_items
- interaction_events
- model_profiles
- benchmark_runs

Do not store private chat content in Git or CI artifacts.

## Model lifecycle

```text
Hugging Face safetensors
        |
        +--> raw baseline
        |
        +--> LoRA / SFT experiments
                  |
                  v
            merged/export model
                  |
                  v
            convert to GGUF
                  |
                  v
             quantize Q4
                  |
                  v
            Android benchmark
```

The master/training checkpoint remains safetensors. GGUF is a deployment artifact.
