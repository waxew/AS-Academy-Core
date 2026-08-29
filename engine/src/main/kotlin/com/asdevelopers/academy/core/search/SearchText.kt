package com.asdevelopers.academy.core.search

import com.asdevelopers.academy.core.content.CourseBundle

/** سند مستقل از Room که بعداً به SearchIndexEntity تبدیل می‌شود. */
data class SearchDocument(
    val courseId: String,
    val refId: String,
    val refType: String,
    val title: String,
    val body: String
)

/** همه اپ‌ها یک شیوه یکسان برای ساخت ایندکس درس و واژه‌نامه دارند. */
object SearchDocumentFactory {
    fun from(bundle: CourseBundle): List<SearchDocument> {
        val lessonDocuments = bundle.lessons.map { lesson ->
            // محتوای Blockها برای جست‌وجوی Offline در یک متن قابل ایندکس ادغام می‌شود.
            val body = buildString {
                appendLine(lesson.summary)
                lesson.blocks.forEach { appendLine(it.content) }
                append(lesson.tags.joinToString(" "))
            }
            SearchDocument(bundle.manifest.courseId, lesson.id, "lesson", lesson.title, body)
        }
        val glossaryDocuments = bundle.glossary.map { entry ->
            SearchDocument(
                courseId = bundle.manifest.courseId,
                refId = entry.id,
                refType = "glossary",
                title = entry.term,
                body = entry.definition + " " + entry.aliases.joinToString(" ")
            )
        }
        return lessonDocuments + glossaryDocuments
    }
}

/** Query کاربر برای استفاده ایمن در Room FTS به Tokenهای قابل پیش‌بینی تبدیل می‌شود. */
object FtsQueryBuilder {
    fun build(rawQuery: String): String = rawQuery
        .trim()
        .split(Regex("\\s+"))
        .map { token -> token.filter { it.isLetterOrDigit() || it == '_' || it == '-' } }
        .filter { it.isNotBlank() }
        .joinToString(" AND ") { "\"$it\"*" }
}
