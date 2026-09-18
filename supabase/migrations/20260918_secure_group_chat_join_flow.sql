-- Secure group-chat discovery, direct joining, and approval requests.

alter table public.chat_rooms
  add column if not exists join_mode text not null default 'open'
  check (join_mode in ('open', 'approval'));

create index if not exists chat_rooms_group_discovery_idx
  on public.chat_rooms (type, join_mode, created_at);

create table if not exists public.chat_group_join_requests (
  id uuid primary key default gen_random_uuid(),
  room_id uuid not null references public.chat_rooms(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  status text not null default 'pending'
    check (status in ('pending','approved','declined','cancelled')),
  requested_at timestamptz not null default now(),
  reviewed_at timestamptz,
  reviewed_by uuid references auth.users(id),
  unique (room_id, user_id)
);

create index if not exists chat_group_join_requests_user_idx
  on public.chat_group_join_requests (user_id, status);

create index if not exists chat_group_join_requests_room_idx
  on public.chat_group_join_requests (room_id, status);

alter table public.chat_group_join_requests enable row level security;

create policy "Chat join requests own read"
  on public.chat_group_join_requests for select to authenticated
  using (user_id = (select auth.uid()));

create policy "Chat join requests group manager read"
  on public.chat_group_join_requests for select to authenticated
  using (
    exists (
      select 1
      from public.chat_rooms r
      left join public.chat_room_members m
        on m.room_id = r.id and m.user_id = (select auth.uid())
      where r.id = chat_group_join_requests.room_id
        and r.type = 'group'
        and (r.created_by = (select auth.uid()) or m.role = 'moderator')
    )
  );

create policy "Chat groups discover"
  on public.chat_rooms for select to authenticated
  using (type = 'group');

create or replace function public.join_chat_group(p_room_id uuid)
returns uuid language plpgsql security definer set search_path = public as $$
declare uid uuid := auth.uid(); room public.chat_rooms%rowtype;
begin
  if uid is null then raise exception 'Authentication required'; end if;
  select * into room from public.chat_rooms where id = p_room_id and type = 'group';
  if room.id is null then raise exception 'Group not found'; end if;
  if room.join_mode <> 'open' then raise exception 'This group requires approval'; end if;
  insert into public.chat_room_members(room_id,user_id,role)
  values(room.id,uid,'member') on conflict (room_id,user_id) do nothing;
  return room.id;
end; $$;

revoke all on function public.join_chat_group(uuid) from public, anon;
grant execute on function public.join_chat_group(uuid) to authenticated;

create or replace function public.request_chat_group_join(p_room_id uuid)
returns uuid language plpgsql security definer set search_path = public as $$
declare uid uuid := auth.uid(); room public.chat_rooms%rowtype;
begin
  if uid is null then raise exception 'Authentication required'; end if;
  select * into room from public.chat_rooms where id=p_room_id and type='group';
  if room.id is null then raise exception 'Group not found'; end if;
  if room.join_mode <> 'approval' then raise exception 'This group can be joined directly'; end if;
  if exists(select 1 from public.chat_room_members where room_id=room.id and user_id=uid) then return room.id; end if;
  insert into public.chat_group_join_requests(room_id,user_id,status)
  values(room.id,uid,'pending')
  on conflict(room_id,user_id) do update set
    status=case when public.chat_group_join_requests.status in ('declined','cancelled') then 'pending' else public.chat_group_join_requests.status end,
    requested_at=case when public.chat_group_join_requests.status in ('declined','cancelled') then now() else public.chat_group_join_requests.requested_at end,
    reviewed_at=case when public.chat_group_join_requests.status in ('declined','cancelled') then null else public.chat_group_join_requests.reviewed_at end,
    reviewed_by=case when public.chat_group_join_requests.status in ('declined','cancelled') then null else public.chat_group_join_requests.reviewed_by end;
  return room.id;
end; $$;

revoke all on function public.request_chat_group_join(uuid) from public, anon;
grant execute on function public.request_chat_group_join(uuid) to authenticated;

create or replace function public.review_chat_group_join(p_request_id uuid,p_approve boolean)
returns uuid language plpgsql security definer set search_path = public as $$
declare uid uuid := auth.uid(); req public.chat_group_join_requests%rowtype; room public.chat_rooms%rowtype; member_role text;
begin
  if uid is null then raise exception 'Authentication required'; end if;
  select * into req from public.chat_group_join_requests where id=p_request_id and status='pending';
  if req.id is null then raise exception 'Join request not found or already reviewed'; end if;
  select * into room from public.chat_rooms where id=req.room_id and type='group';
  if room.id is null then raise exception 'Group not found'; end if;
  select role into member_role from public.chat_room_members where room_id=room.id and user_id=uid;
  if room.created_by <> uid and coalesce(member_role,'') <> 'moderator' then raise exception 'You are not allowed to review requests for this group'; end if;
  if p_approve then
    insert into public.chat_room_members(room_id,user_id,role) values(room.id,req.user_id,'member')
      on conflict(room_id,user_id) do nothing;
    update public.chat_group_join_requests set status='approved',reviewed_at=now(),reviewed_by=uid where id=req.id;
  else
    update public.chat_group_join_requests set status='declined',reviewed_at=now(),reviewed_by=uid where id=req.id;
  end if;
  return room.id;
end; $$;

revoke all on function public.review_chat_group_join(uuid,boolean) from public, anon;
grant execute on function public.review_chat_group_join(uuid,boolean) to authenticated;

update public.chat_rooms set join_mode='open' where type='community';
update public.chat_rooms set join_mode='open'
where id='858462bf-f9a9-4892-a5b3-b59c9f0c2702' and type='group';
