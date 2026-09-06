# استاندارد کامنت‌گذاری Content Layer در AS Academy

## هدف

Content Layer مسئول بارگذاری و آماده‌سازی محتوای آموزشی است.

این لایه بین فایل‌های Course Package و لایه‌های مصرف‌کننده قرار می‌گیرد.

## مسئولیت‌ها

- Load کردن Package آموزشی
- Validation ساختار محتوا
- آماده‌سازی داده برای MainCourse
- مدیریت Source Override ها
- مدیریت Learning Extras

## جریان داده

Content Package

↓

Core Content Layer

↓

MainCourse

↓

Application UI

## قوانین معماری

- Content Layer نباید وابسته به UI باشد.
- منطق نمایش درس داخل Core قرار نمی‌گیرد.
- فرمت ذخیره محتوا باید مستقل از Presentation باشد.

## استاندارد KDoc

هر Loader باید توضیح دهد:

- چه چیزی را Load می‌کند.
- منبع داده چیست.
- خروجی چه ساختاری دارد.
- خطاها چگونه مدیریت می‌شوند.
