package com.aljaz.weighttracker.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant

/** The DataStore file "backup_settings" (one instance for the whole app). */
private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "backup_settings",
)

/**
 * Stores the Nextcloud connection (password encrypted) and the
 * result of the latest backup, using Preferences DataStore.
 *
 * @param context Application context.
 * @param cipher Encrypts the app password before it is written to disk.
 */
class BackupSettingsStore(
    context: Context,
    private val cipher: SecretCipher,
) {
    private val dataStore = context.backupDataStore

    /** All stored values; an unreadable file is treated as empty instead of crashing. */
    private val preferences: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    /** The saved connection, or null if none is saved (or the password can't be decrypted). */
    val config: Flow<NextcloudConfig?> = preferences.map { prefs ->
        val url = prefs[KEY_SERVER_URL] ?: return@map null
        val username = prefs[KEY_USERNAME] ?: return@map null
        val password = prefs[KEY_PASSWORD]?.let(cipher::decrypt) ?: return@map null
        NextcloudConfig(url, username, password)
    }

    /** Result of the latest backups. */
    val status: Flow<BackupStatus> = preferences.map { prefs ->
        BackupStatus(
            lastSuccessAt = prefs[KEY_LAST_SUCCESS_MILLIS]?.let(Instant::ofEpochMilli),
            lastError = prefs[KEY_LAST_ERROR]?.let { name ->
                BackupError.entries.find { it.name == name }
            },
        )
    }

    /** Saves [config], encrypting the password. Clears any previous error. */
    suspend fun saveConfig(config: NextcloudConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = config.serverUrl
            prefs[KEY_USERNAME] = config.username
            prefs[KEY_PASSWORD] = cipher.encrypt(config.appPassword)
            prefs.remove(KEY_LAST_ERROR)
        }
    }

    /** Removes the connection and the backup status. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    /** Records a successful backup finished at [time]. */
    suspend fun recordSuccess(time: Instant) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SUCCESS_MILLIS] = time.toEpochMilli()
            prefs.remove(KEY_LAST_ERROR)
        }
    }

    /** Records that the latest backup attempt failed with [error]. */
    suspend fun recordFailure(error: BackupError) {
        dataStore.edit { it[KEY_LAST_ERROR] = error.name }
    }

    private companion object {
        val KEY_SERVER_URL = stringPreferencesKey("server_url")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_PASSWORD = stringPreferencesKey("app_password_encrypted")
        val KEY_LAST_SUCCESS_MILLIS = longPreferencesKey("last_success_millis")
        val KEY_LAST_ERROR = stringPreferencesKey("last_error")
    }
}