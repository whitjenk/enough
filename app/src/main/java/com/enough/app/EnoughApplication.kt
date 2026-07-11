package com.enough.app

import android.app.Application
import android.util.Log
import com.enough.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point. Owns the [AppContainer] (Room DB, repositories) and
 * kicks off one-time food-list seeding off the main thread on first launch.
 */
class EnoughApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            try {
                container.seedIfNeeded()
            } catch (e: Exception) {
                // Don't crash launch on a seeding failure. The food table stays
                // empty, so the next launch retries; the add-meal screen also
                // surfaces an explicit empty state rather than failing silently.
                Log.e(TAG, "Food-list seeding failed", e)
            }
        }
    }

    private companion object {
        const val TAG = "EnoughApplication"
    }
}
