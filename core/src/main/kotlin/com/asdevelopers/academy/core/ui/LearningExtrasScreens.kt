package com.asdevelopers.academy.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.asdevelopers.academy.core.content.LearningExtras
import com.asdevelopers.academy.core.database.AcademyDatabase
import com.asdevelopers.academy.core.database.LearningCompletionEntity
import com.asdevelopers.academy.core.database.QuizResultEntity
import kotlinx.coroutines.launch

/** UI عمومی Exercise/Quiz/Project/Glossary؛ Course فقط داده فراهم می‌کند. */
@Composable
internal fun ExerciseListScreen(extras: LearningExtras, nav: NavHostController, db: AcademyDatabase) {
    val completed by db.learningCompletionDao().observeAll().collectAsState(initial = emptyList())
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("تمرین‌ها", style = MaterialTheme.typography.headlineMedium) }
        items(extras.exercises, key = { it.id }) { exercise ->
            val done = completed.any { it.targetType == "EXERCISE" && it.targetId == exercise.id && it.completed }
            AcademyCard(exercise.title, "${exercise.difficulty.name}${if (done) " • تکمیل شده" else ""}", onClick = { nav.navigate(AcademyRoutes.exercise(exercise.id)) })
        }
    }
}

@Composable
internal fun ExerciseDetailScreen(extras: LearningExtras, exerciseId: String, db: AcademyDatabase) {
    val exercise = extras.exercises.firstOrNull { it.id == exerciseId } ?: return
    val scope = rememberCoroutineScope()
    val completion by db.learningCompletionDao().observe("EXERCISE", exerciseId).collectAsState(initial = null)
    var showHint by remember(exerciseId) { mutableIntStateOf(0) }
    var showSolution by remember(exerciseId) { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(exercise.title, style = MaterialTheme.typography.headlineMedium) }
        item { Text("سطح: ${exercise.difficulty.name}") }
        item { Text(exercise.description) }
        exercise.starterCode?.let { code -> item { Text(code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge) } }
        if (showHint > 0) items(exercise.hints.take(showHint)) { hint -> Text("راهنما: $hint") }
        if (exercise.hints.isNotEmpty() && showHint < exercise.hints.size) item { Button(onClick = { showHint++ }, modifier = Modifier.fillMaxWidth()) { Text("نمایش راهنمای بعدی") } }
        item { Button(onClick = { showSolution = !showSolution }, modifier = Modifier.fillMaxWidth()) { Text(if (showSolution) "پنهان کردن پاسخ" else "نمایش پاسخ") } }
        if (showSolution) {
            exercise.solution?.let { solution -> item { Text(solution, fontFamily = FontFamily.Monospace) } }
            exercise.explanation?.let { explanation -> item { Text(explanation) } }
            exercise.expectedOutput?.let { expected -> item { Text("خروجی مورد انتظار: $expected") } }
        }
        item {
            Button(onClick = { scope.launch { db.learningCompletionDao().upsert(LearningCompletionEntity("EXERCISE:$exerciseId", "EXERCISE", exerciseId, true, System.currentTimeMillis())) } }, modifier = Modifier.fillMaxWidth()) {
                Text(if (completion?.completed == true) "تمرین تکمیل شده ✓" else "ثبت تکمیل تمرین")
            }
        }
    }
}

@Composable
internal fun QuizListScreen(extras: LearningExtras, nav: NavHostController, db: AcademyDatabase) {
    val attempts by db.quizResultDao().observeAll().collectAsState(initial = emptyList())
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("آزمون‌ها", style = MaterialTheme.typography.headlineMedium) }
        items(extras.quizzes, key = { it.id }) { quiz ->
            val best = attempts.filter { it.quizId == quiz.id }.maxOfOrNull { it.scorePercent }
            val result = best?.let { " • بهترین نتیجه $it%" }.orEmpty()
            AcademyCard(quiz.title, "${quiz.questions.size} سؤال • حدنصاب ${quiz.passingScorePercent}%$result", onClick = { nav.navigate(AcademyRoutes.quiz(quiz.id)) })
        }
    }
}

