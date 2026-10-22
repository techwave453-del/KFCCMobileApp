-- Kanisa chat inbox foundation: private conversations, contacts, and stories.
-- Additive migration: existing community/group chat tables and policies are untouched.
-- Apply through the Supabase migration workflow after reviewing against the target project.

begin;

create table if not exists public.chat_direct_conversations (
    id uuid primary key default gen_random_uuid(),
    participant_a uuid not null references auth.users(id) on delete cascade,
    participant_b uuid not null references auth.users(id) on delete cascade,
    created_by uuid not null references auth.users(id) on delete cascade default auth.uid(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    last_message_at timestamptz not null default now(),
    last_message_preview text,
    last_message_sender_id uuid references auth.users(id) on delete set null,
    participant_a_last_read_at timestamptz,
    participant_b_last_read_at timestamptz,
    constraint chat_direct_conversations_distinct_participants check (participant_a <> participant_b),
    constraint chat_direct_conversations_canonical_pair check (participant_a < participant_b),
    constraint chat_direct_conversations_pair_unique unique (participant_a, participant_b)
);

create table if not exists public.chat_direct_messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid not null references public.chat_direct_conversations(id) on delete cascade,
    sender_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
    body text not null,
    client_message_id uuid,
    created_at timestamptz not null default now(),
    edited_at timestamptz,
    deleted_at timestamptz,
    constraint chat_direct_messages_body_length check (char_length(btrim(body)) between 1 and 4000),
    constraint chat_direct_messages_client_id_unique unique (sender_id, client_message_id)
);

create index if not exists chat_direct_messages_conversation_created_idx
    on public.chat_direct_messages (conversation_id, created_at desc);
create index if not exists chat_direct_conversations_participant_a_idx
    on public.chat_direct_conversations (participant_a, last_message_at desc);
create index if not exists chat_direct_conversations_participant_b_idx
    on public.chat_direct_conversations (participant_b, last_message_at desc);

create table if not exists public.chat_direct_read_receipts (
    conversation_id uuid not null references public.chat_direct_conversations(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
    last_read_at timestamptz not null default now(),
    primary key (conversation_id, user_id)
);

create table if not exists public.chat_contacts (
    user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
    contact_user_id uuid not null references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, contact_user_id),
    constraint chat_contacts_not_self check (user_id <> contact_user_id)
);
create index if not exists chat_contacts_contact_user_idx on public.chat_contacts(contact_user_id);

create table if not exists public.chat_stories (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
    media_path text not null,
    media_type text not null check (media_type in ('image', 'video')),
    caption text not null default '',
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '24 hours'),
    constraint chat_stories_caption_length check (char_length(caption) <= 500),
    constraint chat_stories_expiry_window check (expires_at > created_at and expires_at <= created_at + interval '25 hours')
);
create index if not exists chat_stories_active_idx on public.chat_stories(expires_at desc, created_at desc);
create index if not exists chat_stories_owner_idx on public.chat_stories(user_id, created_at desc);

create table if not exists public.chat_story_views (
    story_id uuid not null references public.chat_stories(id) on delete cascade,
    viewer_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
    viewed_at timestamptz not null default now(),
    primary key (story_id, viewer_id)
);

alter table public.chat_direct_conversations enable row level security;
alter table public.chat_direct_messages enable row level security;
alter table public.chat_direct_read_receipts enable row level security;
alter table public.chat_contacts enable row level security;
alter table public.chat_stories enable row level security;
alter table public.chat_story_views enable row level security;

drop policy if exists "Participants can read direct conversations" on public.chat_direct_conversations;
create policy "Participants can read direct conversations"
on public.chat_direct_conversations for select to authenticated
using (auth.uid() = participant_a or auth.uid() = participant_b);

drop policy if exists "Participants can update direct conversation read state" on public.chat_direct_conversations;
drop policy if exists "Users can read their direct read receipts" on public.chat_direct_read_receipts;
create policy "Users can read their direct read receipts"
on public.chat_direct_read_receipts for select to authenticated
using (
    user_id = auth.uid()
    and exists (
        select 1 from public.chat_direct_conversations c
        where c.id = conversation_id
          and auth.uid() in (c.participant_a, c.participant_b)
    )
);

