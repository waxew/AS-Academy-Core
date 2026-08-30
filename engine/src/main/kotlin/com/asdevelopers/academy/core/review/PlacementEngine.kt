package com.asdevelopers.academy.core.review

import com.asdevelopers.academy.course.model.CourseLevelType
import kotlinx.serialization.Serializable

/** یک بازه نمره و سطح شروع متناظر؛ Course می‌تواند Policy خود را بدون تغییر Engine بسازد. */
@Serializable
data class PlacementBand(
    val minimumScorePercent: Int,
    val levelType: CourseLevelType,
    val title: String,
    val reviewWeakTopics: Boolean = true,
    val fastTrack: Boolean = false
) {
    init {
        require(minimumScorePercent in 0..100) { "minimumScorePercent must be between 0 and 100" }
        require(title.isNotBlank()) { "placement band title cannot be blank" }
    }
}

/** Policy تعیین سطح؛ بازه صفر الزامی است تا برای هر نمره نتیجه قطعی وجود داشته باشد. */
@Serializable
data class PlacementPolicy(val bands: List<PlacementBand>) {
    init {
        require(bands.isNotEmpty()) { "placement policy needs at least one band" }
        require(bands.any { it.minimumScorePercent == 0 }) { "placement policy must include a zero-score band" }
        require(bands.map { it.minimumScorePercent }.distinct().size == bands.size) {
            "placement bands cannot have duplicate minimum scores"
        }
    }
}

/** خروجی آماده UI که علاوه بر سطح شروع، نیاز به مرور یا Fast Track را روشن می‌کند. */
@Serializable
data class PlacementRecommendation(
    val scorePercent: Int,
    val levelType: CourseLevelType,
    val title: String,
    val reviewWeakTopics: Boolean,
    val fastTrack: Boolean
)

/** منطق مشترک تبدیل نمره Placement Test به سطح شروع. */
object PlacementEngine {
    fun recommend(scorePercent: Int, policy: PlacementPolicy): PlacementRecommendation {
        require(scorePercent in 0..100) { "scorePercent must be between 0 and 100" }
        val band = policy.bands
            .filter { scorePercent >= it.minimumScorePercent }
            .maxBy { it.minimumScorePercent }
        return PlacementRecommendation(
            scorePercent = scorePercent,
            levelType = band.levelType,
            title = band.title,
            reviewWeakTopics = band.reviewWeakTopics,
            fastTrack = band.fastTrack
        )
    }

    /**
     * Policy استاندارد مسیر چهارسطحی AS Academy.
     * Course می‌تواند در صورت نیاز Policy دیگری تزریق کند و این مقادیر در Engine Hard-code مصرف اجباری ندارند.
     */
    fun fourLevelPolicy(): PlacementPolicy = PlacementPolicy(
        listOf(
            PlacementBand(0, CourseLevelType.FUNDAMENTALS, "مبانی", reviewWeakTopics = false),
            PlacementBand(35, CourseLevelType.BEGINNER, "مقدماتی"),
            PlacementBand(55, CourseLevelType.ADVANCED, "پیشرفته"),
            PlacementBand(75, CourseLevelType.SPECIALIST, "تخصصی و بازار کار"),
            PlacementBand(90, CourseLevelType.SPECIALIST, "مسیر فشرده تخصصی", fastTrack = true)
        )
    )
}
