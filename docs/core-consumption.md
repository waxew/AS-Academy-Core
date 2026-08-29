# روش مصرف Core در Repositoryهای دوره‌ای

این نام فایل برای حفظ لینک مستندات قبلی نگه داشته شده است. راهنمای canonical و به‌روز در [core-usage.md](core-usage.md) قرار دارد.

روش رسمی فعلی، نگهداری Core به‌صورت checkout مجاور یا Git submodule و اتصال آن با Gradle Composite Build است. Course app فقط مختصات زیر را مصرف می‌کند و هیچ ماژول Core را کپی یا با مسیر داخلی Map نمی‌کند:

```kotlin
// settings.gradle.kts
includeBuild("../AS-Academy-Core")

// app/build.gradle.kts
dependencies {
    implementation("com.asdevelopers.academy:core:1.0.0")
}
```

جزئیات ساخت Course Package، اجرای CLI و Checklist انتقال پروژه JavaScript در [integration-guide.md](integration-guide.md) توضیح داده شده است.