drop policy if exists "Users can write their direct read receipts" on public.chat_direct_read_receipts;
create policy "Users can write their direct read receipts"
on public.chat_direct_read_receipts for insert to authenticated
with check (
    user_id = auth.uid()
    and exists (
        select 1 from public.chat_direct_conversations c
        where c.id = conversation_id
          and auth.uid() in (c.participant_a, c.participant_b)
    )
);

drop policy if exists "Users can update their direct read receipts" on public.chat_direct_read_receipts;
create policy "Users can update their direct read receipts"
on public.chat_direct_read_receipts for update to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid());

drop policy if exists "Participants can read direct messages" on public.chat_direct_messages;
create policy "Participants can read direct messages"
on public.chat_direct_messages for select to authenticated
using (exists (
    select 1 from public.chat_direct_conversations c
    where c.id = conversation_id
      and auth.uid() in (c.participant_a, c.participant_b)
));

drop policy if exists "Participants can send their own direct messages" on public.chat_direct_messages;
create policy "Participants can send their own direct messages"
on public.chat_direct_messages for insert to authenticated
with check (
    sender_id = auth.uid()
    and exists (
        select 1 from public.chat_direct_conversations c
        where c.id = conversation_id
          and auth.uid() in (c.participant_a, c.participant_b)
    )
);

drop policy if exists "Senders can edit their own direct messages" on public.chat_direct_messages;
create policy "Senders can edit their own direct messages"
on public.chat_direct_messages for update to authenticated
using (sender_id = auth.uid())
with check (sender_id = auth.uid());

drop policy if exists "Users can read their own contacts" on public.chat_contacts;
create policy "Users can read their own contacts"
on public.chat_contacts for select to authenticated
using (user_id = auth.uid());

drop policy if exists "Users can add their own contacts" on public.chat_contacts;
create policy "Users can add their own contacts"
on public.chat_contacts for insert to authenticated
with check (
    user_id = auth.uid()
    and contact_user_id <> auth.uid()
    and exists (select 1 from public.chat_profiles p where p.user_id = contact_user_id)
);

drop policy if exists "Users can remove their own contacts" on public.chat_contacts;
create policy "Users can remove their own contacts"
on public.chat_contacts for delete to authenticated
using (user_id = auth.uid());

drop policy if exists "Authenticated users can view active stories" on public.chat_stories;
create policy "Authenticated users can view active stories"
on public.chat_stories for select to authenticated
using (expires_at > now());

drop policy if exists "Users can publish their own stories" on public.chat_stories;
create policy "Users can publish their own stories"
on public.chat_stories for insert to authenticated
with check (
    user_id = auth.uid()
    and expires_at > now()
    and media_path like auth.uid()::text || '/%'
);

drop policy if exists "Owners can update their own stories" on public.chat_stories;
create policy "Owners can update their own stories"
on public.chat_stories for update to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid() and media_path like auth.uid()::text || '/%');

drop policy if exists "Owners can delete their own stories" on public.chat_stories;
create policy "Owners can delete their own stories"
on public.chat_stories for delete to authenticated
using (user_id = auth.uid());

drop policy if exists "Viewers can read story views they are involved in" on public.chat_story_views;
create policy "Viewers can read story views they are involved in"
on public.chat_story_views for select to authenticated
using (
    viewer_id = auth.uid()
    or exists (select 1 from public.chat_stories s where s.id = story_id and s.user_id = auth.uid())
);

drop policy if exists "Users can mark active stories viewed" on public.chat_story_views;
create policy "Users can mark active stories viewed"
on public.chat_story_views for insert to authenticated
with check (
    viewer_id = auth.uid()
    and exists (
        select 1 from public.chat_stories s
        where s.id = story_id and s.expires_at > now() and s.user_id <> auth.uid()
    )
);

create or replace function public.touch_direct_conversation_after_message()
returns trigger
language plpgsql
security definer
set search_path = public
as $
begin
    update public.chat_direct_conversations
       set updated_at = new.created_at,
           last_message_at = new.created_at,
           last_message_preview = left(new.body, 180),
           last_message_sender_id = new.sender_id
     where id = new.conversation_id;
    return new;
end;
$;

drop trigger if exists chat_direct_message_updates_conversation on public.chat_direct_messages;
create trigger chat_direct_message_updates_conversation
after insert on public.chat_direct_messages
for each row execute function public.touch_direct_conversation_after_message();

