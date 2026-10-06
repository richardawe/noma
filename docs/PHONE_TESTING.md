# Test on an Android phone or tablet

Use a 64-bit phone running Android 10 or later and a USB data cable.

1. In About phone (sometimes Software information), tap Build number seven times.
2. Enable USB debugging in Developer options.
3. Connect to the Mac, unlock the phone and accept its USB debugging prompt.
4. Install the app APK using the phone's exact serial: `adb -s PHONE_SERIAL install -r PATH_TO_APK`.

[Official Android connection guide](https://developer.android.com/studio/run/device).

For Android 11+ wireless debugging, connect the device and Mac to the same Wi-Fi. Enable Developer options → Wireless debugging → Pair device with pairing code. Use `adb pair IP:PAIRING_PORT` and enter the displayed code. Check `adb devices -l`; if needed, use `adb connect IP:CONNECTION_PORT` from the main Wireless debugging screen (a different port). Always select the physical device's exact serial when installing the app APK.

The Galaxy Tab S6 Lite (SM-P610), Android 13/arm64, was paired, installed and launched successfully on 2026-09-23. Manual recording/playback and battery acceptance are pending. The tablet can leave the Mac's Wi-Fi for testing: stored summaries and app activity work offline. DNS observation needs network traffic and must be restarted after network loss/change. Reconnect for remote diagnostics or updates.

After installation, open Noma → Controls. Enable App activity, grant Usage access, then return and turn collection on. Enable DNS observation separately after reading its VPN consent. Use a few apps and browse normally, then return to Your Day → Play My Day. Inspect a claim and its original evidence. Stop observation and confirm browsing still works.

Noma preserves active Private DNS. If that prevents DNS observation, app activity and playback can still be tested. Apps may choose another network and bypass observation; IPv6, encrypted DNS and cached names remain outside coverage.

Keep destructive automated tests on the emulator. Do not install the test APK or run the instrumentation suite on a personal phone: those tests erase Noma data and change test-device settings. For battery measurements, follow DEVICE_TEST_PLAN.md; a short session cannot establish all-day battery cost.