@Composable
internal fun QuizDetailScreen(extras: LearningExtras, quizId: String, db: AcademyDatabase) {
    val quiz = extras.quizzes.firstOrNull { it.id == quizId } ?: return
    val scope = rememberCoroutineScope()
    var index by remember(quizId) { mutableIntStateOf(0) }
    var selectedId by remember(quizId, index) { mutableStateOf<String?>(null) }
    var correctCount by remember(quizId) { mutableIntStateOf(0) }
    var finished by remember(quizId) { mutableStateOf(false) }
    var feedback by remember(quizId, index) { mutableStateOf<String?>(null) }
    val question = quiz.questions.getOrNull(index)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(quiz.title, style = MaterialTheme.typography.headlineMedium)
        if (finished) {
            val percent = if (quiz.questions.isEmpty()) 0 else (correctCount * 100) / quiz.questions.size
            Text("نتیجه: $correctCount از ${quiz.questions.size} — $percent%", style = MaterialTheme.typography.titleLarge)
            Text(if (percent >= quiz.passingScorePercent) "آزمون با موفقیت گذرانده شد." else "برای تسلط بیشتر، درس‌های مرتبط را مرور کنید.")
        } else if (question != null) {
            Text("سؤال ${index + 1} از ${quiz.questions.size}")
            Text(question.question, style = MaterialTheme.typography.titleLarge)
            question.answers.forEach { answer -> Button(onClick = { if (feedback == null) selectedId = answer.id }, modifier = Modifier.fillMaxWidth()) { Text(if (selectedId == answer.id) "✓ ${answer.text}" else answer.text) } }
            Button(enabled = selectedId != null && feedback == null, onClick = {
                val selected = question.answers.firstOrNull { it.id == selectedId }
                val correct = selected?.isCorrect == true
                if (correct) correctCount++
                feedback = if (correct) "پاسخ صحیح است. ${question.explanation}" else "پاسخ نادرست است. ${question.explanation}"
            }, modifier = Modifier.fillMaxWidth()) { Text("بررسی پاسخ") }
            feedback?.let { text ->
                Text(text)
                Button(onClick = {
                    val last = index == quiz.questions.lastIndex
                    if (last) {
                        finished = true
                        val percent = if (quiz.questions.isEmpty()) 0 else (correctCount * 100) / quiz.questions.size
                        scope.launch { db.quizResultDao().insert(QuizResultEntity("$quizId:${System.currentTimeMillis()}", quizId, percent, correctCount, quiz.questions.size - correctCount, System.currentTimeMillis())) }
                    } else { index++; selectedId = null; feedback = null }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (index == quiz.questions.lastIndex) "نمایش نتیجه" else "سؤال بعد") }
            }
        }
    }
}

@Composable
internal fun ProjectListScreen(extras: LearningExtras, nav: NavHostController, db: AcademyDatabase) {
    val completed by db.learningCompletionDao().observeAll().collectAsState(initial = emptyList())
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("پروژه‌های عملی", style = MaterialTheme.typography.headlineMedium) }
        items(extras.projects, key = { it.id }) { project ->
            val done = completed.any { it.targetType == "PROJECT" && it.targetId == project.id && it.completed }
            AcademyCard(project.title, "${project.difficulty} • ${project.steps.size} مرحله${if (done) " • تکمیل شده" else ""}", onClick = { nav.navigate(AcademyRoutes.project(project.id)) })
        }
    }
}

@Composable
internal fun ProjectDetailScreen(extras: LearningExtras, projectId: String, db: AcademyDatabase) {
    val project = extras.projects.firstOrNull { it.id == projectId } ?: return
    val scope = rememberCoroutineScope()
    val completion by db.learningCompletionDao().observe("PROJECT", projectId).collectAsState(initial = null)
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(project.title, style = MaterialTheme.typography.headlineMedium) }
        item { Text(project.summary) }
        item { Text("سطح: ${project.difficulty}") }
        item { Text("مراحل پروژه", style = MaterialTheme.typography.titleLarge) }
        items(project.steps.mapIndexed { i, step -> "${i + 1}. $step" }) { step -> Text(step) }
        item { Button(onClick = { scope.launch { db.learningCompletionDao().upsert(LearningCompletionEntity("PROJECT:$projectId", "PROJECT", projectId, true, System.currentTimeMillis())) } }, modifier = Modifier.fillMaxWidth()) { Text(if (completion?.completed == true) "پروژه تکمیل شده ✓" else "ثبت تکمیل پروژه") } }
    }
}

@Composable
internal fun GlossaryScreen(extras: LearningExtras) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, extras.glossary) { extras.glossary.filter { query.isBlank() || it.term.contains(query, true) || it.translation.contains(query, true) || it.definition.contains(query, true) } }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("واژه‌نامه", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("جستجوی اصطلاح") }, modifier = Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(filtered, key = { it.id }) { entry -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(entry.term, style = MaterialTheme.typography.titleMedium); if (entry.translation.isNotBlank()) Text(entry.translation); Text(entry.definition, color = MaterialTheme.colorScheme.onSurfaceVariant); if (entry.related.isNotEmpty()) Text("مرتبط: ${entry.related.joinToString("، ")}", style = MaterialTheme.typography.bodySmall) } } }
    }
}
