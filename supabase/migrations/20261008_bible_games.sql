create table if not exists public.bible_game_questions (
    id uuid primary key default gen_random_uuid(),
    category text not null default 'FAITH_AND_LIFE',
    question text not null,
    options jsonb not null,
    correct_answer_index integer not null,
    explanation text not null default '',
    reference text not null default '',
    is_published boolean not null default false,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint bible_game_questions_category_check check (
        category in ('OLD_TESTAMENT','NEW_TESTAMENT','PEOPLE','PLACES','FAITH_AND_LIFE')
    ),
    constraint bible_game_questions_options_check check (
        jsonb_typeof(options) = 'array' and jsonb_array_length(options) >= 2
    ),
    constraint bible_game_questions_correct_index_check check (
        correct_answer_index >= 0 and correct_answer_index < jsonb_array_length(options)
    )
);

create index if not exists bible_game_questions_published_idx
    on public.bible_game_questions (is_published, category, sort_order);

create or replace function public.set_bible_game_questions_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists bible_game_questions_updated_at on public.bible_game_questions;
create trigger bible_game_questions_updated_at
before update on public.bible_game_questions
for each row execute function public.set_bible_game_questions_updated_at();

alter table public.bible_game_questions enable row level security;

drop policy if exists "Anyone can read published Bible game questions" on public.bible_game_questions;
create policy "Anyone can read published Bible game questions"
on public.bible_game_questions
for select
to authenticated
using (is_published = true);

drop policy if exists "Bible game admins can read all questions" on public.bible_game_questions;
create policy "Bible game admins can read all questions"
on public.bible_game_questions
for select
to authenticated
using (
    coalesce(auth.jwt()->'app_metadata'->>'kfcc_admin', 'false') = 'true'
    and (
        coalesce(auth.jwt()->'app_metadata'->>'admin_role', '') = 'super_admin'
        or position('bible_games.manage' in coalesce(auth.jwt()->'app_metadata'->>'admin_permissions', '')) > 0
    )
);

drop policy if exists "Bible game admins can insert questions" on public.bible_game_questions;
create policy "Bible game admins can insert questions"
on public.bible_game_questions
for insert
to authenticated
with check (
    coalesce(auth.jwt()->'app_metadata'->>'kfcc_admin', 'false') = 'true'
    and (
        coalesce(auth.jwt()->'app_metadata'->>'admin_role', '') = 'super_admin'
        or position('bible_games.manage' in coalesce(auth.jwt()->'app_metadata'->>'admin_permissions', '')) > 0
    )
);

drop policy if exists "Bible game admins can update questions" on public.bible_game_questions;
create policy "Bible game admins can update questions"
on public.bible_game_questions
for update
to authenticated
using (
    coalesce(auth.jwt()->'app_metadata'->>'kfcc_admin', 'false') = 'true'
    and (
        coalesce(auth.jwt()->'app_metadata'->>'admin_role', '') = 'super_admin'
        or position('bible_games.manage' in coalesce(auth.jwt()->'app_metadata'->>'admin_permissions', '')) > 0
    )
)
with check (
    coalesce(auth.jwt()->'app_metadata'->>'kfcc_admin', 'false') = 'true'
    and (
        coalesce(auth.jwt()->'app_metadata'->>'admin_role', '') = 'super_admin'
        or position('bible_games.manage' in coalesce(auth.jwt()->'app_metadata'->>'admin_permissions', '')) > 0
    )
);

drop policy if exists "Bible game admins can delete questions" on public.bible_game_questions;
create policy "Bible game admins can delete questions"
on public.bible_game_questions
for delete
to authenticated
using (
    coalesce(auth.jwt()->'app_metadata'->>'kfcc_admin', 'false') = 'true'
    and (
        coalesce(auth.jwt()->'app_metadata'->>'admin_role', '') = 'super_admin'
        or position('bible_games.manage' in coalesce(auth.jwt()->'app_metadata'->>'admin_permissions', '')) > 0
    )
);
