package com.enough.app.feature.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.enough.app.MainActivity
import com.enough.app.R

/**
 * Builds and posts the daily reminder notification (SPEC §7.8).
 *
 * Deliberately low-key: `PRIORITY_LOW`, no sound, no vibration, auto-cancelling.
 * A reminder from this app is a note left on the counter, not a tap on the
 * shoulder. It always carries a one-tap "turn these off" action, so leaving
 * costs nothing and needs no trip through Settings.
 */
object ReminderNotifier {

    private const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_OPEN = 0
    private const val REQUEST_TURN_OFF = 1

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    /** True when the OS will actually let us post — checked before every fire. */
    fun canPost(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Post the reminder. Returns false when the OS refused (permission revoked
     * between scheduling and firing, notifications disabled at the channel
     * level) so the caller can skip counting it as something the person ignored.
     */
    fun post(context: Context, body: String): Boolean {
        if (!canPost(context)) return false
        ensureChannel(context)

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags,
        )
        val turnOff = PendingIntent.getBroadcast(
            context,
            REQUEST_TURN_OFF,
            Intent(context, ReminderTurnOffReceiver::class.java),
            flags,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.reminder_action_turn_off), turnOff)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            // Permission can be revoked between the check above and the post.
            // Treat it as "the OS said no", never as the person ignoring us.
            false
        }
    }
}
