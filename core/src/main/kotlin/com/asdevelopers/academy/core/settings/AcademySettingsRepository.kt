package com.asdevelopers.academy.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.academyDataStore by preferencesDataStore(name = "as_academy_settings")

/** تنظیمات مشترک UI و یادگیری؛ Courseها نباید DataStore جدا برای موارد عمومی بسازند. */
class AcademySettingsRepository(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val fontScaleKey = floatPreferencesKey("font_scale")
    private val lineNumbersKey = booleanPreferencesKey("code_line_numbers")

    val darkMode: Flow<Boolean> = context.academyDataStore.data.map { it[darkModeKey] ?: false }
    val fontScale: Flow<Float> = context.academyDataStore.data.map { it[fontScaleKey] ?: 1f }
    val codeLineNumbers: Flow<Boolean> = context.academyDataStore.data.map { it[lineNumbersKey] ?: true }

    suspend fun setDarkMode(value: Boolean) = context.academyDataStore.edit { it[darkModeKey] = value }
    suspend fun setFontScale(value: Float) = context.academyDataStore.edit { it[fontScaleKey] = value.coerceIn(0.85f, 1.5f) }
    suspend fun setCodeLineNumbers(value: Boolean) = context.academyDataStore.edit { it[lineNumbersKey] = value }
}
