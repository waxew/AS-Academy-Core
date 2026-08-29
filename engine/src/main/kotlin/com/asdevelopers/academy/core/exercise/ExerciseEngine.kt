package com.asdevelopers.academy.core.exercise

/** ارزیاب عمومی برای تمرین‌هایی که خروجی متنی قطعی دارند. */
object ExpectedOutputExerciseEvaluator : ExerciseEvaluator {
    override suspend fun evaluate(exercise: Exercise, answer: String): ExerciseEvaluation {
        // فاصله انتهای خطوط نباید پاسخ منطقی درست را غلط کند.
        val normalizedAnswer = normalize(answer)
        val normalizedExpected = normalize(exercise.expectedOutput.orEmpty())
        val accepted = normalizedExpected.isNotEmpty() && normalizedAnswer == normalizedExpected
        return ExerciseEvaluation(
            accepted = accepted,
            scorePercent = if (accepted) 100 else 0,
            feedback = if (accepted) "پاسخ درست است." else "خروجی با پاسخ مورد انتظار یکسان نیست.",
            actualOutput = answer
        )
    }

    /** Line ending سیستم‌ها و فاصله انتهای خطوط برای مقایسه یکسان می‌شود. */
    private fun normalize(value: String): String =
        value.replace("\r\n", "\n").lines().joinToString("\n") { it.trimEnd() }.trim()
}
