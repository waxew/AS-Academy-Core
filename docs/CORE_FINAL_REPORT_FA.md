# گزارش نهایی AS Academy Core

## وضعیت

این سند وضعیت نهایی Core Foundation را ثبت می‌کند.

## نقش Core

Core مالک Runtime، Persistence، Repository، Update Infrastructure و Backend Boundary است.

Core نباید شامل Presentation، Screen، Theme یا محتوای اختصاصی Course باشد.

## معماری

```text
MainUi
  |
  | Public Core API
  v
Core Runtime
  |
  +-- Repository
  |      |
  |      v
  |   Database
  |
  +-- Content Engine
  |
  +-- Update Engine
  |
  +-- Backend Boundary

MainCourse
  |
  v
Course Content Contract
```

## قوانین Dependency

- MainUi فقط Public API Core را مصرف می‌کند.
- MainCourse فقط مالک Content است.
- Runtime Owner فقط AS-Academy-Core است.
- Backend implementation داخل Core باقی می‌ماند.

## Final Checklist

- [x] Architecture ownership defined
- [x] Contract v1 defined
- [x] Runtime boundary defined
- [x] Repository boundary defined
- [x] Database ownership defined
- [x] Content ownership separated
- [x] Update strategy documented

## آماده‌سازی برای مرحله بعد

Core پس از این مرحله آماده بررسی یکپارچه‌سازی با AS-Academy-MainUi و AS-Academy-MainCourse است.
