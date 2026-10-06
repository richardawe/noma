# M0–M4 validation — updated 2026-10-06

## 3D scene

Version `0.4.2-evidence-scene` (code 7) adds a separate offline WebView screen. Six mapping tests, two production Chromium browser flows, Android build/test APK, 21 JVM tests and lint pass (zero errors, 17 warnings). All 20 Android regression tests pass on the API 35 emulator (273.631 seconds), including native scene integration and the previously failing playback check. The final mobile caption styling was browser-tested and repackaged afterward. Reviewed screenshots use test fixtures, not tablet data. See [scene validation and mapping](EVIDENCE_SCENE.md). Physical tablet rendering/performance remains unverified.

## Foundation status (2026-09-23)

The foundation version was `0.4.1-foundation` (code 6), covering M0–M4 and the journal UI redesign. Local IPv4 forwarding and bypassable VPN policy resolve the tested API 35 automatic async DNS failure; tests verify observed evidence for each query. This is **not a completed all-day M0 pilot**. Originating-app attribution remains Unknown, and physical-device battery/long-session acceptance remains outstanding. No language model or automated privacy-request submission is included.

## Current foundation checks

- Full build, 21 JVM tests and lint pass; zero lint errors and 11 warnings.
- Latest full emulator regression: 18 tests, 17 passed, one DaySummaryUiTest accessibility-click failure. The test helper was updated to reacquire stale handles; that final test-only change has not yet been rerun. The full suite is not yet green.
- All four collection tests, five storage tests, five request-storage tests, request UI, design review and organisation navigation passed in that run. Coverage includes automatic/typed/explicit async DNS evidence, TCP/UDP DNS, HTTPS, restart, network loss, revocation, usage access, 20,000-event cap, transaction rollback, missing keys and stale edits.
- External emulator process-kill, force-stop and reboot checks pass: collection stops and does not restart when the app reopens.
- Production FLAG_SECURE remains enabled. Design renders use disposable fixtures in test code.
- Samsung Galaxy Tab S6 Lite (SM-P610), Android 13/arm64: application APK installed over wireless debugging and launched successfully. User-led recording/playback and day-long testing are pending; no destructive suite ran on the tablet.
- Remaining limits: active Private DNS, IPv6-only resolvers, encrypted/cached DNS, app bypass, non-TCP/UDP IPv4 traffic, physical network handover, OEM background behaviour, battery/long-session measurements and broader OS/device coverage. See [forwarding](NATIVE_FORWARDING.md) and [project memory](../MEMORY.md).

## Historical results

Everything below records earlier builds. Earlier async-DNS failures and pending emulator recovery checks are superseded by the current results above; old APK hashes and test counts do not describe the current build. Run destructive reproduction commands only on the dedicated emulator, never the user's tablet.

## M4 checks

- Final app build, unit tests and lint succeed. Twenty-one JVM tests pass. Lint: zero errors, 13 warnings (the additional warning suggests the `toUri` Kotlin extension).
- Nine emulator regression tests pass in 54.493 seconds: M4 request UI, two request-storage checks, M3/M2 offline playback, M1 navigation and four original storage checks. The request UI test creates a local draft while offline, records submission and reported completion with test references, confirms history, and removes the local record. It does not open an external website, copy text or send any request.
- Request storage tests verify ciphertext lacks fixture plaintext, request-only key reuse, retention beyond 24-hour event purging, erasure fences, rejection of stale status edits and rejection of event ciphertext substituted into the request table.
- A separate final missing-key test passes in 0.075 seconds: with request-only encrypted data and the Keystore key removed, both request reads and event writes refuse to create a replacement key. Total: ten distinct passing device tests across the regression run and this isolated check.
- Four new unit tests verify preparation never submits, completion requires a prior submission and a reference, sent wording/history is preserved, and directory links are fixed official HTTPS routes with automation disabled.
- Initial UI test reopened a public directory entry instead of the saved request. The selector now targets request history; the app also scrolls back to the request list after saving. The final regression run passes.
- APK SHA-256: `bee5d7d8143638d3d3b0d10370066850c823264d2264cc23f786193e8b9d68eb`.
- M0 collection remains excluded because its known DNS failures are unresolved. Official sites may require sign-in and their live request forms were not submitted or end-to-end tested. The source review and unknown regional contact fields are documented in [PRIVACY_REQUESTS.md](PRIVACY_REQUESTS.md).

