-- Push notifications for private one-to-one chat messages.
-- Uses the existing app_notifications -> Supabase webhook -> FCM pipeline.

alter table public.app_notifications
    add column if not exists conversation_id uuid
        references public.chat_direct_conversations(id) on delete cascade;

create index if not exists app_notifications_conversation_idx
    on public.app_notifications(conversation_id);

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
    -- Only accept inserts made by the authenticated sender; never notify for
    -- soft-deleted messages.
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

    select coalesce(nullif(cp.display_name, ''), nullif(cp.username, ''), 'A church member')
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
        'New message from ' || coalesce(sender_name, 'a church member'),
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

drop trigger if exists direct_chat_message_notification_trigger on public.chat_direct_messages;
create trigger direct_chat_message_notification_trigger
after insert on public.chat_direct_messages
for each row execute function private.create_direct_chat_message_notification();
