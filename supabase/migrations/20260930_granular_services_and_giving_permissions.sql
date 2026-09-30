-- Keep Services and Giving editable through their dedicated permissions
-- instead of requiring the broad site.edit permission.

drop policy if exists site_content_admin_insert on public.site_content;
drop policy if exists site_content_admin_update on public.site_content;
drop policy if exists site_content_admin_delete on public.site_content;

create policy site_content_admin_insert
on public.site_content
for insert
to authenticated
with check (
  (
    key = any (array['churchName','email','phone'])
    and (select private.is_admin_super_admin())
  )
  or (
    key = 'liveStream'
    and (select private.has_admin_permission('live.manage'))
  )
  or (
    key = 'services'
    and (select private.has_admin_permission('content.services.edit'))
  )
  or (
    key = 'givingUrl'
    and (select private.has_admin_permission('content.links.edit'))
  )
  or (
    key not in ('churchName','email','phone','liveStream','services','givingUrl')
    and (select private.has_admin_permission('site.edit'))
  )
);

create policy site_content_admin_update
on public.site_content
for update
to authenticated
using (
  (
    key = any (array['churchName','email','phone'])
    and (select private.is_admin_super_admin())
  )
  or (
    key = 'liveStream'
    and (select private.has_admin_permission('live.manage'))
  )
  or (
    key = 'services'
    and (select private.has_admin_permission('content.services.edit'))
  )
  or (
    key = 'givingUrl'
    and (select private.has_admin_permission('content.links.edit'))
  )
  or (
    key not in ('churchName','email','phone','liveStream','services','givingUrl')
    and (select private.has_admin_permission('site.edit'))
  )
)
with check (
  (
    key = any (array['churchName','email','phone'])
    and (select private.is_admin_super_admin())
  )
  or (
    key = 'liveStream'
    and (select private.has_admin_permission('live.manage'))
  )
  or (
    key = 'services'
    and (select private.has_admin_permission('content.services.edit'))
  )
  or (
    key = 'givingUrl'
    and (select private.has_admin_permission('content.links.edit'))
  )
  or (
    key not in ('churchName','email','phone','liveStream','services','givingUrl')
    and (select private.has_admin_permission('site.edit'))
  )
);

create policy site_content_admin_delete
on public.site_content
for delete
to authenticated
using (
  (
    key = any (array['churchName','email','phone'])
    and (select private.is_admin_super_admin())
  )
  or (
    key = 'liveStream'
    and (select private.has_admin_permission('live.manage'))
  )
  or (
    key = 'services'
    and (select private.has_admin_permission('content.services.edit'))
  )
  or (
    key = 'givingUrl'
    and (select private.has_admin_permission('content.links.edit'))
  )
  or (
    key not in ('churchName','email','phone','liveStream','services','givingUrl')
    and (select private.has_admin_permission('site.edit'))
  )
);
