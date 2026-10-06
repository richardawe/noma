# Noma project memory

Updated 2026-09-23. Repository: https://github.com/richardawe/noma

## Current build

- Native Kotlin/Compose Android app, package `org.noma`, version `0.4.1-foundation` (code 6), Android 10+; arm64-v8a and x86_64.
- M0: opt-in app activity and standard DNS metadata, encrypted local storage. Local IPv4 TCP/UDP forwarding uses vendored HevSocks5Tunnel 2.17.1 and an authenticated loopback relay. No remote proxy or TLS interception.
- M1: exact domain-to-organisation classification and evidence graph; unmatched owners and originating DNS apps remain Unknown.
- M2: offline factual daily summaries. User chose facts first; no model download or inference.
- M3: Play My Day, 15-minute chapters, pause/replay and original evidence links.
- M4: encrypted local privacy-request drafts and user-reported status. Nothing is submitted automatically.
- UI: warm journal design, Your Day / Timeline / Organisations / Take Back navigation, separate Controls page.
- Storage: 24-hour event retention, 20,000-event cap, transactional batches, stale-write/edit fences and recoverable erasure. Request drafts persist until removed or all data is erased.

## Validation

- Latest full build, 21 JVM tests and lint passed; lint reported zero errors and 11 warnings.
- Latest full emulator run: 18 tests, 17 passed. DaySummaryUiTest failed on an accessibility click. Its helper now retries stale handles, but this final test-only change still needs a rerun. Do not describe the full suite as green.
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

Next: rerun the fixed playback UI test on the emulator, collect the user's tablet findings, then validate physical-device connectivity, OEM background behaviour, battery and long sessions. Other gaps include OS/device coverage, 16 KB pages, IPv6-only networks and VPN/work-profile conflicts. See [validation](docs/VALIDATION.md), [device plan](docs/DEVICE_TEST_PLAN.md) and [native forwarding](docs/NATIVE_FORWARDING.md).
