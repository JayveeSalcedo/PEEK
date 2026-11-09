# PEEK 0.1 --- Technical Specification

**Product:** PEEK\
**Platform:** Android\
**Version:** Prototype 0.1\
**Status:** Engineering specification\
**Date:** November 2026

> **Product promise:** See the conversation. Don't open the
> conversation.

## 1. Prototype objective

Build a working Android application that can receive supported Messenger
notifications, group available messages into conversations, and display
a floating preview without opening Messenger.

The first prototype must prove this interaction before expanding to
other messaging applications or implementing monetization.

### Definition of done

-   Receive a Messenger notification after the user grants notification
    access.
-   Extract message content exposed by the notification.
-   Identify and group notifications by conversation where sufficient
    metadata is available.
-   Activate a PEEK preview without opening Messenger.
-   Display a floating preview over another application after the user
    grants overlay permission.
-   Dismiss the preview and return to the original screen.
-   Handle missing, hidden, or unsupported notification content
    gracefully.

**Important:** This is a notification-based preview, not access to
Messenger's private conversation database. PEEK cannot promise complete
historical chat contents.

## 2. Product scope

### Included in Prototype 0.1

-   Native Android application.
-   Notification access onboarding.
-   Overlay permission onboarding.
-   Notification listener service.
-   Initial Messenger notification parser.
-   Local conversation/message model.
-   Basic preview card.
-   In-app test trigger.
-   Basic dismissal behavior.
-   Device-level testing and diagnostic logging that excludes message
    content.

### Explicitly out of scope

-   Full support for every messaging application.
-   Historical message retrieval beyond data exposed by notifications.
-   Sending messages or interacting with conversations.
-   Accessibility-based long-press interception.
-   Cloud accounts or message synchronization.
-   Google Play Billing and Pro unlock.
-   Themes and advanced customization.
-   Advertising or analytics SDKs.

## 3. Recommended technology

  Area                      Choice
  ------------------------- --------------------------------------------------
  Language                  Kotlin
  UI                        Jetpack Compose
  Design system             Material 3 with a custom PEEK theme
  Minimum Android version   Android 10 / API 29 (proposed)
  Build system              Gradle Kotlin DSL
  Notification access       `NotificationListenerService`
  Floating preview          `WindowManager` application overlay
  Local database            Room, only if persistence is needed
  Preferences               DataStore
  Dependency injection      Manual initially
  Testing                   JUnit, Compose UI tests, physical devices
  Billing                   Google Play Billing Library in a later milestone

Use the target SDK required by Google Play at the time of release. Do
not hard-code an old target SDK into the production plan.

## 4. Permissions and user consent

  -------------------------------------------------------------------------------
  Capability              Android mechanism               Required for prototype
  ----------------------- ------------------------------- -----------------------
  Read exposed            Notification access /           Yes
  notification content    `NotificationListenerService`   

  Display floating        `SYSTEM_ALERT_WINDOW`           Yes
  preview                                                 

  Detect cross-app UI     `AccessibilityService`          No
  interactions                                            

  Internet access         `INTERNET`                      Not needed for the
                                                          local prototype
  -------------------------------------------------------------------------------

### Permission onboarding

Explain each permission before sending the user to Android Settings.

**Notification access explanation:** PEEK reads supported notification
content so it can display message previews. It does not access private
app databases.

**Overlay explanation:** PEEK uses a floating window to show a preview
above the current application.

Users must be able to disable either permission and continue using the
parts of the app that remain available.

Do not add AccessibilityService to the initial prototype. It is
policy-sensitive and is not a reliable universal way to intercept
another application's long-press gesture.

## 5. User experience flow

1.  User opens PEEK.
2.  PEEK explains notification access and overlay access.
3.  User grants the permissions in Android Settings.
4.  User returns to PEEK.
5.  A supported Messenger notification arrives.
6.  PEEK parses and stores the available message data locally.
7.  User activates the preview from the prototype's in-app test screen.
8.  PEEK displays a floating conversation card.
9.  User dismisses the card.
10. Messenger remains unopened unless the user independently chooses to
    open it.

For Prototype 0.1, use an in-app test trigger first. A Quick Settings
tile can follow after the notification and overlay pipeline works.

## 6. Architecture

