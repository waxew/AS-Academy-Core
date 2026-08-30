# Changelog

همه تغییرات قابل انتشار AS Academy Core در این فایل ثبت می‌شوند. نسخه‌ها از Semantic Versioning پیروی می‌کنند.

## 1.2.0 — 2026-08-30

### Added

- `QuizResultDao.observeCourse` و `observeLatest` برای تحلیل Course-scoped و بازیابی نتیجه آخر آزمون.
- `WeakTopicReviewRepository` برای تبدیل Quiz history ذخیره‌شده به پیشنهاد درس بدون SQL یا parsing در Course Host.
- `PlacementResultRepository` برای ساخت Placement Summary از آخرین Attempt Persist شده و حفظ نتیجه در Restart/Rotation.
- مسیرهای عمومی Navigation برای Placement، Weak Topic Review و Flashcard Review به همراه helperهای `openPlacement`، `openWeakTopicReview` و `openFlashcardReview`.
- `LearningPathEngine.firstLessonIdForLevelType` برای انتخاب نقطه شروع سطح با همان ترتیب رسمی Level/Chapter/Lesson.
- `FlashcardReviewRepository.loadDueCards` برای Session snapshot پایدار و Batch پیش‌فرض 20 کارتی.
- `SpacedReviewEngine.utcEpochDay` برای محاسبه یکسان روز مرور در تمام Courseها.
- Sample App end-to-end برای Quiz -> Room -> Placement، Weak Review و Flashcard Review.
- Regression Test برای persisted weak tags، UTC day boundaries و first-lesson placement lookup.

### Changed

- `WeakTopicReviewEngine` مستقیماً مجموعه Weak Tagهای Persist شده را نیز تحلیل می‌کند و API قبلی QuizScore را حفظ می‌کند.
- Session فلش‌کارت unseen cardها را نیز due می‌داند و تعداد کارت‌های هر Session را محدود می‌کند.
- Sample Course حداقل Core نسخه 1.2.0 را الزام می‌کند.

## 1.1.0 — 2026-08-30

### Added

- `SpacedReviewEngine` برای ساخت Flashcard از Glossary، زمان‌بندی AGAIN/HARD/GOOD/EASY و انتخاب کارت‌های موعدرسیده.
- `WeakTopicReviewEngine` برای تجمیع `QuizScore.weakTags` و رتبه‌بندی درس‌های مناسب مرور.
- `PlacementEngine` با Policy قابل تنظیم و Policy استاندارد چهارسطحی AS Academy.
- `FlashcardProgressEntity`، DAO و `FlashcardReviewRepository` برای Persistence چنددوره‌ای برنامه مرور.
- Migration غیرمخرب Room `3 -> 4`؛ جدول‌های قبلی بدون بازنویسی حفظ می‌شوند.
- Backup schema v3 برای نگهداری Flashcard Review Progress با Decode سازگار فایل‌های v1 و v2.
- `AcademyFlashcardReviewScreen` برای Session مرور با Reveal و چهار Rating.
- `AcademyWeakTopicReviewScreen` برای نمایش درس‌های اولویت‌دار بر اساس ضعف‌های تکرارشونده.
- `AcademyPlacementSummaryScreen` برای نمایش نمره تعیین‌سطح، سطح شروع و اقدام مرور نقاط ضعف.
- Unit/Regression Test برای Spaced Review، Weak Topic Review، Placement و سازگاری Backupهای قدیمی.

### Changed

- نسخه عمومی Core و مختصات تمام ماژول‌ها به `1.1.0` افزایش یافت.
- Database به schema v4 و Backup به schema v3 ارتقا یافت، بدون `fallbackToDestructiveMigration`.

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
