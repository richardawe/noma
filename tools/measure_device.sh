#!/bin/sh
# Run against a test device only. Outputs process metrics, never event payloads.
set -eu
adb_bin=${ADB:-adb}
serial=${1:?Usage: measure_device.sh DEVICE_SERIAL}
"$adb_bin" -s "$serial" shell dumpsys meminfo org.noma
"$adb_bin" -s "$serial" shell dumpsys cpuinfo | rg 'org.noma' || true
"$adb_bin" -s "$serial" shell run-as org.noma du -k databases
"$adb_bin" -s "$serial" shell dumpsys batterystats org.noma
