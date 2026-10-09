alter table public.bible_game_questions
  add column if not exists game_type text not null default 'quiz';

alter table public.bible_game_questions
  drop constraint if exists bible_game_questions_game_type_check;

alter table public.bible_game_questions
  add constraint bible_game_questions_game_type_check
  check (game_type in ('quiz','guess_character','memory_verse','fill_blank','daily_challenge','choose_path','journey_jerusalem','character_missions'));

create index if not exists bible_game_questions_game_type_published_idx
  on public.bible_game_questions (game_type, is_published, sort_order);

insert into public.bible_game_questions
  (game_type, category, question, options, correct_answer_index, explanation, reference, is_published, sort_order)
values
  ('guess_character','PEOPLE','I built an ark before a great flood. Who am I?','["Noah","Moses","David","Joshua"]'::jsonb,0,'Noah obeyed God and built the ark before the flood.','Genesis 6–9',true,100),
  ('guess_character','PEOPLE','I defeated a giant with a sling and a stone. Who am I?','["Jonathan","David","Saul","Samuel"]'::jsonb,1,'David trusted God and defeated Goliath.','1 Samuel 17',true,101),
  ('guess_character','PEOPLE','I was thrown into a lions'' den because I continued praying to God. Who am I?','["Daniel","Jeremiah","Joseph","Elijah"]'::jsonb,0,'Daniel remained faithful to God despite the royal decree.','Daniel 6',true,102),
  ('guess_character','PEOPLE','I was swallowed by a great fish after running from God''s call. Who am I?','["Jonah","Amos","Elisha","Isaiah"]'::jsonb,0,'Jonah eventually went to Nineveh after God called him.','Jonah 1–4',true,103),
  ('guess_character','PEOPLE','I led Israel out of Egypt and received the Law from God. Who am I?','["Aaron","Joshua","Moses","Caleb"]'::jsonb,2,'Moses led Israel out of Egypt and received God''s commandments.','Exodus 3–20',true,104),
  ('guess_character','PEOPLE','I was known for great wisdom and built the temple in Jerusalem. Who am I?','["David","Solomon","Samuel","Hezekiah"]'::jsonb,1,'Solomon asked God for wisdom and later built the temple.','1 Kings 3–8',true,105)
on conflict do nothing;