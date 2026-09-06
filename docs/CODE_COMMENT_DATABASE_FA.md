# راهنمای کامنت‌گذاری Database Layer

## هدف

این سند استاندارد توضیح‌دهی برای لایه Database در AS Academy است.

## مسئولیت Database Layer

Database Layer مالک نگهداری داده‌های پایدار کاربر است.

داده‌هایی که در این لایه نگهداری می‌شوند:

- Progress کاربر
- Completion وضعیت یادگیری
- Bookmark ها
- Note ها
- Draft تمرین‌ها
- Sync Outbox

## قوانین کامنت‌گذاری

هر Database Class باید مشخص کند:

- مالک چه داده‌ای است.
- چه داده‌ای نباید در آن ذخیره شود.
- Migration چگونه مدیریت می‌شود.
- وابستگی آن با Repository چیست.

## Migration Policy

Migration نباید باعث حذف اطلاعات کاربر شود.

هر تغییر Schema باید:

1. نسخه قبلی را مشخص کند.
2. مسیر انتقال داده را توضیح دهد.
3. دلیل تغییر را ثبت کند.

## Dependency Flow

Application

↓

Repository

↓

Database / DAO

Database نباید به UI یا Presentation وابسته باشد.
