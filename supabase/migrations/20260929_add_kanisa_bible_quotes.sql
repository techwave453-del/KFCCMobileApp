-- Keep the exact Scripture text used by Kanisa Assistant with each persisted AI reply.
-- This allows the mobile UI to render the passage directly beneath the explanation,
-- even after the conversation is reloaded.
alter table public.kanisa_room_messages
  add column if not exists bible_quotes jsonb not null default '[]'::jsonb;

comment on column public.kanisa_room_messages.bible_quotes is
  'Exact Bible passages supplied to Kanisa Assistant, stored as [{reference,text,translation}].';
