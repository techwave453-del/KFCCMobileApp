-- Keep chat group authorization aligned with the authoritative administrator identity.
-- The app-level AdminUser check is not an authorization boundary; chat_rooms RLS
-- must validate the authenticated Supabase user against admin_users.

create or replace function public.is_chat_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $function$
  select exists (
    select 1
    from auth.users u
    join public.admin_users a
      on a.id = nullif(u.raw_app_meta_data->>'admin_user_id', '')::bigint
    where u.id = (select auth.uid())
      and a.is_active = true
      and a.role in ('admin', 'super_admin')
      and coalesce(u.raw_app_meta_data->>'kfcc_admin', 'false') = 'true'
  );
$function$;

revoke execute on function public.is_chat_admin() from public;
grant execute on function public.is_chat_admin() to authenticated;
