-- Allow administrators to pin Today's Scripture to a specific theme.
-- When selected_theme_id is null, the app automatically chooses an active
-- theme with available Scripture for the current day.

create table if not exists public.daily_scripture_settings (
  id integer primary key check (id = 1),
  selected_theme_id uuid references public.daily_scripture_themes(id) on delete set null,
  updated_at timestamptz not null default now()
);

alter table public.daily_scripture_settings enable row level security;

grant select on public.daily_scripture_settings to anon, authenticated;
grant insert,update,delete on public.daily_scripture_settings to authenticated;

drop policy if exists "public can read daily scripture settings" on public.daily_scripture_settings;
create policy "public can read daily scripture settings"
on public.daily_scripture_settings for select to anon,authenticated
using (true);

drop policy if exists "admins manage daily scripture settings" on public.daily_scripture_settings;
create policy "admins manage daily scripture settings"
on public.daily_scripture_settings for all to authenticated
using ((select private.has_admin_permission('daily_scripture.manage')))
with check ((select private.has_admin_permission('daily_scripture.manage')));

insert into public.daily_scripture_settings(id, selected_theme_id)
values (1, null)
on conflict (id) do nothing;
