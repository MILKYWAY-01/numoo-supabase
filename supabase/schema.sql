-- Run this in Supabase: SQL Editor → New query → paste → Run.
-- Fixes: "Could not find the table 'public.app_limits'" and enables blocking sync.
--
-- If a line errors with "already member of publication", remove or comment out that
-- `alter publication supabase_realtime add table ...` line and run the rest again.

-- ---------------------------------------------------------------------------
-- app_limits (admin sets per child user)
-- ---------------------------------------------------------------------------
create table if not exists public.app_limits (
    user_id text not null,
    package_name text not null,
    app_name text not null default '',
    limit_millis bigint not null default 0,
    is_blocked boolean not null default false,
    set_by text not null default '',
    updated_at bigint not null default (floor(extract(epoch from now()) * 1000))::bigint,
    primary key (user_id, package_name)
);

-- Optional: enforce user_id exists in users (skip if users.uid type differs)
-- alter table public.app_limits
--   add constraint app_limits_user_id_fkey
--   foreign key (user_id) references public.users (uid) on delete cascade;

create index if not exists app_limits_user_id_idx on public.app_limits (user_id);

alter table public.app_limits enable row level security;

drop policy if exists "app_limits_select_own" on public.app_limits;
create policy "app_limits_select_own"
    on public.app_limits for select
    to authenticated
    using (user_id = auth.uid()::text);

drop policy if exists "app_limits_admin_select" on public.app_limits;
create policy "app_limits_admin_select"
    on public.app_limits for select
    to authenticated
    using (
        exists (
            select 1 from public.users u
            where u.uid = app_limits.user_id
              and u.admin_id = auth.uid()::text
        )
    );

drop policy if exists "app_limits_admin_insert" on public.app_limits;
create policy "app_limits_admin_insert"
    on public.app_limits for insert
    to authenticated
    with check (
        exists (
            select 1 from public.users u
            where u.uid = app_limits.user_id
              and u.admin_id = auth.uid()::text
        )
    );

drop policy if exists "app_limits_admin_update" on public.app_limits;
create policy "app_limits_admin_update"
    on public.app_limits for update
    to authenticated
    using (
        exists (
            select 1 from public.users u
            where u.uid = app_limits.user_id
              and u.admin_id = auth.uid()::text
        )
    )
    with check (
        exists (
            select 1 from public.users u
            where u.uid = app_limits.user_id
              and u.admin_id = auth.uid()::text
        )
    );

drop policy if exists "app_limits_admin_delete" on public.app_limits;
create policy "app_limits_admin_delete"
    on public.app_limits for delete
    to authenticated
    using (
        exists (
            select 1 from public.users u
            where u.uid = app_limits.user_id
              and u.admin_id = auth.uid()::text
        )
    );

-- Realtime (Dashboard + AppBlockerService listener)
alter publication supabase_realtime add table public.app_limits;

-- ---------------------------------------------------------------------------
-- usage_data (if missing — same error pattern as app_limits)
-- ---------------------------------------------------------------------------
create table if not exists public.usage_data (
    uid text not null,
    date text not null,
    package_name text not null,
    app_name text not null default '',
    usage_time_millis bigint not null default 0,
    last_updated bigint not null default (floor(extract(epoch from now()) * 1000))::bigint,
    primary key (uid, date, package_name)
);

create index if not exists usage_data_uid_date_idx on public.usage_data (uid, date);

alter table public.usage_data enable row level security;

drop policy if exists "usage_data_select_own" on public.usage_data;
create policy "usage_data_select_own"
    on public.usage_data for select
    to authenticated
    using (uid = auth.uid()::text);

drop policy if exists "usage_data_insert_own" on public.usage_data;
create policy "usage_data_insert_own"
    on public.usage_data for insert
    to authenticated
    with check (uid = auth.uid()::text);

drop policy if exists "usage_data_update_own" on public.usage_data;
create policy "usage_data_update_own"
    on public.usage_data for update
    to authenticated
    using (uid = auth.uid()::text)
    with check (uid = auth.uid()::text);

drop policy if exists "usage_data_admin_select" on public.usage_data;
create policy "usage_data_admin_select"
    on public.usage_data for select
    to authenticated
    using (
        exists (
            select 1 from public.users u
            where u.uid = usage_data.uid
              and u.admin_id = auth.uid()::text
        )
    );

-- (Optional) Realtime on usage_data — skip if you see "already member of publication"
-- alter publication supabase_realtime add table public.usage_data;
