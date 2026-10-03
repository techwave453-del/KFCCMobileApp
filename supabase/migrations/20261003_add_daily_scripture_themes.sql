-- Daily Scripture themes and admin-controlled entries
create table if not exists public.daily_scripture_themes (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  name text not null,
  description text not null default '',
  is_active boolean not null default true,
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.daily_scriptures (
  id uuid primary key default gen_random_uuid(),
  theme_id uuid not null references public.daily_scripture_themes(id) on delete restrict,
  translation_id text not null references public.bible_translations(id) on delete restrict,
  book_id text not null references public.bible_books(id) on delete restrict,
  chapter integer not null check (chapter > 0),
  verse_start integer not null check (verse_start > 0),
  verse_end integer not null check (verse_end >= verse_start),
  situation text not null default '',
  reflection text not null default '',
  priority integer not null default 0,
  is_active boolean not null default true,
  available_from date,
  available_until date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint daily_scriptures_reference_unique unique(theme_id,translation_id,book_id,chapter,verse_start,verse_end)
);

create index if not exists daily_scriptures_active_idx on public.daily_scriptures(is_active,priority desc,theme_id);
create index if not exists daily_scriptures_theme_idx on public.daily_scriptures(theme_id);

alter table public.daily_scripture_themes enable row level security;
alter table public.daily_scriptures enable row level security;

grant select on public.daily_scripture_themes to anon, authenticated;
grant select on public.daily_scriptures to anon, authenticated;
grant insert,update,delete on public.daily_scripture_themes to authenticated;
grant insert,update,delete on public.daily_scriptures to authenticated;

drop policy if exists "public can read active daily scripture themes" on public.daily_scripture_themes;
create policy "public can read active daily scripture themes"
on public.daily_scripture_themes for select to anon,authenticated
using (is_active = true);

drop policy if exists "public can read active daily scriptures" on public.daily_scriptures;
create policy "public can read active daily scriptures"
on public.daily_scriptures for select to anon,authenticated
using (
  is_active = true
  and (available_from is null or available_from <= current_date)
  and (available_until is null or available_until >= current_date)
  and exists (
    select 1 from public.daily_scripture_themes t
    where t.id = daily_scriptures.theme_id and t.is_active = true
  )
);

drop policy if exists "admins manage daily scripture themes" on public.daily_scripture_themes;
create policy "admins manage daily scripture themes"
on public.daily_scripture_themes for all to authenticated
using ((select private.has_admin_permission('daily_scripture.manage')))
with check ((select private.has_admin_permission('daily_scripture.manage')));

drop policy if exists "admins manage daily scriptures" on public.daily_scriptures;
create policy "admins manage daily scriptures"
on public.daily_scriptures for all to authenticated
using ((select private.has_admin_permission('daily_scripture.manage')))
with check ((select private.has_admin_permission('daily_scripture.manage')));
