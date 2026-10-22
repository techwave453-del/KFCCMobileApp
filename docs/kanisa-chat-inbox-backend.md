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

The migration is additive and does not alter policies on `chat_rooms` or `chat_messages`.

## Android data access

`DirectChatRepository.kt` provides initial typed access for direct conversation lists, messages, send operations, contacts, and active story metadata. It is not yet wired into the Compose chat screen.

## Rollout checklist

1. Review the SQL against the target Supabase project's current grants, extensions, and profile schema.
2. Apply the migration in a non-production/test Supabase project first.
3. Test cross-user isolation with two ordinary member accounts and one administrator account.
4. Implement member/profile search using an explicitly approved discovery policy; do not expose email addresses or private profile fields.
5. Wire the inbox UI, private chat screen, new-contact flow, and story viewer/composer.
6. Add offline queue/retry and realtime subscriptions for direct messages.
7. Add tests for RLS, conversation creation races, duplicate contacts, story expiry, and storage ownership.
8. Build and run the Android test suite before merging.

## Important limitations

- This migration has not been applied to Supabase from this branch.
- The Android app has not been built in this environment.
- Story upload and signed-media URL handling are not implemented yet.
- No demo contacts or stories are seeded.
