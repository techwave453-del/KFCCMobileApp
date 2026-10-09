-- Admin-configurable Android app updates.
create table if not exists public.app_update_config (
  id text primary key,
  version_code integer not null check (version_code > 0),
  version_name text not null check (char_length(version_name) between 1 and 50),
  download_url text not null check (char_length(download_url) between 1 and 2000),
  release_notes text not null default '',
  is_enabled boolean not null default false,
  updated_at timestamptz not null default now()
);

alter table public.app_update_config enable row level security;

drop policy if exists "Public can read enabled app update" on public.app_update_config;
create policy "Public can read enabled app update"
on public.app_update_config for select to anon, authenticated
using (is_enabled = true);

drop policy if exists "Administrators manage app update" on public.app_update_config;
create policy "Administrators manage app update"
on public.app_update_config for all to authenticated
using ((select private.has_admin_permission('app_update.manage')))
with check ((select private.has_admin_permission('app_update.manage')));

insert into public.app_update_config (
  id, version_code, version_name, download_url, release_notes, is_enabled
) values (
  'android',
  2,
  '1.0.1',
  'https://drive.google.com/file/d/11SZFzGgF9wSYmYebHXpOQKFFmURhop88/view?usp=drive_link',
  'Test release for automatic app updates.',
  true
)
on conflict (id) do update set
  version_code = excluded.version_code,
  version_name = excluded.version_name,
  download_url = excluded.download_url,
  release_notes = excluded.release_notes,
  is_enabled = excluded.is_enabled,
  updated_at = now();
