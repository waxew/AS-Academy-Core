plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.asdevelopers.academy.core"
    // API 36 آخرین Platform پایدار قابل دریافت از کانال پیش‌فرض sdkmanager در CI است.
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        // Migration testها روی AndroidJUnitRunner اجرا می‌شوند تا SQLite/Room واقعی Android بررسی شود.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Schemaهای تولیدی Room کنار سورس نگهداری می‌شوند تا Migrationها قابل audit و تست باشند.
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    // AcademyDatabase یک API عمومی Core و زیرکلاس RoomDatabase است؛ بنابراین مصرف‌کننده باید
    // RoomDatabase را روی compile classpath ببیند. runtime عمداً api است، نه implementation.
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.work)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Unit Testهای Android Runtime برای Repository و قراردادهای JVM استفاده می‌شوند.
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)

    // Instrumentation testها Migration واقعی Room/SQLite را روی Android اجرا می‌کنند.
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
}
