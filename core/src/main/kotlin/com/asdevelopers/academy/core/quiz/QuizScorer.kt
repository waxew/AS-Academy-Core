package com.asdevelopers.academy.core.quiz

/** تحلیل نتیجه آزمون برای نمایش نمره و تشخیص موضوعات نیازمند مرور. */
data class QuizScore(
    val correct: Int,
    val wrong: Int,
    val percent: Int,
    val weakTags: List<String>
)

object QuizScorer {
    /** selectedAnswerIds با Question ID کلیدگذاری می‌شود. */
    fun score(quiz: Quiz, selectedAnswerIds: Map<String, String>): QuizScore {
        var correct = 0
        val wrongTags = mutableMapOf<String, Int>()

        quiz.questions.forEach { question ->
            val selectedId = selectedAnswerIds[question.id]
            val selected = question.answers.firstOrNull { it.id == selectedId }
            if (selected?.isCorrect == true) {
                correct++
            } else {
                question.tags.forEach { tag -> wrongTags[tag] = (wrongTags[tag] ?: 0) + 1 }
            }
        }

        val total = quiz.questions.size
        val wrong = total - correct
        val percent = if (total == 0) 0 else (correct * 100) / total
        return QuizScore(
            correct = correct,
            wrong = wrong,
            percent = percent,
            weakTags = wrongTags.entries.sortedByDescending { it.value }.map { it.key }
        )
    }
}
