-- Notification personalization, images, and automatic chat-message notification records.

alter table public.app_notifications
    add column if not exists image_url text;

insert into storage.buckets (id, name, public)
values ('notification-images', 'notification-images', true)
on conflict (id) do update set public = true;

drop policy if exists "Public notification image read" on storage.objects;
create policy "Public notification image read" on storage.objects
for select to public
using (bucket_id = 'notification-images');

drop policy if exists "Admin notification image upload" on storage.objects;
create policy "Admin notification image upload" on storage.objects
for insert to authenticated
with check (
    bucket_id = 'notification-images'
    and (select private.has_admin_permission('notifications.send'))
);

drop policy if exists "Admin notification image update" on storage.objects;
create policy "Admin notification image update" on storage.objects
for update to authenticated
using (
    bucket_id = 'notification-images'
    and (select private.has_admin_permission('notifications.send'))
)
with check (
    bucket_id = 'notification-images'
    and (select private.has_admin_permission('notifications.send'))
);

drop policy if exists "Admin notification image delete" on storage.objects;
create policy "Admin notification image delete" on storage.objects
for delete to authenticated
using (
    bucket_id = 'notification-images'
    and (select private.has_admin_permission('notifications.send'))
);

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
        nullif(cp.display_name, ''),
        nullif(cp.username, ''),
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
            title,
            message,
            type,
            is_enabled,
            show_on_install,
            show_on_sign_in
        )
        values (
            recipient_id,
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

alter table public.app_notifications enable row level security;
