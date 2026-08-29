package com.asdevelopers.academy.core.code

/** نتیجه استاندارد اجرای یک قطعه کد در Runner اختصاصی Course. */
data class CodeRunResult(
    val success: Boolean,
    val output: String = "",
    val error: String? = null
)

/**
 * API پلاگینی Runner. Core هیچ زبان خاصی را اجرا نمی‌کند.
 * JavaScript/Python/SQL و Runnerهای آینده این قرارداد را در ریپوی Course خود پیاده می‌کنند.
 */
interface CodeRunner {
    val languageId: String
    suspend fun run(code: String): CodeRunResult
}
