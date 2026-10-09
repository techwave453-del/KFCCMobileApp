create table if not exists public.bible_game_player_stats (
    user_id uuid primary key references auth.users(id) on delete cascade,
    xp integer not null default 0,
    games_played integer not null default 0,
    questions_answered integer not null default 0,
    correct_answers integer not null default 0,
    best_score integer not null default 0,
    current_streak integer not null default 0,
    best_streak integer not null default 0,
    last_played_on date,
    updated_at timestamptz not null default now()
);

alter table public.bible_game_player_stats enable row level security;

drop policy if exists "Players can read their Bible game stats" on public.bible_game_player_stats;
create policy "Players can read their Bible game stats"
on public.bible_game_player_stats
for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "Players can insert their Bible game stats" on public.bible_game_player_stats;
create policy "Players can insert their Bible game stats"
on public.bible_game_player_stats
for insert
to authenticated
with check (auth.uid() = user_id);

drop policy if exists "Players can update their Bible game stats" on public.bible_game_player_stats;
create policy "Players can update their Bible game stats"
on public.bible_game_player_stats
for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create index if not exists bible_game_player_stats_xp_idx
    on public.bible_game_player_stats (xp desc);

create or replace function public.set_bible_game_player_stats_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists bible_game_player_stats_updated_at on public.bible_game_player_stats;
create trigger bible_game_player_stats_updated_at
before update on public.bible_game_player_stats
for each row execute function public.set_bible_game_player_stats_updated_at();
