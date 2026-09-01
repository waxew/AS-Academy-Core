# Course Home / Outline

از Core `1.3.0` صفحه Home دوره‌های بزرگ نباید فهرست تخت Lessonها را در Course Host تولید کند. ساختار استاندارد در Core به شکل زیر است:

`Course -> Level -> Chapter -> Lesson`

## اجزای مشترک

### CourseOutlineEngine

`CourseOutlineEngine`، `CourseBundle` و `LessonProgress` را به مدل آماده UI تبدیل می‌کند و این موارد را یک‌جا محاسبه می‌کند:

- ترتیب Levelها
- ترتیب Chapterها
- ترتیب Lessonها
- Progress کل Course
- Progress هر Level
- Progress هر Chapter
- Status و Progress هر Lesson
- Level locking
- مقصد Continue Learning
- اولویت رکورد Course واقعی نسبت به رکورد Legacy با `courseId = ""`

### CourseOutlineRepository

`CourseOutlineRepository` اتصال Room به Engine است. Course Host فقط `CourseBundle` را می‌دهد و `Flow<CourseOutline>` دریافت می‌کند. DAO و Entityهای Room نباید داخل Course Repository مصرف مستقیم شوند.

### AcademyCourseHomeScreen

Screen مشترک این موارد را نمایش می‌دهد:

- عنوان و Progress کل دوره
- Continue Learning / Start Learning
- Placement Test
- Weak Topic Review
- Flashcard / Spaced Review
- Levelهای باز و قفل
- Progress هر Level
- Chapterهای جمع‌شونده
- Lessonهای دارای وضعیت، زمان و Progress

اولین Level باز به‌صورت پیش‌فرض Expand است. Chapterها بسته‌اند تا Courseهای صدهادرسی Home بسیار طولانی نسازند.

## قرارداد Course Host

Host فقط callbackها را وصل می‌کند:

- `onLessonClick`
- `onContinueClick`
- `onPlacementClick`
- `onWeakTopicReviewClick`
- `onFlashcardReviewClick`

منطق گروه‌بندی، Progress و locking متعلق به Core است و نباید در Python/Java/Kotlin/Basic یا Courseهای بعدی Fork شود.

## دلیل این طراحی

فهرست تخت برای دوره‌ای مانند Basic با 157 درس قابل استفاده نیست. با Outline مشترک، همه دوره‌ها Navigation آموزشی یکسان، Back behavior یکسان، Progress قابل فهم و توسعه‌پذیری بدون duplication خواهند داشت.
