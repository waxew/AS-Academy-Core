package com.asdevelopers.academy.core.content

import android.content.res.AssetManager
import com.asdevelopers.academy.course.model.Chapter
import com.asdevelopers.academy.course.model.CourseCapabilities
import com.asdevelopers.academy.course.model.CourseLevel
import com.asdevelopers.academy.course.model.CourseLevelType
import com.asdevelopers.academy.course.model.CourseManifest
import com.asdevelopers.academy.course.model.CoursePackage
import com.asdevelopers.academy.course.model.Lesson
import com.asdevelopers.academy.course.model.LessonBlock
import com.asdevelopers.academy.course.model.LessonBlockType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loader عمومی Course Package از Android assets.
 * Courseها فقط JSON مطابق Contract Core می‌سازند و Parser را تکرار نمی‌کنند.
 */
class AssetCoursePackageLoader(private val assets: AssetManager) {

    suspend fun load(courseId: String): CoursePackage = withContext(Dispatchers.IO) {
        val root = "course/$courseId"
        val manifest = parseManifest(readObject("$root/manifest.json"))
        val levels = parseLevels(readArray("$root/levels.json"))
        val chapters = parseChapters(readArray("$root/chapters.json"))
        val lessonFiles = assets.list("$root/lessons").orEmpty().filter { it.endsWith(".json") }.sorted()
        val lessons = lessonFiles.map { parseLesson(readObject("$root/lessons/$it")) }

        val result = CoursePackage(manifest, levels, chapters, lessons)
        val validation = CoursePackageValidator().validate(manifest, levels, chapters, lessons)
        require(validation.isValid) { "Invalid Course Package: ${validation.errors.joinToString()}" }
        result
    }

    private fun readText(path: String): String = assets.open(path).bufferedReader().use { it.readText() }
    private fun readObject(path: String) = JSONObject(readText(path))
    private fun readArray(path: String) = JSONArray(readText(path))

    private fun parseManifest(json: JSONObject): CourseManifest {
        val caps = json.optJSONObject("capabilities") ?: JSONObject()
        return CourseManifest(
            courseId = json.getString("courseId"),
            titleFa = json.getString("titleFa"),
            titleEn = json.getString("titleEn"),
            version = json.getString("version"),
            contentSchemaVersion = json.getInt("contentSchemaVersion"),
            minimumCoreVersion = json.getString("minimumCoreVersion"),
            rtl = json.optBoolean("rtl", true),
            capabilities = CourseCapabilities(
                codeRunner = caps.optBoolean("codeRunner", false),
                terminalExamples = caps.optBoolean("terminalExamples", false),
                diagrams = caps.optBoolean("diagrams", false),
                quizzes = caps.optBoolean("quizzes", true),
                exercises = caps.optBoolean("exercises", true),
                projects = caps.optBoolean("projects", true),
                glossary = caps.optBoolean("glossary", true)
            )
        )
    }

    private fun parseLevels(array: JSONArray): List<CourseLevel> = (0 until array.length()).map { index ->
        array.getJSONObject(index).let { json ->
            CourseLevel(
                id = json.getString("id"),
                courseId = json.getString("courseId"),
                type = CourseLevelType.valueOf(json.getString("type")),
                title = json.getString("title"),
                order = json.getInt("order")
            )
        }
    }

    private fun parseChapters(array: JSONArray): List<Chapter> = (0 until array.length()).map { index ->
        array.getJSONObject(index).let { json ->
            Chapter(
                id = json.getString("id"),
                levelId = json.getString("levelId"),
                title = json.getString("title"),
                description = json.optString("description"),
                order = json.getInt("order")
            )
        }
    }

    private fun parseLesson(json: JSONObject): Lesson {
        val blocksJson = json.getJSONArray("blocks")
        val blocks = (0 until blocksJson.length()).map { index -> parseBlock(blocksJson.getJSONObject(index)) }
        return Lesson(
            id = json.getString("id"),
            chapterId = json.getString("chapterId"),
            title = json.getString("title"),
            summary = json.optString("summary"),
            order = json.getInt("order"),
            estimatedMinutes = json.optInt("estimatedMinutes", 10),
            blocks = blocks
        )
    }

    private fun parseBlock(json: JSONObject): LessonBlock {
        val metadataJson = json.optJSONObject("metadata")
        val metadata = buildMap {
            metadataJson?.keys()?.forEach { key -> put(key, metadataJson.optString(key)) }
        }
        return LessonBlock(
            id = json.getString("id"),
            type = LessonBlockType.valueOf(json.getString("type")),
            content = json.optString("content"),
            metadata = metadata
        )
    }
}
