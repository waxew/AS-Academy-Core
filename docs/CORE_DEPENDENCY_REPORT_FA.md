# گزارش Dependency معماری AS Academy Core

## هدف

این سند قوانین وابستگی Foundation را ثبت می‌کند تا توسعه آینده بین Core، MainUi و MainCourse بدون ایجاد Coupling انجام شود.

## Dependency Graph

```text
MainUi
  |
  | Public API Only
  v
AS-Academy-Core
  |
  +-- Runtime
  +-- Repository
  +-- Database
  +-- Content Engine
  +-- Update Engine
  +-- Backend Boundary

MainCourse
  |
  | Content Contract Only
  v
Core Content Engine
```

## قوانین

### MainUi

مجاز:
- مصرف Public API
- دریافت Domain Model
- استفاده از Runtime

غیرمجاز:
- ساخت مستقیم Database
- دسترسی مستقیم DAO
- وابستگی به Backend Provider

### MainCourse

مجاز:
- Lesson
- Quiz
- Exercise
- Project
- Curriculum

غیرمجاز:
- Runtime Implementation
- Persistence
- Sync Logic
- Update Logic

### Core

مالک:
- Runtime Composition
- Persistence
- Repository Contract
- Update Infrastructure
- Backend Boundary

## نتیجه

معماری Foundation بر اساس Single Runtime Owner طراحی شده و توسعه Appهای آینده باید فقط از همین Contract استفاده کند.
