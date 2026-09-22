# Fix Real-time Chat, Keyboard Padding, and Email Updates

This plan addresses the broken real-time updates in chat, the excessive gap between the keyboard and typing area, and adds the ability for all users to update their email addresses.

## User Review Required

> [!IMPORTANT]
> Updating an email address in Supabase usually triggers a confirmation email to both the old and new addresses (depending on your Supabase settings). Users will need to verify the change before it becomes active for password recovery.

## Proposed Changes

### Core Logic & Repositories

#### [MODIFY] [ChatRepository.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/data/ChatRepository.kt)
- Update `observeMessages` to explicitly call `channel.subscribe()`. This ensures the Realtime flow actually starts receiving events from the server.

#### [MODIFY] [ChatAuthRepository.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/data/ChatAuthRepository.kt)
- Add `updateEmail(newEmail: String)` method using `auth.updateUser`. This will allow users and admins to change their primary contact and recovery email.

### UI & UX Improvements

#### [MODIFY] [ChatScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/screens/ChatScreen.kt)
- **Fix Keyboard Gap**:
    - Remove `padding(innerPadding)` from the main `Column`.
    - Instead, apply `windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))` to the header.
    - Apply `windowInsetsPadding(WindowInsets.ime.add(WindowInsets.navigationBars))` or use `Scaffold` correctly to ensure the input area sits exactly on top of the keyboard without a gap.
- **Refine Input Area**: Adjust the bottom container to handle both the navigation bar height and the keyboard height dynamically.

#### [MODIFY] [ProfileScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/screens/ProfileScreen.kt)
- **Email Update UI**:
    - Add an "Account Email" section with an editable text field.
    - Add a "Update Email" button that triggers the repository call.
    - Provide clear feedback that a confirmation email might be sent.

## Verification Plan

### Manual Verification
1.  **Real-time Test**: Open the chat on two devices (or use the Supabase dashboard to insert a message). Verify the message appears instantly without logging out.
2.  **Keyboard Gap Test**: Open the keyboard in the chat. Verify the "Write a message" box sits flush against the top of the keyboard.
3.  **Email Update Test**: Change the email in the Profile screen. Verify the request is sent and the UI shows a success/confirmation message.
