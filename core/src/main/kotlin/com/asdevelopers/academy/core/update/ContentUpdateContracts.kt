package com.asdevelopers.academy.core.update

import java.io.File
import java.security.MessageDigest

/** اطلاعات نسخه محتوایی قابل دانلود. */
data class ContentRelease(
    val courseId: String,
    val version: String,
    val minimumCoreVersion: String,
    val sha256: String,
    val downloadUrl: String
)

/** Provider شبکه/فایل به Core تزریق می‌شود تا Core به سرویس خاصی وابسته نباشد. */
interface ContentUpdateProvider {
    suspend fun latest(courseId: String): ContentRelease?
    suspend fun download(release: ContentRelease, destination: File): File
}

/** بررسی یکپارچگی Package قبل از Import. */
object Sha256Verifier {
    fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun matches(file: File, expected: String): Boolean = hash(file).equals(expected, ignoreCase = true)
}
