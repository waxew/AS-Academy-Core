# مشارکت در AS Academy Core

هر تغییری که در دو یا چند دوره قابل استفاده است باید در این Repository انجام شود. Course Repositoryها محل نگهداری محتوا، Branding و Adapter واقعاً اختصاصی‌اند؛ کپی‌کردن Navigation، Database، Renderer یا Engineهای این Repository مجاز نیست.

## قبل از Pull Request

```bash
./gradlew :course:test :engine:test
./gradlew :tools:run --args="validate course/template"
./gradlew :core:lintDebug :sample-app:assembleDebug
```

اگر مدل Course تغییر می‌کند، سازگاری JSON قدیمی، `contentSchemaVersion`، Template، Sample و مستند قرارداد باید هم‌زمان به‌روزرسانی شوند. اگر Entity یا Query تغییر می‌کند، Room migration و Schema export الزامی است؛ `fallbackToDestructiveMigration` برای داده کاربر پذیرفته نیست.

کدهای عمومی باید توضیح نقش و دلیل تصمیم‌های غیرشفاف را در KDoc یا Comment داشته باشند. Secret، Signing Key، Token و فایل محتوای دارای مجوز نامشخص نباید Commit شوند.
