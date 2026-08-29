# AS Academy Core — Implementation Status

این سند مرجع وضعیت پیاده‌سازی هسته مرکزی است. اپ‌های Course نباید قابلیت مشترکی را محلی پیاده‌سازی کنند فقط به این دلیل که هنوز در Core کامل نشده است؛ ابتدا Core تکمیل می‌شود.

## موجود

- معماری مرجع اکوسیستم
- Course Package Contract
- Course Manifest + Capabilities
- Level / Chapter / Lesson / LessonBlock contracts
- Progress contract
- Quiz contract
- Exercise contract
- Course Package Validator پایه
- استاندارد کدنویسی مشترک
- استاندارد نسخه‌بندی

## مرحله Android Runtime

موارد زیر باید در Core Android Runtime پیاده‌سازی و سپس توسط تمام اپ‌ها مصرف شوند:

1. Jetpack Compose Design System
2. Navigation
3. Room entities/DAO/migrations
4. Content JSON parser/import transaction
5. LessonBlockRenderer
6. Progress repository
7. Quiz scoring + weak-topic analysis
8. Exercise state/drafts
9. Projects
10. Room FTS search
11. Bookmark + user notes
12. Profile + Drawer
13. DataStore settings
14. Backup/restore
15. Content update + SHA-256 + rollback
16. CodeRunner plugin API
17. Notifications/WorkManager
18. Tests and CI quality gates

## قانون Done

یک قابلیت مشترک زمانی Done محسوب می‌شود که:
- API آن در Core باشد؛
- تست داشته باشد؛
- حداقل Sample Course آن را مصرف کند؛
- Courseها نیاز به کپی کد آن نداشته باشند؛
- Migration/compatibility آن در صورت نیاز مستند باشد.
