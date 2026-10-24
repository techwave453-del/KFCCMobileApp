-- Enable narrowly scoped direct-chat tables for authenticated realtime refresh.
-- RLS still controls which rows each authenticated subscriber may receive.
do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'chat_direct_messages'
    ) then
        execute 'alter publication supabase_realtime add table public.chat_direct_messages';
    end if;

    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'chat_direct_conversations'
    ) then
        execute 'alter publication supabase_realtime add table public.chat_direct_conversations';
    end if;
end;
$$;
