# Anisa Dataset Workspace

Project-owned Indonesian dialogue data lives here. Model weights and private user chat do not.

## Layout

```text
data/
  seed/                 small reviewed examples that define the format
  persona-style/        stable speaking style
  emotion-expression/   response conditioned on explicit emotion state
  relationship-events/  absence, apology, promise, conflict recovery
  time-awareness/       time/daypart/gap behavior
  memory-grounding/     use supplied memory without fabrication
  safety-boundaries/    expressive persona without coercion or invented access
  eval-heldout/          never used for training
```

## Promotion rule

A seed sample is not automatically training data.

```text
seed -> human review -> scenario-family split -> train/validation
                                |
                                +-> held-out test (never train)
```

## Source policy

- Prefer project-authored data.
- Record provenance and license for every sample.
- Synthetic samples record generator identity and require review.
- Do not import non-commercial datasets into a dataset intended for potentially commercial distribution without a separate legal decision.
- Do not train on private chat logs without explicit documented consent and sanitization.

See `docs/DATASET.md` for the full plan.
