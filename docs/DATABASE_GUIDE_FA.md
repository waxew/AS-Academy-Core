# راهنمای Database Layer در AS Academy Core

## هدف

Database Layer مسئول مدیریت ذخیره‌سازی پایدار داده‌های Core است.

## مسئولیت‌ها

- تعریف ساختار داده
- مدیریت Schema
- Migration
- دسترسی DAO
- نگهداری Integrity داده

## قوانین معماری

Database نباید منطق UI یا منطق آموزشی سطح بالا را در خود داشته باشد.

## جریان داده

Application

↓

Repository

↓

Database

## توسعه

هر تغییر Schema باید با Migration و توضیح نسخه همراه باشد.
