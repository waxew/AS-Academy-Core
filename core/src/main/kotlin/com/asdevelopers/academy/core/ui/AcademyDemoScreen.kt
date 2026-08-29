package com.asdevelopers.academy.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * صفحه Smoke Test برای اطمینان از اینکه Design System و Compose در Core قابل مصرف هستند.
 * اپ‌های دوره‌ای این صفحه را استفاده نمی‌کنند و Screenهای واقعی روی Componentهای Core ساخته می‌شوند.
 */
@Composable
fun AcademyDemoScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AS Academy Core", style = MaterialTheme.typography.headlineMedium)
        Text("Shared learning engine is connected.")
        LinearProgressIndicator(progress = { 0.35f })
        Button(onClick = {}) {
            Text("نمونه کنترل مشترک")
        }
    }
}
