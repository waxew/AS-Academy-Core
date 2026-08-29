plugins {
    // موتورهای آموزشی مستقل از Android باقی می‌مانند تا قابل تست و استفاده در ابزارهای دیگر باشند.
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // JDK 17 با حداقل نسخه مورد نیاز Android Gradle Plugin هم‌راستا است.
    jvmToolchain(17)
}

dependencies {
    // تمام موتورهای عمومی از قرارداد واحد Course استفاده می‌کنند.
    api(project(":course"))

    // State و عملیات asynchronous موتورهای عمومی با Coroutines پیاده‌سازی می‌شود.
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Unit Testها بدون Android SDK اجرا می‌شوند.
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    // همه تست‌ها روی JUnit Platform اجرا می‌شوند.
    useJUnitPlatform()
}
