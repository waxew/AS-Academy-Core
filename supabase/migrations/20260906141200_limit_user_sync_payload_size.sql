-- Keep Postgres limited to important Academy metadata and user state.
-- Course packages and educational assets belong in external content storage, never sync payloads.

alter table public.academy_user_sync_events
    drop constraint if exists academy_user_sync_events_payload_size;

alter table public.academy_user_sync_events
    add constraint academy_user_sync_events_payload_size
    check (octet_length(payload::text) <= 65536)
    not valid;

alter table public.academy_user_sync_events
    validate constraint academy_user_sync_events_payload_size;

comment on constraint academy_user_sync_events_payload_size on public.academy_user_sync_events is
    'Prevents large course files or educational assets from being stored in Postgres sync rows; payloads are limited to 64 KiB.';
