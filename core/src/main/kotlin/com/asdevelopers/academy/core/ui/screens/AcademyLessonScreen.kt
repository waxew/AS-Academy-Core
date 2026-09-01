package com.asdevelopers.academy.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.progress.LessonProgress
import com.asdevelopers.academy.core.progress.LessonStatus
import com.asdevelopers.academy.core.ui.content.LessonRenderer
import com.asdevelopers.academy.course.model.Lesson

/**
 * Screen استاندارد Lesson روی Renderer مشترک.
 * Footer تکمیل درس باعث می‌شود Lesson Progress در همه Courseها workflow یکسان و قابل اندازه‌گیری داشته باشد.
 */
@Composable
fun AcademyLessonScreen(
    lesson: Lesson,
    progress: LessonProgress?,
    modifier: Modifier = Modifier,
    onExerciseClick: (String) -> Unit = {},
    onQuizClick: (String) -> Unit = {},
    onProjectClick: (String) -> Unit = {},
    onCompleteClick: () -> Unit
) {
    val completed = progress?.status == LessonStatus.COMPLETED

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LessonRenderer(
            lesson = lesson,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            onExerciseClick = onExerciseClick,
            onQuizClick = onQuizClick,
            onProjectClick = onProjectClick
        )

        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (completed) "این درس تکمیل شده است." else "بعد از مطالعه و انجام فعالیت‌های درس، آن را تکمیل کنید.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onCompleteClick,
                    enabled = !completed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (completed) "تکمیل‌شده" else "تکمیل درس")
                }
            }
        }
    }
}
