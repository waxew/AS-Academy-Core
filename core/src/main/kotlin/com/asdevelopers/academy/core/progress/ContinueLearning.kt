package com.asdevelopers.academy.core.progress

import com.asdevelopers.academy.course.model.CoursePackage

/** موتور مشترک انتخاب مقصد «ادامه یادگیری» برای همه دوره‌ها. */
object ContinueLearning {
    fun select(course: CoursePackage, progress: Collection<LessonProgress>): String? {
        val byId = progress.associateBy { it.lessonId }
        val recent = progress.filter { it.status == LessonStatus.IN_PROGRESS }
            .maxByOrNull { it.lastOpenedAtEpochMillis ?: 0L }?.lessonId
        if (recent != null && course.lesson(recent) != null) return recent
        return course.lessons.firstOrNull { byId[it.id]?.status != LessonStatus.COMPLETED }?.id
    }
}
