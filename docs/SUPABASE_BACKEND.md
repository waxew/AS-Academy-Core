# AS Academy Supabase backend

The base `core` artifact remains provider-neutral. The optional `supabase-backend` artifact implements `AcademyBackend` using Supabase Auth, Data API and Storage.

## Client configuration

Android hosts may provide only:

- the dedicated AS Academy Supabase project HTTPS URL;
- a Supabase publishable key (`sb_publishable_...`, or a legacy anon key during migration).

`SupabaseAcademyConfig` rejects modern secret keys and legacy JWTs whose role is `service_role`. Never place a service-role/secret key in source code, Gradle resources, APK assets, BuildConfig fields, or client-accessible CI artifacts.

```kotlin
val backend = SupabaseSynchronizedAcademyBackend.create(
    context = applicationContext,
    config = SupabaseAcademyConfig(
        projectUrl = BuildConfig.ACADEMY_SUPABASE_URL,
        publishableKey = BuildConfig.ACADEMY_SUPABASE_PUBLISHABLE_KEY
    )
)

val runtime = AcademyRuntime.create(
    context = applicationContext,
    backend = backend
)
```

The provider encrypts access/refresh session tokens with an AES/GCM key stored in AndroidKeyStore before persisting them in app-private SharedPreferences.

## Database and Storage provisioning

Apply `supabase/migrations/20260906090000_academy_backend.sql` only to the explicitly designated AS Academy project. The migration:

- enables RLS on every exposed Academy table;
- grants public/authenticated clients read-only access to published course release metadata;
- enforces `auth.uid() = user_id` ownership for user sync rows;
- uses both `USING` and `WITH CHECK` for UPDATE;
- creates a private `academy-courses` Storage bucket;
- grants Storage SELECT only for object paths referenced by published releases;
- creates no `SECURITY DEFINER` functions and does not grant client mutation rights to release metadata.

After applying the migration, run Supabase security and performance advisors and resolve all findings before using the project in production.

## Release catalog contract

`academy_course_releases` is the remote delivery catalog. A row contains:

- `course_id`
- `version`
- `content_schema_version`
- `minimum_core_version`
- `sha256`
- `object_path`
- `published_at`

Course packages are uploaded by trusted publishing automation to the private `academy-courses` bucket. The Android client discovers the latest release through the Data API, compares version/checksum, and requests a short-lived signed URL for the selected object.

## Sync contract

`SupabaseSynchronizedAcademyBackend.userSync` implements the provider-neutral `AcademyUserSyncGateway`.

- `push(records)` performs authenticated PostgREST upserts with `resolution=merge-duplicates`.
- the remote unique key `(user_id, course_id, entity_type, entity_id)` makes repeated sends idempotent at the logical-entity level;
- every record also has a stable `operation_id` so a local retry queue can preserve mutation identity;
- `pull(courseId, changedAfterIso8601)` returns remote changes in ascending update order for incremental reconciliation;
- transient HTTP 408/429/5xx failures are classified as retryable rather than silently discarded;
- RLS prevents a signed-in user from reading or mutating another user's records.

`academy_user_sync_events` is the user-owned remote synchronization boundary. Hosts should keep unsent operations in their Core-owned local persistence and retry with the same operation IDs. Conflict resolution should compare application-level timestamps/version clocks rather than trusting network arrival order; ambiguous conflicts should be surfaced rather than overwritten by hidden provider logic.

## Project selection

Do not apply this schema to an unrelated Supabase project. The AS Academy project must be explicitly identified/provisioned before any remote DDL or key retrieval is performed.