``` text
PEEK Android Application
|
+-- core/
|   +-- notifications/
|   |   +-- PeekNotificationService
|   |   +-- NotificationParser
|   |   +-- NotificationAccessChecker
|   |
|   +-- overlay/
|   |   +-- PeekOverlayController
|   |   +-- OverlayPermissionChecker
|   |
|   +-- triggers/
|   |   +-- PreviewTrigger
|   |   +-- InAppPreviewTrigger
|   |
|   +-- permissions/
|
+-- data/
|   +-- model/
|   +-- local/
|   +-- repository/
|
+-- integrations/
|   +-- MessagingAdapter
|   +-- MessengerAdapter
|
+-- features/
|   +-- onboarding/
|   +-- preview/
|   +-- settings/
|
+-- tests/
```

### Main data flow

``` text
Messenger notification
        |
        v
PeekNotificationService
        |
        v
MessengerAdapter / NotificationParser
        |
        v
Normalized conversation model
        |
        v
Local repository
        |
        v
User activates preview
        |
        v
PeekOverlayController
        |
        v
Floating Compose preview card
```

Keep notification parsing, conversation grouping, and overlay
presentation separate. This will make it easier to add other messaging
applications later.

## 7. Data models

Initial Kotlin models:

``` kotlin
data class PeekMessage(
    val id: String,
    val conversationId: String,
    val packageName: String,
    val sender: String?,
    val text: String,
    val timestamp: Long,
    val isGroupConversation: Boolean
)

data class PeekConversation(
    val id: String,
    val packageName: String,
    val title: String,
    val messages: List<PeekMessage>,
    val updatedAt: Long
)
```

The conversation identifier must distinguish conversations within the
same application. Prefer available notification shortcut IDs,
conversation identifiers, grouping keys, and other stable metadata. Do
not use the package name alone as the conversation ID.

Avoid persisting notification action tokens or `PendingIntent` objects.

### Messaging adapter contract

``` kotlin
interface MessagingAdapter {
    fun supports(packageName: String): Boolean

    fun parse(
        notification: StatusBarNotification
    ): PeekConversation?
}
```

Future adapters may include:

``` text
MessagingAdapter
    +-- MessengerAdapter
    +-- WhatsAppAdapter
    +-- InstagramAdapter
    +-- TelegramAdapter
    +-- DiscordAdapter
```

Each adapter must be tested against real notification examples.
Notification formats can vary by app version, Android version, language,
and notification settings.

## 8. Notification listener

Create `PeekNotificationService.kt` as a subclass of
`NotificationListenerService`.

Architectural starter:

``` kotlin
class PeekNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {
        val adapter = adapters.firstOrNull {
            it.supports(sbn.packageName)
        } ?: return

        val conversation = adapter.parse(sbn)
            ?: return

        repository.upsert(conversation)
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification
    ) {
        // Update notification state if needed.
        // Do not automatically erase conversation history.
    }
}
```

This is illustrative starter code. `adapters` and `repository` must be
provided through the app's chosen dependency/lifecycle design before it
will compile.

### Parsing requirements

The parser should:

1.  Inspect notification extras.
2.  Detect structured messaging notifications, including
    `Notification.MessagingStyle` data where available.
3.  Extract sender, text, timestamp, and conversation metadata when
    exposed.
4.  Fall back to ordinary notification title and text only when
    appropriate.
5.  Handle bundled/grouped notifications.
6.  Reject unsupported or empty notifications.
7.  Normalize the result into the PEEK data model.
8.  Avoid logging message content.

Do not assume every messaging application uses `MessagingStyle` or
exposes all message fields.

## 9. Floating overlay

Use `WindowManager` with `TYPE_APPLICATION_OVERLAY` after the user
grants overlay permission.

Illustrative starter:

``` kotlin
class PeekOverlayController(
    private val context: Context
) {
    private val windowManager =
        context.getSystemService(WindowManager::class.java)

    private var overlayView: ComposeView? = null

    fun show(conversation: PeekConversation) {
        if (!Settings.canDrawOverlays(context)) return

        dismiss()

        val view = ComposeView(context).apply {
            setContent {
                PeekPreviewCard(
                    conversation = conversation,
                    onDismiss = { dismiss() }
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(view, params)
        overlayView = view
    }

    fun dismiss() {
        overlayView?.let {
            windowManager.removeView(it)
        }
        overlayView = null
    }
}
```

