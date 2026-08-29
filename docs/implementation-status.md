# AS Academy Core — Implementation Status

## پیاده‌سازی‌شده در هسته
- Multi-module Android foundation
- Course Manifest / Capabilities
- Level / Chapter / Lesson / LessonBlock contracts
- Asset Course Package Loader + validation
- Compose App Shell
- Drawer + Home/Chapter/Lesson navigation
- Generic Lesson Block Renderer
- Room database
- Progress persistence
- Bookmark persistence
- User Note schema
- Quiz result schema
- Room FTS search + index rebuild
- Settings/DataStore dark mode
- Quiz scoring + weak-topic analysis
- Exercise/Project/Glossary contracts
- CodeRunner plugin API
- Content update provider contract + SHA-256 verifier
- Backup contract
- Unit tests for scoring/progress
- GitHub Actions CI
- Sample app

## در نسخه‌های بعدی Core قابل توسعه است
- Content package ZIP importer + rollback snapshot
- Download manager/resume
- Cloud sync/account
- Achievement/Streak runtime
- Notification scheduling UI
- Advanced adaptive/tablet layout
- Full backup codec and SAF UI
- Additional LessonBlock plugins (diagram/video/file)

قانون ثابت: هیچ Course repo نباید موارد بالا را با کپی‌کردن کد دور بزند؛ قابلیت عمومی ابتدا در Core توسعه داده می‌شود.
