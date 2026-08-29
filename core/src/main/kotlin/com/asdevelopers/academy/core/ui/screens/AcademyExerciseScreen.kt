package com.asdevelopers.academy.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.exercise.Exercise

/**
 * UI عمومی Exercise برای پاسخ متنی و کدی.
 * Runner یا ارزیابی خودکار زبان‌محور از طریق Callback/Adapter Course تزریق می‌شود و در Core Hard-code نمی‌شود.
 */
@Composable
fun AcademyExerciseScreen(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    initialAnswer: String = "",
    onDraftChanged: (String) -> Unit = {},
    onCompleted: (String) -> Unit = {}
) {
    // اگر Draft قبلی وجود نداشته باشد Starter Code نقطه شروع تمرین‌های کدی است.
    var answer by remember(exercise.id, initialAnswer) {
        mutableStateOf(initialAnswer.ifBlank { exercise.starterCode.orEmpty() })
    }
    // Hintها فقط با درخواست کاربر نمایش داده می‌شوند تا Challenge حفظ شود.
    var visibleHints by remember(exercise.id) { mutableStateOf(0) }
    // Solution به‌صورت پیش‌فرض مخفی است و دیدن آن تصمیم آگاهانه کاربر است.
    var showSolution by remember(exercise.id) { mutableStateOf(false) }

    // Scroll عمودی برای تمرین‌های طولانی و کد چندخطی ضروری است.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // عنوان و Difficulty از مدل مشترک Exercise خوانده می‌شوند.
        Text(exercise.title, style = MaterialTheme.typography.headlineMedium)
        Text("سطح: ${exercise.difficulty.name}")
        Text(exercise.description, style = MaterialTheme.typography.bodyLarge)

        // یک TextArea عمومی هم پاسخ توضیحی و هم پاسخ کدی را نگه می‌دارد.
        OutlinedTextField(
            value = answer,
            onValueChange = { value ->
                // State محلی برای نمایش فوری به‌روزرسانی می‌شود.
                answer = value
                // Host می‌تواند Callback را به ExerciseDraftRepository وصل کند.
                onDraftChanged(value)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("پاسخ / پیش‌نویس") },
            minLines = 8
        )

        // Expected Output در تمرین‌هایی که Contract آن را مشخص کرده به‌عنوان معیار قابل مشاهده است.
        exercise.expectedOutput?.takeIf(String::isNotBlank)?.let { expected ->
            Text("خروجی مورد انتظار:")
            Text(expected, style = MaterialTheme.typography.bodyMedium)
        }

        // هر بار فشار Button فقط یک Hint جدید آشکار می‌کند.
        if (visibleHints < exercise.hints.size) {
            OutlinedButton(onClick = { visibleHints += 1 }) {
                Text("نمایش راهنمای بعدی")
            }
        }
        // Hintهای آشکارشده با شماره نمایش داده می‌شوند.
        exercise.hints.take(visibleHints).forEachIndexed { index, hint ->
            Text("راهنما ${index + 1}: $hint")
        }

        // Solution تنها وقتی Course آن را دارد قابل نمایش است.
        exercise.solution?.takeIf(String::isNotBlank)?.let { solution ->
            OutlinedButton(onClick = { showSolution = !showSolution }) {
                Text(if (showSolution) "پنهان کردن پاسخ نمونه" else "نمایش پاسخ نمونه")
            }
            if (showSolution) {
                Text(solution, style = MaterialTheme.typography.bodyMedium)
                exercise.explanation?.takeIf(String::isNotBlank)?.let { explanation ->
                    Text(explanation)
                }
            }
        }

        // تکمیل تمرین از ذخیره Draft جداست؛ Host completion را در Repository مشترک ثبت می‌کند.
        Button(
            onClick = { onCompleted(answer) },
            enabled = answer.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ثبت تمرین به‌عنوان انجام‌شده")
        }
    }
}
