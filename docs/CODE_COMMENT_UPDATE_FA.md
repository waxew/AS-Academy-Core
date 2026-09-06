# استاندارد کامنت‌گذاری Update Layer - AS Academy

## هدف

Update Layer مسئول مدیریت چرخه تغییرات محتوای آموزشی و نسخه‌های Course Package است.

## مسئولیت‌ها

- بررسی نسخه فعلی محتوا
- دریافت نسخه جدید
- اعتبارسنجی محتوا
- اعمال Update امن
- جلوگیری از خراب شدن داده‌های کاربر

## جریان Update

Version Check
↓
Validation
↓
Download / Load
↓
Apply
↓
Verify

## قوانین کامنت‌گذاری

هر Manager باید توضیح دهد:

- مالکیت عملیات Update
- ورودی و خروجی
- رفتار در خطا
- سیاست Rollback

## محدودیت معماری

Update Layer نباید:

- UI را بشناسد
- Screen یا ViewModel مدیریت کند
- منطق اختصاصی یک App داشته باشد

Update فقط زیرساخت مشترک Core است.
