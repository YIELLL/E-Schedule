package com.example.eschedule.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Light / dark appearance; [System] follows the phone's setting. */
enum class ThemeMode(val label: String) {
    System("System default"),
    Light("Light"),
    Dark("Dark"),
}

/** A saved API key with a name the user chose (e.g. "Gemini"). */
data class ApiKeyEntry(val id: String, val label: String, val value: String)

/** How class and reminder notifications should behave. */
data class NotificationPrefs(
    val classAlerts: Boolean = true,
    /** Minutes before a class starts. */
    val classLeadMinutes: Int = 10,
    val reminderAlerts: Boolean = true,
    /** Minutes before a reminder is due; 0 = at the due time. */
    val reminderLeadMinutes: Int = 0,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
)

/** Small key-value settings (SharedPreferences), exposed as flows the UI can collect. */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _notifications = MutableStateFlow(
        NotificationPrefs(
            classAlerts = prefs.getBoolean(KEY_CLASS_ALERTS, true),
            classLeadMinutes = prefs.getInt(KEY_CLASS_LEAD, 10),
            reminderAlerts = prefs.getBoolean(KEY_REMINDER_ALERTS, true),
            reminderLeadMinutes = prefs.getInt(KEY_REMINDER_LEAD, 0),
            sound = prefs.getBoolean(KEY_SOUND, true),
            vibrate = prefs.getBoolean(KEY_VIBRATE, true),
        )
    )
    val notifications: StateFlow<NotificationPrefs> = _notifications

    fun updateNotifications(transform: (NotificationPrefs) -> NotificationPrefs) {
        val value = transform(_notifications.value)
        _notifications.value = value
        prefs.edit()
            .putBoolean(KEY_CLASS_ALERTS, value.classAlerts)
            .putInt(KEY_CLASS_LEAD, value.classLeadMinutes)
            .putBoolean(KEY_REMINDER_ALERTS, value.reminderAlerts)
            .putInt(KEY_REMINDER_LEAD, value.reminderLeadMinutes)
            .putBoolean(KEY_SOUND, value.sound)
            .putBoolean(KEY_VIBRATE, value.vibrate)
            .apply()
    }

    // Appearance
    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.Light.name).orEmpty()) }
            .getOrDefault(ThemeMode.Light)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    // Developer: API keys for the recognition service (kept in the app's private storage).
    private val _apiKeys = MutableStateFlow(loadApiKeys())
    val apiKeys: StateFlow<List<ApiKeyEntry>> = _apiKeys

    /** The key the app should use; null when none are saved. */
    private val _activeApiKeyId = MutableStateFlow(
        prefs.getString(KEY_API_ACTIVE, null)?.takeIf { id -> _apiKeys.value.any { it.id == id } }
            ?: _apiKeys.value.firstOrNull()?.id
    )
    val activeApiKeyId: StateFlow<String?> = _activeApiKeyId

    /** Adds a key (id = null) or updates an existing one. The first key added becomes active. */
    fun saveApiKey(id: String?, label: String, value: String) {
        val entry = ApiKeyEntry(id ?: UUID.randomUUID().toString(), label.trim(), value.trim())
        _apiKeys.value =
            if (id != null && _apiKeys.value.any { it.id == id }) _apiKeys.value.map { if (it.id == id) entry else it }
            else _apiKeys.value + entry
        if (_activeApiKeyId.value == null) _activeApiKeyId.value = entry.id
        persistApiKeys()
    }

    fun deleteApiKey(id: String) {
        _apiKeys.value = _apiKeys.value.filterNot { it.id == id }
        if (_activeApiKeyId.value == id) _activeApiKeyId.value = _apiKeys.value.firstOrNull()?.id
        persistApiKeys()
    }

    fun setActiveApiKey(id: String) {
        if (_apiKeys.value.none { it.id == id }) return
        _activeApiKeyId.value = id
        persistApiKeys()
    }

    private fun persistApiKeys() {
        val array = JSONArray()
        _apiKeys.value.forEach {
            array.put(JSONObject().put("id", it.id).put("label", it.label).put("value", it.value))
        }
        prefs.edit()
            .putString(KEY_API_LIST, array.toString())
            .putString(KEY_API_ACTIVE, _activeApiKeyId.value)
            .remove(KEY_API)          // the old single-key setting, now folded into the list
            .apply()
    }

    private fun loadApiKeys(): List<ApiKeyEntry> {
        val json = prefs.getString(KEY_API_LIST, null)
        if (json != null) {
            return runCatching {
                val array = JSONArray(json)
                (0 until array.length()).map {
                    val o = array.getJSONObject(it)
                    ApiKeyEntry(o.getString("id"), o.getString("label"), o.getString("value"))
                }
            }.getOrDefault(emptyList())
        }
        // Earlier versions stored a single key.
        val legacy = prefs.getString(KEY_API, "").orEmpty()
        return if (legacy.isNotBlank()) listOf(ApiKeyEntry(UUID.randomUUID().toString(), "Default", legacy)) else emptyList()
    }

    /** Back to factory defaults: forgets every saved setting and the API key. */
    fun reset() {
        prefs.edit().clear().apply()
        _notifications.value = NotificationPrefs()
        _themeMode.value = ThemeMode.Light
        _apiKeys.value = emptyList()
        _activeApiKeyId.value = null
    }

    private companion object {
        const val KEY_CLASS_ALERTS = "notif_class_alerts"
        const val KEY_CLASS_LEAD = "notif_class_lead"
        const val KEY_REMINDER_ALERTS = "notif_reminder_alerts"
        const val KEY_REMINDER_LEAD = "notif_reminder_lead"
        const val KEY_SOUND = "notif_sound"
        const val KEY_VIBRATE = "notif_vibrate"
        const val KEY_API = "api_key"
        const val KEY_API_LIST = "api_keys"
        const val KEY_API_ACTIVE = "api_key_active"
        const val KEY_THEME = "theme_mode"
    }
}
