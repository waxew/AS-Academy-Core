# معماری کل AS Academy

## هدف
`AS-Academy-Core` تنها مرجع کدهای مشترک تمام اپ‌های آموزشی است. اپ‌های دوره‌ای نباید Navigation، Database، Progress، Quiz، Exercise، Search، Bookmark، Settings، Drawer/Profile، Content Engine یا سایر قابلیت‌های عمومی را کپی کنند.

## معماری
```text
AS Academy Platform
        |
        +-- AS Academy Core
        |    +-- App Shell
        |    +-- Design System
        |    +-- Navigation
        |    +-- Database
        |    +-- Content Engine
        |    +-- Progress
        |    +-- Quiz / Exercise / Project
        |    +-- Search / Bookmark / Glossary
        |    +-- Profile / Settings / Drawer
        |    +-- Update / Backup
        |    +-- Capability Plugins
        |
        +-- Course Repositories
             +-- JavaScript
             +-- Python
             +-- Java
             +-- C
             +-- C++
             +-- C#
             +-- PHP
             +-- Kotlin
             +-- Web / Database / Electronics / MikroTik / ...
```

## تکنولوژی استاندارد Android
- Kotlin
- Jetpack Compose
- MVVM
- Room
- DataStore
- Coroutines + Flow
- Navigation Compose
- WorkManager
- Hilt در صورت نیاز

## Offline First
درس، آزمون، تمرین، جستجو، Bookmark و Progress باید بدون اینترنت کار کنند. اینترنت برای Content Update، App Update و Cloud Sync اختیاری آینده استفاده می‌شود.

## لایه‌ها
```text
UI -> ViewModel -> Domain/UseCase -> Repository -> Room / Course Package
```
UI نباید مستقیماً به DAO یا فایل محتوای خام وابسته باشد.

## قابلیت‌های Core
- Dynamic Lesson Renderer
- Progress Engine
- Quiz Engine
- Exercise Engine
- Project Engine
- Search Engine
- Bookmark Engine
- Glossary Engine
- Achievement/Streak
- Code Runner Framework
- Diagram/Terminal/SQL plugin hooks
- Content Import/Validation/Update/Rollback
- Backup/Restore
- App Update abstraction

## قانون عدم تکرار
هر قابلیت قابل استفاده در حداقل دو دوره باید به Core منتقل شود. ریپوهای دوره‌ای فقط Adapter/Configuration و محتوای اختصاصی نگه می‌دارند.

## مدل انتشار
دو مدل روی همین Core پشتیبانی می‌شود:
1. Standalone App مانند AS Academy JavaScript
2. All-in-One App آینده با چند Course Package

## سازگاری و داده
App Version، Core Version، Database Version، Content Schema Version و Course Version مستقل‌اند. Migration مخرب برای داده کاربر مجاز نیست.
