package com.asdevelopers.academy.core

import com.asdevelopers.academy.core.content.CourseBundle
import com.asdevelopers.academy.core.exercise.Exercise
import com.asdevelopers.academy.core.exercise.ExerciseDifficulty
import com.asdevelopers.academy.core.exercise.ExerciseType
import com.asdevelopers.academy.core.project.LearningProject
import com.asdevelopers.academy.core.project.ProjectMilestone
import com.asdevelopers.academy.core.quiz.QuestionType
import com.asdevelopers.academy.core.quiz.Quiz
import com.asdevelopers.academy.core.quiz.QuizAnswer
import com.asdevelopers.academy.core.quiz.QuizQuestion
import com.asdevelopers.academy.course.model.Chapter
import com.asdevelopers.academy.course.model.CourseBranding
import com.asdevelopers.academy.course.model.CourseCapabilities
import com.asdevelopers.academy.course.model.CourseLevel
import com.asdevelopers.academy.course.model.CourseLevelType
import com.asdevelopers.academy.course.model.CourseManifest
import com.asdevelopers.academy.course.model.Lesson
import com.asdevelopers.academy.course.model.LessonBlock
import com.asdevelopers.academy.course.model.LessonBlockType

/** Factory تست، Bundle معتبر واحدی می‌سازد تا هر تست فقط بخش موردنظر را تغییر دهد. */
fun validCourseBundle(): CourseBundle {
    val manifest = CourseManifest(
        courseId = "sample",
        titleFa = "دوره نمونه",
        titleEn = "Sample Course",
        version = "1.0.0",
        contentSchemaVersion = 1,
        minimumCoreVersion = "1.0.0",
        rtl = true,
        capabilities = CourseCapabilities()
    )
    val level = CourseLevel("sample-fundamentals", "sample", CourseLevelType.FUNDAMENTALS, "مبانی", 0)
    val chapter = Chapter("sample-introduction", level.id, "مقدمه", "شروع", 0)
    val lesson = Lesson(
        id = "sample-lesson-001",
        chapterId = chapter.id,
        title = "درس نمونه",
        summary = "خلاصه",
        order = 0,
        estimatedMinutes = 10,
        blocks = listOf(LessonBlock("sample-block-001", LessonBlockType.PARAGRAPH, "محتوا"))
    )
    val quiz = Quiz(
        id = "sample-quiz-001",
        lessonId = lesson.id,
        title = "آزمون",
        passingScorePercent = 70,
        questions = listOf(
            QuizQuestion(
                id = "sample-question-001",
                type = QuestionType.MULTIPLE_CHOICE,
                question = "پاسخ؟",
                explanation = "توضیح",
                tags = setOf("basics"),
                answers = listOf(
                    QuizAnswer("sample-answer-001", "درست", true),
                    QuizAnswer("sample-answer-002", "غلط", false)
                )
            )
        ),
        courseId = "sample"
    )
    val exercise = Exercise(
        id = "sample-exercise-001",
        lessonId = lesson.id,
        title = "تمرین",
        description = "پاسخ دهید",
        type = ExerciseType.READ_AND_ANSWER,
        difficulty = ExerciseDifficulty.EASY,
        courseId = "sample"
    )
    val project = LearningProject(
        id = "sample-project-001",
        courseId = "sample",
        title = "پروژه",
        description = "پروژه نمونه",
        difficulty = "EASY",
        estimatedMinutes = 30,
        relatedLessonIds = listOf(lesson.id),
        milestones = listOf(ProjectMilestone("sample-step-001", "مرحله", "توضیح", 0))
    )
    return CourseBundle(
        manifest = manifest,
        branding = CourseBranding("#6750A4", "#625B71", "#7D5260"),
        levels = listOf(level),
        chapters = listOf(chapter),
        lessons = listOf(lesson),
        quizzes = listOf(quiz),
        exercises = listOf(exercise),
        projects = listOf(project)
    )
}
