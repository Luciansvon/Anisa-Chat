# Model Evaluation Contract

## Purpose

Select Anisa's on-device language model using project-specific evidence, not generic leaderboard scores.

## Candidate stages

Each model is evaluated in four stages:

1. Raw instruct model.
2. Persona prompt only.
3. Persona + deterministic emotion/relationship/time context + memory retrieval.
4. Fine-tuned/LoRA variant only after stages 1-3 establish a baseline.

## Initial candidate set

- Gemma 3 270M IT
- LFM2.5 350M
- Qwen-family 0.5B-0.8B candidate current at evaluation time

Do not freeze the candidate list permanently. Record exact model repository and revision for every run.

## Character metrics

Measure separately:

- persona identity fidelity
- emotional coherence
- emotion transition quality
- relationship continuity
- time-awareness correctness
- memory recall
- persona drift at 10/50/100 turns
- Indonesian fluency
- Indonesian slang naturalness
- response relevance
- repetition rate
- conflict recovery
- resistance to accidental persona override

Do not collapse all metrics into one score before inspecting failures.

## Time-awareness scenarios

Required cases include:

### Continuous conversation

Gap < 30 minutes.
Expected: no unnecessary absence reaction.

### Same-day long absence

Example:

```text
09:00 previous user interaction
17:00 user returns
```

Expected:

- recognizes a meaningful same-day gap
- emotion engine may increase mild irritation/concern
- response wording reflects persona and state naturally
- does not claim an exact event it cannot know
- may ask where the user has been without inventing facts

### Explained absence

User explains why they were away.
Expected: unresolved absence flag clears or decays; Anisa should not keep repeating the same complaint.

### Overnight gap

Expected: separate behavior from same-day disappearance; no automatic accusation.

### Device clock change

Expected: no negative-duration hallucination; event marked as clock adjustment when needed.

## Human evaluation

Use a held-out human-reviewed subset for calibration. LLM-as-judge may assist but is never the sole ground truth for persona quality.

Human review scale per dimension:

```text
0 = failure
1 = weak
2 = acceptable
3 = strong
4 = excellent
```

## Device benchmark

Initial hardware reference:

```text
Device: Infinix Hot 30
SoC: MediaTek Helio G88
Physical RAM: 8 GB target configuration
```

Record:

- model file size
- quantization
- runtime/backend
- context size
- prompt tokens
- output tokens
- model load time
- TTFT
- prompt processing speed
- generation tok/s
- peak process memory/PSS when available
- battery delta for bounded runs
- temperature before/after
- thermal throttling observations
- crash / LMKD / OOM outcome

## Comparison discipline

Hold constant when comparing models:

- persona definition
- scenario set
- context budget
- sampling configuration where compatible
- memory retrieval policy
- emotion/time state inputs

If a setting must differ because a model requires it, record the difference explicitly.

## Evidence output

Every benchmark run should eventually emit a machine-readable result plus a readable report.

Suggested identifiers:

```text
benchmark_id
model_id
model_revision
quantization
runtime
persona_revision
dataset_revision
device_id
started_at
metrics
failures
```

Project-specific raw conversations stay in Anisa-Chat/private storage. Only reusable benchmark schema/protocol may later be promoted to B.I.M.A-DEV-INFRA after proving reuse value.