-- Private story media is stored under a per-user folder. Storage policies enforce
-- the same ownership boundary as the story metadata policies.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('kanisa-story-media', 'kanisa-story-media', false, 52428800,
        array['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/quicktime'])
on conflict (id) do nothing;

drop policy if exists "Users can upload their own story media" on storage.objects;
create policy "Users can upload their own story media"
on storage.objects for insert to authenticated
with check (
    bucket_id = 'kanisa-story-media'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "Users can read media for active stories" on storage.objects;
create policy "Users can read media for active stories"
on storage.objects for select to authenticated
using (
    bucket_id = 'kanisa-story-media'
    and exists (
        select 1 from public.chat_stories s
        where s.media_path = name and s.expires_at > now()
    )
);

drop policy if exists "Owners can update their own story media" on storage.objects;
create policy "Owners can update their own story media"
on storage.objects for update to authenticated
using (
    bucket_id = 'kanisa-story-media'
    and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
    bucket_id = 'kanisa-story-media'
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "Owners can delete their own story media" on storage.objects;
create policy "Owners can delete their own story media"
on storage.objects for delete to authenticated
using (
    bucket_id = 'kanisa-story-media'
    and (storage.foldername(name))[1] = auth.uid()::text
);

-- The function creates/reuses one canonical conversation for a pair. Clients cannot
-- create arbitrary conversations or add a third participant to a private thread.
create or replace function public.start_direct_conversation(p_other_user_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public, auth
as $$
declare
    v_user_id uuid := auth.uid();
    v_a uuid;
    v_b uuid;
    v_conversation_id uuid;
begin
    if v_user_id is null then
        raise exception 'Authentication required';
    end if;
    if p_other_user_id is null or p_other_user_id = v_user_id then
        raise exception 'Choose another member to start a conversation';
    end if;
    if not exists (select 1 from public.chat_profiles p where p.user_id = p_other_user_id) then
        raise exception 'That member profile was not found';
    end if;

    v_a := least(v_user_id, p_other_user_id);
    v_b := greatest(v_user_id, p_other_user_id);

    insert into public.chat_direct_conversations(participant_a, participant_b, created_by)
    values (v_a, v_b, v_user_id)
    on conflict (participant_a, participant_b) do update
       set updated_at = public.chat_direct_conversations.updated_at
    returning id into v_conversation_id;

    return v_conversation_id;
end;
$$;

revoke all on function public.start_direct_conversation(uuid) from public;
grant execute on function public.start_direct_conversation(uuid) to authenticated;

create or replace function public.add_chat_contact(p_contact_user_id uuid)
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
declare
    v_user_id uuid := auth.uid();
begin
    if v_user_id is null then raise exception 'Authentication required'; end if;
    if p_contact_user_id is null or p_contact_user_id = v_user_id then
        raise exception 'Choose another member as a contact';
    end if;
    if not exists (select 1 from public.chat_profiles p where p.user_id = p_contact_user_id) then
        raise exception 'That member profile was not found';
    end if;
    insert into public.chat_contacts(user_id, contact_user_id)
    values (v_user_id, p_contact_user_id)
    on conflict do nothing;
end;
$$;

revoke all on function public.add_chat_contact(uuid) from public;
grant execute on function public.add_chat_contact(uuid) to authenticated;

-- Return only the minimal profile fields needed to start a conversation.
-- Search is bounded and does not expose email addresses or administrator metadata.
create or replace function public.search_chat_members(p_query text)
returns table(user_id uuid, username text, display_name text, avatar_url text)
language plpgsql
security definer
set search_path = public
as $
declare
    v_query text := lower(btrim(coalesce(p_query, '')));
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;
    if char_length(v_query) < 2 then
        return;
    end if;
    return query
    select p.user_id, p.username, p.display_name, p.avatar_url
    from public.chat_profiles p
    where p.user_id <> auth.uid()
      and (lower(p.username) like '%' || v_query || '%'
           or lower(coalesce(p.display_name, '')) like '%' || v_query || '%')
    order by lower(coalesce(p.display_name, p.username)), lower(p.username)
    limit 25;
end;
$;

revoke all on function public.search_chat_members(text) from public;
grant execute on function public.search_chat_members(text) to authenticated;

commit;
