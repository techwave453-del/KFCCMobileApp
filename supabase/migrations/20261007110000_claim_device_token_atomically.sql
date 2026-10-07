-- Atomically claim an FCM token for the currently authenticated user.
--
-- device_tokens.token is globally unique. A single physical installation may
-- sign out of one account and sign into another, so token registration must
-- reassign the existing row rather than attempt a second insert.

create or replace function public.claim_device_token(
    p_token text,
    p_platform text default 'android'
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if auth.uid() is null then
        raise exception 'Authentication required to claim an FCM token';
    end if;

    if coalesce(trim(p_token), '') = '' then
        raise exception 'FCM token cannot be blank';
    end if;

    insert into public.device_tokens (
        user_id,
        token,
        platform
    )
    values (
        auth.uid(),
        p_token,
        coalesce(nullif(trim(p_platform), ''), 'android')
    )
    on conflict (token)
    do update set
        user_id = excluded.user_id,
        platform = excluded.platform;
end;
$$;

revoke all on function public.claim_device_token(text, text)
from public, anon, authenticated;

grant execute on function public.claim_device_token(text, text)
to authenticated;
