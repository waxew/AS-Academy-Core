package com.asdevelopers.academy.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.settings.AcademySettings
import com.asdevelopers.academy.core.settings.AcademyThemeMode

/** Settings UI فقط State/Callback می‌گیرد و به DataStore مستقیم وابسته نیست. */
@Composable
fun AcademySettingsScreen(
    settings: AcademySettings,
    onThemeChanged: (AcademyThemeMode) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineMedium)
        Text("نمای برنامه", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AcademyThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { onThemeChanged(mode) },
                    label = { Text(mode.localizedTitle()) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("اعلان‌ها", style = MaterialTheme.typography.titleMedium)
                Text("یادآوری ادامه مطالعه", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = settings.notificationsEnabled, onCheckedChange = onNotificationsChanged)
        }
        Text("اندازه متن: ${(settings.fontScale * 100).toInt()}٪", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = settings.fontScale,
            onValueChange = onFontScaleChanged,
            valueRange = 0.85f..1.35f,
            steps = 4
        )
    }
}

/** عنوان فارسی Theme در Core نگهداری می‌شود تا دوره‌ها ترجمه متفاوت نسازند. */
private fun AcademyThemeMode.localizedTitle(): String = when (this) {
    AcademyThemeMode.SYSTEM -> "سیستم"
    AcademyThemeMode.LIGHT -> "روشن"
    AcademyThemeMode.DARK -> "تیره"
}
