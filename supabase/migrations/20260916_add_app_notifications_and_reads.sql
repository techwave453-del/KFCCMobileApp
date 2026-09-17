create table if not exists public.app_notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade,
  title text not null check (char_length(title) between 1 and 160),
  message text not null check (char_length(message) between 1 and 2000),
  type text not null default 'general',
  created_at timestamptz not null default now()
);

create table if not exists public.notification_reads (
  notification_id uuid not null references public.app_notifications(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  read_at timestamptz not null default now(),
  primary key (notification_id, user_id)
);

create index if not exists app_notifications_user_created_idx on public.app_notifications(user_id, created_at desc);
create index if not exists notification_reads_user_idx on public.notification_reads(user_id, read_at desc);

alter table public.app_notifications enable row level security;
alter table public.notification_reads enable row level security;

drop policy if exists "Notifications visible to recipient" on public.app_notifications;
create policy "Notifications visible to recipient"
on public.app_notifications for select
to authenticated
using (user_id is null or user_id = auth.uid());

drop policy if exists "Users read own notification receipts" on public.notification_reads;
create policy "Users read own notification receipts"
on public.notification_reads for select
to authenticated
using (user_id = auth.uid());

drop policy if exists "Users create own notification receipts" on public.notification_reads;
create policy "Users create own notification receipts"
on public.notification_reads for insert
to authenticated
with check (user_id = auth.uid());

drop policy if exists "Users update own notification receipts" on public.notification_reads;
create policy "Users update own notification receipts"
on public.notification_reads for update
to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid());
