# Model Artifacts

Do not commit model weights here.

Local layout recommendation:

```text
models-local/
  base/
    gemma-3-270m-it/        original safetensors checkpoint
  adapters/
    anisa-gemma-270m-v001/  LoRA adapter
  merged/
    anisa-gemma-270m-v001/  optional merged safetensors
  android/
    anisa-gemma-270m-v001-q4_k_m.gguf
```

Git stores only manifests, hashes, benchmark results, and reproducible configuration.

## Required identity fields

Every evaluated artifact should record:

```text
base_model
base_revision
adapter_revision
training_dataset_revision
merge_method
export_tool_revision
gguf_quantization
sha256
created_at
```

The original safetensors checkpoint remains the training/master source. GGUF Q4 is a deployment artifact, not the training source.
