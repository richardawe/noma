# M4 — Take Back My Data

Version 0.4.0 adds a local request workspace. It prepares access, deletion, objection, consent-withdrawal and personalised-advertising opt-out drafts. These are editable starting points, not legal advice or a guarantee that a particular right applies. The user checks the service, region, controller and identity requirements on the organisation's official route. Noma does not assert that an observed DNS name proves the organisation holds personal data.

## Public directory and sources

Entries are matched only by the exact owner name in the pinned M1 dataset. The full small directory is also available without observations, clearly labelled as public information. Reviewed 2026-09-23:

- **Google LLC:** [official privacy policy](https://policies.google.com/privacy), which links to export, deletion, contact and [My Ad Center](https://myadcenter.google.com/).
- **Facebook, Inc.:** the dataset retains this legacy name. The [official Meta privacy policy entry](https://www.facebook.com/privacy/policy/) redirected to login during review. Specific regional forms, DPO contact and available deletion mechanisms are therefore not asserted.
- **Amazon Technologies, Inc.:** [Amazon Ads' privacy-notice entry](https://advertising.amazon.com/en-gb/legal/privacy-notice). Users must check the applicable service and controller. A direct regional request/opt-out form is not bundled.

The directory stores source URL, review date, contact guidance, deletion/verification guidance and disabled automation support. Unverified regional forms and DPO email addresses remain unknown. GDPR/CCPA applicability is not inferred from device locale. The privacy-directory text is independently written; the M1 Tracker Radar licence still applies to ownership classifications. No runtime directory fetch or silent redirect validation is performed. Links may change; review them before expanding distribution.

## User-controlled workflow

1. Select a public directory entry or an organisation with an observed dataset match.
2. Choose a request type, edit the draft and save locally. Replace bracketed identity/scope fields before using it.
3. Open the official website or explicitly copy the draft to the system clipboard. Noma never attaches telemetry, opens an email composer, sends a message, submits a form or clicks a deletion control.
4. After independently submitting, record submission and a dated receipt/reference. Opening a site or copying text never changes status.
5. Record acknowledgement, required action, rejection or reported completion with a response reference. Completion is labelled **not verified by Noma**. A user-entered reference is not independent proof that data was deleted.

No stored row represents NOT_REQUESTED; that is the absence of a prepared request. PREPARED may advance only to SUBMITTED. Later status changes follow explicit transitions, retain earlier history and require a reference. Prepared wording is editable; recorded sent wording is preserved. No deadline reminders, automatic follow-ups or legal deadline calculations are included.

## Local data and erasure

Requests have a dedicated table in the existing private SQLite database. Body, organisation, type and status history use AES-GCM under the existing Android Keystore key, with `request:id:created` as associated data. IDs and creation timestamps remain plaintext, as do event timestamps. Request ciphertext cannot be replayed into event rows. A request-only database is treated as existing encrypted data; a missing key is not silently replaced.

Requests are retained until individually removed or **Erase my data** deletes all Noma data. They are not removed by the event timeline's 24-hour retention. Limits are 100 request records, 6,000 characters per draft, 100 updates per record and 1,000 characters per update reference. Stale epoch writes after erasure fail. Stale status-history edits cannot overwrite newer history. Removing local records does not withdraw anything submitted externally.

Copying is explicit and moves data to the Android system clipboard; on API 33+ it is marked sensitive. Opening a website involves that site and the browser under their own policies. Erasing Noma cannot erase a copied draft or a third party's records. No identity documents or passwords should be stored in request notes.

M0's DNS/reliability and hardware acceptance gaps remain open. M4 is implemented at the user's direction, not a claim that those earlier gates have passed.
