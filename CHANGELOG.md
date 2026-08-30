# Changelog

همه تغییرات قابل انتشار AS Academy Core در این فایل ثبت می‌شوند. نسخه‌ها از Semantic Versioning پیروی می‌کنند.

## 1.0.1 — 2026-08-30

### Fixed

- `androidx.room:room-runtime` به‌عنوان dependency عمومی Core منتشر می‌شود تا Course Hostهایی که `AcademyDatabase` را مصرف می‌کنند به `RoomDatabase` دسترسی کامل داشته باشند.
- `EXERCISE_LINK` به‌عنوان alias سازگار برای Course Packageهای اولیه پذیرفته می‌شود؛ Renderer آن را مانند `EXERCISE` اجرا می‌کند و Validator برای Content جدید استفاده از نام canonical `EXERCISE` را پیشنهاد می‌دهد.
- سازگاری Content Packageهای منتشرشده بدون شکستن Stable ID یا Progress کاربر حفظ می‌شود.

## 1.0.0 — 2026-08-29

### Added

- معماری چندماژولی `course`، `engine`، `core`، `tools` و `sample-app`
- قرارداد JSON نسخه‌دار برای Manifest، Branding، Level، Chapter، Lesson، Quiz، Exercise، Project، Glossary، Asset و Reference
- Validator و Compiler خط فرمان برای Course Package
- موتورهای Progress، Continue Learning، Level unlocking، Quiz، Exercise، Project، Achievement، Search، Code Runner و Content Update
- Runtime اندروید شامل Room، Repositoryها، DataStore، Navigation Compose، Design System، Drawer/Profile، Settings و Lesson Renderer
- Routeهای عمومی و Screenهای پایه برای Quiz، Exercise و Project تا Courseها workflow آموزشی را دوباره پیاده‌سازی نکنند
- Quiz UI متصل به `QuizEngine` با پشتیبانی QuestionTypeهای عمومی و weak-topic result
- Exercise UI عمومی برای Draft، Hint، Solution و completion callback
- Project UI عمومی برای Milestone، Draft و ProjectProgress callback
- Backup/Restore تراکنشی، نصب محتوای Atomic، SHA-256 و Rollback
- ثبت تکمیل Exercise/Project در Repository و Backup/Restore schema v2 با پشتیبانی خواندن v1
- Migration غیرمخرب دیتابیس از نسخه 1 تا 3، شامل یکسان‌سازی دو Schema آزمایشی v2 و اتصال داده Legacy هنگام Import
- Sample Course اجرایی، Unit Testها و GitHub Actions
