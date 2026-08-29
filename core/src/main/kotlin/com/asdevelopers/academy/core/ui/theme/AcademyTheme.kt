package com.asdevelopers.academy.core.ui.theme

import android.graphics.Color.parseColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.asdevelopers.academy.course.model.CourseBranding

/** Branding پیش‌فرض تنها وقتی Course رنگ اختصاصی نداده استفاده می‌شود. */
val DefaultAcademyBranding = CourseBranding(
    primaryColorHex = "#6750A4",
    secondaryColorHex = "#625B71",
    accentColorHex = "#7D5260"
)

/**
 * Theme مشترک Light/Dark که Branding هر دوره را بدون کپی Design System اعمال می‌کند.
 */
@Composable
fun AcademyTheme(
    branding: CourseBranding = DefaultAcademyBranding,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // رنگ نامعتبر قبل از رسیدن به UI توسط Validator رد می‌شود؛ fallback از Crash Preview جلوگیری می‌کند.
    val primary = branding.primaryColorHex.toComposeColor(Color(0xFF6750A4))
    val secondary = branding.secondaryColorHex.toComposeColor(Color(0xFF625B71))
    val tertiary = branding.accentColorHex.toComposeColor(Color(0xFF7D5260))
    val colors = if (darkTheme) {
        darkColorScheme(primary = primary, secondary = secondary, tertiary = tertiary)
    } else {
        lightColorScheme(primary = primary, secondary = secondary, tertiary = tertiary)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/** تبدیل Hex در یک نقطه نگهداری می‌شود تا Courseها Utility تکراری نسازند. */
private fun String.toComposeColor(fallback: Color): Color =
    runCatching { Color(parseColor(this)) }.getOrDefault(fallback)
