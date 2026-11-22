# Green Stage — Screens and Navigation

## 1. Navigation Model

Green Stage uses one Firebase Authentication session and two separate
Navigation Compose graphs:

1. **Student Experience**
2. **Admin Experience**

The authenticated user's Firestore profile determines the experience:

```text
users/{userId}.accountType
        |
        +--> student --> Student Navigation
        |
        +--> admin   --> Admin Navigation
```

DataStore may store a convenience preference, but startup always validates
`accountType` from Firestore. A missing, invalid, or unsupported profile opens
a safe recovery/error state instead of a protected graph.

## 2. Authentication Screens

### Login

Collect email and password, authenticate with Firebase, load `users/{uid}`, and
route by the trusted `accountType`:

| Scenario | Destination |
|---|---|
| Existing student logs in | Student Experience |
| Existing admin logs in | Admin Experience |
| Missing or invalid profile | Recovery/error state |
| User logs out | Authentication screens |

### Registration

Registration offers two paths:

```text
Register
  |
  +--> Student
  |       +--> Firebase Auth account
  |       +--> users/{uid}: accountType = student
  |       +--> Student Experience
  |
  +--> Community Admin
          +--> Firebase Auth account
          +--> users/{uid}: accountType = admin
          +--> communities/{communityId}: createdBy = uid
          +--> Admin Experience
```

Admin registration creates the profile and community in one Firestore batch or
transaction after Auth succeeds. If Firestore
writes fail after Auth succeeds, show recovery and do not open Admin
Experience until the required documents are valid.

### Forgot Password

Collect an email address and use Firebase Authentication's password reset
operation. Show an application-level success or failure state.

## 3. Student Experience

The Student Experience has exactly four bottom navigation destinations:

| Order | Destination | Responsibility |
|---:|---|---|
| 1 | Home | Relevant and upcoming published events |
| 2 | Explore Events | Published events from all communities |
| 3 | Saved Events | The user's bookmarked events |
| 4 | Profile | Account details and interests |

### Home

Show active and upcoming published events, recommendations when available,
category shortcuts, and event cards from different communities.

### Explore Events

Query events with `isPublished == true`, then apply initial search and
category filtering locally. Firestore Security Rules are not query filters, so
the query must request only published events.

### Event Details

This nested screen shows title, description, image, category, date, time,
venue, and the community resolved from `communityId`. It provides bookmark and
registration actions. Registration and bookmark documents use deterministic
event IDs.

Event status is:

- **Upcoming:** `startAt > currentTime`
- **Active:** `startAt <= currentTime` and `endAt > currentTime`
- **Completed:** `endAt <= currentTime`

### Saved Events

Load the current user's bookmarks, show related events, support removal, and
include an empty state.

### Profile

Support viewing account information and editing permitted interests/profile
fields. Display the account type but do not allow it to be edited. Admins can view their owned community information. Students do not need a
community relationship to discover or register for events.

## 4. Admin Experience

The Admin Experience has exactly four bottom navigation destinations:

| Order | Destination | Responsibility |
|---:|---|---|
| 1 | Dashboard | Statistics and active/upcoming events |
| 2 | Create Event | Create and publish community events |
| 3 | My Events | List and manage owned-community events |
| 4 | Profile | Admin account and community information |

There are no Manage Members, Community Members, Join Community, Member
Requests, or Membership Management screens.

### Dashboard

Show the owned community, active and upcoming events, basic event counts, and
registration counts where available. Selecting an event opens a nested details
or management screen. The dashboard query must constrain `communityId` to the
admin's owned community.

### Create Event

Collect title, description, category, venue, start/end time, optional image
URL, and publish/save state. The repository writes a top-level event with the
admin's `communityId`, authenticated `createdBy`, timestamps, and
`isPublished`.

### My Events

List events where `communityId` equals the admin's owned community. Admins can
open nested event details, edit, publish, unpublish, or delete events where
allowed. Ownership is verified through `communities.createdBy`, not only the
event's `createdBy`.

### Profile

Show account information, account type, owned community information, and
logout. Multiple-community selection is not part of the initial scope.

## 5. Suggested Routes

### Authentication

| Route | Screen |
|---|---|
| `login` | Login |
| `register` | Registration choice |
| `register/student` | Student registration |
| `register/admin` | Community Admin registration |
| `forgot-password` | Password recovery |

### Student Graph

| Route | Screen |
|---|---|
| `student/home` | Home |
| `student/explore` | Explore Events |
| `student/saved` | Saved Events |
| `student/profile` | Profile |
| `student/event/{eventId}` | Event Details |

### Admin Graph

| Route | Screen |
|---|---|
| `admin/dashboard` | Dashboard |
| `admin/create-event` | Create Event |
| `admin/my-events` | My Events |
| `admin/profile` | Profile |
| `admin/event/{eventId}` | Nested event details |
| `admin/event/{eventId}/edit` | Nested event editing |

Pass identifiers such as `eventId`; do not pass complete Firestore model
objects through navigation.

## 6. Compose Navigation Structure

```text
navigation/
    AppNavigation.kt
    AuthNavigation.kt
    StudentNavigation.kt
    AdminNavigation.kt
    Routes.kt
```

- `AppNavigation` observes authentication and profile state.
- `AuthNavigation` handles login, registration, and password recovery.
- `StudentNavigation` exposes exactly the four student destinations.
- `AdminNavigation` exposes exactly the four admin destinations.

ViewModels receive repository interfaces through constructors. Firebase access
stays in data-layer implementations. Do not introduce Hilt, Dagger, Koin,
AppContainer, a UseCase layer, or another dependency-injection container.

## 7. Initial Scope

Included:

- Student and Community Admin registration.
- Login, logout, and password recovery.
- Student discovery, details, registration, bookmarks, saved events, profile,
  and interests.
- Admin dashboard, event creation, My Events, event editing, publishing,
  unpublishing, deletion where appropriate, and profile.

Deferred and excluded:

- Role transfers.
- Student-created communities.
- Multiple communities per admin account.
