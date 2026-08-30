package com.asdevelopers.academy.core.content

import com.asdevelopers.academy.core.exercise.Exercise
import com.asdevelopers.academy.core.project.LearningProject
import com.asdevelopers.academy.core.quiz.Quiz
import com.asdevelopers.academy.course.model.Chapter
import com.asdevelopers.academy.course.model.CourseAsset
import com.asdevelopers.academy.course.model.CourseBranding
import com.asdevelopers.academy.course.model.CourseLevel
import com.asdevelopers.academy.course.model.CourseManifest
import com.asdevelopers.academy.course.model.CourseReference
import com.asdevelopers.academy.course.model.Flashcard
import com.asdevelopers.academy.course.model.GlossaryEntry
import com.asdevelopers.academy.course.model.Lesson
import kotlinx.serialization.Serializable

/**
 * فایل منطقی کامل یک دوره که می‌تواند از Asset داخلی یا بسته دانلودشده خوانده شود.
 * همه لیست‌ها مقدار پیش‌فرض دارند تا افزودن قابلیت جدید، Package قدیمی را خراب نکند.
 */
@Serializable
data class CourseBundle(
    val manifest: CourseManifest,
    val branding: CourseBranding,
    val levels: List<CourseLevel>,
    val chapters: List<Chapter>,
    val lessons: List<Lesson>,
    val quizzes: List<Quiz> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val projects: List<LearningProject> = emptyList(),
    val glossary: List<GlossaryEntry> = emptyList(),
    val assets: List<CourseAsset> = emptyList(),
    val references: List<CourseReference> = emptyList(),
    /**
     * در انتهای قرارداد و با مقدار پیش‌فرض افزوده شده تا Bundleهای قدیمی و مصرف‌کننده‌های positional قبلی نشکنند.
     */
    val flashcards: List<Flashcard> = emptyList()
)
