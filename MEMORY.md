# Noma project memory

Updated 2026-10-06. Repository: https://github.com/richardawe/noma

The M0–M4 baseline and this memory were pushed to `main` in commit `7f5d74d`. The 3D feature is developed on `codex/your-day-3d`; do not merge without user approval.

## Current build

- Native Kotlin/Compose Android app, package `org.noma`, version `0.4.2-evidence-scene` (code 7), Android 10+; arm64-v8a and x86_64.
- M0: opt-in app activity and standard DNS metadata, encrypted local storage. Local IPv4 TCP/UDP forwarding uses vendored HevSocks5Tunnel 2.17.1 and an authenticated loopback relay. No remote proxy or TLS interception.
- M1: exact domain-to-organisation classification and evidence graph; unmatched owners and originating DNS apps remain Unknown.
- M2: offline factual daily summaries. User chose facts first; no model download or inference.
- M3: Play My Day, 15-minute chapters, pause/replay and original evidence links.
- M4: encrypted local privacy-request drafts and user-reported status. Nothing is submitted automatically.
- UI: warm journal design, Your Day / Timeline / Organisations / Take Back navigation, separate Controls page.
- Storage: 24-hour event retention, 20,000-event cap, transactional batches, stale-write/edit fences and recoverable erasure. Request drafts persist until removed or all data is erased.

## 3D screen (2026-10-06)

- User approved a separate Your Day in 3D screen, with a link from Your Day. React/Three.js are bundled offline inside a restricted Android WebView; there is no website deployment.
- Two shelves show app resumes and DNS records separately. Exact organisation matches, unknown names, bounded fill, chapter replay and original evidence links use retained real records only. No synthetic traffic, inferred ownership or comparison baseline.
- Source: `scene/`; build with `npm ci`, `npm test`, `npm run build`, `npm run notices`, `npm run size`, `npm run test:browser` from that directory. Generated assets and licence notices are committed so ordinary Gradle builds need no Node installation.
- Six mapping tests and two production Chromium flows pass; reviewed fixture screenshots are under `docs/screenshots/`. Entry is 925 bytes gzip and all scene assets about 338 KB gzip.
- Android build, test APK, 21 JVM tests and lint pass (zero errors, 17 warnings). Tablet performance and full accessibility acceptance remain pending.
- All 20 Android regression tests pass on the dedicated API 35 emulator (273.631 seconds), including the new snapshot and bundled-page/native-inspector integration tests and the previously failing playback test. Production FLAG_SECURE stays enabled.
- Details and mapping: [EVIDENCE_SCENE.md](docs/EVIDENCE_SCENE.md).

## Earlier foundation validation

- Latest full build, 21 JVM tests and lint passed; lint reported zero errors and 11 warnings.
- September 23 emulator run: 18 tests, 17 passed. DaySummaryUiTest failed on an accessibility click. Its helper now retries stale handles; the playback test passed in the October 6 run. See the current scene validation above.
- All four collection tests passed in that run, including automatic/typed/explicit async DNS evidence, TCP/UDP DNS, HTTPS, restart, network loss, revocation and usage access.
- Emulator process-kill, force-stop and reboot recovery checks passed: VPN is removed and reopening does not restart collection.
- Storage and request-storage tests passed, including cap, rollback, missing key, corruption, retention and stale edits.
- Galaxy Tab S6 Lite (SM-P610), Android 13, arm64: paired over wireless debugging; app-only APK installed and launched successfully. User is taking it away for manual testing. Permission setup, recording, playback, battery and day-long results are not yet confirmed.

## Rules for continuing

- Use an explicit ADB serial. Destructive instrumentation and recovery scripts run only on `emulator-5554`; never run them on the user's tablet or install its test APK there.
- Wireless debugging needs the tablet and Mac on the same reachable Wi-Fi. The installed app operates independently; summaries and app activity work offline. Do not store pairing codes, device identifiers or collected personal evidence in this repository.
- DNS observation stops after network loss/change, reboot or revocation and needs manual restart. Active Private DNS is preserved and unsupported for observation. IPv6, encrypted/cached DNS and apps explicitly choosing another network can bypass observation. ICMP is not forwarded.
- No app-to-DNS ownership inference, confirmed-transfer claims, model-generated stories or verified-deletion claims.
- Retain native licence notices. Tracker Radar data is CC BY-NC-SA 4.0; commercial distribution needs permission or replacement.

## Build and next work

Use JDK 17+, SDK 35, build-tools 35.0.0 and NDK 27.2.12479018. Keep SDK paths in ignored `local.properties`. Use `-PnomaBuildRoot=/tmp/noma-outputs` in this synced workspace to avoid duplicate generated files.

```sh
./gradlew -PnomaBuildRoot=/tmp/noma-outputs testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

App APK: `/tmp/noma-outputs/app/outputs/apk/debug/app-debug.apk`. Test APK is emulator-only. Inspect instrumentation's final `OK` / `FAILURES` output; its process can exit zero on test failures.

Next: collect the user's tablet findings, verify the 3D screen on the physical tablet, then validate physical-device connectivity, OEM background behaviour, battery and long sessions. Other gaps include OS/device coverage, 16 KB pages, IPv6-only networks and VPN/work-profile conflicts. See [validation](docs/VALIDATION.md), [device plan](docs/DEVICE_TEST_PLAN.md) and [native forwarding](docs/NATIVE_FORWARDING.md).
