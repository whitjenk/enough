package com.enough.app.feature.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.enough.app.EnoughApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles the "turn these off" action on the reminder notification.
 *
 * Turning reminders off has to be as easy as ignoring them, or the offer to
 * turn them off isn't sincere — one tap, from the notification itself, no trip
 * into Settings and no confirmation dialog asking the person to reconsider.
 */
class ReminderTurnOffReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as? EnoughApplication)?.container ?: return
        val pending = goAsync()
        NotificationManagerCompat.from(context).cancelAll()
        ReminderScheduler.cancel(context)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                container.userPreferencesRepository.setReminderEnabled(false)
            } finally {
                pending.finish()
            }
        }
    }
}
