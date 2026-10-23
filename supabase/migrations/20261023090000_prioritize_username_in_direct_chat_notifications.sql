-- Prefer the sender's username for private-chat notification identity.
-- Keep display_name only as a fallback for accounts without a username.

create or replace function private.create_direct_chat_message_notification()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
declare
    recipient_id uuid;
    sender_name text;
begin
    if auth.uid() is null or new.sender_id <> auth.uid() or new.deleted_at is not null then
        return new;
    end if;

    select case
        when c.participant_a = new.sender_id then c.participant_b
        else c.participant_a
    end
    into recipient_id
    from public.chat_direct_conversations c
    where c.id = new.conversation_id
      and (c.participant_a = new.sender_id or c.participant_b = new.sender_id);

    if recipient_id is null then
        return new;
    end if;

    select coalesce(nullif(trim(cp.username), ''), nullif(trim(cp.display_name), ''), 'Church member')
    into sender_name
    from public.chat_profiles cp
    where cp.user_id = new.sender_id;

    insert into public.app_notifications (
        user_id, sender_id, conversation_id, title, message, type,
        is_enabled, show_on_install, show_on_sign_in
    )
    values (
        recipient_id,
        new.sender_id,
        new.conversation_id,
        'New message from ' || coalesce(sender_name, 'Church member'),
        left(new.body, 180),
        'direct_chat_message',
        true,
        false,
        false
    );

    return new;
end;
$$;

revoke all on function private.create_direct_chat_message_notification()
from public, anon, authenticated;
