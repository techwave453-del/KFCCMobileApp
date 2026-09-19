# Enhanced Services, Giving, and Events Implementation Plan

Upgrade the user-facing Services, Giving, and Events screens and extend the Admin Panel with management capabilities for church services and giving configurations.

## User Review Required

> [!IMPORTANT]
> - **Navigation Update**: We will add a dedicated "Services" destination to the main app navigation.
> - **Admin Modules**: A new "Services & Giving" administration module will be added to allow admins to manage worship times and online giving links without code changes.

## Proposed Changes

### Component 1: Mobile App UI Enhancements
- **[MODIFY] [MainActivity.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/MainActivity.kt)**: Add `AppDestinations.SERVICES` and wire the `ServicesScreen` with real data from `ChurchViewModel`.
- **[MODIFY] [ServicesScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/screens/ServicesScreen.kt)**: Enhance the visual layout to match the app's modern theme.
- **[MODIFY] [GivingScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/screens/GivingScreen.kt)**: Add a more interactive giving experience with category selection and dynamic URL handling from `ChurchInfo`.
- **[MODIFY] [HomeScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/screens/HomeScreen.kt)**: Update the "Services" Quick Access card to navigate to the dedicated screen.

### Component 2: Admin Panel Expansion
- **[NEW] AdminServicesScreen.kt**: Create a management interface for adding, editing, and removing church service records (Title, Time, Image).
- **[MODIFY] [WebsiteContentScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/admin/content/WebsiteContentScreen.kt)**: Add a field for the primary "Giving Link" used in the Giving screen.
- **[MODIFY] [AdminShell.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/admin/AdminShell.kt)**: Integrate the new Services management module into the dashboard and permission checks.

## Verification Plan

### Automated Tests
- Build verification via `./gradlew :app:compileDebugKotlin`.

### Manual Verification
- **Admin Side**: Log in as admin, add a new service record, save, and verify it appears in the public Services screen.
- **User Side**: Navigate to Services and Giving pages, verify layout and "Give" button functionality.
