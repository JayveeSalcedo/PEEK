# PEEK privacy implementation notes (Prototype 0.1)

How the spec §12 requirements are met in code. Re-audit before publishing any privacy claim.

| Requirement | Implementation |
| --- | --- |
| Process messages locally | Parsing and storage run in-process. The manifest has no `INTERNET` permission, so the app cannot open network sockets. |
| No message content to servers / crash reports / analytics | No networking, analytics, crash-reporting or ads SDKs are included. |
| Avoid storing messages | `InMemoryConversationRepository` keeps previews in process memory only. DataStore holds only the *PEEK enabled* setting. |
| Short retention + deletion controls | Entries expire after 24 h; at most 50 messages × 30 conversations. *Clear cached previews* deletes everything immediately. |
| Clear cache when PEEK is disabled | Turning *PEEK enabled* off clears the cache, diagnostics and any visible preview, and stops capture. |
| Exclude sensitive categories | `MessengerAdapter.EXCLUDED_CATEGORIES` rejects calls, progress, system, transport, alarms, etc. Ongoing notifications are rejected. |
| No action tokens | `NotificationSnapshot` has no field that can hold a `PendingIntent`, action or `RemoteInput`. |
| No message content in logs | `PeekLog` is the only logger; it receives structural summaries only (`NotificationDiagnostic.toLogLine()`), logs exception *types* not messages, and debug lines are compiled out of release builds. Message IDs are SHA-256 hashes. |
| No backups | `allowBackup="false"`, `fullBackupContent="false"`, and data-extraction rules exclude all domains. |
| Explain before requesting | Each permission card explains its purpose before linking to Settings. |
| Revocable | Both permission cards stay visible with *Manage in Settings*; the app adapts when either is revoked. |

Only notifications from supported packages (currently `com.facebook.orca`) are inspected;
notifications from other apps return immediately without being read.
