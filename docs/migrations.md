# Migration و حفاظت از داده

## دسته‌بندی داده

Course Package و Search Index قابل بازسازی‌اند. Progress، Bookmark، Note، Quiz history، Exercise draft، Project progress، Achievement، Flashcard review progress، Settings و Profile داده کاربرند و نباید در Upgrade حذف شوند.

## Database نسخه 4

Migration `MIGRATION_1_2` ساختار تک‌دوره‌ای اولیه را به کلیدهای چنددوره‌ای ارتقا می‌دهد:

- `courseId` به رکوردهای کاربر اضافه می‌شود.
- Progress از کلید `lessonId` به کلید ترکیبی `courseId + lessonId` منتقل می‌شود.
- وضعیت صریح `NOT_STARTED/IN_PROGRESS/COMPLETED/NEEDS_REVIEW` جای Boolean قبلی را می‌گیرد.
- weak topicهای Quiz و جدول‌های Draft، Project و Achievement اضافه می‌شوند.
- فقط FTS cache حذف و پس از Import Course دوباره ساخته می‌شود.

در دوره توسعه، دو Schema متفاوت با شماره v2 ساخته شدند: یک شاخه فقط جدول `learning_completion` را اضافه کرده بود و شاخه دیگر ساختار چنددوره‌ای کامل را داشت. `MIGRATION_2_3` شکل واقعی جدول‌ها را با `PRAGMA table_info` تشخیص می‌دهد، مسیر لازم را اجرا می‌کند و هر دو را بدون پاک‌کردن داده به Schema واحد v3 می‌رساند.

`MIGRATION_3_4` فقط جدول `flashcard_progress` و index مرکب `courseId + dueEpochDay` را ایجاد می‌کند. هیچ جدول یا ستون قبلی Drop یا Rewrite نمی‌شود. وضعیت Review با کلید ترکیبی `courseId + cardId` نگهداری می‌شود تا چند Course روی یک دیتابیس تداخل نداشته باشند.

رکوردهای نسخه تک‌دوره‌ای ابتدا با `courseId = ""` حفظ می‌شوند، چون Migration دیتابیس هنوز نمی‌داند کدام Course Package نصب خواهد شد. اولین `CoursePackageImporter.import(bundle)` آن‌ها را داخل همان Transaction و بر اساس Stable IDهای Bundle به Course درست متصل می‌کند:

- Progress و Note با Lesson ID
- Quiz history با Quiz ID
- Exercise draft و Completion با Exercise ID
- Project progress و Completion با Project ID
- Bookmark با ID درس، Block، Quiz، Exercise، Project، Glossary، Asset یا Reference

در صورت وجود هم‌زمان رکورد قدیمی و رکورد جدید، رکورد دارای Course واقعی حفظ می‌شود. Repository نیز اجازه ایجاد Progress جدید با Course ID خالی را نمی‌دهد.

## قانون تغییر Schema

1. نسخه `@Database` افزایش یابد.
2. Migration صریح و بدون حذف داده کاربر اضافه شود.
3. Schema خروجی Room در `core/schemas` Commit شود.
4. تست Migration برای تمام شکل‌های منتشرشده Schema اضافه شود؛ شماره نسخه به‌تنهایی برای شاخه‌های آزمایشی کافی نیست.
5. Backup/Restore model و مستندات بررسی شوند.

`fallbackToDestructiveMigration` برای این Database ممنوع است. اگر شکل داده قابل بازسازی مانند Search عوض شود، فقط همان Cache می‌تواند Drop و Rebuild شود.

## Content Migration

افزایش `contentSchemaVersion` با افزایش Course Content version متفاوت است. Core باید Schema قدیمی را Decode یا قبل از انتشار Migration مشخص ارائه کند. Validator Package با Schema بالاتر از `CoreVersion.COURSE_SCHEMA` را رد می‌کند.

## Backup

`AcademyBackup` نسخه مستقل دارد.

- Schema v2 فهرست Completionهای Exercise/Project را اضافه کرد.
- Schema v3 وضعیت Flashcard/Spaced Review را اضافه می‌کند.
- فیلدهای جدید مقدار پیش‌فرض خالی دارند، بنابراین Core 1.1.0 فایل‌های Backup v1 و v2 را بدون از دست‌دادن داده قبلی Decode می‌کند.
- فایل جدید با schema v3 نوشته می‌شود تا Core قدیمی آن را به اشتباه به‌عنوان قالب کامل‌تر قبول نکند.

Restore ابتدا کل JSON را Decode و نسخه را کنترل می‌کند و سپس همه Upsertها، از جمله Flashcard Progress، را در یک Transaction انجام می‌دهد. Search Index در Backup نیست، چون از Course Package بازسازی می‌شود.
