// Build مرکزی AS Academy Core؛ نسخه تمام ابزارها فقط در Version Catalog نگهداری می‌شود.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
}

// این مختصات در Composite Build و انتشارهای آینده، وابستگی پروژه‌های Course را پایدار نگه می‌دارد.
allprojects {
    group = "com.asdevelopers.academy"
    version = "1.0.1"
}
