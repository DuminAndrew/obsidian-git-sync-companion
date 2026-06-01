package io.github.duminandrew.obsidiangitsync

import android.app.Application
import androidx.work.Configuration
import io.github.duminandrew.obsidiangitsync.sync.SyncRepository

/**
 * Application entry point. Eagerly creates the [SyncRepository] singleton and
 * provides a manual WorkManager configuration (the default initializer is
 * removed in the manifest).
 */
class ObsidianGitSyncApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // Warm up the repository so WorkManager-triggered syncs share state.
        SyncRepository.getInstance(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