This is a skeleton, not production-ready code. Add lifecycle cleanup,
configuration-change handling, safe window removal, error handling,
touch behavior, and accessibility semantics.

The overlay must not unexpectedly steal focus or block the user's normal
interaction with the underlying app.

## 10. Preview UI

Initial Compose component:

``` kotlin
@Composable
fun PeekPreviewCard(
    conversation: PeekConversation,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .padding(12.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    conversation.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close preview"
                    )
                }
            }

            HorizontalDivider()

            conversation.messages.forEach { message ->
                Text(
                    message.sender ?: "Unknown",
                    fontWeight = FontWeight.Medium
                )
                Text(message.text)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
```

The final UI should include:

-   Application identity.
-   Conversation title or sender.
-   Available messages and timestamps.
-   Clear visual distinction between senders.
-   Scrollable message area when necessary.
-   Close and dismissal gestures.
-   Optional action to open the original conversation, only if safely
    supported.

Never imply that the preview contains the complete conversation history
when it only contains notification data.

## 11. Manifest starter

``` xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission
        android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <application
        android:label="PEEK"
        android:theme="@style/Theme.PEEK">

        <service
            android:name=".core.notifications.PeekNotificationService"
            android:label="PEEK Notification Access"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:exported="true">

            <intent-filter>
                <action android:name=
                    "android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>

    </application>
</manifest>
```

This is a starting manifest fragment. Confirm the actual package paths,
theme, exported-component requirements, and target SDK behavior in the
project.

## 12. Privacy and security requirements

PEEK handles sensitive notification content. Privacy must be part of the
architecture.

-   Process messages locally.
-   Do not transmit message content to a server.
-   Do not include message text in crash reports or analytics.
-   Avoid storing messages unless persistence is required.
-   If cached messages are stored, define a short retention policy and
    deletion controls.
-   Clear cached preview data when the user disables PEEK.
-   Exclude sensitive notification categories where identifiable.
-   Do not store notification action tokens.
-   Explain permission use before requesting access.
-   Provide a clear way to revoke or disable PEEK's functionality.

Product privacy statement, subject to implementation verification:

> Your messages stay on your device. PEEK uses supported notification
> content to create previews and does not upload message content to its
> servers.

Do not publish this claim until the implementation and all included SDKs
have been audited.

## 13. Trigger strategy

  -----------------------------------------------------------------------
  Trigger                 Priority                Notes
  ----------------------- ----------------------- -----------------------
  In-app test button      P0                      First implementation

  Quick Settings tile     P1                      Useful cross-app
                                                  activation

  Floating handle         P2                      Requires careful
                                                  background and
                                                  lifecycle design

  Accessibility-based     Experimental            Optional and
  gesture                                         policy-sensitive

  Universal long-press    Not guaranteed          Do not promise this
  interception                                    capability
  -----------------------------------------------------------------------

The core product should remain useful without AccessibilityService.

## 14. Development milestones

  -----------------------------------------------------------------------
  Milestone                           Work
  ----------------------------------- -----------------------------------
  1\. Environment                     Create native Kotlin/Compose
                                      project

  2\. Permissions                     Implement notification and overlay
                                      permission onboarding

  3\. Notification capture            Receive and inspect Messenger
                                      notifications

  4\. Parsing                         Normalize available messages and
                                      conversation metadata

  5\. Local repository                Group and retrieve conversations

  6\. Overlay                         Display and dismiss floating
                                      preview

  7\. Integration test                Validate the entire flow on a
                                      physical device

  8\. Trigger improvement             Add Quick Settings tile if the core
                                      flow is stable
  -----------------------------------------------------------------------

A broader public-ready release may take approximately 6--8 weeks for one
developer, but the initial proof of concept should be attempted first.
Platform restrictions or notification-format limitations may change the
schedule.

## 15. QA checklist

-   [ ] Notification with one message.
-   [ ] Multiple messages from one sender.
-   [ ] Group conversation notification.
-   [ ] Several simultaneous conversations.
-   [ ] Notification with hidden content.
-   [ ] Notification without message text.
-   [ ] Conversation notification update.
-   [ ] Dismiss overlay without opening Messenger.
-   [ ] Revoke notification permission.
-   [ ] Revoke overlay permission.
-   [ ] Restart device and verify service recovery.
-   [ ] Test dark mode.
-   [ ] Test screen rotation and configuration changes.
-   [ ] Test on more than one Android manufacturer.
-   [ ] Verify no message content appears in logs or analytics.

