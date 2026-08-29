package com.asdevelopers.academy.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** حالت Theme مشترک تمام اپ‌های دوره‌ای. */
enum class AcademyThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/** تنظیمات قابل Backup/نمایش که UI به کلیدهای DataStore وابسته نمی‌شود. */
data class AcademySettings(
    val themeMode: AcademyThemeMode = AcademyThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val fontScale: Float = 1f,
    val lastCourseId: String? = null
)

/** اطلاعات پروفایل محلی مشترک Drawer. */
data class AcademyProfile(
    val displayName: String = "کاربر AS Academy",
    val imageUri: String? = null
)

/**
 * Repository واحد DataStore برای Settings و Profile؛ اپ‌ها فقط متدهای تایپ‌شده را مصرف می‌کنند.
 */
class AcademyPreferencesRepository(
    context: Context,
    fileName: String = "as_academy_preferences"
) {
    // DataStore برای هر فایل باید Singleton باشد؛ Factory مشترک از ساخت دو Instance ناسازگار جلوگیری می‌کند.
    private val store: DataStore<Preferences> = sharedStore(context.applicationContext, fileName)
    private val safeData: Flow<Preferences> = store.data.catch { error ->
        // خرابی موقت خواندن فایل به تنظیمات پیش‌فرض برمی‌گردد؛ خطاهای برنامه‌نویسی پنهان نمی‌شوند.
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val settings: Flow<AcademySettings> = safeData.map { preferences ->
        AcademySettings(
            themeMode = preferences[THEME_MODE]?.let { runCatching { AcademyThemeMode.valueOf(it) }.getOrNull() }
                ?: AcademyThemeMode.SYSTEM,
            notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
            fontScale = (preferences[FONT_SCALE] ?: 1f).coerceIn(0.85f, 1.35f),
            lastCourseId = preferences[LAST_COURSE_ID]
        )
    }

    val profile: Flow<AcademyProfile> = safeData.map { preferences ->
        AcademyProfile(
            displayName = preferences[DISPLAY_NAME] ?: "کاربر AS Academy",
            imageUri = preferences[PROFILE_IMAGE_URI]
        )
    }

    suspend fun setThemeMode(mode: AcademyThemeMode) = store.edit { it[THEME_MODE] = mode.name }.let { Unit }
    suspend fun setNotificationsEnabled(enabled: Boolean) = store.edit { it[NOTIFICATIONS_ENABLED] = enabled }.let { Unit }
    suspend fun setFontScale(scale: Float) = store.edit { it[FONT_SCALE] = scale.coerceIn(0.85f, 1.35f) }.let { Unit }
    suspend fun setLastCourse(courseId: String?) = store.edit { preferences ->
        if (courseId == null) preferences.remove(LAST_COURSE_ID) else preferences[LAST_COURSE_ID] = courseId
    }.let { Unit }

    suspend fun updateProfile(displayName: String, imageUri: String?) = store.edit { preferences ->
        preferences[DISPLAY_NAME] = displayName.trim().ifBlank { "کاربر AS Academy" }
        if (imageUri == null) preferences.remove(PROFILE_IMAGE_URI) else preferences[PROFILE_IMAGE_URI] = imageUri
    }.let { Unit }

    companion object {
        private val stores = mutableMapOf<String, DataStore<Preferences>>()

        private fun sharedStore(context: Context, fileName: String): DataStore<Preferences> {
            val file = context.preferencesDataStoreFile(fileName)
            return synchronized(stores) {
                stores.getOrPut(file.absolutePath) {
                    // Scope مستقل IO تا عمر DataStore به Activity یا Repository instance وابسته نباشد.
                    PreferenceDataStoreFactory.create(
                        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                        produceFile = { file }
                    )
                }
            }
        }

        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val FONT_SCALE = floatPreferencesKey("font_scale")
        private val LAST_COURSE_ID = stringPreferencesKey("last_course_id")
        private val DISPLAY_NAME = stringPreferencesKey("profile_display_name")
        private val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")
    }
}
