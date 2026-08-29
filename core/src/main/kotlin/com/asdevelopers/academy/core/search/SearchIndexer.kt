package com.asdevelopers.academy.core.search

import com.asdevelopers.academy.core.database.SearchDao
import com.asdevelopers.academy.core.database.SearchIndexEntity
import com.asdevelopers.academy.course.model.CoursePackage

/** ایندکس جستجو از Course Package ساخته می‌شود و بعد از Content Update قابل بازسازی است. */
class SearchIndexer(private val dao: SearchDao) {
    suspend fun rebuild(course: CoursePackage) {
        val items = course.lessons.map { lesson ->
            SearchIndexEntity(
                refId = lesson.id,
                refType = "LESSON",
                title = lesson.title,
                body = buildString {
                    append(lesson.summary)
                    append(' ')
                    lesson.blocks.forEach { append(it.content).append(' ') }
                }
            )
        }
        dao.clear()
        dao.insertAll(items)
    }
}
