alter table public.kanisa_assistant_messages
  add column if not exists bible_quotes jsonb not null default '[]'::jsonb;

comment on column public.kanisa_assistant_messages.bible_quotes is
  'Exact Bible passages supplied to Kanisa Assistant, stored as [{reference,text,translation}].';
