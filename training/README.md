# Training Workflow

Training is intentionally blocked until raw and prompt/state-assisted baselines are recorded.

## Sequence

```text
1. raw safetensors baseline
2. persona prompt baseline
3. persona + deterministic state/time/memory baseline
4. review project-owned dataset
5. LoRA/SFT experiment
6. held-out character regression
7. merge only if needed
8. convert to GGUF
9. Q4 candidate
10. Infinix Hot 30 benchmark
```

## Initial base candidate

`google/gemma-3-270m-it` is the first lightweight baseline because the original safetensors checkpoint is small enough to experiment with locally and the deployment artifact can later be quantized for Android.

This is not a permanent model selection. LFM2.5-350M and a current Qwen sub-1B candidate remain comparison targets under `docs/MODEL-EVAL.md`.

## Fine-tuning default

Start with LoRA through Hugging Face PEFT/TRL rather than full fine-tuning.

Initial experiment envelope:

```text
method: LoRA + SFT
sequence length: <= 1024 initially
LoRA rank: 8-16 experiment
LoRA alpha: 16-32 experiment
small batch + gradient accumulation
FP16 training on consumer NVIDIA GPU when supported
held-out scenario families excluded from training
```

Do not freeze hyperparameters until a pilot run records VRAM, speed, loss, and held-out behavior.

## Dataset conversion

`data/seed/*.jsonl` defines project semantics only. Reviewed training samples are converted to the target model's native chat template at training time. Do not hard-code Gemma control tokens into the canonical dataset.

Structured state/time/memory should be rendered into a compact system context equivalent to the Android `SystemContextBuilder` contract.

## Outputs

Keep these separate:

```text
base safetensors
LoRA adapter
optional merged safetensors
GGUF export
quantized GGUF
benchmark evidence
```

Never overwrite the base checkpoint with a trained/merged copy.
