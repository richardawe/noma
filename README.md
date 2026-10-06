# Noma · M4 Take Back My Data

Native Android prototype for seeing a local, evidence-backed data trail.

Current build and handoff: [project memory](MEMORY.md).

**Experimental and incomplete M0:** app lifecycle events and standard DNS requests share a timeline. DNS requests are not confirmed connections, and their originating app is **Unknown**. No AI, cloud account, analytics SDK, TLS decryption or location collection.

## Build

JDK 17+, Android SDK 35, build-tools 35.0.0. Set `ANDROID_HOME` or create an ignored `local.properties` with `sdk.dir=/your/sdk`.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

Open Noma, grant special usage access and explicitly enable app activity. Enable DNS observation separately after reading its limits and granting Android VPN consent. Use another app, return, then refresh and tap an event to inspect evidence. The first read never imports activity from before opt-in.

DNS uses the network’s existing resolver. IPv4 TCP/UDP passes through a local relay; there is no remote proxy. Apps may explicitly bypass observation; IPv6 also bypasses it. Active Private DNS and IPv6-only DNS resolvers are unsupported; app-encrypted DNS and cached names remain invisible. Stop observation if connectivity is affected. Restart manually after reboot, network changes or permission revocation. See [forwarding and limits](docs/NATIVE_FORWARDING.md).

Data stays in private local storage, with encrypted event payloads and Android Keystore keys. Timestamps are plaintext. Erase stops observation and deletes local events/key; public registry stays. See [privacy model](docs/PRIVACY_MODEL.md).

The bundled public registry has **CC BY-NC-SA 4.0** terms and is for noncommercial evaluation. Commercial distribution needs a licensed replacement or permission; see [dataset notes](docs/TRACKER_DATASETS.md).

The **Organisations** tab adds M1: a local Personal Data Graph of recorded names, exact dataset owners, categories and original evidence. Expand an organisation, select a name, then a request to inspect its source and timestamp. Unmatched names remain Unknown. The graph is rebuilt from retained events; erasing events also removes its derived nodes. No app-to-domain link is inferred from timing.

**Your Day** adds M2's factual summary path: Today/Yesterday in the device's local timezone, grounded counts, matched organisations and unknown names. Select a statement, then a supporting record, to inspect its evidence. Summaries work offline, update from retained events and disappear with erasure. Yesterday is marked partial because retention is only 24 hours. Missing records never become a claim of no activity.

**▶ Play My Day** opens an offline, full-screen visual story: date introduction, chronological chapters, cumulative counts and a recap. Pause, next/back, replay, skip to recap and evidence inspection are available. Chapters group retained records into 15-minute windows without linking nearby apps to DNS. Playback uses a fixed snapshot, pauses in the background and closes if its records are erased or expire. Empty dates have no invented story. Screen-reader users start in manual mode.

**Take Back** adds editable privacy-request drafts, a small official-page directory and encrypted local status history. Prepare requests for access, deletion, objection, consent withdrawal or advertising opt-out. Open an official route or copy the draft yourself; Noma never submits it. Status updates require a reference and completion is explicitly unverified. Requests stay until individually removed or all Noma data is erased, beyond the activity timeline's 24-hour retention. See [M4 workflow and sources](docs/PRIVACY_REQUESTS.md).

At the user's request, factual summaries come first: no language model, model download or model-generated prose is included. SketchGPT's structured grounding and safe-fallback concepts and OTR's progressive storytelling are adapted to native Kotlin. Local model evaluation remains deferred. Emulator networking is now validated; physical-device and all-day validation remain separate.

For a real phone, use [PHONE_TESTING.md](docs/PHONE_TESTING.md). The instrumentation suite is destructive and belongs only on the dedicated emulator.

Start with [foundation review](docs/FOUNDATION_REVIEW.md), [architecture](docs/ARCHITECTURE.md), [API research](docs/ANDROID_PERMISSIONS.md) and [validation status](docs/VALIDATION.md).

## Emulator development

Install NDK 27.2.12479018 alongside the Android SDK. Build both app and test APKs. In a synced workspace, use `-PnomaBuildRoot=/tmp/noma-outputs` to keep generated build files outside Documents. The APK then lives under `/tmp/noma-outputs/app/outputs/apk/debug/`. Native ABIs: arm64-v8a and x86_64.

Your Day uses a warm journal layout with prominent playback, persistent bottom navigation and a separate Controls page. All illustrations are decorative local vectors; counts and stories still come only from evidence. Empty dates offer collection setup rather than a made-up story.
