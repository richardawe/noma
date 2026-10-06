# Physical-device acceptance plan

For USB setup, see [PHONE_TESTING.md](PHONE_TESTING.md). Automated destructive tests belong only on emulator-5554.

Goal: Noma can remain enabled without materially degrading normal phone use. The emulator is not a battery benchmark.

## Performance gates (targets, not measured results)

On one named Android phone, compare two matched 24-hour usage sessions with Noma off/on. Record device, Android build, network, screen time, background restrictions and ambient conditions. Target no more than 3 percentage points additional battery use per 24 hours and average collector CPU below 1% during idle periods. Measure DNS overhead against the same resolver; target median added latency under 20 ms and p95 under 100 ms. These provisional targets must be reconsidered from device measurements, not marketed as achieved.

Run `ADB=/path/to/adb tools/measure_device.sh DEVICE_SERIAL` at start, after warmup and at end. Record process PSS, CPU time, database size, wakeups and batterystats. For network overhead, compare resolver latency with capture disabled/enabled and count query/response bytes; current in-memory counters expose received/rejected packet counts only. No payloads or behavioural logs should enter diagnostics. Native/Java memory pressure and storage exhaustion still require fault injection.

## Required manual cases before a tester uses it all day

1. Onboard with both collectors off; deny each permission independently.
2. Enable usage; open several apps, including split-screen; inspect real lifecycle events. Missing pauses must not produce invented duration.
3. Enable DNS; browse with ordinary DNS, then cached DNS, Private DNS and in-app DoH. Verify coverage wording, no downgrade of active Private DNS and normal network recovery after Stop.
4. Switch Wi-Fi/mobile and lose connectivity. Noma must stop safely and require explicit restart.
5. Stop and restart repeatedly; confirm no old reader can stop a new tunnel.
6. Revoke VPN through Android Settings and usage access separately. Confirm visible OFF state and no subsequent collection.
7. Reboot while observing. No automatic VPN restart; reopen, inspect retained evidence and restart explicitly.
8. Leave screen off through Doze/OEM battery restrictions. Record gaps and delayed usage imports without invented events.
9. Fill storage and trigger memory pressure. Confirm safe failure and connectivity recovery.
10. Erase while DNS queries are in flight. Confirm empty timeline, disabled controls and no historical reimport after re-enabling.
11. Disable connectivity; inspect existing timeline and organisation evidence with no model/network dependency.

Test Android 10, 14, 15 and the current OS on hardware before widening the supported claim. Verify corporate/work-profile and another-VPN conflicts separately. Full-flow destinations and originating-app attribution remain outside this DNS prototype.
