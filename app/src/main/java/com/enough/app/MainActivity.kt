package com.enough.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.enough.app.feature.main.EnoughApp
import com.enough.app.feature.reminder.ReminderScheduler
import com.enough.app.ui.theme.EnoughTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EnoughTheme {
                EnoughApp()
            }
        }
    }

    /**
     * Record that the person actually opened the app, which clears the reminder
     * back-off (SPEC §7.8).
     *
     * This deliberately lives on the Activity, **not** in
     * [EnoughApplication.onCreate]: WorkManager starts the same process to run
     * [com.enough.app.feature.reminder.ReminderWorker], so an application-level
     * hook would count the reminder firing as the person showing up and the
     * back-off would never engage at all.
     */
    override fun onStart() {
        super.onStart()
        val container = (application as EnoughApplication).container
        lifecycleScope.launch {
            container.userPreferencesRepository.recordAppOpened(LocalDate.now().toEpochDay())
            // Re-arm on launch: cheap insurance if a pending request was lost to
            // a force-stop or a "clear data" on the WorkManager database.
            val prefs = container.userPreferencesRepository
            if (prefs.reminderEnabled.first()) {
                ReminderScheduler.schedule(
                    this@MainActivity,
                    prefs.reminderMinuteOfDay.first(),
                    ZonedDateTime.now(),
                )
            }
        }
    }
}
