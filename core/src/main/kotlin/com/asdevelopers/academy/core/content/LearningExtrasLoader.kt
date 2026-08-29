package com.asdevelopers.academy.core.content

import android.content.res.AssetManager
import com.asdevelopers.academy.core.exercise.Exercise
import com.asdevelopers.academy.core.exercise.ExerciseDifficulty
import com.asdevelopers.academy.core.exercise.ExerciseType
import com.asdevelopers.academy.core.glossary.GlossaryEntry
import com.asdevelopers.academy.core.project.LearningProject
import com.asdevelopers.academy.core.quiz.QuestionType
import com.asdevelopers.academy.core.quiz.Quiz
import com.asdevelopers.academy.core.quiz.QuizAnswer
import com.asdevelopers.academy.core.quiz.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loader محتوای تکمیلی آموزشی هر Course.
 * تمرین، آزمون، پروژه و واژه‌نامه در Course repo قرار دارند ولی Parser فقط در Core نگهداری می‌شود.
 */
data class LearningExtras(
    val exercises: List<Exercise> = emptyList(),
    val quizzes: List<Quiz> = emptyList(),
    val projects: List<LearningProject> = emptyList(),
    val glossary: List<GlossaryEntry> = emptyList()
)

class LearningExtrasLoader(private val assets: AssetManager) {
    suspend fun load(courseId: String): LearningExtras = withContext(Dispatchers.IO) {
        val root = "course/$courseId"
        LearningExtras(
            exercises = loadJsonObjects("$root/exercises").map(::parseExercise),
            quizzes = loadJsonObjects("$root/quizzes").map(::parseQuiz),
            projects = loadJsonObjects("$root/projects").map(::parseProject),
            glossary = loadGlossary("$root/glossary")
        )
    }

    private fun loadJsonObjects(path: String): List<JSONObject> =
        assets.list(path).orEmpty().filter { it.endsWith(".json") }.sorted().mapNotNull { file ->
            runCatching { JSONObject(readText("$path/$file")) }.getOrNull()
        }

    private fun loadGlossary(path: String): List<GlossaryEntry> =
        assets.list(path).orEmpty().filter { it.endsWith(".json") }.sorted().flatMap { file ->
            runCatching {
                val array = JSONArray(readText("$path/$file"))
                (0 until array.length()).map { parseGlossary(array.getJSONObject(it)) }
            }.getOrDefault(emptyList())
        }

    private fun readText(path: String): String = assets.open(path).bufferedReader().use { it.readText() }

    private fun parseExercise(json: JSONObject): Exercise = Exercise(
        id = json.getString("id"),
        lessonId = json.getString("lessonId"),
        title = json.getString("title"),
        description = json.getString("description"),
        type = ExerciseType.valueOf(json.getString("type")),
        difficulty = ExerciseDifficulty.valueOf(json.getString("difficulty")),
        starterCode = json.optString("starterCode").takeIf { it.isNotBlank() },
        expectedOutput = json.optString("expectedOutput").takeIf { it.isNotBlank() },
        hints = json.optJSONArray("hints").toStringList(),
        solution = json.optString("solution").takeIf { it.isNotBlank() },
        explanation = json.optString("explanation").takeIf { it.isNotBlank() }
    )

    private fun parseQuiz(json: JSONObject): Quiz {
        val questions = json.getJSONArray("questions")
        return Quiz(
            id = json.getString("id"),
            lessonId = json.optString("lessonId").takeIf { it.isNotBlank() },
            title = json.getString("title"),
            passingScorePercent = json.optInt("passingScorePercent", 70),
            questions = (0 until questions.length()).map { index ->
                val question = questions.getJSONObject(index)
                val answers = question.optJSONArray("answers") ?: JSONArray()
                QuizQuestion(
                    id = question.getString("id"),
                    type = QuestionType.valueOf(question.getString("type")),
                    question = question.getString("question"),
                    explanation = question.optString("explanation"),
                    tags = question.optJSONArray("tags").toStringList().toSet(),
                    answers = (0 until answers.length()).map { answerIndex ->
                        answers.getJSONObject(answerIndex).let { answer ->
                            QuizAnswer(
                                id = answer.getString("id"),
                                text = answer.getString("text"),
                                isCorrect = answer.getBoolean("isCorrect")
                            )
                        }
                    }
                )
            }
        )
    }

    private fun parseProject(json: JSONObject): LearningProject = LearningProject(
        id = json.getString("id"),
        title = json.getString("title"),
        summary = json.optString("summary"),
        difficulty = json.optString("difficulty", "BEGINNER"),
        steps = json.optJSONArray("steps").toStringList()
    )

    private fun parseGlossary(json: JSONObject): GlossaryEntry = GlossaryEntry(
        id = json.getString("id"),
        term = json.getString("term"),
        translation = json.optString("translation"),
        definition = json.getString("definition"),
        related = json.optJSONArray("related").toStringList()
    )

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { optString(it) }
    }
}
