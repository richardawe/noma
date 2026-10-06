# Architecture — M0 collection through M4 request tracking

Observe deterministically. Explain intelligently. M0 contains no inference engine.

Android 10+ (minSdk 29), Kotlin and Jetpack Compose. Target/compile SDK 35 for the initial sideloaded prototype; re-check current Play target requirements before distribution.

## Pipeline

UsageStats queryEvents → immutable resume/pause events → encrypted local store.
Local VpnService → native IPv4 TCP/UDP forwarding → validated standard DNS question → immutable DNS_REQUEST → pinned domain registry → classified organisation → chronological timeline.

The VPN forwards IPv4 TCP/UDP using a pinned native stack and an authenticated on-device relay, with protected sockets bound to the underlying network. Only standard DNS names become evidence. IPv6 bypasses observation. Private DNS is never downgraded. See [native forwarding](NATIVE_FORWARDING.md) for protocol, bounds, build provenance and limits. No remote proxy or TLS interception is involved. Unsupported packets never become evidence.

DNS_REQUEST means a name was requested, not a connection, visit, tracking action, or data transfer. Android often attributes resolver sockets to the system resolver. The relay does not receive originating-app UIDs, so DNS ownership is explicitly Unknown. App events and DNS requests share a timeline only. No time-proximity rule assigns an owner. Full app-to-flow attribution remains an M0 acceptance gap requiring a separate attribution design and real device tests.

## Storage

Android SQLite stores timestamps and encrypted event payloads. AES-256-GCM uses a non-exportable Android Keystore key, fresh random nonce per record, and event id/time as associated data. Payload contains type, package/domain, evidence level and source; no packet bytes. SQLite schema metadata and timestamps remain visible to file extraction. No external storage or backup. Prefer Room + SQLCipher for a later fully encrypted indexed database after native-library and 16 KB page-size validation. The storage layer uses Android platform crypto; the separate networking layer vendors a native forwarding stack.

Retention is 24 hours, bounded to 20,000 newest events. Purge at reads/writes, resume and a best-effort periodic worker; Android can delay background purge. Erase synchronizes against writers, disables collection, increments an epoch, drops the database and deletes the key. Old in-flight operations cannot write into the new epoch. Re-enable is explicit. No crash analytics.

## Lifecycle

Network collection starts from a visible activity after VPN consent, with a foreground notification. START_NOT_STICKY; revoke, fatal collector failure, network change or reboot stops capture. No VPN boot receiver or always-on support. WorkManager may resume already-authorised usage imports after reboot/unlock. Usage collection is independently enabled and imports from the opt-in timestamp, every 15 minutes best-effort and on refresh. Revoked access stops imports; gaps are not filled with guesses. No continuously held wake lock.

## Proposed repository structure

- app/src/main/java/org/noma: Compose UI, collectors, encrypted store, deterministic parsers and registry.
- app/src/main/assets: small pinned public registry with source and digest.
- app/src/test: parser, mapping and false-attribution unit tests.
- app/src/androidTest: local storage/deletion and device checks.
- tools: reproducible registry import and validation utilities.
- docs: product, API research, architecture, threats and test results.

M1's pure Kotlin `PersonalDataGraph` projects retained events into domain evidence and organisation groups. Each domain carries its exact Classification, source and revision, plus its original Event references. App events remain a separate branch. There is no inferred app-to-domain edge. Stable event IDs prevent duplicate counting, normalized domains group repeated names, and unknown records remain unclassified. The graph is held in memory and rebuilt when events change; no second sensitive database or deletion path is introduced. Registry exact lookups use an in-memory map.

M2's pure Kotlin `DailySummarizer` builds structured `DayFacts` and deterministic `SummaryClaim` text from retained events and the graph. Each statement has a level and original evidence IDs. It filters calendar boundaries in the current device timezone, expires records older than 24 hours, excludes future and unsupported events, and deduplicates IDs. Resume counts do not become durations or app-to-domain joins. Daylight saving uses local calendar arithmetic. The UI rebuilds on each refresh and date selection; summaries have no second store or background job. Erasure clears open summary selections. No model or external generation request is made.

M3 `DayStory` builds immutable chronological 15-minute chapters from an already scoped `DailySummary`. Cumulative request and distinct-owner counts come from the same evidence graph. Intro and recap frames reference the entire snapshot; chapter frames reference only their own events. No chapter is created for an empty time window. The secure Compose dialog holds no extra persisted data. A disposable coroutine advances every seven seconds; pause, evidence inspection and backgrounding stop advancement. Navigating resets the content scroll. A short fade honours Compose animation scaling. New records do not shift an active story; erasure, expiry or a read failure closes it on refresh. Rotation recreates the activity and closes the transient player. Touch exploration starts playback paused; manual controls remain available.

M4 adds an independently curated `PrivacyDirectory`, generic request templates and an explicit `PrivacyRequest` state machine. The `requests` table shares the existing Keystore key and erasure lock, but uses request-specific authenticated metadata. Request data survives event purges and is removed individually or by full erasure. Key creation checks both tables, preventing replacement of a missing key over request-only encrypted data. Status updates preserve history and sent wording; opening a page and copying do not alter status. Network operations remain outside the request workflow: official HTTPS links open only on a user tap, clipboard export is explicit, and no submission API or email sender exists. Public directory entries and local tracking work offline. See [PRIVACY_REQUESTS.md](PRIVACY_REQUESTS.md).

M2's optional local model and automated third-party request submission remain unimplemented.

## Compatibility validation

The previous DNS-only implementation failed automatic A/AAAA preflight. It is replaced by full IPv4 TCP/UDP forwarding, not by dummy routes. Current test results and previous failures are recorded in [VALIDATION.md](VALIDATION.md). Physical-device acceptance remains separate.

## Atomic storage and deletion

Usage imports commit in bounded batches with stable IDs; the cursor advances only after successful imports. Inserts and retention pruning share a transaction. A persisted erasure marker is completed before any later database access. Capture generations fence in-flight events; a separate erasure generation protects request editors without invalidating drafts when a collector stops. Request writes compare the exact prior snapshot, rejecting lost updates and resurrection after removal.

Full-day reads copy an encrypted snapshot under the storage lock, then decrypt outside it so Stop and erasure are not blocked by a long Keystore scan. Results are discarded if their erasure generation changes; unreadable data clears the visible UI.
