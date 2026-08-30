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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.exercise.Exercise
import com.asdevelopers.academy.core.project.LearningProject
import com.asdevelopers.academy.core.quiz.Quiz

/** نوع آیتم قابل مرور در مرکز تمرین و ارزیابی مشترک. */
enum class LearningCatalogFilter {
    ALL,
    QUIZZES,
    EXERCISES,
    PROJECTS
}

/**
 * مرکز مشترک تمرین و ارزیابی؛ Course فقط مدل‌های Bundle و Callback مقصدها را می‌دهد.
 * Search و Filter در Core می‌ماند تا اپ‌های زبان‌محور Catalog جداگانه نسازند.
 */
@Composable
fun AcademyLearningCatalogScreen(
    quizzes: List<Quiz>,
    exercises: List<Exercise>,
    projects: List<LearningProject>,
    modifier: Modifier = Modifier,
    onQuizClick: (String) -> Unit = {},
    onExerciseClick: (String) -> Unit = {},
    onProjectClick: (String) -> Unit = {}
) {
    // Query و Filter state فقط presentation هستند و نیازی به persistence ندارند.
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LearningCatalogFilter.ALL) }
    // Normalization یک بار برای هر recomposition انجام می‌شود و مقایسه فارسی/انگلیسی را case-insensitive می‌کند.
    val normalizedQuery = query.trim().lowercase()

    // هر سه نوع محتوا فقط وقتی Filter متناظر اجازه دهد و Query match شود نمایش داده می‌شوند.
    val visibleQuizzes = remember(quizzes, normalizedQuery, filter) {
        if (filter !in setOf(LearningCatalogFilter.ALL, LearningCatalogFilter.QUIZZES)) emptyList()
        else quizzes.filter { quiz ->
            normalizedQuery.isBlank() ||
                quiz.title.lowercase().contains(normalizedQuery) ||
                quiz.questions.any { question ->
                    question.question.lowercase().contains(normalizedQuery) ||
                        question.tags.any { tag -> tag.lowercase().contains(normalizedQuery) }
                }
        }
    }
    val visibleExercises = remember(exercises, normalizedQuery, filter) {
        if (filter !in setOf(LearningCatalogFilter.ALL, LearningCatalogFilter.EXERCISES)) emptyList()
        else exercises.filter { exercise ->
            normalizedQuery.isBlank() ||
                exercise.title.lowercase().contains(normalizedQuery) ||
                exercise.description.lowercase().contains(normalizedQuery) ||
                exercise.tags.any { tag -> tag.lowercase().contains(normalizedQuery) }
        }
    }
    val visibleProjects = remember(projects, normalizedQuery, filter) {
        if (filter !in setOf(LearningCatalogFilter.ALL, LearningCatalogFilter.PROJECTS)) emptyList()
        else projects.filter { project ->
            normalizedQuery.isBlank() ||
                project.title.lowercase().contains(normalizedQuery) ||
                project.description.lowercase().contains(normalizedQuery) ||
                project.tags.any { tag -> tag.lowercase().contains(normalizedQuery) }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "catalog-header") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "مرکز تمرین و ارزیابی",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${quizzes.size} آزمون • ${exercises.size} تمرین • ${projects.size} پروژه",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("جست‌وجو در عنوان، توضیح و موضوع") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CatalogFilterChip("همه", LearningCatalogFilter.ALL, filter) { filter = it }
                    CatalogFilterChip("آزمون", LearningCatalogFilter.QUIZZES, filter) { filter = it }
                    CatalogFilterChip("تمرین", LearningCatalogFilter.EXERCISES, filter) { filter = it }
                    CatalogFilterChip("پروژه", LearningCatalogFilter.PROJECTS, filter) { filter = it }
                }
            }
        }

        if (visibleQuizzes.isNotEmpty()) {
            item(key = "quiz-section") {
                CatalogSectionTitle("آزمون‌ها", visibleQuizzes.size)
            }
            items(visibleQuizzes, key = { quiz -> "quiz-${quiz.id}" }) { quiz ->
                CatalogCard(
                    title = quiz.title,
                    metadata = "${quiz.questions.size} سؤال • قبولی ${quiz.passingScorePercent}%",
                    description = quiz.questions.firstOrNull()?.question.orEmpty(),
                    actionLabel = "شروع آزمون",
                    onClick = { onQuizClick(quiz.id) }
                )
            }
        }

        if (visibleExercises.isNotEmpty()) {
            item(key = "exercise-section") {
                CatalogSectionTitle("تمرین‌ها", visibleExercises.size)
            }
            items(visibleExercises, key = { exercise -> "exercise-${exercise.id}" }) { exercise ->
                CatalogCard(
                    title = exercise.title,
                    metadata = "${exercise.difficulty.name} • ${exercise.type.name}",
                    description = exercise.description,
                    actionLabel = "باز کردن تمرین",
                    onClick = { onExerciseClick(exercise.id) }
                )
            }
        }

        if (visibleProjects.isNotEmpty()) {
            item(key = "project-section") {
                CatalogSectionTitle("پروژه‌ها", visibleProjects.size)
            }
            items(visibleProjects, key = { project -> "project-${project.id}" }) { project ->
                CatalogCard(
                    title = project.title,
                    metadata = "${project.difficulty} • ${project.estimatedMinutes} دقیقه • ${project.milestones.size} مرحله",
                    description = project.description,
                    actionLabel = "باز کردن پروژه",
                    onClick = { onProjectClick(project.id) }
                )
            }
        }

        // اگر Query هیچ نتیجه‌ای نداشت، State خالی به‌جای صفحه سفید نمایش داده می‌شود.
        if (visibleQuizzes.isEmpty() && visibleExercises.isEmpty() && visibleProjects.isEmpty()) {
            item(key = "empty-state") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("موردی پیدا نشد", style = MaterialTheme.typography.titleMedium)
                        Text("عبارت جست‌وجو یا نوع محتوا را تغییر دهید.")
                    }
                }
            }
        }

        item(key = "catalog-bottom-space") {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {}
        }
    }
}

/** FilterChip کوچک، انتخاب نوع محتوا را بدون کپی UI در Courseها استاندارد می‌کند. */
@Composable
private fun CatalogFilterChip(
    label: String,
    value: LearningCatalogFilter,
    selected: LearningCatalogFilter,
    onSelected: (LearningCatalogFilter) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelected(value) },
        label = { Text(label) }
    )
}

/** تیتر هر گروه تعداد نتیجه فیلترشده را هم نشان می‌دهد. */
@Composable
private fun CatalogSectionTitle(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/** Card عمومی برای Quiz/Exercise/Project تا hierarchy بصری همه Courseها یکسان بماند. */
@Composable
private fun CatalogCard(
    title: String,
    metadata: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(metadata, style = MaterialTheme.typography.labelLarge)
            description.takeIf(String::isNotBlank)?.let { text ->
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel)
            }
        }
    }
}
