-- AS Academy Supabase backend schema.
-- Apply only to the dedicated AS Academy project after the project is explicitly identified.

create extension if not exists pgcrypto;

create table if not exists public.academy_course_releases (
    course_id text not null,
    version text not null,
    content_schema_version integer not null check (content_schema_version > 0),
    minimum_core_version text not null,
    sha256 text not null check (sha256 ~ '^[0-9a-fA-F]{64}$'),
    object_path text not null check (object_path <> '' and object_path !~ '(^|/)\.\.(/|$)'),
    published_at timestamptz not null default now(),
    primary key (course_id, version)
);

create index if not exists academy_course_releases_latest_idx
    on public.academy_course_releases (course_id, published_at desc);

alter table public.academy_course_releases enable row level security;

revoke all on table public.academy_course_releases from anon, authenticated;
grant select on table public.academy_course_releases to anon, authenticated;

-- Course release metadata is public catalog data. Mutations remain server/admin-only because
-- anon/authenticated have SELECT only; no client INSERT/UPDATE/DELETE grants are issued.
drop policy if exists academy_course_releases_read on public.academy_course_releases;
create policy academy_course_releases_read
on public.academy_course_releases
for select
to anon, authenticated
using (true);

create table if not exists public.academy_user_sync_events (
    operation_id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    course_id text not null,
    entity_type text not null,
    entity_id text not null,
    payload jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now(),
    deleted_at timestamptz null,
    unique (user_id, course_id, entity_type, entity_id)
);

create index if not exists academy_user_sync_events_owner_idx
    on public.academy_user_sync_events (user_id, course_id, updated_at desc);

alter table public.academy_user_sync_events enable row level security;

revoke all on table public.academy_user_sync_events from anon, authenticated;
grant select, insert, update, delete on table public.academy_user_sync_events to authenticated;

-- Ownership is enforced for every operation. UPDATE has both USING and WITH CHECK so a row
-- cannot be reassigned to another user through a client request.
drop policy if exists academy_user_sync_events_select_own on public.academy_user_sync_events;
create policy academy_user_sync_events_select_own
on public.academy_user_sync_events
for select
to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists academy_user_sync_events_insert_own on public.academy_user_sync_events;
create policy academy_user_sync_events_insert_own
on public.academy_user_sync_events
for insert
to authenticated
with check ((select auth.uid()) = user_id);

drop policy if exists academy_user_sync_events_update_own on public.academy_user_sync_events;
create policy academy_user_sync_events_update_own
on public.academy_user_sync_events
for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

drop policy if exists academy_user_sync_events_delete_own on public.academy_user_sync_events;
create policy academy_user_sync_events_delete_own
on public.academy_user_sync_events
for delete
to authenticated
using ((select auth.uid()) = user_id);

-- Private course package bucket. Publishing must happen through trusted deployment/admin tooling,
-- never with a service-role key embedded in an Android client.
insert into storage.buckets (id, name, public)
values ('academy-courses', 'academy-courses', false)
on conflict (id) do update set public = excluded.public;

-- Clients may read only objects that are actually referenced by the published release catalog.
-- Signed URL creation therefore inherits the same release-level authorization boundary.
drop policy if exists academy_course_objects_read_published on storage.objects;
create policy academy_course_objects_read_published
on storage.objects
for select
to anon, authenticated
using (
    bucket_id = 'academy-courses'
    and exists (
        select 1
        from public.academy_course_releases release
        where release.object_path = storage.objects.name
    )
);

comment on table public.academy_course_releases is
    'Immutable release metadata consumed by AS Academy Core for version/checksum discovery.';
comment on table public.academy_user_sync_events is
    'User-owned idempotent sync records; RLS limits each user to their own rows.';
