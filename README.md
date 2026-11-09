# PEEK

> See the conversation. Don't open the conversation.

PEEK is an Android app that turns supported messaging notifications into a floating
conversation preview, so you can read what arrived without opening the messaging app.

**Status:** Prototype 0.1 — Messenger only, in-app trigger only. See [`docs/SPEC-0.1.md`](docs/SPEC-0.1.md).

PEEK previews are built **only from notification content**. It does not read any app's
private database, so it cannot show history the notifications didn't contain.

## Build and run

Requirements: Android Studio (Narwhal or newer) or JDK 17+ with the Android SDK, platform 36.

```bash
./gradlew :app:assembleDebug        # build the APK
./gradlew :app:installDebug         # install on a connected device
./gradlew :app:testDebugUnitTest    # JVM unit tests (parser, grouping, retention)
./gradlew :app:connectedDebugAndroidTest   # Compose UI test on a device/emulator
```

| Setting | Value |
| --- | --- |
| Package | `com.peek.app` |
| Min SDK | 29 (Android 10) |
| Target / compile SDK | 36 — keep in line with Google Play's current requirement |
| Language / UI | Kotlin, Jetpack Compose, Material 3 with a custom PEEK theme |

## Trying it on a device

1. Open PEEK and tap **Allow in Settings** under *Notification access*, then enable PEEK.
   - On Android 13+, a sideloaded APK may show the switch greyed out. Open **App info → ⋮ →
     Allow restricted settings**, then try again (PEEK links to App info for this).
2. Tap **Allow in Settings** under *Display over other apps* and enable it.
3. Return to PEEK. Both cards should read **Allowed**, and notification access should say
   *Listening for Messenger notifications*.
4. Tap **Peek** under *Sample preview* to check the floating card works at all.
5. Have someone message you on Messenger. The conversation appears under *Captured
   conversations*. Do not open Messenger.
6. Tap **Peek in 5 s**, switch to any other app, and the preview appears on top of it.
7. Dismiss with ✕, a sideways swipe, or a tap outside the card. Messenger stays unopened.

The *Diagnostics* panel shows the **structure** of each Messenger notification received
(MessagingStyle present? shortcut ID? message count? extras key names) with no names or
text. Use it to record real Messenger structures (spec §18 step 7) and log results in
[`docs/DEVICE_TESTING.md`](docs/DEVICE_TESTING.md).

## Architecture

```
com.peek.app
├── PeekApplication / AppContainer   manual dependency container (one per process)
├── MainActivity                     onboarding + in-app test screen
├── core/
│   ├── notifications/   PeekNotificationService  ─ NotificationListenerService
│   │                    NotificationParser       ─ StatusBarNotification → NotificationSnapshot
│   │                    NotificationSnapshot     ─ Android-free copy, no PendingIntents
│   │                    NotificationAccessChecker
│   ├── overlay/         PeekOverlayController    ─ WindowManager TYPE_APPLICATION_OVERLAY
│   │                    OverlayLifecycleOwner, OverlayPermissionChecker
│   ├── triggers/        PreviewTrigger, InAppPreviewTrigger
│   ├── permissions/     PermissionStatus, PermissionSettings (Settings intents)
│   └── diagnostics/     NotificationDiagnostics (content-free), PeekLog
├── data/
│   ├── model/           PeekMessage, PeekConversation, SampleConversation
│   ├── local/           PeekPreferences (DataStore: settings only)
│   └── repository/      ConversationRepository, InMemoryConversationRepository
├── integrations/        MessagingAdapter, MessengerAdapter, ConversationIds
└── features/            onboarding/, preview/ (PeekPreviewCard), settings/
```

Data flow: Messenger notification → `PeekNotificationService` → `NotificationParser`
(snapshot) → `MessengerAdapter` (normalize) → `InMemoryConversationRepository` (group,
de-duplicate) → in-app trigger → `PeekOverlayController` → `PeekPreviewCard`.

### Decisions worth knowing

- **Adapters take a `NotificationSnapshot`, not a `StatusBarNotification`.** The spec's
  contract is illustrative; this split keeps every adapter unit-testable on the JVM and
  makes it structurally impossible for adapters to keep `PendingIntent`s or action tokens.
- **Conversation identity**: shortcut ID → locus ID → notification tag + ID, always prefixed
  by package. The app-level group key is not used because messaging apps often share one
  group key across all chats.
- **Message IDs are hashes**, so text never appears in identifiers that could reach logs.
- **No persistence.** Spec §12 says to avoid storing messages unless needed; the prototype
  doesn't need to, so the cache is in memory only (24 h retention, 50 messages per
  conversation, 30 conversations). Room can be added later if persistence becomes a
  requirement.
- **Bundle summaries** (e.g. "3 new messages") are ignored unless they contain structured
  messages; the child notifications carry the real content.
- **Overlay window** is `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_WATCH_OUTSIDE_TOUCH`:
  it never takes the keyboard or back button, and touches outside the card still reach the
  app underneath.
- **No AccessibilityService, no INTERNET permission, no analytics or ads SDKs.**

## Known limitations

- The initial scaffold was written in an environment without the Android SDK. The
  Android-free core (adapter, IDs, repository, diagnostics) was compiled and its unit tests
  pass on the JVM; the Android/Compose code has not been compiled yet, so the first
  `assembleDebug` may need small fixes.
- Messenger compatibility is **not yet verified on a physical device** (spec §15 release gate).
  The parser is written against Android's documented `MessagingStyle` format and must be
  checked against real captured structures.
- If the user has Messenger's message previews turned off, the notification text is a
  generic placeholder and PEEK will show that placeholder.
- Captured previews are lost when Android kills PEEK's process (by design: no storage).
- Image/sticker messages without text are skipped.
