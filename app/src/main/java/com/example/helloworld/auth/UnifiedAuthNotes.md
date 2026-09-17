# Unified authentication integration

The shared sign-in screen must route administrator credentials through the existing admin API and member credentials through the member authentication backend. The member backend currently accepts email/password, so username-only member login requires a server-side username resolver before it can be enabled safely.

Do not display raw Supabase/Ktor exceptions, request URLs, authorization headers, or API keys in the UI. Map failures to a generic authentication message.
