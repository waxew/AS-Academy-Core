package com.asdevelopers.academy.core.repository

import com.asdevelopers.academy.core.content.CourseBundle
import com.asdevelopers.academy.core.database.QuizResultDao
import com.asdevelopers.academy.core.database.QuizResultEntity
import com.asdevelopers.academy.core.review.LessonReviewRecommendation
import com.asdevelopers.academy.core.review.PlacementEngine
import com.asdevelopers.academy.core.review.PlacementPolicy
import com.asdevelopers.academy.core.review.PlacementRecommendation
import com.asdevelopers.academy.core.review.WeakTopicReviewEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** نتیجه Persist شده Placement که UI به آن نیاز دارد؛ Entity دیتابیس از API Host پنهان می‌ماند. */
data class PlacementReviewState(
    val recommendation: PlacementRecommendation,
    val weakTags: Set<String>,
    val completedAtEpochMillis: Long
)

/**
 * Repository عمومی مرور نقاط ضعف.
 * تاریخچه Quiz را به تفکیک Course می‌خواند و Engine مشترک آن را به درس‌های واقعی همان Bundle نگاشت می‌کند.
 */
class WeakTopicReviewRepository(private val dao: QuizResultDao) {
    fun observeRecommendations(
        bundle: CourseBundle,
        limit: Int = 10
    ): Flow<List<LessonReviewRecommendation>> {
        require(limit > 0) { "limit must be positive" }
        val courseId = bundle.manifest.courseId
        require(courseId.isNotBlank()) { "courseId is required for weak-topic review" }

        return dao.observeCourse(courseId).map { attempts ->
            WeakTopicReviewEngine.recommendLessonsFromWeakTags(
                bundle = bundle,
                weakTagSets = attempts.map(QuizResultEntity::weakTagSet),
                limit = limit
            )
        }
    }
}

/**
 * Repository عمومی نتیجه Placement.
 * نتیجه آخر آزمون را از Room می‌گیرد تا Rotation یا Restart صفحه نتیجه را از بین نبرد.
 */
class PlacementResultRepository(private val dao: QuizResultDao) {
    fun observeLatest(
        courseId: String,
        placementQuizId: String,
        policy: PlacementPolicy
    ): Flow<PlacementReviewState?> {
        require(courseId.isNotBlank()) { "courseId is required for placement result" }
        require(placementQuizId.isNotBlank()) { "placementQuizId is required" }

        return dao.observeLatest(courseId, placementQuizId).map { attempt ->
            attempt?.let {
                PlacementReviewState(
                    recommendation = PlacementEngine.recommend(it.scorePercent, policy),
                    weakTags = it.weakTagSet(),
                    completedAtEpochMillis = it.completedAt
                )
            }
        }
    }
}

/** Weak Tags در Room به صورت رشته پایدار Pipe-separated ذخیره می‌شوند. */
private fun QuizResultEntity.weakTagSet(): Set<String> = weakTags
    .split(TAG_SEPARATOR)
    .map(String::trim)
    .filter(String::isNotBlank)
    .toSet()

private const val TAG_SEPARATOR = "|"
