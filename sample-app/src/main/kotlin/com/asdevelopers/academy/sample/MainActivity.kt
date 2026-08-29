package com.asdevelopers.academy.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.asdevelopers.academy.core.ui.AcademyDemoScreen

/**
 * برنامه نمونه فقط برای تست Core است و محتوای هیچ دوره واقعی را در خود نگه نمی‌دارد.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AcademyDemoScreen()
                }
            }
        }
    }
}
