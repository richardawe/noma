#!/usr/bin/env python3
"""Destructive lifecycle checks, hard-locked to emulator-5554. Never runs on a phone."""
import os
import re
import subprocess
import time

ADB = os.environ.get("ADB", "/tmp/noma-sdk/platform-tools/adb")
SERIAL = "emulator-5554"

def adb(*args, check=True, timeout=30):
    result = subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True, text=True, timeout=timeout)
    if check and result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    return result.stdout.strip()

def shell(*args, **kwargs):
    return adb("shell", *args, **kwargs)

def until(predicate, timeout=25):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        if predicate():
            return
        time.sleep(.5)
    raise AssertionError("Emulator condition timed out")

def active():
    return any(int(value)>=0 for value in re.findall(r"Active vpn type: (-?\d+)",shell("dumpsys", "vpn_management")))

def open_app():
    shell("am", "start", "-n", "org.noma/.MainActivity")
    time.sleep(1)

def start():
    open_app()
    shell("run-as", "org.noma", "/system/bin/am", "start-foreground-service", "--user", "0", "-n", "org.noma/.DnsVpnService")
    until(active)

def stopped_after_open():
    until(lambda: not active())
    open_app()
    time.sleep(2)
    assert not active(), "Opening Noma unexpectedly restarted observation"

assert shell("getprop", "ro.kernel.qemu") == "1", "Refusing to test a physical phone"
previous_dns = shell("settings", "get", "global", "private_dns_mode")
try:
    shell("settings", "put", "global", "private_dns_mode", "off")
    shell("appops", "set", "org.noma", "ACTIVATE_VPN", "allow")
    if not active():
        start()
    pid = shell("pidof", "org.noma")
    assert pid.isdecimal()
    shell("run-as", "org.noma", "kill", "-9", pid)
    stopped_after_open()
    print("PASS: process SIGKILL removes VPN; reopening leaves observation off", flush=True)

    start()
    shell("am", "force-stop", "--user", "0", "org.noma")
    stopped_after_open()
    print("PASS: force-stop removes VPN; reopening leaves observation off", flush=True)

    start()
    adb("reboot")
    adb("wait-for-device", timeout=60)
    until(lambda: shell("getprop", "sys.boot_completed", check=False) == "1", timeout=60)
    stopped_after_open()
    print("PASS: reboot while observing removes VPN; reopening leaves observation off", flush=True)
finally:
    adb("wait-for-device", timeout=60)
    shell("am", "force-stop", "--user", "0", "org.noma", check=False)
    if previous_dns == "null":
        shell("settings", "delete", "global", "private_dns_mode")
    else:
        shell("settings", "put", "global", "private_dns_mode", previous_dns)
    open_app()
