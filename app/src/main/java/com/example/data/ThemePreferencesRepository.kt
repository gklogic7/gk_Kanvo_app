package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kanvoPrefsDataStore by preferencesDataStore(name = "kanvo_preferences")

class ThemePreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("studio_theme_mode")

    val themeMode: Flow<ThemeMode> = context.kanvoPrefsDataStore.data.map { prefs ->
        val raw = prefs[themeKey] ?: ThemeMode.LIGHT.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.LIGHT)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.kanvoPrefsDataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }
}
