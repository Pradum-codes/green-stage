# Green Stage — System Architecture

## 1. Overview

Green Stage is an Android application for event discovery and community event
management. It has exactly two account types and experiences:

1. **Student:** Discover published events, register, bookmark, and manage
   interests.
2. **Community Admin:** Own one community and manage its events.

Multiple-community administration is outside the initial implementation.

The application uses:

- Kotlin and Jetpack Compose
- Material 3
- MVVM and the Repository Pattern
- Navigation Compose
- Firebase Authentication
- Cloud Firestore
- Kotlin Coroutines and Flow
- DataStore for convenience preferences
- Manual object creation

There is no custom backend, UseCase layer, separate domain module, AppContainer,
Hilt, Dagger, or Koin.

## 2. Layered Architecture

```text
Compose Screens
        |
        v
ViewModels
        |
        v
Repository Interfaces
        |
        +-----------------------+
        |                       |
        v                       v
Mock Implementations     Firebase Implementations
                                |
                    +-----------+-----------+
                    |                       |
                    v                       v
          Firebase Authentication   Cloud Firestore
```

Screens and ViewModels never access Firebase SDKs directly. Mock and Firebase
implementations follow the same interfaces so UI development can proceed
independently of Firebase configuration.

## 3. Responsibilities

### Presentation

Compose screens render state, collect input, and navigate within the Student
or Admin graph. They display loading, success, empty, and error states.

### Application logic

ViewModels coordinate authentication, registration, event discovery, event
management, registrations, bookmarks, and recommendations. They obtain data
only through repository interfaces.

### Data

Repositories map application models to Firebase documents, perform validation,
observe data with `Flow`, and convert Firebase failures into `Result<T>`.
Firestore Security Rules independently enforce authorization.

## 4. Account and Ownership Model

Account type is stored in `users/{userId}`:

```text
accountType: "student" | "admin"
```

An email address can belong to only one Firebase Authentication account in the same Firebase project. The account type is selected during registration and remains immutable.

An admin owns the community created during registration, with the community document ID equal to the admin's UID:

```text
users/{adminUid}
    |
    | communityId == adminUid (and communities.createdBy == adminUid)
    v
communities/{adminUid}
    |
    | events.communityId == adminUid
    v
events/{eventId}
```

The community document ID is the admin's Firebase Authentication UID (`communities/{adminUid}`), enabling direct lookup using the authenticated UID. `events.communityId` equals the owning admin's UID. Event authorization checks the authenticated admin against the community document; it never trusts only the event's `createdBy` audit field.

## 5. Registration and Startup Flow

```text
Application launch
        |
        v
Observe Firebase Authentication
        |
        +-------------------------+
        |                         |
        v                         v
Signed out                  Signed in
Login/Register               Load users/{uid}
                                  |
                                  v
                         Validate accountType & Community
                         |                |
                         v                v
                    Student           Admin (requires communities/{uid})
                 Student graph     Admin graph
```

Student registration creates Auth credentials and a `users/{uid}` profile with
`accountType = "student"`, then opens Student Experience. No community document is created.

Community Admin registration (public self-registration) creates Auth credentials, obtains the authenticated
UID, and writes the admin profile (`users/{uid}`) plus the community (`communities/{uid}`) with `createdBy = uid` in
one Firestore batch or transaction after Auth succeeds. It opens Admin
Experience only after both documents are valid. AuthRepository owns this
orchestration; CommunityRepository retrieves and updates the owned
community directly using `communities/{authenticatedUid}`.

If Auth succeeds but Firestore writes fail, show a recovery state, retry
missing writes safely, and do not route an incomplete admin account into Admin Experience. On startup, admin profiles must also have a valid `communities/{uid}` document before entering Admin Experience.

## 6. Navigation Experiences

Student Experience has exactly four bottom destinations:

1. Home
2. Explore Events
3. Saved Events
4. Profile

Admin Experience has exactly four bottom destinations:

1. Dashboard
2. Create Event
3. My Events
4. Profile

Event details and editing are nested screens, not bottom navigation
destinations. Logging out returns to authentication screens.

## 7. Event Flow and Status

```text
Admin Dashboard
    |
    +--> Select event --> Nested details/edit screen

Create Event
    |
    +--> events/{eventId}
            communityId = admin's UID (community ID)
            createdBy = authenticated admin UID

Student Explore Events
    |
    +--> Published events across communities
            +--> Details
            +--> Register
            +--> Bookmark
```

Event status is calculated consistently:

- Upcoming: `startAt > currentTime`
- Active: `startAt <= currentTime` and `endAt > currentTime`
- Completed: `endAt <= currentTime`

## 8. Configuration and Constraints

Manual object creation selects mock or Firebase repositories at an application
or navigation boundary. Firebase access remains in data-layer implementations.
Do not add unnecessary dependencies or architecture layers.

The recommendation engine remains local and notifications remain device-local.
Both mock and Firebase implementations must use the same models, method
signatures, result handling, account types, and ownership assumptions.
