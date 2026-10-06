# Android API verification — 2026-09-22

Verified official sources by fetching their current pages before implementation.

- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager): PACKAGE_USAGE_STATS manifest declaration is insufficient; user grants special access through ACTION_USAGE_ACCESS_SETTINGS. queryEvents is historical, not a real-time callback. Data may be unavailable before user unlock. Use ACTIVITY_RESUMED/PAUSED from API 29; do not infer complete sessions.
- [VPN guide](https://developer.android.com/develop/connectivity/vpn): VpnService.prepare consent; one prepared VPN; protect forwarding sockets; foreground notification; close tunnel on revoke. Local VPN is a packet interface and does not forward by itself.
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types): configured VPN apps qualify for systemExempted with FOREGROUND_SERVICE_SYSTEM_EXEMPTED on API 34+. Must prepare VPN first; no silent background start. Network service uses this type, not dataSync.
- [ConnectivityManager](https://developer.android.com/reference/android/net/ConnectivityManager#getConnectionOwnerUid(int,java.net.InetSocketAddress,java.net.InetSocketAddress)): API 29 TCP/UDP owner UID available to the active VPN; INVALID_UID and SecurityException must be handled. A DNS resolver UID is not the originating app.
- [Background location](https://developer.android.com/develop/sensors-and-location/location/permissions/background): separate permission flow on Android 10+, settings grant on 11+; approximate location and background limits apply. Deferred; no location permission requested.
- POST_NOTIFICATIONS on API 33+: optional runtime request for notification visibility; foreground service still must supply its notification.

No accessibility, QUERY_ALL_PACKAGES, root, certificate installation, boot auto-start or battery-optimisation exemption requested. Private DNS / VPN conflicts and IPv6-only resolver networks are explicit unsupported conditions.

AGP 8.9.2 lint models only the alarm-permission route to systemExempted and flags this service. A narrow manifest suppression cites the documented configured-VPN eligibility; no alarm permission is added. The runtime VPN path is exercised by the emulator collection test.

The [AOSP DnsResolver implementation](https://android.googlesource.com/platform/frameworks/base/+/300b48ff117c4f98ebc2d71e0594a9504f33348a/core/java/android/net/DnsResolver.java) checks IPv4/IPv6 reachability in its automatic A/AAAA overload. The earlier ENONET result alone did not identify its cause. Full forwarding still failed automatic network selection under non-bypassable policy, while explicit-network queries passed. With [Builder.allowBypass](https://developer.android.com/reference/android/net/VpnService.Builder#allowBypass()) automatic and explicit async DNS both pass in the API 35 emulator, with observed evidence asserted separately. This permits apps to choose another network; Noma is an observation tool, not a network lockdown.
