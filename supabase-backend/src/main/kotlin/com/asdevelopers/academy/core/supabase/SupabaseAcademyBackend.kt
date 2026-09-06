package com.asdevelopers.academy.core.supabase

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.asdevelopers.academy.core.backend.AcademyAuthSession
import com.asdevelopers.academy.core.backend.AcademyBackend
import com.asdevelopers.academy.core.backend.AcademySessionAuthGateway
import com.asdevelopers.academy.core.backend.AcademyStorageGateway
import com.asdevelopers.academy.core.backend.AcademySyncGateway
import com.asdevelopers.academy.core.backend.AcademySyncResult
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Provider configuration containing only client-safe Supabase credentials. */
data class SupabaseAcademyConfig(
    val projectUrl: String,
    val publishableKey: String,
    val releaseTable: String = "academy_course_releases",
    val courseBucket: String = "academy-courses",
    val signedUrlLifetimeSeconds: Int = 900
) {
    init {
        val uri = runCatching { URI(projectUrl) }.getOrNull()
        require(uri != null && uri.scheme == "https" && !uri.host.isNullOrBlank()) {
            "Supabase projectUrl must be an absolute HTTPS URL"
        }
        require(publishableKey.isNotBlank()) { "Supabase publishableKey must not be blank" }
        require(!looksLikeSecretKey(publishableKey)) {
            "A Supabase secret/service-role key must never be embedded in an Academy client"
        }
        require(IDENTIFIER.matches(releaseTable)) { "Invalid Supabase release table identifier" }
        require(courseBucket.isNotBlank() && !courseBucket.contains('/')) { "Invalid course bucket name" }
        require(signedUrlLifetimeSeconds in 60..3600) { "Signed URL lifetime must be between 60 and 3600 seconds" }
    }

    companion object {
        private val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
        private val json = Json { ignoreUnknownKeys = true }

        private fun looksLikeSecretKey(key: String): Boolean {
            if (key.startsWith("sb_secret_")) return true
            val parts = key.split('.')
            if (parts.size != 3) return false
            return runCatching {
                val payload = String(
                    Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                    StandardCharsets.UTF_8
                )
                json.parseToJsonElement(payload).jsonObject["role"]?.jsonPrimitive?.contentOrNull == "service_role"
            }.getOrDefault(false)
        }
    }
}

/** Minimal remote release descriptor used by sync and Storage. */
data class SupabaseCourseRelease(
    val courseId: String,
    val version: String,
    val contentSchemaVersion: Int,
    val minimumCoreVersion: String,
    val sha256: String,
    val objectPath: String,
    val publishedAt: String? = null
)

/** Session persistence stays injectable so tests and special hosts can provide their own implementation. */
interface AcademySessionStore {
    suspend fun read(): AcademyAuthSession?
    suspend fun write(session: AcademyAuthSession)
    suspend fun clear()
}

class InMemoryAcademySessionStore : AcademySessionStore {
    @Volatile private var value: AcademyAuthSession? = null
    override suspend fun read(): AcademyAuthSession? = value
    override suspend fun write(session: AcademyAuthSession) { value = session }
    override suspend fun clear() { value = null }
}

/**
 * Android session store encrypted with an app-local AES/GCM key held by AndroidKeyStore.
 * Access/refresh tokens are never persisted as plaintext SharedPreferences values.
 */
