# Architecture decisions

## ADR 001 — Native Android (accepted)
Empty destination; Kotlin/Compose, minSdk 29. Browser foundations contribute ideas, not a web app.

## ADR 002 — Narrow DNS observation (superseded by ADR 011)
A full TUN interface without a mature forwarding stack breaks networking. Scope initial relay to its own synthetic DNS address and preserve ordinary routing. DNS requests are not destination connections. No guessed app attribution. Full-flow collection remains a gate for complete M0.

## ADR 003 — Platform encrypted payload store (accepted for prototype)
SQLite indexes only timestamp/id; sensitive payloads use Keystore AES-GCM. Avoid a native database dependency until its build/16 KB compatibility is verified. Tradeoff: unencrypted timing metadata; evaluate Room + SQLCipher before production.

## ADR 004 — No automatic restart (accepted)
START_NOT_STICKY, no always-on and no reboot receiver. Explicit restart after failure/reboot avoids silently restoring monitoring. UI reflects actual process status, not a persisted ON claim.

## ADR 005 — No AI in M0 (accepted)
Prove observations first. No network calls for narration or model downloads.

## ADR 006 — Record the async resolver gap (emulator gap resolved by ADR 011; hardware acceptance pending)
The ordinary InetAddress path works through the narrow tunnel, but Android DnsResolver's automatic A/AAAA route preflight fails on the tested API 35 device. Do not disguise this by counting explicitly bound probe traffic as universal compatibility. Keep the prototype experimental and evaluate a vetted full forwarding stack before an all-day pilot or M1.

## ADR 007 — Start M1 with M0 gaps disclosed (accepted by user direction)
The user explicitly requested moving to M1 on 2026-09-22, with emulator-only testing for now. This supersedes the M1 sequencing gate in ADR 006, but does not close the DNS compatibility or hardware acceptance gaps. Add deterministic evidence classification and the Personal Data Graph using existing retained observations. Do not invent missing traffic or app ownership. Graph nodes are derived in memory, preserving existing retention and erasure. M2–M4 remain separate work.

## ADR 008 — Factual M2 first (accepted by user direction)
The user requested M2 and explicitly selected factual summaries before downloading a local model. Implement Your Day with deterministic structured facts and evidence-linked statements, fully offline. Model runtime, weights and local inference benchmarks remain deferred. No AI-generated prose or inferred behaviour is claimed. Respect local calendar days, the shorter available retention window and incomplete collection. M0 gaps remain open; M3 playback is not part of this step.

## ADR 009 — Evidence-only M3 playback (accepted)
At the user's request, add OTR-inspired chronological reveal and cumulative counts, without copying its illustrative corpus or inferred behaviours. Use fixed 15-minute grouping windows solely for pacing; do not associate DNS with a nearby app. Preserve exact evidence links and source classifications. No location, duration, personal-data transfer or confirmed-connection story is invented. Playback is transient, offline and secure, with explicit controls and lifecycle pause. M0 gaps remain open; M4 is separate.

## ADR 010 — Local M4 preparation and user-reported tracking (accepted)
The user requested M4 despite the disclosed earlier acceptance gaps. Add independently curated official privacy links, editable drafts and encrypted request history. Do not automate submission or destructive account actions. Separate observation from any claim of personal-data possession. A status transition requires a user-provided reference; reported completion never becomes independently verified deletion. Retain requests until explicit removal, separately from 24-hour events, but erase both with the existing key/database reset. Unknown contacts and regional mechanisms stay unknown rather than being guessed.

## ADR 011 — Local native forwarding with explicit bypass limits (accepted)
Replace the narrow DNS route with HevSocks5Tunnel 2.17.1 and an authenticated local Kotlin TCP/UDP relay. Protect and bind outbound sockets to the underlying network. Bypassable VPN policy fixes automatic async DNS selection on the API 35 emulator, with observed evidence asserted for each query. Apps may explicitly bypass collection; this is not a security VPN. Preserve Private DNS, disclose IPv6/encrypted-DNS gaps, bound resources and require manual restart after loss/revocation/reboot. Physical-device acceptance remains separate.

## ADR 012 — Durable storage and evidence-led journal UI (accepted)
Make event batches and pruning transactional, separate capture and erasure epochs, reject stale request edits and persist an erase-pending marker before destructive work. Decrypt snapshots outside the store lock so Stop remains responsive. Use a journal-style Your Day page with prominent playback, persistent navigation and separate Controls. Decorative art never represents invented observations. Test destructive recovery only on the dedicated emulator; use the user's tablet for non-destructive manual acceptance.

## ADR 013 — Offline 3D evidence archive (accepted by user direction)
The user approved a separate Your Day in 3D screen on 2026-10-06. Keep the native Android app and its collectors intact; bundle a lazy React Three Fiber island in a restricted WebView. Project only the selected retained evidence and collector status across a one-way JSON snapshot boundary. Keep app resumes and DNS groups separate, show every scale rule, and open original evidence in the existing native inspector. Use on-demand rendering, reduced motion and a text fallback. Do not invent traffic, risk scores, historical health or comparisons. Publish on a feature branch and PR; merging remains subject to explicit user approval.
