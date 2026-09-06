# Learning State Model

## User Content States

- NOT_STARTED
- STARTED
- IN_PROGRESS
- COMPLETED
- NEEDS_REVIEW

Each lesson must support:

- progress tracking
- completion timestamp
- bookmark state
- review priority
- search indexing metadata

Core owns runtime handling of these states. Courses only provide content identifiers.
