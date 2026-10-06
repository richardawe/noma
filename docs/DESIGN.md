# Your Day redesign

Warm cream, pine green, apricot actions, serif headings and local vector art give the app a journal style. The path illustration is decorative; it does not represent unobserved activity.

Four persistent bottom destinations keep the main tasks visible: Your Day, Timeline, Organisations and Take Back. Controls are separate from the evidence timeline and reachable from the header. This follows [Android's primary navigation guidance](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns).

Your Day makes playback the main action when records exist. Empty dates offer collection setup instead of an invented story. Date filters and original-evidence links remain available. Closing playback returns to the top of Your Day.

Technical limits stay in Controls, consent and evidence explanations. Counts remain factual. Privacy requests have a three-step introduction and clearly unverified manual status history.

DesignReviewTest renders six screens with disposable test records. It keeps production FLAG_SECURE enabled and draws only its test activity. No real user screenshots are exported. Navigation, offline playback, evidence and request editing are tested; a complete accessibility audit remains separate.
