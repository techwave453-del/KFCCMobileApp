# Implementation Plan: Church Mobile App (Native Compose)

Convert the "Kingdom Fellowship Christian Church" website into a native Android application using Jetpack Compose Material 3.

## User Review Required

> [!IMPORTANT]
> This plan focuses on building the **UI screens** based on the existing website content.
> To fetch live data (like upcoming events or testimonies), we will eventually need to integrate the Supabase SDK or a REST API. For this initial phase, we will use the data from `site-config.js` as local defaults.

## Proposed Changes

### Data Layer

#### [NEW] [ChurchContent.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/data/ChurchContent.kt)
Define data models matching the website's structure (Service, Link, ChurchInfo).

### UI Components

#### [NEW] [ChurchComponents.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/components/ChurchComponents.kt)
Create reusable Compose components for:
- Hero Header
- Service Cards
- Info/Link Cards
- Membership Class Tiles

### Screens

#### [NEW] [HomeScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/screens/HomeScreen.kt)
The main landing page showing the Hero, About, and Services sections.

#### [NEW] [EventsScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/screens/EventsScreen.kt)
A screen dedicated to Upcoming Programs and Membership Classes.

#### [NEW] [MediaScreen.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/ui/screens/MediaScreen.kt)
A screen for Testimonies, Resources, and the Live Stream link.

### Main Activity & Navigation

#### [MODIFY] [MainActivity.kt](file:///C:/Users/DENNIE/AndroidStudioProjects/HelloWorld/app/src/main/java/com/example/helloworld/MainActivity.kt)
Update the `NavigationSuiteScaffold` to switch between the new screens.

## Verification Plan

### Automated Tests
- Build the project to ensure all new components compile.
- Run `render_compose_preview` on `HomeScreen` to verify the layout.

### Manual Verification
- Deploy the app to the emulator/device.
- Verify navigation between Home, Events, and Media.
- Check that the UI styling (accent colors) matches the website's theme.
