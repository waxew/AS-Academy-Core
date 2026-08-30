package com.asdevelopers.academy.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.review.LessonReviewRecommendation

/**
 * صفحه مشترک مرور نقاط ضعف؛ Recommendationها از WeakTopicReviewEngine می‌آیند و UI فقط آن‌ها را نمایش می‌دهد.
 */
@Composable
fun AcademyWeakTopicReviewScreen(
    recommendations: List<LessonReviewRecommendation>,
    modifier: Modifier = Modifier,
    onLessonClick: (lessonId: String) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("مرور نقاط ضعف", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "درس‌ها بر اساس موضوع‌هایی که در آزمون‌ها بیشتر اشتباه شده‌اند اولویت‌بندی می‌شوند.",
            style = MaterialTheme.typography.bodyLarge
        )

        if (recommendations.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "فعلاً ضعف تکرارشونده‌ای ثبت نشده است. آزمون‌های بیشتری حل کنید تا پیشنهادها دقیق‌تر شوند.",
                    modifier = Modifier.padding(16.dp)
                )
            }
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recommendations, key = LessonReviewRecommendation::lessonId) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("اولویت ${item.priority}", style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            "موضوع‌های نیازمند مرور: ${item.matchedTags.sorted().joinToString("، ")}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { onLessonClick(item.lessonId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("مرور این درس")
                        }
                    }
                }
            }
        }
    }
}
