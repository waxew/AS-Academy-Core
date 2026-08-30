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

/** Loader محتوای تکمیلی Course با پشتیبانی از فایل object یا array. */
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
            glossary = loadJsonObjects("$root/glossary").map(::parseGlossary)
        )
    }

    private fun loadJsonObjects(path: String): List<JSONObject> =
        assets.list(path).orEmpty().filter { it.endsWith(".json") }.sorted().flatMap { file ->
            runCatching {
                val text = readText("$path/$file").trim()
                when {
                    text.startsWith("[") -> JSONArray(text).let { array -> (0 until array.length()).map { array.getJSONObject(it) } }
                    text.startsWith("{") -> listOf(JSONObject(text))
                    else -> emptyList()
                }
            }.getOrDefault(emptyList())
        }

    private fun readText(path: String): String = assets.open(path).bufferedReader().use { it.readText() }

    private fun parseExercise(json: JSONObject): Exercise = Exercise(
        id = json.getString("id"), lessonId = json.getString("lessonId"), title = json.getString("title"),
        description = json.optString("description", json.optString("prompt")),
        type = enumValue(json.optString("type", "READ_AND_ANSWER"), ExerciseType.READ_AND_ANSWER),
        difficulty = enumValue(json.optString("difficulty", "EASY"), ExerciseDifficulty.EASY),
        starterCode = json.optString("starterCode").takeIf { it.isNotBlank() },
        expectedOutput = json.optString("expectedOutput").takeIf { it.isNotBlank() },
        hints = json.optJSONArray("hints").toStringList().ifEmpty { json.optJSONArray("acceptance").toStringList() },
        solution = json.optString("solution").takeIf { it.isNotBlank() },
        explanation = json.optString("explanation").takeIf { it.isNotBlank() }
    )

    /**
     * Courseهای قدیمی quiz را به صورت nested نگه می‌دارند و قرارداد canonical هر object را یک سؤال می‌داند.
     * این adapter هر دو schema را می‌خواند تا CoursePackage و UI در مهاجرت محتوا سازگار بمانند.
     */
    private fun parseQuiz(json: JSONObject): Quiz {
        val quizId = json.getString("id")
        val nested = json.optJSONArray("questions")
        val questions = if (nested != null) {
            (0 until nested.length()).map { index -> parseNestedQuestion(nested.getJSONObject(index)) }
        } else {
            listOf(parseFlatQuestion(json, quizId))
        }
        return Quiz(
            id = quizId,
            lessonId = json.optString("lessonId").takeIf { it.isNotBlank() },
            title = json.optString("title", "آزمون درس"),
            passingScorePercent = json.optInt("passingScorePercent", 70),
            questions = questions
        )
    }

    private fun parseNestedQuestion(question: JSONObject): QuizQuestion {
        val answers = question.optJSONArray("answers") ?: JSONArray()
        return QuizQuestion(
            id = question.getString("id"),
            type = enumValue(question.optString("type", "MULTIPLE_CHOICE"), QuestionType.MULTIPLE_CHOICE),
            question = question.getString("question"),
            explanation = question.optString("explanation"),
            tags = question.optJSONArray("tags").toStringList().toSet(),
            answers = (0 until answers.length()).map { index ->
                answers.getJSONObject(index).let { answer ->
                    QuizAnswer(answer.getString("id"), answer.getString("text"), answer.getBoolean("isCorrect"))
                }
            }
        )
    }

    private fun parseFlatQuestion(json: JSONObject, quizId: String): QuizQuestion {
        val options = json.optJSONArray("options") ?: JSONArray()
        val correctIndex = json.optInt("correctIndex", -1)
        return QuizQuestion(
            id = "$quizId-question",
            type = QuestionType.MULTIPLE_CHOICE,
            question = json.getString("question"),
            explanation = json.optString("explanation"),
            answers = (0 until options.length()).map { index ->
                QuizAnswer(id = "option-${index + 1}", text = options.getString(index), isCorrect = index == correctIndex)
            }
        )
    }

    private fun parseProject(json: JSONObject): LearningProject = LearningProject(
        id = json.getString("id"), title = json.getString("title"), summary = json.optString("summary"),
        difficulty = json.optString("difficulty", "BEGINNER"),
        steps = json.optJSONArray("steps").toStringList().ifEmpty { json.optJSONArray("deliverables").toStringList() }
    )

    private fun parseGlossary(json: JSONObject): GlossaryEntry = GlossaryEntry(
        id = json.optString("id").takeIf { it.isNotBlank() } ?: json.getString("term").lowercase().replace(" ", "-"),
        term = json.getString("term"), translation = json.optString("translation", json.optString("fa")),
        definition = json.getString("definition"), related = json.optJSONArray("related").toStringList()
    )

    private inline fun <reified T : Enum<T>> enumValue(raw: String, fallback: T): T =
        runCatching { enumValueOf<T>(raw.trim().uppercase()) }.getOrDefault(fallback)

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { optString(it) }
}
