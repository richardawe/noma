# M2 summary and local model strategy

The user selected **Build factual summaries first** for M2. Version `0.2.0-your-day` ships deterministic local summaries and no model/runtime/download. The native Kotlin `DailySummarizer` computes `DayFacts` and allowlisted `SummaryClaim` statements, each linked to original Event IDs. It never fills evidence gaps or claims a request was a visit, data transfer or tracking action. This is M2's factual fallback path, not validated LLM inference.

SketchGPT's concepts are adapted rather than its browser runtime: counts and ownership come from code, evidence links remain visible, and unsupported generation must fall back to verified text. No untrusted domain string is executed or treated as a prompt. A future model should select/reorder permitted claim IDs or pass a strict grounded-output validator; free-form prose must not be rendered merely because its prompt asked for truthfulness.

`DayFacts` uses a named timezone and calendar bounds, filters future/expired/other-day records, keeps distinct request/name/app counts, and exposes the provenance graph. Today and Yesterday operate on retained data only; summaries add no persistence or separate deletion path. Daylight saving boundaries use calendar arithmetic, not a fixed 24-hour day. Retention remains a fixed 24-hour window and may truncate yesterday or a 25-hour day.

Local model evaluation below remains deferred. No RAM-to-parameter-tier promises.

Evaluate [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM), [llama.cpp Android](https://github.com/ggml-org/llama.cpp/tree/master/examples/llama.android) and [MLC Android](https://llm.mlc.ai/docs/deploy/android.html) against actual devices before selecting. Verify supported model architectures, CPU/GPU backend, artifact license, 16 KB native-library alignment, cancellation, context limits and offline loading.

Candidate sizes around 0.5B–2B are experiments only. Measure cold/warm load, peak RSS including first decode, tokens/sec, thermal throttling, battery cost, grounded-claim error rate and unload behaviour. Use structured evidence IDs and allowlisted facts; an instruction to be truthful is not validation. Render deterministic text if unavailable, cancelled, low memory or unsupported. Download only with consent, pinned digest and disk budget; never send telemetry to a model provider.

Verified on 2026-09-22: the upstream LiteRT-LM README lists a stable Kotlin API for Android/JVM and describes CPU/GPU/NPU paths. This is API availability, not a Noma model benchmark. No runtime dependency was added.
