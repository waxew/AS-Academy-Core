package com.asdevelopers.academy.core.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asdevelopers.academy.core.quiz.QuestionSubmission
import com.asdevelopers.academy.core.quiz.QuestionType
import com.asdevelopers.academy.core.quiz.Quiz
import com.asdevelopers.academy.core.quiz.QuizAnswer
import com.asdevelopers.academy.core.quiz.QuizEngine
import com.asdevelopers.academy.core.quiz.QuizQuestion
import com.asdevelopers.academy.core.quiz.QuizScore

/**
 * UI عمومی Quiz برای تمام Courseها.
 * تصحیح همچنان فقط توسط QuizEngine انجام می‌شود و Screen هیچ قانون امتیازدهی جداگانه‌ای ندارد.
 */
@Composable
fun AcademyQuizScreen(
    quiz: Quiz,
    modifier: Modifier = Modifier,
    onCompleted: (QuizScore) -> Unit = {}
) {
    // پاسخ‌های انتخابی با Stable ID سؤال نگهداری می‌شوند تا ترتیب نمایش روی نتیجه اثر نگذارد.
    val selectedAnswers = remember(quiz.id) { mutableStateMapOf<String, Set<String>>() }
    // پاسخ متنی برای FILL_CODE جدا از گزینه‌ها ذخیره می‌شود.
    val textAnswers = remember(quiz.id) { mutableStateMapOf<String, String>() }
    // ترتیب اولیه ORDER_STEPS عمداً از ترتیب پاسخ صحیح متفاوت نمایش داده می‌شود.
    val orderedAnswers = remember(quiz.id) {
        mutableStateMapOf<String, List<String>>().apply {
            quiz.questions
                .filter { it.type == QuestionType.ORDER_STEPS }
                .forEach { question -> put(question.id, question.answers.shuffled().map(QuizAnswer::id)) }
        }
    }
    // Matching برای هر answerId کلید انتخاب‌شده را نگه می‌دارد.
    val matchingAnswers = remember(quiz.id) {
        mutableStateMapOf<String, Map<String, String>>()
    }
    // Shuffle تنها یک بار برای هر Quiz انجام می‌شود تا Recomposition ترتیب سؤال را تغییر ندهد.
    val displayedQuestions = remember(quiz.id) {
        if (quiz.shuffleQuestions) quiz.questions.shuffled() else quiz.questions
    }
    // ترتیب گزینه‌های هر سؤال نیز یک بار محاسبه می‌شود؛ ORDER_STEPS State اختصاصی دارد.
    val displayedAnswers = remember(quiz.id) {
        quiz.questions.associate { question ->
            question.id to if (quiz.shuffleAnswers && question.type != QuestionType.ORDER_STEPS) {
                question.answers.shuffled()
            } else {
                question.answers
            }
        }
    }
    // نتیجه null یعنی آزمون هنوز ثبت نشده و بعد از Submit همان Attempt را نمایش می‌دهیم.
    var score by remember(quiz.id) { mutableStateOf<QuizScore?>(null) }

    // LazyColumn اجازه می‌دهد آزمون‌های طولانی بدون ساخت همه Viewها در حافظه نمایش داده شوند.
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header نام آزمون و حداقل نمره قبولی را واضح می‌کند.
        item(key = "quiz-header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(quiz.title, style = MaterialTheme.typography.headlineMedium)
                Text("حد نصاب قبولی: ${quiz.passingScorePercent}٪")
                Text("تعداد سؤال: ${quiz.questions.size}")
            }
        }

        // هر سؤال بر اساس QuestionType کنترل مناسب خود را دریافت می‌کند.
        items(displayedQuestions, key = QuizQuestion::id) { question ->
            QuizQuestionCard(
                question = question,
                answers = displayedAnswers[question.id].orEmpty(),
                selectedIds = selectedAnswers[question.id].orEmpty(),
                textAnswer = textAnswers[question.id].orEmpty(),
                orderedIds = orderedAnswers[question.id].orEmpty(),
                matchedKeys = matchingAnswers[question.id].orEmpty(),
                locked = score != null,
                onSingleSelected = { answerId -> selectedAnswers[question.id] = setOf(answerId) },
                onMultiSelected = { answerId, checked ->
                    val current = selectedAnswers[question.id].orEmpty().toMutableSet()
                    if (checked) current += answerId else current -= answerId
                    selectedAnswers[question.id] = current
                },
                onTextChanged = { value -> textAnswers[question.id] = value },
                onOrderChanged = { value -> orderedAnswers[question.id] = value },
                onMatchChanged = { answerId, key ->
                    val current = matchingAnswers[question.id].orEmpty().toMutableMap()
                    current[answerId] = key
                    matchingAnswers[question.id] = current
                }
            )
        }

        // قبل از ثبت، Button پاسخ‌های UI را به قرارداد QuestionSubmission تبدیل می‌کند.
        item(key = "quiz-submit") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (score == null) {
                    Button(
                        onClick = {
                            val submissions = quiz.questions.map { question ->
                                QuestionSubmission(
                                    questionId = question.id,
                                    selectedAnswerIds = selectedAnswers[question.id].orEmpty(),
                                    textAnswer = textAnswers[question.id],
                                    orderedAnswerIds = orderedAnswers[question.id].orEmpty(),
                                    matchedAnswerKeys = matchingAnswers[question.id].orEmpty()
                                )
                            }
                            // QuizEngine تنها مرجع تعیین درست/غلط و Weak Tagهاست.
                            val resolvedScore = QuizEngine.score(quiz, submissions)
                            score = resolvedScore
                            // Host می‌تواند همین Callback را به QuizHistoryRepository وصل کند.
                            onCompleted(resolvedScore)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ثبت پاسخ‌ها")
                    }
                } else {
                    // نتیجه نهایی بدون محاسبه مجدد از QuizScore استاندارد نمایش داده می‌شود.
                    val resolvedScore = score ?: return@Column
                    Text(
                        text = if (resolvedScore.passed) "قبول شدید" else "نیاز به مرور دارید",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text("امتیاز: ${resolvedScore.scorePercent}٪")
                    Text("پاسخ درست: ${resolvedScore.correctQuestionIds.size} از ${quiz.questions.size}")
                    if (resolvedScore.weakTags.isNotEmpty()) {
                        Text("موضوعات نیازمند مرور: ${resolvedScore.weakTags.sorted().joinToString("، ")}")
                    }
                }
            }
        }
    }
}

