# Your Day in 3D

A separate screen linked from Your Day. Existing collection, summary, playback and request workflows remain native Compose. Version 0.4.2 adds an offline React island in a WebView; there is no hosted site or remote scene API.

## Data and visual meaning

`MainActivity` projects its selected `DailySummary` and current collector state through `sceneSnapshot`. The page receives a read-only snapshot on the existing refresh cycle (normally ten seconds while the activity is started). `scene/src/frame.ts` derives plain scene numbers with no Three.js imports.

| Element | Source | Transform / limit |
| --- | --- | --- |
| App tiles | `Event.kind == APP_RESUMED`, `Event.value` | Distinct names; eight tiles plus labelled overflow |
| Organisation cabinets | Exact `Classification.organisation` matches | Eight cabinets plus labelled overflow |
| Unknown cabinet | DNS events without an exact registry match | One neutral group, not a danger indicator |
| Fill | Number of represented `Event.id` values | Count / 10, clamped 0–6; exact count remains visible |
| Timeline | `Event.time` | Occupied 15-minute bins; cumulative replay through selection |
| Status | `error`, `dns`, `usage`, `status`, `partialRetention` | Good / neutral / warning / failing; current collector condition, not privacy or historical health |
| Detail and recent records | Original IDs, times, values and sources | Open the existing native evidence inspector |

App and DNS shelves never connect. The scene does not infer originating apps, confirmed visits, data transfer, privacy risk, deletion success or rates. App pauses appear in the record list and chapters, but do not create resume tiles. There are no simulated vehicles, delta pills or comparison state. Empty dates stay empty. Errors remove records; expired or erased evidence removes objects and invalid selections.

## Rendering and accessibility

React 19.2, React Three Fiber 9.8.1, drei 10.7.9, Three.js 0.180.0. The entry dynamically loads the page, which lazy-loads the renderer. Rounded procedural geometry, an in-code light environment, warm shadows and DOM cards require no external images, fonts or HDR files. Source assets and pinned npm lockfile are committed; Gradle packages the generated assets without requiring Node for a normal Android build.

A driver damps fill values at 2.2 per second before cabinet frames run. Rendering is on demand and stops when interpolation/camera motion settles. Reduced motion snaps to current values; there is no flashing or automatic timeline playback. DPR is capped at 1.5, shadows at 1024 and environment at 128 for mobile use. Cards stack below the canvas on narrow screens. Text buttons expose every object and evidence record independently of WebGL. Missing WebGL, context loss or render failure leaves the data interface available.

The WebView only serves bundled `https://noma.local/archive/` resources. Network loads are disabled. All other resource requests and navigations are blocked, except a validated evidence ID handed back to the native inspector. No JavaScript interface, file/content access, remote requests, web storage or new collection permission is added. Snapshot strings are JSON-quoted before evaluation. The WebView is paused in the background and destroyed on exit; the Activity retains FLAG_SECURE. Privacy-request text and keys never cross into JavaScript.

## Build and check

```sh
cd scene
npm ci
npm test
npm run build
npm run notices
npm run size
npm run test:browser
```

Then build Android from the repository root using the normal Gradle command. `npm run build` replaces `app/src/main/assets/evidence-scene`; run it whenever scene source changes. `SCENE_NOTICES.txt` includes production dependency licences.

Browser checks use the production build and explicitly labelled fixtures, never the tablet's personal evidence. They cover replay, details, live snapshots, failing/empty states, removal after erasure, narrow layout, dark mode, reduced motion and no-WebGL evidence navigation. Screenshots live in [screenshots](screenshots/). No comparison screenshot exists because the application has no truthful comparison baseline.

Physical tablet rendering/performance and a complete accessibility audit still require device acceptance. Screenshot examples do not establish battery cost.

## Measured checks (2026-10-06)

- Six frame tests pass, including a 20,000-record input, scale bounds, deduplication, retained times, unknown owners, overflow, replay, stale-data removal and all four reachable health states.
- Two Chromium/SwiftShader production browser flows pass; no page errors or external resource requests. Screenshots include desktop, pinned details, failure, phone and dark mode. Narrow view has no horizontal page overflow.
- The isolated entry is 925 bytes gzip versus no JavaScript entry in the existing native app. Total generated scene assets are about 338 KB gzip / 1.2 MB uncompressed. This is an APK addition, not a promise that a native bundle grows by only 1 KB.
- Android build, test APK, all 21 existing JVM tests and lint pass. Lint has zero errors and 17 warnings, including existing style and updated-dependency suggestions.

- All 20 Android regression tests pass on the API 35 emulator in 273.631 seconds, including the native snapshot/inspector bridge, the previously failing playback check, collection and storage checks. A final mobile-only caption styling adjustment was then browser-tested and repackaged.
