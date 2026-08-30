package com.asdevelopers.academy.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.progress.CourseOutline
import com.asdevelopers.academy.core.progress.CourseOutlineChapter
import com.asdevelopers.academy.core.progress.CourseOutlineLesson
import com.asdevelopers.academy.core.progress.CourseOutlineLevel
import com.asdevelopers.academy.core.progress.LessonStatus

/**
 * Home/Outline عمومی AS Academy برای دوره‌های بزرگ.
 * Course Host فقط مدل CourseOutline و callbackها را می‌دهد؛ گروه‌بندی Level/Chapter/Lesson در Courseها تکرار نمی‌شود.
 */
@Composable
fun AcademyCourseHomeScreen(
    outline: CourseOutline,
    modifier: Modifier = Modifier,
    onLessonClick: (String) -> Unit,
    onContinueClick: (String) -> Unit = onLessonClick,
    onPlacementClick: (() -> Unit)? = null,
    onWeakTopicReviewClick: (() -> Unit)? = null,
    onFlashcardReviewClick: (() -> Unit)? = null
) {
    // اولین Level باز به‌صورت پیش‌فرض Expand می‌شود؛ بقیه برای جلوگیری از Scroll بسیار طولانی بسته می‌مانند.
    var expandedLevelIds by remember(outline.courseId, outline.levels.map { it.id }) {
        mutableStateOf(outline.levels.firstOrNull { it.isUnlocked }?.let(::setOf) ?: emptySet())
    }
    // Chapterها در شروع بسته‌اند و کاربر فقط بخش مورد نیاز را باز می‌کند.
    var expandedChapterIds by remember(outline.courseId) { mutableStateOf(emptySet<String>()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "course-summary") {
            CourseSummaryCard(
                outline = outline,
                onContinueClick = onContinueClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (onPlacementClick != null || onWeakTopicReviewClick != null || onFlashcardReviewClick != null) {
            item(key = "adaptive-actions") {
                AdaptiveActionsCard(
                    onPlacementClick = onPlacementClick,
                    onWeakTopicReviewClick = onWeakTopicReviewClick,
                    onFlashcardReviewClick = onFlashcardReviewClick,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        items(outline.levels, key = CourseOutlineLevel::id) { level ->
            val expanded = level.id in expandedLevelIds
            LevelCard(
                level = level,
                expanded = expanded,
                expandedChapterIds = expandedChapterIds,
                onToggleLevel = {
                    if (level.isUnlocked) {
                        expandedLevelIds = expandedLevelIds.toggle(level.id)
                    }
                },
                onToggleChapter = { chapterId ->
                    expandedChapterIds = expandedChapterIds.toggle(chapterId)
                },
                onLessonClick = onLessonClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "course-bottom-space") {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {}
        }
    }
}

/** Summary بالای Home درصد کل Course و مقصد Continue Learning را نمایش می‌دهد. */
@Composable
private fun CourseSummaryCard(
    outline: CourseOutline,
    onContinueClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(outline.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "${outline.completedLessons} از ${outline.totalLessons} درس تکمیل شده",
                style = MaterialTheme.typography.bodyLarge
            )
            LinearProgressIndicator(
                progress = { outline.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("پیشرفت کل: ${outline.progressPercent}٪", style = MaterialTheme.typography.labelLarge)
            outline.nextLessonId?.let { lessonId ->
                Button(
                    onClick = { onContinueClick(lessonId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (outline.progressPercent == 0) "شروع یادگیری" else "ادامه یادگیری")
                }
            }
        }
    }
}

/** ابزارهای یادگیری تطبیقی در یک Card ثابت قرار می‌گیرند و فقط در صورت اتصال Host نمایش داده می‌شوند. */
@Composable
private fun AdaptiveActionsCard(
    onPlacementClick: (() -> Unit)?,
    onWeakTopicReviewClick: (() -> Unit)?,
    onFlashcardReviewClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("مسیر هوشمند یادگیری", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "سطح شروع، نقاط ضعف و مرور فاصله‌دار را از داده واقعی یادگیری مدیریت کنید.",
                style = MaterialTheme.typography.bodyMedium
            )
            onPlacementClick?.let { action ->
                Button(onClick = action, modifier = Modifier.fillMaxWidth()) {
                    Text("آزمون تعیین سطح")
                }
            }
            onWeakTopicReviewClick?.let { action ->
                OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
                    Text("مرور نقاط ضعف")
                }
            }
            onFlashcardReviewClick?.let { action ->
                OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
                    Text("فلش‌کارت و مرور فاصله‌دار")
                }
            }
        }
    }
}

/** Level Card وضعیت قفل، درصد و Chapterهای همان Level را نشان می‌دهد. */
@Composable
private fun LevelCard(
    level: CourseOutlineLevel,
    expanded: Boolean,
    expandedChapterIds: Set<String>,
    onToggleLevel: () -> Unit,
    onToggleChapter: (String) -> Unit,
    onLessonClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(level.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (level.description.isNotBlank()) {
                        Text(level.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    if (level.isUnlocked) "${level.progressPercent}٪" else "قفل",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            LinearProgressIndicator(
                progress = { level.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${level.completedLessons}/${level.totalLessons} درس",
                style = MaterialTheme.typography.labelMedium
            )

            OutlinedButton(
                onClick = onToggleLevel,
                enabled = level.isUnlocked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        !level.isUnlocked -> "این سطح هنوز باز نشده است"
                        expanded -> "بستن سرفصل‌های سطح"
                        else -> "نمایش سرفصل‌های سطح"
                    }
                )
            }

            if (expanded && level.isUnlocked) {
                level.chapters.forEach { chapter ->
                    ChapterSection(
                        chapter = chapter,
                        expanded = chapter.id in expandedChapterIds,
                        onToggle = { onToggleChapter(chapter.id) },
                        onLessonClick = onLessonClick
                    )
                }
            }
        }
    }
}

/** Chapter فقط هنگام Expand شدن Lessonها را می‌سازد تا Home دوره‌های بزرگ سبک‌تر بماند. */
@Composable
private fun ChapterSection(
    chapter: CourseOutlineChapter,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLessonClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(chapter.title, fontWeight = FontWeight.SemiBold)
                Text("${chapter.completedLessons}/${chapter.totalLessons} • ${chapter.progressPercent}٪")
            }
        }
        if (expanded) {
            if (chapter.description.isNotBlank()) {
                Text(chapter.description, style = MaterialTheme.typography.bodySmall)
            }
            chapter.lessons.forEach { lesson ->
                LessonRow(lesson = lesson, onClick = { onLessonClick(lesson.id) })
            }
        }
    }
}

/** Lesson Row زمان، وضعیت و Progress را در یک Card قابل لمس نمایش می‌دهد. */
@Composable
private fun LessonRow(
    lesson: CourseOutlineLesson,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (lesson.summary.isNotBlank()) {
                Text(lesson.summary, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "${lessonStatusLabel(lesson.status)} • ${lesson.estimatedMinutes} دقیقه",
                style = MaterialTheme.typography.labelMedium
            )
            if (lesson.progressPercent > 0) {
                LinearProgressIndicator(
                    progress = { lesson.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(if (lesson.status == LessonStatus.NOT_STARTED) "شروع درس" else "باز کردن درس")
            }
        }
    }
}

private fun lessonStatusLabel(status: LessonStatus): String = when (status) {
    LessonStatus.NOT_STARTED -> "شروع نشده"
    LessonStatus.IN_PROGRESS -> "در حال یادگیری"
    LessonStatus.COMPLETED -> "تکمیل‌شده"
    LessonStatus.NEEDS_REVIEW -> "نیازمند مرور"
}

private fun Set<String>.toggle(id: String): Set<String> =
    if (id in this) this - id else this + id
