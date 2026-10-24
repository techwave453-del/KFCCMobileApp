-- Direct-message unread counts and read-state mutation.
-- All functions derive the caller from auth.uid(); clients cannot mark another user's receipt.

begin;

create or replace function public.get_direct_chat_unread_counts()
returns table(conversation_id uuid, unread_count bigint)
language sql
stable
security definer
set search_path = public
as $$
    select c.id,
           count(m.id) filter (
               where m.sender_id <> auth.uid()
                 and m.deleted_at is null
                 and m.created_at > coalesce(r.last_read_at, '-infinity'::timestamptz)
           ) as unread_count
    from public.chat_direct_conversations c
    left join public.chat_direct_read_receipts r
      on r.conversation_id = c.id and r.user_id = auth.uid()
    left join public.chat_direct_messages m
      on m.conversation_id = c.id
    where auth.uid() in (c.participant_a, c.participant_b)
    group by c.id, r.last_read_at;
$$;

create or replace function public.mark_direct_conversation_read(p_conversation_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    if not exists (
        select 1
        from public.chat_direct_conversations c
        where c.id = p_conversation_id
          and auth.uid() in (c.participant_a, c.participant_b)
    ) then
        raise exception 'Conversation not found or access denied';
    end if;

    insert into public.chat_direct_read_receipts(conversation_id, user_id, last_read_at)
    values (p_conversation_id, auth.uid(), now())
    on conflict (conversation_id, user_id)
    do update set last_read_at = excluded.last_read_at;
end;
$$;

revoke all on function public.get_direct_chat_unread_counts() from public, anon;
revoke all on function public.mark_direct_conversation_read(uuid) from public, anon;
grant execute on function public.get_direct_chat_unread_counts() to authenticated;
grant execute on function public.mark_direct_conversation_read(uuid) to authenticated;

commit;