## M3 checks

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --offline`: success. Seventeen JVM tests pass; lint has zero errors and 12 existing warnings.
- Story tests check chronological grouping, duplicate-ID removal, complete original evidence references, separate app/DNS branches, unknown owners, cumulative request/distinct-owner counts, no chapters for gaps and no playback on an empty day.
- Six targeted emulator tests pass in 45.734 seconds. Playback checks use explicitly labelled fixtures, disabled connectivity, and erasure afterward. The initial run exposed an ambiguous test selector (Pause button versus Paused status); the selector now requires an enabled clickable control. The final run verifies timed advancement, Back/Next, replay, pause, recap, original evidence and closure after erasure, alongside M1 navigation and storage checks.
- APK SHA-256: `f437a50a324ae341ffdbf300e30b1c9bead4e8f9835badb0617bef25272b97dc`.
- Background pause and touch-exploration manual mode are implemented but have not had dedicated acceptance tests on physical hardware. There is no claimed complete accessibility audit. M0 network compatibility remains open and its failing collection test is excluded from the targeted playback checks.

## Earlier M2 checks

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --offline`: success. Fifteen JVM tests pass. Lint: zero errors and 12 existing warnings.
- Six targeted emulator tests pass in 23.957 seconds: `DaySummaryUiTest`, `UnderstandingTest` and four `StorageTest` cases. The Your Day test stops DNS observation, stores an explicitly labelled fixture, disables connectivity, checks the summary and original evidence link, checks yesterday's empty/partial state and erasure, then restores connectivity and removes fixtures.
- Five daily-summary unit tests cover original evidence IDs, duplicate records, separate request/name/app counts, exact ownership, unknown names, excluded future/expired/unsupported events, calendar midnight boundaries, 23/25-hour DST days, another timezone, pause-only records and no claims after erasure.
- M0 `CollectionTest` was not included in these targeted M1/M2 checks; its earlier failure remains recorded below. No battery or full-day claim is made.
- Final APK SHA-256: `34ba899f85cd690cadcbfc29fad4c866262e6d58e14e0505dcee328ecf6f9027`.

```sh
adb -s DEVICE shell am instrument -w \
  -e class org.noma.DaySummaryUiTest,org.noma.UnderstandingTest,org.noma.StorageTest \
  org.noma.test/androidx.test.runner.AndroidJUnitRunner
```

## Earlier M1 checks

- Final build: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --offline`, successful. Ten JVM tests pass (seven parser/registry and three graph tests). Lint: zero errors, 12 warnings.
- Five targeted emulator tests pass in 18.317 seconds: `UnderstandingTest` plus four `StorageTest` cases. UI coverage follows organisation → exact domain → original request, verifies Unknown app ownership and opens an unmatched domain. The test scrolls through long content and waits for Compose transitions. Fixture events are erased afterward.
- Graph tests cover duplicate event IDs, domain normalization, category grouping, preserved source/revision, unknown names, separation from nearby app events, and empty derived state after erasure. The device offline test now builds the graph without connectivity and checks it is empty after erasure.
- The full six-test device run did **not** pass: the existing M0 `CollectionTest` timed out on its UDP DNS probe (probe event absent, 17 packets seen / 15 rejected). This is an additional observed M0 reliability failure, not masked by the targeted M1 results. The first UI-test attempt also failed; tree traversal and scroll/transition handling were corrected and the final targeted run passed.
- Final APK SHA-256: `1b727694006e259d59a835428227a3c7d852cc293c9bf773647d211b31ade29f`.

Reproduce M1's device checks with:

```sh
adb -s DEVICE shell am instrument -w \
  -e class org.noma.UnderstandingTest,org.noma.StorageTest \
  org.noma.test/androidx.test.runner.AndroidJUnitRunner
