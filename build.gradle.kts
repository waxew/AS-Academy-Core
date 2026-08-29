// Build مرکزی AS Academy Core.
// AGP 9 از Kotlin داخلی استفاده می‌کند؛ نسخه KGP جدیدتر برای Compose Compiler و ابزارهای Kotlin تثبیت می‌شود.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
}
