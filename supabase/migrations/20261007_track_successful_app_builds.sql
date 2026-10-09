-- Track successful production builds separately from the published update.
-- This prevents a new build from pointing member devices at an older APK URL.

alter table public.app_update_config
  add column if not exists latest_build_version_code integer,
  add column if not exists latest_build_version_name text,
  add column if not exists latest_build_at timestamptz,
  add column if not exists latest_build_commit text;

alter table public.app_update_config
  drop constraint if exists app_update_config_latest_build_version_code_check;

alter table public.app_update_config
  add constraint app_update_config_latest_build_version_code_check
  check (latest_build_version_code is null or latest_build_version_code > 0);

comment on column public.app_update_config.latest_build_version_code is
  'Version code from the most recent successful production release build.';
comment on column public.app_update_config.latest_build_version_name is
  'Version name from the most recent successful production release build.';
comment on column public.app_update_config.latest_build_at is
  'Timestamp of the most recent successful production release build.';
comment on column public.app_update_config.latest_build_commit is
  'Git commit SHA that produced the most recent successful production release build.';
