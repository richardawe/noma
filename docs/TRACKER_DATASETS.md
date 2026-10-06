# Tracker intelligence

M0 bundles a small derivative of [DuckDuckGo Tracker Radar](https://github.com/duckduckgo/tracker-radar) pinned to `06c65bd1e7f214bca9553fb2430d2eae040276b2`. The importer selects four domain records and expands only upstream-listed subdomains. No arbitrary suffix matching and no manual ownership guesses. Unknown coverage is expected.

Every record carries source URL and revision. Registry source-file hashes, derived JSON digest and upstream license are bundled. Corrupt/missing registry fails closed to Unknown. Upstream organisation names are preserved, even if dated; the app does not silently rename an entity. Categories describe the registry entry, not the observed purpose or data contents of a request.

**License gate:** Tracker Radar is CC BY-NC-SA 4.0. This bundle is for noncommercial evaluation; commercial distribution requires permission or replacement. The transformed registry keeps that license, credits Duck Duck Go, Inc., and identifies the transformation. Source code has no dependency on this dataset's format beyond the small import output schema.

Alternatives to evaluate before release: [Disconnect tracking protection](https://github.com/disconnectme/disconnect-tracking-protection) (check its current redistribution terms), [Exodus trackers](https://github.com/Exodus-Privacy/etip) (signatures often describe SDK presence rather than observed connections). Neither is assumed safe to relicense. A blocking list is not a complete ownership database. Do not equate CDN, authentication, crash reporting and all third parties with tracking.

Updates are developer-triggered via `tools/import_registry.py`, not a runtime network call. Review dataset changes and licensing before changing the pin. Add signed update manifests and rollback protection only when runtime updates are needed.

`googleapis.com` has no US record at this pin and was excluded after HTTP 404. Its names remain Unknown; no substitute mapping was invented.
