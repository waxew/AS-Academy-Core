package com.asdevelopers.academy.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** صفحه About مشترک بدون نمایش Package Name یا جزئیات فنی غیرضروری. */
@Composable
fun AcademyAboutScreen(
    appTitle: String,
    description: String,
    versionName: String,
    supportEmail: String = "AS.Developers.Support@Gmail.Com",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("درباره نرم‌افزار", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(appTitle, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodyLarge)
        Text("نسخه $versionName", style = MaterialTheme.typography.labelLarge)
        Divider()
        Text("راه‌های ارتباطی با ما:", fontWeight = FontWeight.Bold)
        Text(supportEmail)

        // Spacer جای Footer را نزدیک بخش پایینی صفحه و نه چسبیده به لبه حفظ می‌کند.
        Spacer(modifier = Modifier.weight(1f))
        Divider()
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp, top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Develop by AS Team Group",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(supportEmail, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        }
    }
}
