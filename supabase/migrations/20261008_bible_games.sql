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
to anon, authenticated
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

insert into public.bible_game_questions
    (category, question, options, correct_answer_index, explanation, reference, is_published, sort_order)
select *
from (values
    ('OLD_TESTAMENT','Who built the ark?','["Abraham","Noah","Moses","David"]'::jsonb,1,'God instructed Noah to build the ark before the flood.','Genesis 6–7',true,10),
    ('OLD_TESTAMENT','Who led the Israelites out of Egypt?','["Joshua","Aaron","Moses","Joseph"]'::jsonb,2,'Moses was called to lead Israel out of slavery in Egypt.','Exodus 3–14',true,20),
    ('PEOPLE','Who defeated Goliath?','["Saul","David","Jonathan","Samuel"]'::jsonb,1,'David faced Goliath and defeated him with a sling and a stone.','1 Samuel 17',true,30),
    ('PEOPLE','What did Solomon ask God for?','["Wealth","Long life","Wisdom","Military power"]'::jsonb,2,'Solomon asked for an understanding heart so he could govern God’s people wisely.','1 Kings 3',true,40),
    ('PEOPLE','Which prophet was sent to Nineveh?','["Jonah","Elijah","Isaiah","Jeremiah"]'::jsonb,0,'God sent Jonah to preach to the people of Nineveh.','Jonah 1–4',true,50),
    ('PEOPLE','Where was Daniel thrown because of his faithfulness to God?','["A furnace","A lion''s den","A prison ship","A cave"]'::jsonb,1,'Daniel was thrown into the lions’ den after continuing to pray to God.','Daniel 6',true,60),
    ('PLACES','In which town was Jesus born?','["Nazareth","Jerusalem","Bethlehem","Capernaum"]'::jsonb,2,'Jesus was born in Bethlehem of Judea.','Matthew 2; Luke 2',true,70),
    ('NEW_TESTAMENT','How many apostles did Jesus appoint?','["10","12","40","70"]'::jsonb,1,'Jesus appointed twelve apostles to be with Him and to preach.','Mark 3:13–19',true,80),
    ('PEOPLE','Which disciple walked on water toward Jesus?','["John","Peter","Andrew","Thomas"]'::jsonb,1,'Peter stepped out of the boat and walked toward Jesus on the water.','Matthew 14:22–33',true,90),
    ('NEW_TESTAMENT','Who told the parable of the Good Samaritan?','["Peter","Paul","Jesus","James"]'::jsonb,2,'Jesus used the parable to teach about loving and showing mercy to our neighbor.','Luke 10:25–37',true,100),
    ('NEW_TESTAMENT','What was Paul’s name before his conversion?','["Saul","Simon","Stephen","Silas"]'::jsonb,0,'Paul was known as Saul before his encounter with Jesus on the road to Damascus.','Acts 9',true,110),
    ('FAITH_AND_LIFE','Which of these is listed as a fruit of the Spirit?','["Jealousy","Patience","Pride","Greed"]'::jsonb,1,'Patience is one of the qualities Paul lists as fruit produced by the Spirit.','Galatians 5:22–23',true,120)
) as seed(category, question, options, correct_answer_index, explanation, reference, is_published, sort_order)
where not exists (select 1 from public.bible_game_questions);

