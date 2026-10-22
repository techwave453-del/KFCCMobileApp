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
- `get_chat_member_suggestions()`: returns at most 25 member identities for the horizontal People row, excluding the signed-in user and all email/admin metadata.
- `get_direct_chat_inbox()`: returns the authenticated user's conversations with the other participant's minimal public chat identity.
- `get_active_chat_stories()`: returns active story metadata with the owner's minimal chat identity.
- `get_direct_chat_unread_counts()`: returns per-conversation unread message counts for the authenticated participant.
- `mark_direct_conversation_read(p_conversation_id uuid)`: upserts the caller's read timestamp after validating conversation membership.

The migration is additive and does not alter policies on `chat_rooms` or `chat_messages`. Private message updates and story-view updates include participant/owner checks.

## Android UI/data access

- `DirectChatRepository.kt` provides typed inbox, member suggestions/search, existing room loading, private message, contact, and story-feed calls.
- `DirectChatInboxScreen.kt` shows a horizontally scrolling People row, Stories strip with signed private image/video playback, unread badges, private conversations, and existing community/group rooms in one Messages list.
- Selecting a member starts or reuses a private conversation; selecting a room opens the existing community/group chat through `ChatViewModel`.
- `ChatScreen.kt` retains the existing community chat and Kanisa Assistant.

## Rollout checklist

1. Build and run the Android app; verify the People row, member-to-message flow, and existing group/community room navigation.
2. Test cross-user isolation with two ordinary member accounts and one administrator account.
3. Implement private story-media upload and signed URL retrieval; the current story UI does not yet render private media.
4. Add offline queue/retry. Direct messages subscribe to Supabase Realtime and retain bounded polling as a missed-event/reconnect fallback; unread counts and read-state use participant-checked RPCs.
5. Add tests for RLS, conversation creation races, duplicate contacts, story expiry, and storage ownership.

## Important limitations

- The direct-chat foundation and the member-suggestions RPC have been applied to the active KFCC Supabase project; deployment history and grants should still be rechecked before production rollout.
- The Android app has not been built in this environment.
- This remains an initial UI/data foundation, not a verified production release.
- Story viewing requests short-lived signed URLs and plays private images/videos. The Add story action uploads supported images/videos to the private bucket and then creates metadata under the authenticated user's UUID folder.
- Direct messages use Supabase Realtime insert events to refresh the open thread, with a 15-second polling fallback and 15-second inbox polling. The direct-message and conversation tables must remain in the `supabase_realtime` publication. Offline queuing is not implemented.
- Unread counts and read timestamps are wired to the direct-message inbox, but realtime refresh is not yet implemented.
- No demo contacts or stories are seeded.
