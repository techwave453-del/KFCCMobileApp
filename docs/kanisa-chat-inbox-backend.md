# Kanisa chat inbox backend foundation

This branch adds the first backend slice for the planned inbox, direct messaging, contacts, and Stories experience. It does not replace the existing community/group chat tables.

## New schema

- `chat_direct_conversations`: one canonical private conversation per pair of Auth users; participant columns are ordered and unique.
- `chat_direct_messages`: private messages; RLS permits reads only to participants and inserts only by the authenticated sender.
- `chat_direct_read_receipts`: per-user read timestamps, isolated from conversation participant fields.
- `chat_contacts`: each user's saved contacts.
- `chat_stories` and `chat_story_views`: story metadata, 24-hour default expiry, and view records.
- Private Storage bucket `kanisa-story-media`: objects must live under a folder named for the authenticated user's UUID.

## RPCs

- `start_direct_conversation(p_other_user_id uuid)`: validates the target profile and returns the existing or newly created conversation ID.
- `add_chat_contact(p_contact_user_id uuid)`: validates the target profile and saves the contact idempotently.
- `search_chat_members(p_query text)`: returns at most 25 matching member identities (username, display name, avatar URL); requires a two-character query and excludes email/admin metadata.
- `get_direct_chat_inbox()`: returns the authenticated user's conversations with the other participant's minimal public chat identity.
- `get_active_chat_stories()`: returns active story metadata with the owner's minimal chat identity.

The migration is additive and does not alter policies on `chat_rooms` or `chat_messages`. Private message updates and story-view updates include participant/owner checks.

## Android UI/data access

- `DirectChatRepository.kt` provides typed inbox, member search, private message, contact, and story-feed calls.
- `DirectChatInboxScreen.kt` adds the inbox landing screen, member avatars, Stories strip, private message thread, and `+ New Chat` menu with New Chat, New Contact, and New Community.
- `ChatScreen.kt` routes signed-in users to the inbox by default and retains the existing community chat and Kanisa Assistant.

## Rollout checklist

1. Review the SQL against the target Supabase project's current grants, extensions, and `chat_profiles` schema.
2. Apply the migration in a non-production/test Supabase project first.
3. Test cross-user isolation with two ordinary member accounts and one administrator account.
4. Compile and run the Android app; verify the installed Supabase Kotlin SDK response-decoding APIs.
5. Implement private story-media upload and signed URL retrieval; the current story UI only shows the story identity/caption and explicitly does not render private media.
6. Add direct-message realtime subscriptions, read-state/unread counts, and offline queue/retry.
7. Add tests for RLS, conversation creation races, duplicate contacts, story expiry, and storage ownership.
8. Run the Android test suite and verify migration execution before merging.

## Important limitations

- This migration has not been applied to Supabase from this branch.
- The Android app has not been built in this environment.
- This remains an initial UI/data foundation, not a verified production release.
- Story upload and signed-media URL handling are not implemented; the Stories strip uses real metadata but does not display the private story media.
- Direct messages currently reload after send but do not yet subscribe to realtime changes or support offline queuing.
- Unread counts/read receipts are not wired to the UI yet.
- No demo contacts or stories are seeded.
