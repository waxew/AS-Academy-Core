plugins {
    // این ماژول Android نیست تا ابزارهای دسکتاپ و CI تولید محتوا نیز قرارداد Course را مصرف کنند.
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("course") {
            from(components["java"])
            artifactId = "course"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/waxew/AS-Academy-Core")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }
    }
}