class EncryptedAcademySessionStore(
    context: Context,
    private val preferenceName: String = "as_academy_supabase_session",
    private val keyAlias: String = "as_academy_supabase_session_key"
) : AcademySessionStore {
    private val preferences = context.applicationContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(): AcademyAuthSession? = withContext(Dispatchers.IO) {
        val encoded = preferences.getString(KEY_SESSION, null) ?: return@withContext null
        runCatching { decodeSession(decrypt(encoded)) }
            .onFailure { preferences.edit().remove(KEY_SESSION).apply() }
            .getOrNull()
    }

    override suspend fun write(session: AcademyAuthSession) = withContext(Dispatchers.IO) {
        preferences.edit().putString(KEY_SESSION, encrypt(encodeSession(session))).commit()
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit().remove(KEY_SESSION).commit()
        Unit
    }

    private fun encodeSession(session: AcademyAuthSession): String = buildJsonObject {
        put("userId", session.userId)
        put("accessToken", session.accessToken)
        put("refreshToken", session.refreshToken)
        put("expiresAtEpochSeconds", session.expiresAtEpochSeconds)
    }.toString()

    private fun decodeSession(value: String): AcademyAuthSession {
        val obj = json.parseToJsonElement(value).jsonObject
        return AcademyAuthSession(
            userId = obj.requiredString("userId"),
            accessToken = obj.requiredString("accessToken"),
            refreshToken = obj.requiredString("refreshToken"),
            expiresAtEpochSeconds = obj["expiresAtEpochSeconds"]?.jsonPrimitive?.longOrNull
                ?: error("Missing session expiry")
        )
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val data = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "$iv.$data"
    }

    private fun decrypt(encoded: String): String {
        val parts = encoded.split('.', limit = 2)
        require(parts.size == 2) { "Invalid encrypted session payload" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_SESSION = "session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

class AcademyBackendHttpException(
    val statusCode: Int,
    val responseBody: String
) : IllegalStateException("Academy backend request failed with HTTP $statusCode")

/**
 * Supabase implementation owned by AS-Academy-Core. It uses the stable Supabase HTTP surfaces
 * (Auth, Data API and Storage) so the base Core remains independent of provider SDK churn.
 */
class SupabaseAcademyBackend(
    val config: SupabaseAcademyConfig,
    private val sessionStore: AcademySessionStore,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000L }
) : AcademyBackend {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = SupabaseHttpClient(config)

    override val auth: AcademySessionAuthGateway = SupabaseAuthGateway()
    override val sync: AcademySyncGateway = SupabaseSyncGateway()
    override val storage: AcademyStorageGateway = SupabaseStorageGateway()

    companion object {
        @JvmStatic
        fun create(context: Context, config: SupabaseAcademyConfig): SupabaseAcademyBackend =
            SupabaseAcademyBackend(config, EncryptedAcademySessionStore(context))
    }

    private inner class SupabaseAuthGateway : AcademySessionAuthGateway {
        override suspend fun currentUserId(): String? = currentSession()?.userId

        override suspend fun currentSession(): AcademyAuthSession? {
            val session = sessionStore.read() ?: return null
            return if (session.isExpired(nowEpochSeconds())) refreshSession() else session
        }

        override suspend fun signInWithPassword(email: String, password: String): AcademyAuthSession {
            require(email.isNotBlank()) { "email must not be blank" }
            require(password.isNotBlank()) { "password must not be blank" }
            val response = http.request(
                method = "POST",
                path = "/auth/v1/token?grant_type=password",
                body = buildJsonObject {
                    put("email", email.trim())
                    put("password", password)
                }.toString()
            )
            return parseSession(response).also { sessionStore.write(it) }
        }

        override suspend fun refreshSession(): AcademyAuthSession? {
            val existing = sessionStore.read() ?: return null
            return try {
                val response = http.request(
                    method = "POST",
                    path = "/auth/v1/token?grant_type=refresh_token",
                    body = buildJsonObject { put("refresh_token", existing.refreshToken) }.toString()
                )
                parseSession(response).also { sessionStore.write(it) }
            } catch (error: AcademyBackendHttpException) {
                if (error.statusCode == 400 || error.statusCode == 401) {
                    sessionStore.clear()
                    null
                } else {
                    throw error
                }
            }
        }

        override suspend fun signOut() {
            val existing = sessionStore.read()
            try {
                if (existing != null) {
                    http.request("POST", "/auth/v1/logout", accessToken = existing.accessToken)
                }
            } finally {
                sessionStore.clear()
            }
        }

        private fun parseSession(body: String): AcademyAuthSession {
            val obj = json.parseToJsonElement(body).jsonObject
            val user = obj["user"]?.jsonObject ?: error("Supabase Auth response did not include user")
            val expiresAt = obj["expires_at"]?.jsonPrimitive?.longOrNull
                ?: (nowEpochSeconds() + (obj["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L))
            return AcademyAuthSession(
                userId = user.requiredString("id"),
                accessToken = obj.requiredString("access_token"),
                refreshToken = obj.requiredString("refresh_token"),
                expiresAtEpochSeconds = expiresAt
            )
        }
    }

    private inner class SupabaseSyncGateway : AcademySyncGateway {
        override suspend fun syncCourse(courseId: String): AcademySyncResult =
            syncCourse(courseId, localVersion = null, localSha256 = null)

        override suspend fun syncCourse(
            courseId: String,
            localVersion: String?,
            localSha256: String?
        ): AcademySyncResult {
            require(courseId.isNotBlank()) { "courseId must not be blank" }
            val release = fetchRelease(courseId, version = null)
                ?: return AcademySyncResult(changed = false, message = "no published release")
            val changed = localVersion != release.version ||
                (localSha256 != null && !localSha256.equals(release.sha256, ignoreCase = true))
            return AcademySyncResult(
                changed = changed,
                message = if (changed) "remote release available" else "up to date",
                remoteVersion = release.version,
                remoteSha256 = release.sha256
            )
        }
    }

    private inner class SupabaseStorageGateway : AcademyStorageGateway {
        override suspend fun resolveCourseContentUrl(courseId: String, version: String): String? {
            require(courseId.isNotBlank()) { "courseId must not be blank" }
            require(version.isNotBlank()) { "version must not be blank" }
            val release = fetchRelease(courseId, version) ?: return null
            val path = "/storage/v1/object/sign/${encodePath(config.courseBucket)}/${encodePath(release.objectPath)}"
            val session = auth.currentSession()
            val response = http.request(
                method = "POST",
                path = path,
                body = buildJsonObject { put("expiresIn", config.signedUrlLifetimeSeconds) }.toString(),
                accessToken = session?.accessToken
            )
            val obj = json.parseToJsonElement(response).jsonObject
            val signed = obj["signedURL"]?.jsonPrimitive?.contentOrNull
                ?: obj["signedUrl"]?.jsonPrimitive?.contentOrNull
                ?: return null
            return when {
                signed.startsWith("https://") -> signed
                signed.startsWith("/storage/v1/") -> config.projectUrl.trimEnd('/') + signed
                signed.startsWith('/') -> config.projectUrl.trimEnd('/') + "/storage/v1" + signed
                else -> config.projectUrl.trimEnd('/') + "/storage/v1/" + signed
            }
        }
    }

    private suspend fun fetchRelease(courseId: String, version: String?): SupabaseCourseRelease? {
        val versionFilter = version?.let { "&version=eq.${encodeQuery(it)}" } ?: "&order=published_at.desc&limit=1"
        val path = "/rest/v1/${config.releaseTable}" +
            "?select=course_id,version,content_schema_version,minimum_core_version,sha256,object_path,published_at" +
            "&course_id=eq.${encodeQuery(courseId)}$versionFilter"
        val session = auth.currentSession()
        val response = http.request("GET", path, accessToken = session?.accessToken)
        val item = json.parseToJsonElement(response).jsonArray.firstOrNull()?.jsonObject ?: return null
        return SupabaseCourseRelease(
            courseId = item.requiredString("course_id"),
            version = item.requiredString("version"),
            contentSchemaVersion = item["content_schema_version"]?.jsonPrimitive?.intOrNull
                ?: error("Missing content_schema_version"),
            minimumCoreVersion = item.requiredString("minimum_core_version"),
            sha256 = item.requiredString("sha256"),
            objectPath = item.requiredString("object_path"),
            publishedAt = item["published_at"]?.jsonPrimitive?.contentOrNull
        )
    }
}

private class SupabaseHttpClient(private val config: SupabaseAcademyConfig) {
    suspend fun request(
        method: String,
        path: String,
        body: String? = null,
        accessToken: String? = null
    ): String = withContext(Dispatchers.IO) {
        val connection = URL(config.projectUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("apikey", config.publishableKey)
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Client-Info", "as-academy-core/1.5.0")
            if (accessToken != null) {
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw AcademyBackendHttpException(status, response.take(4096))
            }
            response
        } finally {
            connection.disconnect()
        }
    }
}

private fun JsonObject.requiredString(key: String): String =
    this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        ?: error("Missing required field $key")

private fun encodeQuery(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

private fun encodePath(value: String): String =
    value.split('/').joinToString("/") { encodeQuery(it) }
