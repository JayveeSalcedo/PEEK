package com.peek.app.core.notifications

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person

/**
 * Copies the useful, non-actionable parts of a [StatusBarNotification] into a
 * [NotificationSnapshot].
 *
 * - Reads `MessagingStyle` data where present (via AndroidX, which understands both the
 *   platform and compat encodings, including historic messages).
 * - Always reads the plain title/text extras so adapters can fall back when appropriate.
 * - Never copies `PendingIntent`s, actions, remote inputs or bitmaps.
 */
object NotificationParser {

    fun snapshot(sbn: StatusBarNotification): NotificationSnapshot {
        val notification = sbn.notification
        val extras: Bundle = notification.extras ?: Bundle.EMPTY

        val style = runCatching {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        }.getOrNull()

        val selfPerson: Person? = style?.user
        val messages = style?.let { messagingStyle ->
            (messagingStyle.historicMessages + messagingStyle.messages).map { message ->
                val person = message.person
                SnapshotMessage(
                    sender = person?.name?.toString(),
                    senderKey = person?.key,
                    text = message.text?.toString(),
                    timestamp = message.timestamp,
                    isFromSelf = person == null || isSamePerson(person, selfPerson),
                )
            }
        }.orEmpty()

        return NotificationSnapshot(
            packageName = sbn.packageName,
            postTime = sbn.postTime,
            notificationId = sbn.id,
            tag = sbn.tag,
            shortcutId = notification.shortcutId,
            locusId = notification.locusId?.id,
            groupKey = notification.group,
            category = notification.category,
            isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            isOngoing = sbn.isOngoing,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            hasMessagingStyle = style != null,
            conversationTitle = style?.conversationTitle?.toString(),
            isGroupConversation = style?.let {
                if (extras.containsKey(Notification.EXTRA_IS_GROUP_CONVERSATION)) {
                    extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)
                } else {
                    null
                }
            },
            selfDisplayName = selfPerson?.name?.toString(),
            messages = messages,
            extrasKeys = extras.keySet().toList(),
        )
    }

    private fun isSamePerson(person: Person, self: Person?): Boolean {
        if (self == null) return false
        val key = person.key
        val selfKey = self.key
        if (key != null && selfKey != null) return key == selfKey
        val name = person.name?.toString()
        return name != null && name == self.name?.toString()
    }
}
