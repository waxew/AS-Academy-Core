plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.asdevelopers.academy.core"
    // API 36 آخرین Platform پایدار قابل دریافت از کانال پیش‌فرض sdkmanager در CI است.
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // API ماژول Engine از طریق Core به همه اپ‌های دوره‌ای منتقل می‌شود.
    api(project(":engine"))
    // مدل‌های Course نیز برای ساخت UI و Adapterهای دوره در دسترس مصرف‌کننده هستند.
    api(project(":course"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    // AcademyDatabase یک API عمومی Core و زیرکلاس RoomDatabase است؛ بنابراین مصرف‌کننده باید
    // RoomDatabase را روی compile classpath ببیند. runtime عمداً api است، نه implementation.
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.work)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}
