package com.asdevelopers.academy.core.achievement

import kotlinx.serialization.Serializable

/** نوع رویدادهایی که موتور Achievement دریافت می‌کند. */
@Serializable
enum class LearningEventType {
    LESSON_COMPLETED,
    QUIZ_PASSED,
    EXERCISE_COMPLETED,
    PROJECT_COMPLETED,
    STUDY_DAY
}

/** رویداد مستقل از UI که توسط Repositoryها ثبت می‌شود. */
@Serializable
data class LearningEvent(
    val type: LearningEventType,
    val courseId: String,
    val targetId: String,
    val occurredAtEpochMillis: Long
)

/** تعریف نشان مشترک؛ دوره‌ها فقط می‌توانند عنوان و تصویر اختصاصی ارائه دهند. */
@Serializable
data class AchievementRule(
    val id: String,
    val title: String,
    val description: String,
    val eventType: LearningEventType,
    val requiredCount: Int
) {
    init {
        // نشان بدون هدف مثبت هیچ معنای آموزشی ندارد.
        require(requiredCount > 0) { "requiredCount must be positive" }
    }
}

/** موتور نشان‌ها تعداد رویدادهای مرتبط را با Rule مقایسه می‌کند. */
object AchievementEngine {
    fun unlockedRules(events: List<LearningEvent>, rules: List<AchievementRule>): Set<String> =
        rules.filter { rule -> events.count { it.type == rule.eventType } >= rule.requiredCount }
            .mapTo(mutableSetOf()) { it.id }
}
