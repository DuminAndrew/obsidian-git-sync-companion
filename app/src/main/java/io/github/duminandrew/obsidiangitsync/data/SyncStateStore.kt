package io.github.duminandrew.obsidiangitsync.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Tracks the last-synced git blob SHA for each vault path. This is the "base"
 * version used by the three-way conflict detection in the sync engine
 * (local vs remote vs base). See SyncEngine for the decision table.
 *
 * Stored as one SharedPreferences key per relative path.
 */
class SyncStateStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** Last-synced git blob SHA for [path], or null if never synced. */
    fun baseSha(path: String): String? = prefs.getString(path, null)

    fun setBaseSha(path: String, sha: String) {
        prefs.edit().putString(path, sha).apply()
    }

    fun remove(path: String) {
        prefs.edit().remove(path).apply()
    }

    fun all(): Map<String, String> =
        prefs.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val FILE_NAME = "ogs_sync_state"
    }
}
