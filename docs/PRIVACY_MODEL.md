# Privacy model

No accounts, analytics, remote logging, cloud inference or Noma server endpoints. Collectors do not capture typed content, full URLs, payloads or location. M4 stores only the draft text and status references the user explicitly enters. DNS names and request text can reveal sensitive information; treat them as personal data.

The local DNS relay sends the original DNS query to the network's existing resolver; that resolver still sees DNS. Noma does not make those names private from the resolver or intercept application-encrypted DNS. The app must explain this before VPN consent. It occupies the Android VPN slot and may replace another VPN.

All activity payloads are AES-GCM encrypted with Android Keystore; timestamps are not encrypted. The database is in private internal storage, backup is disabled, and the UI disallows screenshots. Keys are not authentication-bound so the approved collector can run while the screen is locked after first unlock. This is a tradeoff, not protection from root or a compromised unlocked OS.

Independent app-usage and DNS controls are off by default. Public dataset bundled, no runtime update calls. No model in M0. Retention: at most 24 hours when the app/worker next runs, and 20,000 events; this is not a guaranteed wall-clock deletion schedule while Android suspends the app. Erase stops collectors, invalidates pending writes, deletes database and key and clears in-memory UI records. Historical OS UsageStats belongs to Android and is not erased by Noma; a new opt-in timestamp prevents reimport.

The DNS collector refuses active Private DNS, including opportunistic encryption, and stops if that state changes. It never silently downgrades encrypted DNS to UDP.

M4 privacy-request bodies, organisations and history use AES-GCM in a separate table under the same Keystore key, with table-specific associated data. Creation timestamps and IDs are plaintext. Requests remain until individual removal or erasing all Noma data; the 24-hour event policy does not apply. Limits and external-data boundaries are in [PRIVACY_REQUESTS.md](PRIVACY_REQUESTS.md). Copying to the system clipboard and opening an official website require explicit taps. No draft, telemetry or status is sent by Noma. Erasure cannot recall clipboard copies or requests independently sent to another organisation.
