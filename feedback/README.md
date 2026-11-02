# HazRob Tank — User Feedback (research only)

This directory is reserved for **user-authorized, original measurement-linked feedback** from HazRob Tank V8. Records use schema `hazrob.tank.v8.feedback/1`. A local feedback record is kept in the Android app's separate immutable SQLite outbox until successful authorized transfer; without a GitHub writer credential, it remains local/pending.

**Security:** A public GitHub repository can be read without a token but cannot be modified anonymously. Do not embed a GitHub write token in Android sources, APKs, repository files or public issues. A fine-grained GitHub token or GitHub App/OAuth authorization must be kept outside the APK. Write permissions must be limited to this repository. Confirm a real GitHub API success + returned repository path before marking SENT.

**Privacy and science:** Only records with per-submission explicit opt-in to public upload may leave the phone. The notes can contain identifying information; inform the operator that a public repository is visible to everyone. Fields include the user comment, immutable decision UID, snapshot SHA, original MQ sensor readings with units/quality, app and engine version, and original candidate ordering. They contain no invented ppm or G3 authorization. Do not use as training truth without research review, consent/rights checks, verified gas provenance and independent source G2/G3 validation.

Planned layout: `feedback/records/YYYY/MM/FB-<UUID>.json` uploaded through an authenticated write client. Idempotent filename + payload digest prevent duplicates. Neither software compilation nor an outbox record proves a remote upload.
