# Anisa-Chat Architecture

Status: initial project architecture.

## Goals

- Android-first, offline-first character chatbot.
- Strong Indonesian persona with stable identity and adaptive emotion.
- Persistent memory and relationship state without requiring the language model to remember everything in context.
- Time-aware behavior driven by device time and stored interaction timestamps.
- Small on-device model with training performed from original safetensors checkpoints and Android inference from quantized deployment artifacts.
- Event-driven inference: deterministic tools stay available without keeping the language model resident in memory.

## Runtime architecture

```text
User / Android event
    |
    v
Conversation Orchestrator
    |
    +--> Deterministic Router
    |      |
    |      +--> Time / date / elapsed-gap calculation
    |      +--> SQLite / Room reads and writes
    |      +--> Emotion-state arithmetic
    |      +--> Relationship-state arithmetic
    |      +--> Exact memory lookup / metadata
    |      +--> Formatting / simple templates
    |      +--> Bounded local tools and scripts
    |      |
    |      +--> sufficient result? --> respond / persist without LLM
    |                                |
    |                                no
    |                                v
    +--> Persona Core
    +--> Relationship State
    +--> Emotion Engine
    +--> Memory Retrieval
    +--> Time Context
            |
            v
       Prompt Builder
            |
            v
       Model Manager
       load on demand
            |
            v
       llama.cpp JNI + GGUF Q4
            |
            v
       Generated response
            |
            +--> persist conversation metadata
            +--> deterministic state update
            +--> memory candidate pipeline
            |
            v
       idle grace period
            |
            v
       unload model/context
```

The application may stay alive according to normal Android lifecycle rules, but the LLM must not remain resident simply to service deterministic tools.

## Deterministic-first policy

Use Kotlin/native code or bounded scripts for tasks whose correct result can be computed directly.

Examples that must not require the model:

```text
get_current_time()
get_current_date()
calculate_elapsed_time()
classify_daypart()
read_last_message_timestamp()
update_numeric_emotion_state()
read/write Room records
exact memory lookup
session bookkeeping
simple notification metadata
health/runtime counters
```

A simple user request may also be answered without loading the model when a deterministic response remains natural enough. Persona-aware response templates may use current state, but must remain bounded and non-deceptive.

Examples:

```text
"jam berapa?"
  -> Android clock
  -> optional bounded persona template
  -> no model load required

"berapa lama aku nggak chat?"
  -> stored timestamp + clock arithmetic
  -> no model load required
```

Invoke the language model when at least one is true:

- free-form conversational response is required;
- persona expression needs natural variation beyond a bounded template;
- user intent is materially ambiguous;
- relevant retrieved memories must be synthesized into a natural reply;
- emotional/relationship context must be expressed conversationally;
- a benchmark explicitly targets model behavior.

Do not use the LLM as a calculator, clock, database, state machine, scheduler, or generic wrapper around deterministic APIs.

## Model lifecycle

The model manager has explicit states:

```text
UNLOADED
   |
   | conversational request
   v
LOADING
   |
   v
READY
   |
   +--> GENERATING
   |       |
   |       v
   |     READY
   |
   +--> idle grace period expires
           |
           v
       UNLOADING
           |
           v
       UNLOADED
```

Initial policy:

- cold start with model `UNLOADED`;
- load only on conversational/model-required requests;
- keep a configurable short idle grace period after generation so a rapid follow-up does not reload immediately;
- never keep the model resident because of background bookkeeping;
- unload inference context after the grace period;
- persist persona, relationship, emotion, memory, and time state outside model memory;
- Android process death must not lose authoritative state.

The idle grace period is not a permanent magic number. Benchmark at least `0s`, `30s`, `60s`, and `120s` on the Infinix Hot 30 and record:

- reload latency;
- peak PSS/RSS;
- battery cost;
- temperature;
- rapid multi-turn UX;
- Android LMKD/process survival behavior.

The selected default must come from device evidence.

## Background behavior

Background jobs may perform deterministic work such as:

```text
time/event bookkeeping
state decay
SQLite maintenance
notification scheduling metadata
benchmark/evidence bookkeeping
```

They must not wake or keep the LLM loaded merely to maintain the illusion of continuous thought.

If a future feature needs a proactive natural-language message, the deterministic scheduler creates an event first. The model may be loaded only at the actual generation boundary, subject to Android background-execution constraints and product policy.

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

The wording should normally be generated when a conversational turn already requires the model. Deterministic templates may be used for simple tool-only responses, but must avoid repetitive NPC behavior.

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

## Training and deployment lifecycle

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
