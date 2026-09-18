# Redesign Chat and Profile Screens

This plan addresses the issue where the Chat and Profile screens are not accessible even after an administrator sign-in, and provides a polished design for both screens.

## User Review Required

> [!IMPORTANT]
> I am assuming that administrators should be able to access the Chat and Profile screens using their administrator identity, even if they haven't explicitly created a separate "Community Member" account. However, participating in the chat (sending messages) currently requires a Supabase account. I will design the UI to show a "View Only" or "Join Community" prompt for admins who are not yet signed into the community chat.

## Proposed Changes

### UI & UX Redesign

#### [MODIFY] [ChatScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/screens/ChatScreen.kt)
- Update the access logic to show the chat interface if either a member OR an administrator is signed in.
- Enhance the `CommunityChat` UI:
    - Add date separators between messages.
    - Improve message bubbles with better contrast and padding.
    - Add a "Signed in as [User]" indicator at the top of the chat list.
    - Add a specialized UI state for administrators who are in "view-only" mode (missing Supabase session).

#### [MODIFY] [ProfileScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/screens/ProfileScreen.kt)
- Completely redesign the profile screen with a modern, card-based layout.
- Integrate `AdminViewModel` to display administrator-specific information (Role, Permissions).
- Display member-specific information (Email, Chat Username) if available.
- Add a "Manage Security" section for administrators.
- Provide a clear, unified logout button.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/MainActivity.kt)
- Update the navigation destination mapping for `AppDestinations.ACCOUNT` and `AppDestinations.PROFILE` to ensure consistency.
- Ensure `ProfileScreen` and `ChatScreen` receive all required ViewModels.

## Verification Plan

### Manual Verification
1.  Sign in as an **Administrator** and verify that the Profile button in the bottom nav takes you to a well-designed Profile screen showing your admin role and permissions.
2.  Click the **Chat** button and verify that the chat interface is shown instead of the login screen.
3.  Sign in as a **Member** and verify the same for the member profile and chat functionality.
4.  Verify that clicking **Sign Out** from either screen correctly clears all sessions and returns to the home/login state.