```

## Earlier M0 checks (historical; see latest failures above)

Environment: Android 15/API 35 Google APIs arm64 emulator, Pixel 6 profile, emulator 37.1.11. Host build: Temurin JDK 21.0.2, Gradle 8.13, AGP 8.9.2, Kotlin 2.1.20; app JVM target 17.

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`: success. Seven JVM tests pass. Lint: zero errors, 12 warnings (preference API/style suggestions, pinned dependency updates, application-context lifetime). The configured-VPN foreground permission rule has a narrowly documented suppression; no alarm permission was added.
- Five instrumentation tests pass, final run 10.814 seconds, executed directly through AndroidJUnitRunner on the emulator. Gradle's connected-test launcher stalled while fetching its extra runner dependencies; the APKs were instead installed and run with ADB.
- A real UDP DNS probe traverses the TUN, receives a response, is stored and matches the pinned organisation registry.
- A separate test APK/UID resolves `www.google-analytics.com` using ordinary `InetAddress.getAllByName`. It receives an answer, and Noma records the request and its source-backed organisation match. This is a deliberate integration probe, not a claim that a full natural day was measured.
- Android UsageStats records the Settings app's resume event after explicit opt-in. Revoking usage access disables further imports.
- VPN explicit Stop and restart work. The initial stopService-only implementation left Android's binding alive; the final implementation explicitly closes the tunnel, sockets and worker pool before stopping.
- Encrypted database roundtrip, absence of fixture plaintext in the database file, 24-hour expiry, erasure and rejection of writes with an old epoch pass.
- Corrupt ciphertext is rejected; wrong registry digest yields Unknown. Unknown/suffix-spoofed domains never acquire an owner.
- Offline test disables connectivity and reads stored evidence plus bundled classifications, then restores the emulator's prior airplane-mode setting.
- Parser tests cover truncation, invalid headers/checksums, fragmentation, compressed/multiple questions, mismatched DNS responses, and 10,000 deterministic hostile byte arrays.
- Final UI hierarchy contains the expected controls, an empty post-erasure timeline and DNS OFF. Production UI uses FLAG_SECURE; no screenshot bypass was introduced.

## Confirmed failure: async resolver

Android's `DnsResolver.query` overload that automatically selects A/AAAA returns ENONET on this DNS-only network before issuing the DNS request. Its public-address reachability check expects more general routes. The standard InetAddress path above works. An API 33 route-exclusion experiment did not fix the problem and was removed. Do not describe this as universal Android network monitoring or recommend an all-day pilot yet.

The failure remains reproducible with the test-only probe:

```sh
adb -s DEVICE shell am instrument -w \
  -e class org.noma.CollectionTest -e asyncDns true \
  org.noma.test/androidx.test.runner.AndroidJUnitRunner
```

That diagnostic mode is expected to fail until the compatibility gap is fixed. It is not counted among the passing checks. The probe activity exists only in the test APK, not Noma's application APK.

## Reproduce the passing checks

Use a dedicated emulator/test device: the instrumentation grants test app-ops, creates and erases test data, starts a VPN and briefly toggles airplane mode.

```sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
adb -s DEVICE install -r app/build/outputs/apk/debug/app-debug.apk
adb -s DEVICE install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s DEVICE shell am instrument -w org.noma.test/androidx.test.runner.AndroidJUnitRunner
```

Android's `am instrument` can exit zero even when assertions fail. Inspect its `OK (5 tests)` or `FAILURES` result, not just the shell exit code.

## Resource snapshot and remaining work

A single foreground debug-UI snapshot after erasure measured 108,340 KiB total PSS (about 106 MiB), 209,916 KiB RSS and 40 KiB in the events database directory. Collection was off. These are not steady-state collector figures or a battery benchmark.

No physical battery, CPU-over-a-day, DNS overhead distribution, storage-growth curve, sustained load, low-memory or low-storage fault tests have been completed. `tools/measure_device.sh` and DEVICE_TEST_PLAN.md define the measurement path and provisional gates. No claim of negligible battery impact is made.

Also pending: active-VPN reboot, VPN revocation through Settings, OEM Doze behaviour, network handover on hardware, API 29/34/current-OS coverage and another-VPN/work-profile conflicts. Private DNS refusal is implemented and API-checked, but needs on-device encrypted-DNS acceptance tests. DNS over TCP from clients, fragmented queries, IPv6-only resolver networks, cached/encrypted DNS and general TCP/UDP destination capture remain outside coverage.

For complete M0, resolve DNS client compatibility and decide on a vetted forwarding engine before adding app-to-flow ownership. Then run the physical-device day test. Dataset redistribution also needs a commercial license or replacement before commercial release.

Earlier M0 debug APK SHA-256: `8762d323d5a363d0e05f2c2b6df30668c31261008473df17e8f541006b93f317`. The emulator is now running the newer M4 build for inspection.
