-- Return a bounded set of member profiles for the horizontal People row.
-- This intentionally exposes only public chat identity fields, never emails or admin metadata.
create or replace function public.get_chat_member_suggestions()
returns table(user_id uuid, username text, display_name text, avatar_url text)
language plpgsql
security definer
set search_path = public
as $$
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    return query
    select p.user_id, p.username, p.display_name, p.avatar_url
    from public.chat_profiles p
    where p.user_id <> auth.uid()
    order by lower(coalesce(p.display_name, p.username)), lower(p.username)
    limit 25;
end;
$$;

revoke all on function public.get_chat_member_suggestions() from public, anon;
grant execute on function public.get_chat_member_suggestions() to authenticated;
