# استفاده از AS Academy Core در پروژه‌های دوره‌ای

## اصل
هر اپ دوره‌ای فقط App configuration، Branding، Course Package و Capability اختصاصی خود را نگه می‌دارد.

## بخش‌هایی که از Core مصرف می‌شوند
- App Shell / Navigation
- Design System
- Drawer/Profile
- Room database abstractions
- Content Engine
- Lesson Renderer
- Progress
- Quiz
- Exercise
- Project
- Search
- Bookmark
- Glossary
- Settings
- Update/Backup
- Code Runner framework در صورت فعال بودن

## الزام README پروژه دوره‌ای
هر README باید بخش `AS Academy Core Usage` داشته باشد و موارد زیر را ثبت کند:
1. Core version مورد استفاده
2. ماژول‌ها/قابلیت‌های Core مورد استفاده
3. قابلیت‌های اختصاصی پروژه
4. Course Content version
5. هر override یا adapter اختصاصی

## قانون اصلاح
اگر باگ در چند اپ قابل تکرار است، اصلاح در Core انجام می‌شود. پروژه‌های دوره‌ای بعد از ارتقای Core version اصلاح را دریافت می‌کنند. Fork یا Copy/Paste منطق Core داخل Course repo ممنوع است.
