package com.asdevelopers.academy.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.review.PlacementRecommendation
import com.asdevelopers.academy.course.model.CourseLevelType

/**
 * نتیجه تعیین سطح را بدون وابستگی به یک Course خاص نمایش می‌دهد.
 * Course عنوان سطح را از PlacementPolicy و weakTags را از QuizScore فراهم می‌کند.
 */
@Composable
fun AcademyPlacementSummaryScreen(
    recommendation: PlacementRecommendation,
    weakTags: Set<String>,
    modifier: Modifier = Modifier,
    onStartLevel: (CourseLevelType) -> Unit,
    onReviewWeakTopics: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("نتیجه تعیین سطح", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("امتیاز ${recommendation.scorePercent}٪", style = MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(
                    progress = { recommendation.scorePercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("پیشنهاد شروع: ${recommendation.title}", style = MaterialTheme.typography.titleLarge)

                if (recommendation.fastTrack) {
                    Text(
                        "نتیجه شما برای مسیر فشرده مناسب است؛ آزمون‌های جامع، پروژه‌های سطح بالا و Capstone در اولویت قرار می‌گیرند.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        "این سطح نقطه شروع پیشنهادی است و بر اساس عملکرد واقعی در ادامه می‌تواند با مرور هدفمند تکمیل شود.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (weakTags.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("موضوع‌های نیازمند مرور", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(weakTags.sorted().joinToString("، "))
                    if (recommendation.reviewWeakTopics) {
                        OutlinedButton(onClick = onReviewWeakTopics, modifier = Modifier.fillMaxWidth()) {
                            Text("مشاهده برنامه مرور")
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onStartLevel(recommendation.levelType) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("شروع ${recommendation.title}")
        }
    }
}
