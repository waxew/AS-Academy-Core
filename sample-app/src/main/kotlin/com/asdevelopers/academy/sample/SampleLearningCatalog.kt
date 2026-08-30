package com.asdevelopers.academy.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.asdevelopers.academy.core.content.CourseBundle
import com.asdevelopers.academy.core.ui.screens.AcademyLearningCatalogScreen

/**
 * Reference integration برای Catalog مشترک؛ اپ‌های Course فقط Bundle و Callbackهای Navigation را متصل می‌کنند.
 * این Composable در sample-app نگه داشته می‌شود تا CI مصرف API عمومی Core را نیز compile کند.
 */
@Composable
fun SampleLearningCatalog(
    bundle: CourseBundle,
    modifier: Modifier = Modifier,
    onQuizClick: (String) -> Unit = {},
    onExerciseClick: (String) -> Unit = {},
    onProjectClick: (String) -> Unit = {}
) {
    AcademyLearningCatalogScreen(
        quizzes = bundle.quizzes,
        exercises = bundle.exercises,
        projects = bundle.projects,
        modifier = modifier,
        onQuizClick = onQuizClick,
        onExerciseClick = onExerciseClick,
        onProjectClick = onProjectClick
    )
}
