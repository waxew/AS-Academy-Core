package com.asdevelopers.academy.core.ui

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.asdevelopers.academy.core.content.LearningExtras
import com.asdevelopers.academy.core.exercise.Exercise
import com.asdevelopers.academy.core.project.LearningProject
import com.asdevelopers.academy.core.quiz.Quiz

/** فهرست تمرین‌های دوره؛ داده اختصاصی Course است اما تجربه کاربری در Core مشترک می‌ماند. */
@Composable
internal fun ExerciseListScreen(extras: LearningExtras, nav: NavHostController) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("تمرین‌ها", style = MaterialTheme.typography.headlineMedium) }
        items(extras.exercises, key = { it.id }) { exercise ->
            AcademyCard(exercise.title, "${exercise.difficulty} • ${exercise.type}") {
                nav.navigate(AcademyRoutes.exercise(exercise.id))
            }
        }
    }
}

@Composable
internal fun ExerciseDetailScreen(extras: LearningExtras, exerciseId: String) {
    val exercise = extras.exercises.firstOrNull { it.id == exerciseId }
        ?: return ExtraMessage("تمرین پیدا نشد", exerciseId)
    var showHint by remember { mutableStateOf(false) }
    var showSolution by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(exercise.title, style = MaterialTheme.typography.headlineMedium) }
        item { Text(exercise.description) }
        exercise.starterCode?.let { code -> item { ExtraCodeCard("کد شروع", code) } }
        exercise.expectedOutput?.let { output -> item { ExtraCodeCard("خروجی مورد انتظار", output) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showHint = !showHint }) { Text(if (showHint) "بستن راهنما" else "راهنما") }
                Button(onClick = { showSolution = !showSolution }) { Text(if (showSolution) "پنهان کردن پاسخ" else "نمایش پاسخ") }
            }
        }
        if (showHint) item { Text(exercise.hints.joinToString("\n") { "• $it" }) }
        if (showSolution) {
            exercise.solution?.let { solution -> item { ExtraCodeCard("راه‌حل", solution) } }
            exercise.explanation?.let { explanation -> item { Text(explanation) } }
        }
    }
}

@Composable
internal fun QuizListScreen(extras: LearningExtras, nav: NavHostController) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("آزمون‌ها", style = MaterialTheme.typography.headlineMedium) }
        items(extras.quizzes, key = { it.id }) { quiz ->
            AcademyCard(quiz.title, "${quiz.questions.size} سؤال • حد نصاب ${quiz.passingScorePercent}٪") {
                nav.navigate(AcademyRoutes.quiz(quiz.id))
            }
        }
    }
}

/** آزمون تعاملی: پاسخ‌ها تا Submit خصوصی می‌مانند و سپس امتیاز و توضیح سؤال نمایش داده می‌شود. */
@Composable
internal fun QuizDetailScreen(extras: LearningExtras, quizId: String) {
    val quiz = extras.quizzes.firstOrNull { it.id == quizId }
        ?: return ExtraMessage("آزمون پیدا نشد", quizId)
    val selected = remember(quizId) { mutableStateMapOf<String, String>() }
    var submitted by remember(quizId) { mutableStateOf(false) }
    val correct = quiz.questions.count { question ->
        val answerId = selected[question.id]
        question.answers.firstOrNull { it.id == answerId }?.isCorrect == true
    }
    val score = if (quiz.questions.isEmpty()) 0 else correct * 100 / quiz.questions.size

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(quiz.title, style = MaterialTheme.typography.headlineMedium) }
        items(quiz.questions, key = { it.id }) { question ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(question.question, style = MaterialTheme.typography.titleMedium)
                    question.answers.forEach { answer ->
                        Row(Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = selected[question.id] == answer.id,
                                onClick = { if (!submitted) selected[question.id] = answer.id }
                            )
                            Text(answer.text, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    if (submitted) {
                        val ok = question.answers.firstOrNull { it.id == selected[question.id] }?.isCorrect == true
                        Text(if (ok) "پاسخ صحیح" else "پاسخ نادرست")
                        if (question.explanation.isNotBlank()) Text(question.explanation)
                    }
                }
            }
        }
        item {
            if (!submitted) {
                Button(onClick = { submitted = true }, enabled = selected.size == quiz.questions.size, modifier = Modifier.fillMaxWidth()) {
                    Text("ثبت پاسخ‌ها")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("امتیاز: $score٪", style = MaterialTheme.typography.headlineSmall)
                    Text(if (score >= quiz.passingScorePercent) "قبول شدید" else "برای تسلط بیشتر دوباره تلاش کنید")
                    Button(onClick = { selected.clear(); submitted = false }, modifier = Modifier.fillMaxWidth()) { Text("تلاش دوباره") }
                }
            }
        }
    }
}

@Composable
internal fun ProjectListScreen(extras: LearningExtras, nav: NavHostController) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("پروژه‌های عملی", style = MaterialTheme.typography.headlineMedium) }
        items(extras.projects, key = { it.id }) { project ->
            AcademyCard(project.title, project.summary) { nav.navigate(AcademyRoutes.project(project.id)) }
        }
    }
}

@Composable
internal fun ProjectDetailScreen(extras: LearningExtras, projectId: String) {
    val project = extras.projects.firstOrNull { it.id == projectId }
        ?: return ExtraMessage("پروژه پیدا نشد", projectId)
    ProjectSteps(project)
}

@Composable
private fun ProjectSteps(project: LearningProject) {
    val completed = remember(project.id) { mutableStateMapOf<Int, Boolean>() }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(project.title, style = MaterialTheme.typography.headlineMedium) }
        item { Text(project.summary) }
        item { Text("سطح: ${project.difficulty}") }
        items(project.steps.indices.toList()) { index ->
            Row(Modifier.fillMaxWidth()) {
                Checkbox(checked = completed[index] == true, onCheckedChange = { completed[index] = it })
                Text("${index + 1}. ${project.steps[index]}", modifier = Modifier.padding(top = 12.dp))
            }
        }
        item { Text("پیشرفت پروژه: ${completed.values.count { it }} از ${project.steps.size} مرحله") }
    }
}

@Composable
internal fun GlossaryScreen(extras: LearningExtras) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("واژه‌نامه", style = MaterialTheme.typography.headlineMedium) }
        items(extras.glossary, key = { it.id }) { entry ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.term, style = MaterialTheme.typography.titleMedium)
                    if (entry.translation.isNotBlank()) Text(entry.translation, color = MaterialTheme.colorScheme.primary)
                    Text(entry.definition)
                }
            }
        }
    }
}

@Composable
private fun ExtraCodeCard(title: String, code: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(code)
        }
    }
}

@Composable
private fun ExtraMessage(title: String, message: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(message)
    }
}
