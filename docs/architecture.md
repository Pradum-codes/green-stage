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

An admin owns the community created during registration:

```text
users/{adminUid}
    |
    | communities.createdBy
    v
communities/{communityId}
    |
    | events.communityId
    v
events/{eventId}
```

Event authorization checks the authenticated admin against the community's
`createdBy`; it never trusts only the event's `createdBy` field.

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
                         Validate accountType
                         |                |
                         v                v
                    Student           Admin
                 Student graph     Admin graph
```

Student registration creates Auth credentials and a `users/{uid}` profile with
`accountType = "student"`, then opens Student Experience.

Community Admin registration creates Auth credentials, obtains the authenticated
UID, and writes the admin profile plus the community with `createdBy = uid` in
one Firestore batch or transaction after Auth succeeds. It opens Admin
Experience only after both documents are valid. AuthRepository owns this
orchestration; CommunityRepository only retrieves and updates the owned
community.

If Auth succeeds but Firestore writes fail, show a recovery state, retry
missing writes safely, and do not route into an unauthorized experience.

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
            communityId = admin's community
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
