package com.asdevelopers.academy.core.content

import android.content.res.AssetManager
import com.asdevelopers.academy.core.exercise.Exercise
import com.asdevelopers.academy.core.exercise.ExerciseDifficulty
import com.asdevelopers.academy.core.exercise.ExerciseType
import com.asdevelopers.academy.core.project.LearningProject
import com.asdevelopers.academy.core.project.ProjectMilestone
import com.asdevelopers.academy.core.quiz.QuestionType
import com.asdevelopers.academy.core.quiz.Quiz
import com.asdevelopers.academy.core.quiz.QuizAnswer
import com.asdevelopers.academy.core.quiz.QuizQuestion
import com.asdevelopers.academy.course.model.GlossaryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Loader محتوای تکمیلی Course با پشتیبانی از فایل object یا array و schemaهای قدیمی. */
data class LearningExtras(
    val exercises: List<Exercise> = emptyList(),
    val quizzes: List<Quiz> = emptyList(),
    val projects: List<LearningProject> = emptyList(),
    val glossary: List<GlossaryEntry> = emptyList()
)

/**
 * Adapter اندروید برای Course Packageهای پوشه‌ای قدیمی.
 *
 * مسیر اصلی تولید محتوا از DirectoryCoursePackageReader و CourseBundle استفاده می‌کند، اما این Loader برای
 * Hostهایی که هنوز Assetهای تفکیک‌شده دارند حفظ می‌شود. تمام خروجی‌ها به مدل canonical فعلی Core تبدیل می‌شوند.
 */
class LearningExtrasLoader(private val assets: AssetManager) {
    suspend fun load(courseId: String): LearningExtras = withContext(Dispatchers.IO) {
        val root = "course/$courseId"
        LearningExtras(
            exercises = loadJsonObjects("$root/exercises").map { parseExercise(it, courseId) },
            quizzes = loadJsonObjects("$root/quizzes").map { parseQuiz(it, courseId) },
            projects = loadJsonObjects("$root/projects").map { parseProject(it, courseId) },
            glossary = loadJsonObjects("$root/glossary").map { parseGlossary(it, courseId) }
        )
    }

    /** فایل‌های JSON را مستقل از اینکه object یا array باشند به مجموعه objectهای قابل parse تبدیل می‌کند. */
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

    /** Exercise قدیمی و جدید هر دو به مدل استاندارد Exercise Engine نگاشت می‌شوند. */
    private fun parseExercise(json: JSONObject, fallbackCourseId: String): Exercise = Exercise(
        id = json.getString("id"),
        lessonId = json.getString("lessonId"),
        title = json.getString("title"),
        description = json.optString("description", json.optString("prompt")),
        type = enumValue(json.optString("type", "READ_AND_ANSWER"), ExerciseType.READ_AND_ANSWER),
        difficulty = enumValue(json.optString("difficulty", "EASY"), ExerciseDifficulty.EASY),
        starterCode = json.optString("starterCode").takeIf { it.isNotBlank() },
        expectedOutput = json.optString("expectedOutput").takeIf { it.isNotBlank() },
        hints = json.optJSONArray("hints").toStringList().ifEmpty { json.optJSONArray("acceptance").toStringList() },
        solution = json.optString("solution").takeIf { it.isNotBlank() },
        explanation = json.optString("explanation").takeIf { it.isNotBlank() },
        courseId = json.optString("courseId", fallbackCourseId).ifBlank { fallbackCourseId },
        language = json.optString("language").takeIf { it.isNotBlank() },
        tags = json.optJSONArray("tags").toStringList().toSet()
    )