/** Card هر سؤال جزئیات Input را از Screen اصلی جدا نگه می‌دارد. */
@Composable
private fun QuizQuestionCard(
    question: QuizQuestion,
    answers: List<QuizAnswer>,
    selectedIds: Set<String>,
    textAnswer: String,
    orderedIds: List<String>,
    matchedKeys: Map<String, String>,
    locked: Boolean,
    onSingleSelected: (String) -> Unit,
    onMultiSelected: (String, Boolean) -> Unit,
    onTextChanged: (String) -> Unit,
    onOrderChanged: (List<String>) -> Unit,
    onMatchChanged: (String, String) -> Unit
) {
    // Card مرز بصری سؤال را مشخص می‌کند و از وابستگی Course به UI جلوگیری می‌کند.
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // متن سؤال قبل از Inputها نمایش داده می‌شود.
            Text(question.question, style = MaterialTheme.typography.titleMedium)

            // هر QuestionType رفتار Input اختصاصی اما قرارداد Submission مشترک دارد.
            when (question.type) {
                QuestionType.MULTIPLE_SELECT -> {
                    answers.forEach { answer ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = answer.id in selectedIds,
                                onCheckedChange = if (locked) null else { checked ->
                                    onMultiSelected(answer.id, checked)
                                }
                            )
                            Text(answer.text)
                        }
                    }
                }

                QuestionType.FILL_CODE -> {
                    OutlinedTextField(
                        value = textAnswer,
                        onValueChange = if (locked) ({}) else onTextChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("پاسخ") },
                        enabled = !locked,
                        minLines = 3
                    )
                }

                QuestionType.ORDER_STEPS -> {
                    // ترتیب فعلی از Stable IDها به Answer قابل نمایش تبدیل می‌شود.
                    orderedIds.forEachIndexed { index, answerId ->
                        val answer = question.answers.firstOrNull { it.id == answerId }
                        if (answer != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${index + 1}. ${answer.text}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = !locked && index > 0,
                                        onClick = {
                                            val next = orderedIds.toMutableList()
                                            val previous = next[index - 1]
                                            next[index - 1] = next[index]
                                            next[index] = previous
                                            onOrderChanged(next)
                                        }
                                    ) { Text("بالا") }
                                    OutlinedButton(
                                        enabled = !locked && index < orderedIds.lastIndex,
                                        onClick = {
                                            val next = orderedIds.toMutableList()
                                            val following = next[index + 1]
                                            next[index + 1] = next[index]
                                            next[index] = following
                                            onOrderChanged(next)
                                        }
                                    ) { Text("پایین") }
                                }
                            }
                        }
                    }
                }

                QuestionType.MATCHING -> {
                    // کلیدهای موجود از Contract سؤال استخراج می‌شوند و Button هر ردیف میان آن‌ها می‌چرخد.
                    val keys = question.answers.mapNotNull(QuizAnswer::matchKey).distinct()
                    answers.forEach { answer ->
                        val currentKey = matchedKeys[answer.id]
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(answer.text)
                            OutlinedButton(
                                enabled = !locked && keys.isNotEmpty(),
                                onClick = {
                                    val currentIndex = keys.indexOf(currentKey)
                                    val nextIndex = if (currentIndex < 0 || currentIndex == keys.lastIndex) 0 else currentIndex + 1
                                    onMatchChanged(answer.id, keys[nextIndex])
                                }
                            ) {
                                Text(currentKey ?: "انتخاب جفت")
                            }
                        }
                    }
                }

                QuestionType.MULTIPLE_CHOICE,
                QuestionType.TRUE_FALSE,
                QuestionType.CODE_OUTPUT,
                QuestionType.FIND_ERROR -> {
                    answers.forEach { answer ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = answer.id in selectedIds,
                                onClick = if (locked) null else { { onSingleSelected(answer.id) } }
                            )
                            Text(answer.text)
                        }
                    }
                }
            }

            // پس از Submit توضیح آموزشی سؤال نمایش داده می‌شود؛ قبل از آن پاسخ را لو نمی‌دهد.
            if (locked && question.explanation.isNotBlank()) {
                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
