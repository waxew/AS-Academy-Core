// Build مرکزی AS Academy Core؛ نسخه تمام ابزارها فقط در Version Catalog نگهداری می‌شود.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

// Foundation v1.5.0 remains the immutable release baseline. New public backend/sync APIs belong to
// the next minor development line so publishing main can never overwrite the 1.5.0 coordinates.
allprojects {
    group = "com.asdevelopers.academy"
    version = "1.6.0-SNAPSHOT"
}