    /**
     * Courseهای قدیمی quiz را به صورت nested نگه می‌دارند و بعضی Packageهای اولیه سؤال flat داشتند.
     * این adapter هر دو schema را می‌خواند تا مهاجرت محتوا بدون از دست رفتن Progress انجام شود.
     */
    private fun parseQuiz(json: JSONObject, fallbackCourseId: String): Quiz {
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
            questions = questions,
            courseId = json.optString("courseId", fallbackCourseId).ifBlank { fallbackCourseId },
            chapterId = json.optString("chapterId").takeIf { it.isNotBlank() },
            shuffleQuestions = json.optBoolean("shuffleQuestions", false),
            shuffleAnswers = json.optBoolean("shuffleAnswers", false)
        )
    }

    /** سؤال nested تمام metadataهای فعلی Quiz Engine را حفظ می‌کند. */
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
                    QuizAnswer(
                        id = answer.getString("id"),
                        text = answer.getString("text"),
                        isCorrect = answer.optBoolean("isCorrect", false),
                        matchKey = answer.optString("matchKey").takeIf { it.isNotBlank() },
                        order = answer.takeIf { it.has("order") && !it.isNull("order") }?.optInt("order")
                    )
                }
            },
            points = question.optInt("points", 1).coerceAtLeast(1),
            codeLanguage = question.optString("codeLanguage").takeIf { it.isNotBlank() }
        )
    }

    /** Schema بسیار قدیمی options/correctIndex به یک سؤال استاندارد تبدیل می‌شود. */
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

    /** Project جدید milestone object دارد؛ steps/deliverables قدیمی نیز به milestone پایدار تبدیل می‌شوند. */
    private fun parseProject(json: JSONObject, fallbackCourseId: String): LearningProject {
        val projectId = json.getString("id")
        val milestonesJson = json.optJSONArray("milestones")
        val milestones = if (milestonesJson != null) {
            (0 until milestonesJson.length()).map { index ->
                val milestone = milestonesJson.getJSONObject(index)
                ProjectMilestone(
                    id = milestone.optString("id").ifBlank { "$projectId-step-${index + 1}" },
                    title = milestone.optString("title", "مرحله ${index + 1}"),
                    description = milestone.optString("description"),
                    order = milestone.optInt("order", index),
                    acceptanceCriteria = milestone.optJSONArray("acceptanceCriteria").toStringList()
                        .ifEmpty { milestone.optJSONArray("acceptance").toStringList() }
                )
            }
        } else {
            val legacySteps = json.optJSONArray("steps").toStringList()
                .ifEmpty { json.optJSONArray("deliverables").toStringList() }
            legacySteps.mapIndexed { index, step ->
                ProjectMilestone(
                    id = "$projectId-step-${index + 1}",
                    title = "مرحله ${index + 1}",
                    description = step,
                    order = index
                )
            }
        }

        return LearningProject(
            id = projectId,
            courseId = json.optString("courseId", fallbackCourseId).ifBlank { fallbackCourseId },
            title = json.getString("title"),
            description = json.optString("description", json.optString("summary")),
            difficulty = json.optString("difficulty", "BEGINNER"),
            estimatedMinutes = json.optInt("estimatedMinutes", 60).coerceAtLeast(1),
            relatedLessonIds = json.optJSONArray("relatedLessonIds").toStringList(),
            milestones = milestones,
            starterAssetId = json.optString("starterAssetId").takeIf { it.isNotBlank() },
            solutionAssetId = json.optString("solutionAssetId").takeIf { it.isNotBlank() },
            tags = json.optJSONArray("tags").toStringList().toSet()
        )
    }

    /** Glossary schema قدیمی translation/related را نیز به aliases/relatedLessonIds فعلی منتقل می‌کند. */
    private fun parseGlossary(json: JSONObject, fallbackCourseId: String): GlossaryEntry {
        val term = json.getString("term")
        val legacyTranslation = json.optString("translation", json.optString("fa")).takeIf { it.isNotBlank() }
        val aliases = json.optJSONArray("aliases").toStringList().toMutableList().apply {
            legacyTranslation?.takeIf { it !in this }?.let(::add)
        }
        val relatedLessonIds = json.optJSONArray("relatedLessonIds").toStringList()
            .ifEmpty { json.optJSONArray("related").toStringList() }

        return GlossaryEntry(
            id = json.optString("id").takeIf { it.isNotBlank() } ?: term.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
            courseId = json.optString("courseId", fallbackCourseId).ifBlank { fallbackCourseId },
            term = term,
            definition = json.getString("definition"),
            aliases = aliases,
            relatedLessonIds = relatedLessonIds,
            tags = json.optJSONArray("tags").toStringList().toSet()
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String, fallback: T): T =
        runCatching { enumValueOf<T>(raw.trim().uppercase()) }.getOrDefault(fallback)

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { optString(it) }
}
