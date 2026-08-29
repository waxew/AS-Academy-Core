# روش مصرف Core در ریپوهای دوره‌ای

روش استاندارد فعلی Git submodule است تا سورس مشترک کپی نشود و هر اپ روی Commit مشخصی از Core قفل شود.

## ساختار اپ دوره‌ای
```text
repo/
├── academy-core/      # Git submodule -> AS-Academy-Core
├── app/
├── course-<id>/
└── course/<id>/       # JSON/Assets اختصاصی
```

در `settings.gradle.kts`، ماژول‌های `academy-core/course` و `academy-core/core` به پروژه اصلی Map می‌شوند. Version Catalog نیز مستقیماً از `academy-core/gradle/libs.versions.toml` خوانده می‌شود. بنابراین نسخه‌های Android/Kotlin/Compose در Course repo تکرار نمی‌شوند.

هر بار که منطق عمومی تغییر می‌کند:
1. تغییر در AS-Academy-Core انجام می‌شود.
2. CI Core باید سبز باشد.
3. Submodule دوره‌ها به Commit جدید Core ارتقا داده می‌شود.
4. Course repo فقط سازگاری و محتوای اختصاصی خود را تست می‌کند.
