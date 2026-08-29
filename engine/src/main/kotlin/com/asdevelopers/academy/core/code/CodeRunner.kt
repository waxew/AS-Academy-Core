package com.asdevelopers.academy.core.code

import kotlinx.serialization.Serializable

/** درخواست مستقل از زبان برای اجرای نمونه یا تمرین کدنویسی. */
@Serializable
data class CodeRunRequest(
    val languageId: String,
    val sourceCode: String,
    val standardInput: String = "",
    val timeoutMillis: Long = 5_000,
    val metadata: Map<String, String> = emptyMap()
)

/** نتیجه استاندارد اجرا؛ Adapter محلی یا Remote هر دو همین قرارداد را برمی‌گردانند. */
@Serializable
data class CodeRunResult(
    val successful: Boolean,
    val standardOutput: String = "",
    val standardError: String = "",
    val exitCode: Int? = null,
    val durationMillis: Long = 0,
    val timedOut: Boolean = false
)

/** هر دوره فقط Plugin زبان خود را ثبت می‌کند و Core مدیریت مشترک را انجام می‌دهد. */
interface CodeRunnerPlugin {
    val languageId: String
    suspend fun run(request: CodeRunRequest): CodeRunResult
}

/** Registry مانع شرط‌های پراکنده if/when برای زبان‌های مختلف در اپ‌ها می‌شود. */
class CodeRunnerRegistry(plugins: Collection<CodeRunnerPlugin> = emptyList()) {
    private val pluginsByLanguage = plugins.associateByTo(mutableMapOf()) { it.languageId.lowercase() }

    /** Plugin جدید با شناسه یکتا ثبت می‌شود. */
    fun register(plugin: CodeRunnerPlugin) {
        require(plugin.languageId.isNotBlank()) { "languageId cannot be blank" }
        pluginsByLanguage[plugin.languageId.lowercase()] = plugin
    }

    /** اجرای زبان ناشناخته به جای Crash نتیجه خطای قابل نمایش می‌دهد. */
    suspend fun run(request: CodeRunRequest): CodeRunResult {
        val plugin = pluginsByLanguage[request.languageId.lowercase()]
            ?: return CodeRunResult(successful = false, standardError = "No runner registered for ${request.languageId}")
        return plugin.run(request)
    }
}
