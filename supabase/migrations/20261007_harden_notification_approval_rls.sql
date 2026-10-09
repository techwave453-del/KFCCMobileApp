-- Restrict member notification visibility to administrator-approved (enabled) rows.
-- Administrators with notifications.send may still review and manage disabled rows.

drop policy if exists "Notifications visible to recipient" on public.app_notifications;
drop policy if exists "notifications_owner_read" on public.app_notifications;

create policy "notifications_recipient_enabled_read"
on public.app_notifications
for select
to authenticated
using (
  (user_id = (select auth.uid()) and is_enabled = true)
  or (user_id is null and is_enabled = true)
);

create policy "notifications_admin_read"
on public.app_notifications
for select
to authenticated
using (
  (select private.has_admin_permission('notifications.send'))
);

drop policy if exists "notifications_public_defaults_read" on public.app_notifications;
create policy "notifications_public_defaults_read"
on public.app_notifications
for select
to anon
using (
  user_id is null
  and is_enabled = true
  and (show_on_install = true or show_on_sign_in = true)
);
