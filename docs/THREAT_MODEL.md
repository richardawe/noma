# Threat model

| Threat | Mitigation | Residual risk |
|---|---|---|
| Extracted database / stolen locked phone | Keystore AES-GCM, app sandbox, backups disabled | Timestamps and volume visible; unlocked/rooted OS can access decrypted data |
| Malicious app | Service requires BIND_VPN_SERVICE; internal worker; no exports/reports | Compromised OS bypasses isolation |
| Logs/crash reports | No analytics or event logging | OS/system diagnostics remain outside app control |
| Forged DNS packet | Native TCP/IP stack, strict SOCKS framing and single-question checks; connected protected sockets | DNS itself is unauthenticated; network observer can spoof DNS |
| False ownership | Label-boundary matching, exact source and version, Unknown by default | Public dataset may be stale or wrong; ownership is a classification |
| Wrong app attribution | No DNS-to-app ownership claim; timeline coincidence only | Full flow attribution not implemented |
| Corrupt registry | Digest check and schema validation; fail closed to Unknown | A compromised signed build can replace both |
| Erasure race | Shared lock, epoch fence, stop collection, delete key and DB | Flash remanence; cryptographic erasure assumes key deletion works |
| Resource exhaustion | Bounded native/relay sessions, authenticated loopback, timeouts, packet size, event count and retention | Burst traffic can exceed capacity and flows can time out |
| Prompt leakage | M2–M3 use deterministic evidence summaries; no model provider calls | Must be revisited before adding inference |
| False deletion claim | M4 completion is user-reported and requires a response reference; never marked verified | References are not independently authenticated |
| Wrong request recipient | Exact owner-to-public-directory mapping; official HTTPS links only; unknown contact details not invented | Correct legal controller, region and service still require user review; links may change |
| Request leakage | Encrypted body/history, private storage, secure UI; copying and browser opening are explicit | System clipboard and destination browser/site are outside Noma's erasure boundary |
| Request-history overwrite | Append-only status validation, table-specific AES-GCM AAD and erasure epoch fencing | Rooted/compromised OS remains outside protection |

Release gates: physical device VPN compatibility, independent security review, battery measurement, reboot/revoke/erase tests, dataset redistribution terms. Do not claim production readiness.

The VPN is bypassable and IPv6 remains outside observation. Apps can explicitly select another network. Noma does not promise traffic confinement, blocking, anonymity or complete observation.
