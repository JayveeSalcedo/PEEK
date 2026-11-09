# Device testing log

Record every run (spec §16). Do not paste message content, names, or screenshots of real
conversations here — use the in-app Diagnostics panel's structural output only.

## Device matrix

| Device | Purpose |
| --- | --- |
| Vivo V70 FE | Primary physical-device test |
| Samsung Galaxy | One UI compatibility |
| Google Pixel | Near-stock Android behavior |
| Android emulator | Repeatable development tests (use the *Sample preview*) |

## Run template

```
Date:
Device / OEM skin:
Android version:
PEEK version:
Messenger version:
Messenger notification settings (previews on/off, conversation muted?):
Notification access: granted / denied      Overlay: granted / denied
Diagnostics (structure only): messagingStyle= shortcut= locus= tag= messages= keys=
Outcome:
Notes:
```

## QA checklist (spec §15)

- [ ] Notification with one message
- [ ] Multiple messages from one sender
- [ ] Group conversation notification
- [ ] Several simultaneous conversations
- [ ] Notification with hidden content
- [ ] Notification without message text
- [ ] Conversation notification update
- [ ] Dismiss overlay without opening Messenger
- [ ] Revoke notification permission
- [ ] Revoke overlay permission
- [ ] Restart device and verify service recovery
- [ ] Test dark mode
- [ ] Test screen rotation and configuration changes
- [ ] Test on more than one Android manufacturer
- [ ] Verify no message content appears in logs (`adb logcat -s PEEK`) or analytics

**Release gate:** do not claim Messenger compatibility until a physical device passes
capture, extraction, grouping, overlay display and dismissal.
