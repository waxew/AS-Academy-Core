package com.asdevelopers.academy.core.update

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Provider عمومی HTTPS برای Course Content Update.
 *
 * Host فقط URL فایل metadata را می‌دهد. Core فایل JSON را می‌خواند، ContentRelease را می‌سازد و
 * CourseContentUpdater ادامه دانلود، SHA-256، Validation، Version Gate و نصب Atomic را انجام می‌دهد.
 */
class HttpsJsonContentUpdateProvider(
    private val metadataUrl: String,
    private val connectTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 30_000
) : ContentUpdateProvider {

    init {
        // کانال Update نباید روی HTTP ساده قرار بگیرد؛ همه Hopهای Redirect نیز پایین‌تر کنترل می‌شوند.
        require(URL(metadataUrl).protocol.equals("https", ignoreCase = true)) {
            "Content metadata URL must use HTTPS"
        }
        require(connectTimeoutMillis > 0) { "connectTimeoutMillis must be positive" }
        require(readTimeoutMillis > 0) { "readTimeoutMillis must be positive" }
    }

    override suspend fun latest(courseId: String): ContentRelease? = withContext(Dispatchers.IO) {
        // Metadata کوچک است و با سقف مشخص خوانده می‌شود تا پاسخ غیرعادی حافظه برنامه را مصرف نکند.
        val text = request(metadataUrl).use { connection ->
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val content = reader.readText()
                require(content.toByteArray(Charsets.UTF_8).size <= MAX_METADATA_BYTES) {
                    "Content metadata is larger than $MAX_METADATA_BYTES bytes"
                }
                content
            }
        }
        val release = parseRelease(text)
        require(release.courseId == courseId) {
            "Metadata courseId ${release.courseId} does not match requested courseId $courseId"
        }
        release
    }

    override suspend fun download(release: ContentRelease, destination: File): File = withContext(Dispatchers.IO) {
        // Parent همان Cache خصوصی Host است؛ هیچ مجوز Storage عمومی نیاز نیست.
        destination.parentFile?.let { parent ->
            require(parent.exists() || parent.mkdirs()) { "Unable to create content download directory" }
        }
        request(release.downloadUrl).use { connection ->
            connection.inputStream.use { input ->
                destination.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }
        }
        destination
    }

    /** Metadata Release را بدون وابستگی Host به kotlinx.serialization به مدل پایدار Core تبدیل می‌کند. */
    private fun parseRelease(text: String): ContentRelease {
        val json = Json.parseToJsonElement(text).jsonObject
        return ContentRelease(
            courseId = json.requiredString("courseId"),
            version = json.requiredString("version"),
            minimumCoreVersion = json.requiredString("minimumCoreVersion"),
            sha256 = json.requiredString("sha256"),
            downloadUrl = json.requiredString("downloadUrl").also { url ->
                require(URL(url).protocol.equals("https", ignoreCase = true)) {
                    "Content download URL must use HTTPS"
                }
            }
        )
    }

    /** اتصال GET با Redirect محدود و HTTPS-only؛ URL نهایی نیز اجازه downgrade به HTTP ندارد. */
    private fun request(initialUrl: String): HttpURLConnection {
        var current = URL(initialUrl)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            require(current.protocol.equals("https", ignoreCase = true)) {
                "Content update redirect must stay on HTTPS"
            }
            val connection = (current.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMillis
                readTimeout = readTimeoutMillis
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json, application/octet-stream;q=0.9, */*;q=0.8")
                setRequestProperty("User-Agent", "AS-Academy-Core-ContentUpdater")
            }
            val code = connection.responseCode
            if (code in 200..299) return connection
            if (code in 300..399 && redirectCount < MAX_REDIRECTS) {
                val location = connection.getHeaderField("Location")
                    ?: run {
                        connection.disconnect()
                        error("Content update redirect has no Location header")
                    }
                val next = URL(current, location)
                connection.disconnect()
                current = next
            } else {
                val message = runCatching {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull().orEmpty()
                connection.disconnect()
                error("Content update HTTP $code${message.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()}")
            }
        }
        error("Too many content update redirects")
    }

    private fun kotlinx.serialization.json.JsonObject.requiredString(name: String): String =
        this[name]?.jsonPrimitive?.content?.trim()?.takeIf(String::isNotEmpty)
            ?: error("Missing or blank content metadata field: $name")

    private companion object {
        const val MAX_REDIRECTS = 5
        const val MAX_METADATA_BYTES = 128 * 1024
    }
}
