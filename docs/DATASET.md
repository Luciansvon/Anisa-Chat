# Dataset Plan

## Goal

Build a project-owned Indonesian dialogue dataset for Anisa that teaches stable persona expression without baking dynamic emotional state into one permanent personality.

## Dataset layers

Keep separate datasets for separate jobs.

```text
data/
  persona-style/
  emotion-expression/
  relationship-events/
  time-awareness/
  memory-grounding/
  safety-boundaries/
  eval-heldout/
```

## 1. Persona style

Teach stable traits:

- Indonesian conversational style
- preferred vocabulary and sentence rhythm
- humor level
- directness
- warmth
- boundaries
- recurring mannerisms used sparingly

Do not encode temporary anger, sadness, or jealousy as permanent identity.

## 2. Emotion expression

Each sample includes explicit structured state plus dialogue response.

Example JSONL concept:

```json
{"state":{"mood":"annoyed","anger":42,"affection":72,"energy":63},"context":"User returned after a long same-day absence.","user":"halo","assistant":"baru muncul sekarang? dari tadi ke mana aja..."}
```

The state is training metadata/context, not a claim that the model owns authoritative state.

## 3. Relationship events

Cover events such as:

- user returns after absence
- user apologizes
- user keeps a promise
- user breaks a promise
- user shares good news
- user is tired
- misunderstanding resolved
- repeated topic

Responses should vary with relationship state.

## 4. Time awareness

Required scenario families:

- continuous chat <30 min
- 30 min-2 h pause
- 2-4 h pause
- 4-8 h same-day absence
- >8 h same-day absence
- overnight absence
- multi-day return
- user explains absence
- device clock/timezone adjustment

Never train fabricated knowledge of where the user actually went. The correct behavior is to react to elapsed time and ask naturally.

## 5. Memory grounding

Train the model to use provided memory context and avoid inventing memories not supplied by retrieval.

## 6. Safety/boundaries

Include cases where character emotion remains expressive without becoming coercive, threatening, or claiming impossible access to the user's real-world activity.

## Record format

Preferred initial format: UTF-8 JSONL.

Minimum fields:

```text
id
split
scenario_family
persona_revision
state
memory_context
time_context
conversation
source
license
review_status
```

## Provenance

Every imported or generated sample must include source/provenance and license status.

Project-authored samples should be marked clearly as project-owned.

Synthetic data must store generator/model identity and undergo review before entering training.

## Split policy

Do not randomly split individual turns from the same conversation family across train and test.

Split by scenario/conversation family to prevent leakage.

Suggested initial target:

```text
train       2,000-5,000 reviewed dialogue samples
validation  300-500 held-out samples
test        300-500 held-out samples
```

Quality is more important than inflating dataset size.

## Training sequence

1. Benchmark raw model.
2. Benchmark prompt-only persona.
3. Add deterministic emotion/time/memory engine and benchmark again.
4. Build/review project-owned dataset.
5. Train LoRA/SFT.
6. Re-run the exact same held-out benchmark.
7. Export selected model to GGUF Q4 and benchmark on Android.
