-- Authorize chat group creation from the canonical authenticated admin profile.
-- admin_profiles maps auth_user_id directly and is_active/role are authoritative.

create or replace function public.is_chat_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $function$
  select exists (
    select 1
    from public.admin_profiles p
    where p.auth_user_id = (select auth.uid())
      and p.is_active = true
      and p.role in ('admin', 'super_admin')
  );
$function$;

revoke execute on function public.is_chat_admin() from public;
grant execute on function public.is_chat_admin() to authenticated;
