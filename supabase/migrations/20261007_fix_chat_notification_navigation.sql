-- Preserve the originating chat room on chat notifications so notification taps
-- can open the exact conversation and the sender avatar can be rendered consistently.

alter table public.app_notifications
    add column if not exists room_id uuid references public.chat_rooms(id) on delete cascade;

create index if not exists app_notifications_room_idx
    on public.app_notifications(room_id);

create or replace function private.create_chat_message_notifications()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
declare
    sender_name text;
    room_title text;
    recipient_id uuid;
begin
    if auth.uid() is null or new.sender_id <> auth.uid() then
        return new;
    end if;

    if new.deleted_at is not null then
        return new;
    end if;

    select coalesce(
        nullif(cp.username, ''),
        nullif(cp.display_name, ''),
        'A church member'
    )
    into sender_name
    from public.chat_profiles cp
    where cp.user_id = new.sender_id;

    select cr.title
    into room_title
    from public.chat_rooms cr
    where cr.id = new.room_id;

    for recipient_id in
        select m.user_id
        from public.chat_room_members m
        where m.room_id = new.room_id
          and m.user_id <> new.sender_id
    loop
        insert into public.app_notifications (
            user_id,
            sender_id,
            room_id,
            title,
            message,
            type,
            is_enabled,
            show_on_install,
            show_on_sign_in
        )
        values (
            recipient_id,
            new.sender_id,
            new.room_id,
            'New message from ' || coalesce(sender_name, 'a member'),
            case
                when coalesce(room_title, '') <> ''
                    then room_title || ': ' || left(new.message, 180)
                else left(new.message, 180)
            end,
            'chat_message',
            true,
            false,
            false
        );
    end loop;

    return new;
end;
$$;

revoke all on function private.create_chat_message_notifications()
from public, anon, authenticated;

drop trigger if exists "kfcc-fcm-chat-message" on public.chat_messages;
drop trigger if exists chat_message_notification_trigger on public.chat_messages;

create trigger chat_message_notification_trigger
after insert on public.chat_messages
for each row
execute function private.create_chat_message_notifications();
