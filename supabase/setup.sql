-- Zone Finder: Supabase setup (run once in the Supabase SQL Editor)
-- Safe to re-run: uses IF NOT EXISTS / DROP POLICY IF EXISTS.

-- 1) Tables -----------------------------------------------------------------
-- Existing project? Run once: alter table public.shipments alter column day type text using day::text;

create table if not exists public.shipments (
  id          uuid primary key default gen_random_uuid(),
  day         text not null,                                  -- daily key: 'd:<name>' (or legacy YYYY-MM-DD)
  tab         text not null check (tab in ('w','t','b','g')), -- w=current, t=moved, b=bombo, g=tasweer
  pos         integer not null default 0,                     -- row order inside the tab
  data        jsonb not null default '{}'::jsonb,             -- the row fields
  version     integer not null default 1,                     -- optimistic-lock counter
  updated_at  timestamptz not null default now(),
  updated_by  uuid default auth.uid()
);

create index if not exists shipments_day_tab_pos_idx on public.shipments (day, tab, pos);

create table if not exists public.app_settings (
  key         text primary key,                               -- e.g. column widths, column mapping, keywords
  value       jsonb not null default '{}'::jsonb,
  version     integer not null default 1,
  updated_at  timestamptz not null default now(),
  updated_by  uuid default auth.uid()
);

-- 2) Version bump on every update (the app updates "where id = ? and version = ?") --

create or replace function public.bump_row()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.version    := old.version + 1;
  new.updated_at := now();
  new.updated_by := auth.uid();
  return new;
end;
$$;

drop trigger if exists shipments_bump on public.shipments;
create trigger shipments_bump before update on public.shipments
  for each row execute function public.bump_row();

drop trigger if exists app_settings_bump on public.app_settings;
create trigger app_settings_bump before update on public.app_settings
  for each row execute function public.bump_row();

-- 3) Security (RLS): only logged-in users, nothing for anonymous visitors ---------

alter table public.shipments    enable row level security;
alter table public.app_settings enable row level security;

revoke all on public.shipments    from anon;
revoke all on public.app_settings from anon;
grant select, insert, update, delete on public.shipments    to authenticated;
grant select, insert, update, delete on public.app_settings to authenticated;

drop policy if exists "auth full access" on public.shipments;
create policy "auth full access" on public.shipments
  for all to authenticated using (true) with check (true);

drop policy if exists "auth full access" on public.app_settings;
create policy "auth full access" on public.app_settings
  for all to authenticated using (true) with check (true);

-- 4) Realtime ---------------------------------------------------------------------

alter table public.shipments    replica identity full;
alter table public.app_settings replica identity full;

do $$
begin
  if not exists (select 1 from pg_publication_tables
                 where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'shipments') then
    alter publication supabase_realtime add table public.shipments;
  end if;
  if not exists (select 1 from pg_publication_tables
                 where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'app_settings') then
    alter publication supabase_realtime add table public.app_settings;
  end if;
end $$;
