plugins {
    // این ماژول Android نیست تا ابزارهای دسکتاپ و CI تولید محتوا نیز قرارداد Course را مصرف کنند.
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // JDK 17 نسخه مشترک توسعه محلی، GitHub Actions و Android build است.
    jvmToolchain(17)
}

dependencies {
    // JSON قرارداد رسمی Course Package با kotlinx.serialization خوانده و نوشته می‌شود.
    api(libs.kotlinx.serialization.json)

    // تست قراردادها بدون نیاز به Android Emulator اجرا می‌شود.
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    // JUnit Platform گزارش استاندارد و قابل استفاده در CI تولید می‌کند.
    useJUnitPlatform()
}
