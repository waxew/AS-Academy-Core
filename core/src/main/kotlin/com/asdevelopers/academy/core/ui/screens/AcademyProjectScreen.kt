package com.asdevelopers.academy.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.project.LearningProject
import com.asdevelopers.academy.core.project.ProjectProgress
import com.asdevelopers.academy.core.project.ProjectMilestone

/**
 * Workflow عمومی پروژه‌های عملی AS Academy.
 * Course فقط متن Milestoneها را تعریف می‌کند و Core وضعیت Checklist و Draft را مدیریت می‌کند.
 */
@Composable
fun AcademyProjectScreen(
    project: LearningProject,
    progress: ProjectProgress? = null,
    modifier: Modifier = Modifier,
    onProgressChanged: (ProjectProgress) -> Unit = {}
) {
    // Completed milestoneها از Progress ذخیره‌شده بازیابی می‌شوند.
    var completedIds by remember(project.id, progress?.updatedAtEpochMillis) {
        mutableStateOf(progress?.completedMilestoneIds.orEmpty())
    }
    // Draft فضای یادداشت/پاسخ پروژه است و بین اجراها توسط Repository قابل ذخیره است.
    var draft by remember(project.id, progress?.updatedAtEpochMillis) {
        mutableStateOf(progress?.draft.orEmpty())
    }

    /** هر تغییر UI را به یک ProjectProgress معتبر و قابل ذخیره تبدیل می‌کند. */
    fun publishProgress(nextCompletedIds: Set<String>, nextDraft: String) {
        // Timestamp زمان آخرین تغییر برای Resume و Conflict resolution محلی لازم است.
        val now = System.currentTimeMillis()
        // پروژه زمانی کامل است که همه Milestoneها تیک خورده باشند.
        val allCompleted = project.milestones.isNotEmpty() &&
            project.milestones.all { it.id in nextCompletedIds }
        // Completion timestamp قبلی در صورت تکمیل قبلی حفظ می‌شود.
        val completedAt = if (allCompleted) progress?.completedAtEpochMillis ?: now else null
        // Callback مدل کامل را تحویل Repository یا Host می‌دهد.
        onProgressChanged(
            ProjectProgress(
                courseId = project.courseId,
                projectId = project.id,
                completedMilestoneIds = nextCompletedIds,
                draft = nextDraft,
                updatedAtEpochMillis = now,
                completedAtEpochMillis = completedAt
            )
        )
    }

    // Scroll اجازه می‌دهد Milestone و Acceptance Criteria طولانی بدون بریدگی نمایش داده شوند.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // مشخصات اصلی پروژه از Course Package می‌آید.
        Text(project.title, style = MaterialTheme.typography.headlineMedium)
        Text("سطح: ${project.difficulty} • زمان تقریبی: ${project.estimatedMinutes} دقیقه")
        Text(project.description, style = MaterialTheme.typography.bodyLarge)

        // Milestoneها با order مرتب می‌شوند تا ترتیب JSON یا Serialization نتیجه را عوض نکند.
        project.milestones.sortedBy(ProjectMilestone::order).forEach { milestone ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Checkbox وضعیت تکمیل مرحله را کنترل می‌کند.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = milestone.id in completedIds,
                            onCheckedChange = { checked ->
                                val next = completedIds.toMutableSet()
                                if (checked) next += milestone.id else next -= milestone.id
                                completedIds = next
                                publishProgress(next, draft)
                            }
                        )
                        Text(milestone.title, style = MaterialTheme.typography.titleMedium)
                    }
                    // Description توضیح می‌دهد کاربر در این مرحله چه کاری باید انجام دهد.
                    Text(milestone.description)
                    // Acceptance Criteria معیار Done مرحله را قابل مشاهده و قابل بررسی می‌کند.
                    milestone.acceptanceCriteria.forEach { criterion ->
                        Text("• $criterion", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Draft برای نوشتن خروجی، تصمیم‌ها یا لینک تحویل پروژه استفاده می‌شود.
        OutlinedTextField(
            value = draft,
            onValueChange = { value ->
                draft = value
                publishProgress(completedIds, value)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("یادداشت و خروجی پروژه") },
            minLines = 6
        )

        // Summary با تعداد Milestone تکمیل‌شده وضعیت فعلی را روشن می‌کند.
        Text("پیشرفت مراحل: ${completedIds.size} از ${project.milestones.size}")
        if (project.milestones.isNotEmpty() && project.milestones.all { it.id in completedIds }) {
            Text("پروژه تکمیل شده است.", style = MaterialTheme.typography.titleMedium)
        }
    }
}
