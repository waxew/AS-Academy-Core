package com.asdevelopers.academy.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * برنامه نمونه فقط برای تست Core است و محتوای هیچ دوره واقعی را در خود نگه نمی‌دارد.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // SampleAcademyApp همان مسیر مصرفی را استفاده می‌کند که دوره‌های واقعی خواهند داشت.
            SampleAcademyApp()
        }
    }
}