### Release gate

Do not claim Messenger compatibility until a physical device passes
notification capture, message extraction, conversation grouping, overlay
display, and dismissal tests.

## 16. Initial device matrix

  Device             Purpose
  ------------------ ------------------------------
  Vivo V70 FE        Primary physical-device test
  Samsung Galaxy     One UI compatibility
  Google Pixel       Near-stock Android behavior
  Android emulator   Repeatable development tests

Record Android version, app version, notification settings, permission
state, and test outcome for every test.

## 17. Known technical risks

  -----------------------------------------------------------------------
  Risk                    Severity                Mitigation
  ----------------------- ----------------------- -----------------------
  Notification formats    High                    Separate per-app
  differ by app                                   adapters

  Historical messages     High                    Clearly communicate
  unavailable                                     notification-based
                                                  limitations

  Long-press cannot be    High                    Alternative trigger
  intercepted universally                         system

  Accessibility policy    High                    Do not require
  restrictions                                    AccessibilityService

  OEM background          Medium                  Test multiple
  restrictions                                    manufacturers

  Sensitive permission    High                    Transparent onboarding
  concerns                                        and local processing

  Android updates break   Medium                  Compatibility
  integrations                                    regression tests
  -----------------------------------------------------------------------

## 18. Immediate implementation order

1.  Create the Android Studio project named `PEEK`.
2.  Use package name `com.peek.app`.
3.  Select Kotlin and Jetpack Compose.
4.  Set minimum SDK to API 29.
5.  Implement permission-status checks and onboarding.
6.  Implement `PeekNotificationService`.
7.  Capture and inspect real Messenger notification structures.
8.  Implement the Messenger adapter and local conversation repository.
9.  Implement `PeekOverlayController`.
10. Implement `PeekPreviewCard`.
11. Connect the in-app test trigger.
12. Run the complete flow on a physical Android device.

**First milestone:** PEEK can preview one real Messenger conversation
without opening Messenger.

## 19. Future work after Prototype 0.1

Only after the first integration works:

-   Add WhatsApp, Instagram, Telegram, and Discord adapters.
-   Add Quick Settings activation.
-   Evaluate floating-handle activation.
-   Refine the visual design and animations.
-   Add privacy customization.
-   Evaluate AccessibilityService only if a genuinely necessary,
    policy-compliant feature remains.
-   Implement Google Play Billing for a permanent Pro unlock.
-   Conduct Play policy and privacy review.
-   Prepare store listing and release testing.

## 20. Product and monetization direction

  -----------------------------------------------------------------------
  Decision                            Direction
  ----------------------------------- -----------------------------------
  Product                             PEEK

  Platform                            Android

  Audience                            Global Android users

  Core experience                     iOS-inspired conversation previews

  Integrations                        Multiple messaging applications,
                                      individually validated

  Architecture                        Local-first

  Monetization                        Free download + permanent Pro
                                      unlock

  Proposed Pro price                  ₱149 one-time, subject to
                                      validation

  Subscription                        None

  Advertising                         None in the core product
  -----------------------------------------------------------------------

The initial price is a hypothesis, not validated willingness to pay.
Billing is deliberately excluded from Prototype 0.1.

------------------------------------------------------------------------

## Official Android and Google Play references

Verify current documentation before implementation and again before
release:

-   [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
-   [Notification.MessagingStyle](https://developer.android.com/reference/android/app/Notification.MessagingStyle)
-   [Notification access and listener
    guide](https://developer.android.com/develop/ui/views/notifications/notification-listener)
-   [SYSTEM_ALERT_WINDOW](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)
-   [WindowManager.LayoutParams](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)
-   [Accessibility API
    policy](https://support.google.com/googleplay/android-developer/answer/10964491)
-   [Google Play
    Billing](https://developer.android.com/google/play/billing)

**Engineering note:** Code snippets in this document are architectural
starting points. They are not a complete, compiled Android Studio
project. Validate APIs, imports, lifecycle behavior, permissions, and
current Play requirements during implementation.
