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

insert into public.daily_scripture_themes(slug,name,description,sort_order) values
('faith','Faith','Trusting God when circumstances are uncertain.',10),
('hope','Hope','Finding hope through setbacks, waiting, and difficult seasons.',20),
('love','Love','Living out God''s love in everyday relationships.',30),
('forgiveness','Forgiveness','Releasing resentment and practicing forgiveness.',40),
('leadership','Leadership','Leading with wisdom, humility, integrity, and service.',50),
('wisdom','Wisdom','Seeking God''s wisdom for decisions and daily life.',60),
('peace','Peace','Turning to God in seasons of worry, stress, and conflict.',70),
('courage','Courage','Facing fear, opposition, and difficult responsibilities.',80),
('patience','Patience','Waiting faithfully without giving up.',90),
('gratitude','Gratitude','Recognizing God''s goodness and cultivating thankfulness.',100),
('prayer','Prayer','Bringing needs, decisions, and desires before God.',110),
('purpose','Purpose','Seeking direction, calling, and meaning in life.',120),
('family','Family','Strengthening family relationships through biblical principles.',130),
('work','Work','Honoring God through diligence, integrity, and responsibility.',140),
('relationships','Relationships','Building healthy relationships with wisdom, grace, and kindness.',150),
('comfort','Comfort','Finding God''s comfort in grief, pain, loneliness, and hardship.',160),
('temptation','Temptation','Standing firm when facing temptation and unhealthy desires.',170),
('perseverance','Perseverance','Continuing faithfully through trials and setbacks.',180)
on conflict(slug) do update set name=excluded.name,description=excluded.description,sort_order=excluded.sort_order,updated_at=now();

insert into public.daily_scriptures(theme_id,translation_id,book_id,chapter,verse_start,verse_end,situation,reflection,priority)
select t.id,v.translation_id,v.book_id,v.chapter,v.verse_start,v.verse_end,v.situation,v.reflection,10
from public.daily_scripture_themes t join (values
('faith','kjv','HEB',11,1,1,'When you need to trust God before you can see the outcome.','Faith begins with trusting God even when the full picture is not yet visible.'),
('hope','kjv','LAM',3,22,23,'When yesterday was difficult and tomorrow feels uncertain.','God''s mercies are not exhausted. Each day gives you another reason to hope in His faithfulness.'),
('love','kjv','1CO',13,4,7,'When relationships require patience and selflessness.','Biblical love is patient, kind, and committed to the good of others.'),
('forgiveness','kjv','EPH',4,31,32,'When someone has hurt you and resentment is growing.','Forgiveness does not deny the hurt; it calls you to replace bitterness with grace and kindness.'),
('leadership','kjv','MRK',10,42,45,'When you are given responsibility over other people.','Jesus presents leadership as service rather than a search for status or control.'),
('wisdom','kjv','JAS',1,5,5,'When you need direction and do not know what to do.','Ask God for wisdom with confidence that He is willing to guide you.'),
('peace','kjv','PHP',4,6,7,'When worry is taking over your thoughts.','Bring your concerns to God in prayer and allow His peace to guard your heart and mind.'),
('courage','kjv','JOS',1,9,9,'When you are facing something intimidating.','Courage grows from remembering that God is with you wherever you go.'),
('patience','kjv','PSA',27,14,14,'When you have been waiting longer than expected.','Waiting can become an opportunity to strengthen your heart and continue trusting God.'),
('gratitude','kjv','1TH',5,16,18,'When you want to develop a thankful outlook.','Rejoicing, praying, and giving thanks can reshape how you see each day.'),
('prayer','kjv','JER',33,3,3,'When you need God''s direction or want to understand something deeply.','God invites His people to call on Him and seek what they cannot discover by themselves.'),
('purpose','kjv','EPH',2,10,10,'When you are questioning whether your life has meaning.','Your life has purpose in God''s work, including the good things He has prepared for you to do.'),
('family','kjv','COL',3,20,21,'When family relationships need patience and wisdom.','Healthy family life includes respectful obedience and thoughtful, encouraging leadership.'),
('work','kjv','COL',3,23,24,'When your work feels ordinary or frustrating.','Work can become an act of service when you approach it wholeheartedly as something done for the Lord.'),
('relationships','kjv','PRO',15,1,1,'When a conversation is becoming heated.','A gentle response can interrupt the cycle of anger and create room for peace.'),
('comfort','kjv','PSA',34,18,18,'When you are grieving or emotionally overwhelmed.','God is not distant from the brokenhearted; His presence is especially meaningful in painful seasons.'),
('temptation','kjv','1CO',10,13,13,'When you feel trapped by temptation.','Temptation is not a sign that you must give in; God provides a way to endure it.'),
('perseverance','kjv','HEB',12,1,2,'When you are tired of continuing.','Keep moving forward by fixing your attention on Jesus rather than the weight of the journey.')
) as v(slug,translation_id,book_id,chapter,verse_start,verse_end,situation,reflection) on v.slug=t.slug
on conflict(theme_id,translation_id,book_id,chapter,verse_start,verse_end) do update set situation=excluded.situation,reflection=excluded.reflection,updated_at=now();
