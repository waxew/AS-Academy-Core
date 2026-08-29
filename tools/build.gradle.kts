plugins {
    // CLI روی JVM اجرا می‌شود و برای Validate محتوا به Android SDK نیاز ندارد.
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    // نسخه JDK ابزار با سایر ماژول‌ها و CI یکسان نگه داشته می‌شود.
    jvmToolchain(17)
}

dependencies {
    // Reader، Codec و Validator رسمی تنها منبع قواعد Course Package هستند.
    implementation(project(":engine"))
}

application {
    // نام کامل کلاس Main برای اجرای قابل پیش‌بینی از Gradle ثبت می‌شود.
    mainClass.set("com.asdevelopers.academy.tools.CourseCompilerCliKt")
}
