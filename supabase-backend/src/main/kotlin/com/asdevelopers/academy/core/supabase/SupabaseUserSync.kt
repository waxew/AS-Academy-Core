package com.asdevelopers.academy.core.supabase

import com.asdevelopers.academy.core.backend.AcademyAuthGateway
import com.asdevelopers.academy.core.backend.AcademyBackend
import com.asdevelopers.academy.core.backend.AcademySessionAuthGateway
import com.asdevelopers.academy.core.backend.AcademyStorageGateway
import com.asdevelopers.academy.core.backend.AcademySyncGateway
import com.asdevelopers.academy.core.backend.AcademyUserSyncGateway
import com.asdevelopers.academy.core.backend.AcademyUserSyncPushResult
import com.asdevelopers.academy.core.backend.AcademyUserSyncRecord
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Full Supabase backend facade including authenticated user sync. This wrapper deliberately uses
 * the existing provider's public Auth session boundary rather than exposing stored credentials.
 */
class SupabaseSynchronizedAcademyBackend(
    private val delegate: SupabaseAcademyBackend
) : AcademyBackend {
    override val auth: AcademyAuthGateway = delegate.auth
    override val sync: AcademySyncGateway = delegate.sync
    override val storage: AcademyStorageGateway = delegate.storage
    override val userSync: AcademyUserSyncGateway = SupabaseAcademyUserSyncGateway(
        config = delegate.config,
        auth = delegate.auth
    )

    companion object {
        @JvmStatic
        fun create(
            context: android.content.Context,
            config: SupabaseAcademyConfig
        ): SupabaseSynchronizedAcademyBackend =
            SupabaseSynchronizedAcademyBackend(SupabaseAcademyBackend.create(context, config))
    }
}

/**
 * Pushes user-owned state through an idempotent PostgREST upsert and pulls incremental changes.
 * The remote unique key (user_id, course_id, entity_type, entity_id) prevents duplicate logical
 * entities on retries, while operation_id gives each attempted mutation a stable identity.
 */
class SupabaseAcademyUserSyncGateway(
    private val config: SupabaseAcademyConfig,
    private val auth: AcademySessionAuthGateway
) : AcademyUserSyncGateway {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun push(records: List<AcademyUserSyncRecord>): AcademyUserSyncPushResult {
        if (records.isEmpty()) return AcademyUserSyncPushResult(accepted = 0)
        val session = auth.currentSession()
            ?: return AcademyUserSyncPushResult(accepted = 0, retryable = true, message = "authentication required")

        val invalid = records.firstOrNull {
            it.operationId.isBlank() || it.courseId.isBlank() || it.entityType.isBlank() ||
                it.entityId.isBlank() || it.updatedAtIso8601.isBlank() ||
                runCatching { json.parseToJsonElement(it.payloadJson) }.isFailure
        }
        require(invalid == null) { "User sync records require stable IDs, timestamps and valid JSON payloads" }

        val body = buildJsonArray {
            records.forEach { record ->
                add(buildJsonObject {
                    put("operation_id", record.operationId)
                    put("user_id", session.userId)
                    put("course_id", record.courseId)
                    put("entity_type", record.entityType)
                    put("entity_id", record.entityId)
                    put("payload", json.parseToJsonElement(record.payloadJson))
                    put("updated_at", record.updatedAtIso8601)
                    record.deletedAtIso8601?.let { put("deleted_at", it) }
                })
            }
        }.toString()

        return try {
            request(
                method = "POST",
                path = "/rest/v1/academy_user_sync_events?on_conflict=user_id,course_id,entity_type,entity_id",
                accessToken = session.accessToken,
                body = body,
                prefer = "resolution=merge-duplicates,return=minimal"
            )
            AcademyUserSyncPushResult(accepted = records.size)
        } catch (error: AcademyBackendHttpException) {
            AcademyUserSyncPushResult(
                accepted = 0,
                retryable = error.statusCode == 408 || error.statusCode == 429 || error.statusCode >= 500,
                message = "HTTP ${error.statusCode}"
            )
        }
    }

    override suspend fun pull(
        courseId: String,
        changedAfterIso8601: String?
    ): List<AcademyUserSyncRecord> {
        require(courseId.isNotBlank()) { "courseId must not be blank" }
        val session = auth.currentSession() ?: return emptyList()
        val changedFilter = changedAfterIso8601
            ?.takeIf(String::isNotBlank)
            ?.let { "&updated_at=gt.${encodeQuery(it)}" }
            .orEmpty()
        val path = "/rest/v1/academy_user_sync_events" +
            "?select=operation_id,course_id,entity_type,entity_id,payload,updated_at,deleted_at" +
            "&course_id=eq.${encodeQuery(courseId)}$changedFilter&order=updated_at.asc"
        val response = request("GET", path, session.accessToken)
        return json.parseToJsonElement(response).jsonArray.map { element ->
            val obj = element.jsonObject
            AcademyUserSyncRecord(
                operationId = obj.requiredText("operation_id"),
                courseId = obj.requiredText("course_id"),
                entityType = obj.requiredText("entity_type"),
                entityId = obj.requiredText("entity_id"),
                payloadJson = obj["payload"]?.toString() ?: "{}",
                updatedAtIso8601 = obj.requiredText("updated_at"),
                deletedAtIso8601 = obj["deleted_at"]?.jsonPrimitive?.contentOrNull
            )
        }
    }

    private suspend fun request(
        method: String,
        path: String,
        accessToken: String,
        body: String? = null,
        prefer: String? = null
    ): String = withContext(Dispatchers.IO) {
        val connection = URL(config.projectUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("apikey", config.publishableKey)
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            prefer?.let { connection.setRequestProperty("Prefer", it) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw AcademyBackendHttpException(status, response.take(4096))
            response
        } finally {
            connection.disconnect()
        }
    }
}

private fun kotlinx.serialization.json.JsonObject.requiredText(key: String): String =
    this[key]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
        ?: error("Missing required user sync field $key")

private fun encodeQuery(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
