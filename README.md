# PEEK

> See the conversation. Don't open the conversation.

PEEK is an Android app that turns supported messaging notifications into a floating
conversation preview, so you can read what arrived without opening the messaging app.

**Status:** Prototype 0.1 — Messenger only. Working on a physical device. See [`docs/SPEC-0.1.md`](docs/SPEC-0.1.md).

## Ways to peek

| Trigger | How | Build |
| --- | --- | --- |
| In-app | Tap (or press and hold) a chat in PEEK; **5s** delays it so you can switch apps | Standard |
| Home screen icon | Long-press PEEK's icon → recent chats → tap one | Standard |
| Quick Settings tile | Pull down the shade → **PEEK** (latest chat) | Standard |
| Edge handle | Swipe the slim tab on the right edge inward, or tap it; drag to move | Standard |
| Long-press in Messenger | Press and hold a chat row inside Messenger | **Lab only** |

Android does not let one app intercept long-presses inside another, so the iOS-style
"long-press the real chat" gesture is only possible with an AccessibilityService. Google Play
does not allow Accessibility for this purpose, so it lives in a separate **PEEK Lab** build
(`com.peek.app.lab`) for research: it measures whether Messenger's long-presses can be detected
and matched to a captured chat. The standard build never contains it.

## Design

The UI follows Apple's Human Interface Guidelines rather than Material defaults: semantic iOS
colors (grouped backgrounds, layered labels, hairline separators, one indigo tint), the Dynamic
Type scale, inset grouped lists with gray press highlights and green switches, and monogram
avatars. The preview behaves like an iOS context-menu peek — the screen behind blurs and dims
(Android 12+ where supported), the card springs in with a haptic tick, messages render as
iMessage-style bubbles, and an action menu sits beneath it. The preview is modal: tapping
outside closes it instead of reaching the app underneath, so a stray tap can never open the
chat and mark it read.

## Install on a phone

Download from the rolling pre-release (rebuilt on every push):
`https://github.com/JayveeSalcedo/PEEK/releases/download/latest-debug/peek-debug.apk`
(Lab: `peek-lab-debug.apk`).

Google Play Protect blocks sideloaded apps that request notification access in some regions.
To install a test build: Play Store → profile → Play Protect → ⚙ → turn off *Scan apps with
Play Protect*, install, then turn it back on. `adb install` and Play Console test tracks are
not affected.

PEEK previews are built **only from notification content**. It does not read any app's
private database, so it cannot show history the notifications didn't contain.

## Build and run

Requirements: Android Studio (Narwhal or newer) or JDK 17+ with the Android SDK, platform 36.

```bash
./gradlew :app:assembleStandardDebug          # standard APK
./gradlew :app:assembleLabDebug               # PEEK Lab APK (Accessibility experiment)
./gradlew :app:installStandardDebug           # install on a connected device
./gradlew :app:testStandardDebugUnitTest      # JVM unit tests
./gradlew :app:connectedStandardDebugAndroidTest   # Compose UI test on a device/emulator
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
4. Tap **Sample preview** to check the preview works at all.
5. Have someone message you on Messenger. The chat appears under *Conversations*. Do not
   open Messenger.
6. Tap the chat's **5s**, switch to any other app, and the preview appears on top of it.
7. Close it with **Close**, Back, or a tap outside the card. Messenger stays unopened.
8. Try the other triggers under *Ways to peek*.

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
│   ├── shortcuts/       ConversationShortcuts (launcher), PeekLauncherActivity (trampoline)
│   ├── notifications/   PeekNotificationService  ─ NotificationListenerService
│   │                    NotificationParser       ─ StatusBarNotification → NotificationSnapshot
│   │                    NotificationSnapshot     ─ Android-free copy, no PendingIntents
│   │                    NotificationAccessChecker
│   ├── overlay/         PeekOverlayController    ─ full-screen modal peek window
│   │                    EdgeHandleController     ─ edge-handle trigger
│   │                    OverlayLifecycleOwner, OverlayPermissionChecker
│   ├── triggers/        PreviewTrigger, InAppPreviewTrigger, PeekTileService
│   ├── permissions/     PermissionStatus, PermissionSettings (Settings intents)
│   └── diagnostics/     NotificationDiagnostics (content-free), PeekLog
├── data/
│   ├── model/           PeekMessage, PeekConversation, SampleConversation
│   ├── local/           PeekPreferences (DataStore: settings only)
│   └── repository/      ConversationRepository, InMemoryConversationRepository
├── integrations/        MessagingAdapter, MessengerAdapter, ConversationIds, ChatTitleMatcher
├── features/            home/ (HomeScreen), preview/ (PeekPreview, PeekOverlayContent)
└── ui/                  theme/ (iOS colors, type scale), components/ (grouped lists)

app/src/lab/             PEEK Lab only: PeekAccessibilityService + its config
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
- **The preview window is modal** (full screen, takes Back) because it only appears when the
  user asks for it, and letting a tap fall through to Messenger would defeat the product.
  The **edge handle** is the opposite: a tiny non-focusable, non-modal window.
- **Launcher shortcuts follow the cache**: they show chat names on the home screen, so they
  are cleared whenever the in-memory cache is (process start, *Clear*, PEEK off).
- **Standard build: no AccessibilityService, no INTERNET permission, no analytics or ads SDKs.**

## Known limitations

- "Open Messenger" opens Messenger's main screen, not the specific chat (PEEK keeps no
  notification PendingIntents, by design).
- The Lab long-press relies on Messenger reporting long-press accessibility events and
  exposing chat names on its rows; both can change with any Messenger update.
- Messenger compatibility is **not yet verified on a physical device** (spec §15 release gate).
  The parser is written against Android's documented `MessagingStyle` format and must be
  checked against real captured structures.
- If the user has Messenger's message previews turned off, the notification text is a
  generic placeholder and PEEK will show that placeholder.
- Captured previews are lost when Android kills PEEK's process (by design: no storage).
- Image/sticker messages without text are skipped.
