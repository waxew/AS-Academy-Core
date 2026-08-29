# AS Academy Course Contract

هر پروژه آموزشی باید این قرارداد را رعایت کند و منطق عمومی برنامه را دوباره پیاده‌سازی نکند.

## ساختار Course Package
```text
course/
├── manifest.json
├── course.json
├── levels.json
├── chapters.json
├── lessons/
├── exercises/
├── quizzes/
├── projects/
├── glossary/
├── assets/
├── images/
├── diagrams/
└── branding/
```

## Manifest حداقلی
```json
{
  "courseId": "javascript",
  "titleFa": "آموزش JavaScript",
  "titleEn": "JavaScript",
  "version": "1.0.0",
  "contentSchemaVersion": 1,
  "minimumCoreVersion": "1.0.0",
  "rtl": true,
  "capabilities": {
    "codeRunner": true,
    "terminalExamples": false,
    "diagrams": true,
    "quizzes": true,
    "exercises": true,
    "projects": true,
    "glossary": true
  }
}
```

## Lesson Blocks
TITLE, SUBTITLE, PARAGRAPH, LIST, TABLE, IMAGE, DIAGRAM, CODE, OUTPUT, NOTE, TIP, WARNING, IMPORTANT, EXERCISE, QUIZ, PROJECT_LINK, REFERENCE.

## Stable IDs
شناسه‌های منتشرشده نباید تغییر کنند. نمونه‌ها:
- `js-fnd-001`
- `js-qz-fnd-001`
- `js-ex-fnd-001`
- `js-prj-001`

## Course Versioning
- MAJOR: تغییر اساسی ساختار/برنامه آموزشی
- MINOR: درس، فصل یا قابلیت محتوایی جدید
- PATCH: اصلاح متن، مثال یا خطای محتوا

## مرز Course
Course مجاز است محتوا، Branding، Asset و Capability configuration اختصاصی داشته باشد. Navigation، Database، Progress، Quiz Engine، Search و سایر سرویس‌های مشترک نباید در Course کپی شوند.
