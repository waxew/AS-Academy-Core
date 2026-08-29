package com.asdevelopers.academy.core.quiz

/** موتور واحد تصحیح آزمون برای جلوگیری از اختلاف نتیجه میان اپ‌های دوره‌ای. */
object QuizEngine {

    /** پاسخ‌ها را با کلید Stable ID تصحیح و Tagهای ضعیف را استخراج می‌کند. */
    fun score(quiz: Quiz, submissions: List<QuestionSubmission>): QuizScore {
        // پاسخ تکراری برای یک سؤال نشانه State ناسازگار UI است و رد می‌شود.
        require(submissions.map { it.questionId }.distinct().size == submissions.size) {
            "duplicate question submissions are not allowed"
        }
        val questionIds = quiz.questions.mapTo(mutableSetOf()) { it.id }
        require(submissions.all { it.questionId in questionIds }) { "submission references an unknown question" }
        val submissionByQuestion = submissions.associateBy { it.questionId }
        val correctIds = mutableSetOf<String>()
        val wrongIds = mutableSetOf<String>()
        val weakTags = mutableSetOf<String>()
        var earnedPoints = 0
        val totalPoints = quiz.questions.sumOf { it.points }

        quiz.questions.forEach { question ->
            val submission = submissionByQuestion[question.id]
            val knownAnswerIds = question.answers.mapTo(mutableSetOf()) { it.id }
            require(submission?.selectedAnswerIds.orEmpty().all { it in knownAnswerIds }) {
                "submission for ${question.id} references an unknown answer"
            }
            require(submission?.orderedAnswerIds.orEmpty().all { it in knownAnswerIds }) {
                "ordered submission for ${question.id} references an unknown answer"
            }
            require(submission?.matchedAnswerKeys.orEmpty().keys.all { it in knownAnswerIds }) {
                "matching submission for ${question.id} references an unknown answer"
            }

            // هر نوع سؤال با ساختار پاسخ خودش تصحیح می‌شود و معنای ترتیب یا Matching از بین نمی‌رود.
            val isCorrect = when (question.type) {
                QuestionType.FILL_CODE -> {
                    val expectedText = question.answers.singleOrNull { it.isCorrect }?.text?.normalizeText()
                    expectedText != null && submission?.textAnswer?.normalizeText() == expectedText
                }
                QuestionType.ORDER_STEPS -> {
                    val expectedOrder = question.answers.sortedBy { it.order }.map { it.id }
                    expectedOrder.isNotEmpty() && submission?.orderedAnswerIds == expectedOrder
                }
                QuestionType.MATCHING -> {
                    val expectedMatches = question.answers.associate { it.id to it.matchKey.orEmpty() }
                    expectedMatches.isNotEmpty() && submission?.matchedAnswerKeys == expectedMatches
                }
                else -> {
                    // مجموعه دقیق پاسخ‌های صحیح باید با مجموعه انتخاب کاربر برابر باشد.
                    val expectedIds = question.answers.filter { it.isCorrect }.mapTo(mutableSetOf()) { it.id }
                    expectedIds.isNotEmpty() && submission?.selectedAnswerIds.orEmpty() == expectedIds
                }
            }
            if (isCorrect) {
                correctIds += question.id
                earnedPoints += question.points
            } else {
                wrongIds += question.id
                weakTags += question.tags
            }
        }

        // آزمون بدون سؤال در Validator رد می‌شود؛ محافظ زیر موتور را در برابر ورودی مستقیم ایمن می‌کند.
        val percent = if (totalPoints == 0) 0 else (earnedPoints * 100 / totalPoints)
        return QuizScore(
            scorePercent = percent,
            earnedPoints = earnedPoints,
            totalPoints = totalPoints,
            correctQuestionIds = correctIds,
            wrongQuestionIds = wrongIds,
            weakTags = weakTags,
            passed = percent >= quiz.passingScorePercent
        )
    }

    /** تفاوت Line ending و فاصله انتهای پاسخ کدی نباید نتیجه FILL_CODE را عوض کند. */
    private fun String.normalizeText(): String =
        replace("\r\n", "\n").lines().joinToString("\n") { it.trimEnd() }.trim()
}
